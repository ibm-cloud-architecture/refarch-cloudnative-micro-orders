/*
    To learn how to use this sample pipeline, follow the guide below and enter the
    corresponding values for your environment and for this repository:
    - https://github.com/ibm-cloud-architecture/refarch-cloudnative-devops-kubernetes
*/

// Pod Template
def podLabel = "orders"
def cloud = env.CLOUD ?: "kubernetes"
def registryCredsID = env.REGISTRY_CREDENTIALS ?: "registry-credentials-id"
def serviceAccount = env.SERVICE_ACCOUNT ?: "jenkins"

// Pod Environment Variables
def registry = env.REGISTRY ?: "docker.io"
def imageName = env.IMAGE_NAME ?: "ibmcase/bluecompute-orders"

podTemplate(label: podLabel, cloud: cloud, serviceAccount: serviceAccount, envVars: [
        envVar(key: 'REGISTRY', value: registry),
        envVar(key: 'IMAGE_NAME', value: imageName),
    ],
    volumes: [
        emptyDirVolume(mountPath: '/home/gradle/.gradle'),
    ],
    containers: [
        containerTemplate(name: 'jdk', image: 'ibmcase/openjdk-bash:alpine', ttyEnabled: true, command: 'cat'),
        containerTemplate(name: 'makisu', image: 'jkwong/makisu-alpine:v0.1.11', ttyEnabled: true, command: 'cat')
  ]) {
    node(podLabel) {
        checkout scm

        stage('Local - Build and Unit Test') {
            container(name:'jdk', shell:'/bin/bash') {
                sh """
                #!/bin/bash
                ./gradlew build
                """
            }
        }

        stage('Docker - Build Image') {
                container(name:'makisu', shell:'/bin/sh') {
                    withCredentials([usernamePassword(credentialsId: registryCredsID,
                                    usernameVariable: 'USERNAME',
                                    passwordVariable: 'PASSWORD')]) {
                            sh """
                            #!/bin/sh
                            echo "{'${REGISTRY}':{'.*':{'security':{'basic':{'password':'${PASSWORD}','username':'${USERNAME}'},'tls':{'client':{'disabled':true}}}}}}" | tee registry-config.yaml
                            /makisu-internal/makisu build --modifyfs --registry-config=registry-config.yaml --push ${REGISTRY} -t ${REGISTRY}/${IMAGE_NAME}:${env.BUILD_NUMBER} `pwd`
                            """
                        }
                    }
            }
        }
    }
