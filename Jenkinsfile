podTemplate(label: 'pod',
    volumes: [hostPathVolume(hostPath: '/var/run/docker.sock', mountPath: '/var/run/docker.sock'),
              secretVolume(secretName: 'bluemix-api-key', mountPath: '/var/run/secrets/bluemix-api-key'),
              configMapVolume(configMapName: 'bluemix-target', mountPath: '/var/run/configs/bluemix-target')],
    containers: [
        containerTemplate(name: 'java', image: 'openjdk:8-jdk-alpine', alwaysPullImage: true, ttyEnabled: true, command: 'cat'),
        containerTemplate(name: 'docker', image: 'ibmcase/bluemix-image-deploy:latest', alwaysPullImage: true, ttyEnabled: true),
        containerTemplate(name: 'helm', image: 'ibmcase/helm:latest', alwaysPullImage: true, ttyEnabled: true, command: 'cat'),
        containerTemplate(name: 'kubectl', image: 'ibmcase/kubectl:latest', alwaysPullImage: true, ttyEnabled: true, command: 'cat')
    ]) {

    node ('pod') {

        stage('Build') {
            checkout scm
            container('java') {
                stage('Build Gradle Project') {
                    sh """
                    #!/bin/bash
                    ./gradlew build -x test
                    ./gradlew docker
                    """
                }
            }
        }

        stage('Docker') {
            container('docker') {
                stage ('Build Docker Image') {
                    sh """
                    #!/bin/bash
                    BX_REGISTRY=`cat /var/run/configs/bluemix-target/bluemix-registry`
                    BX_CR_NAMESPACE=`cat /var/run/configs/bluemix-target/bluemix-registry-namespace`

                    cd docker
                    docker build -t \${BX_REGISTRY}/\${BX_NAMESPACE}/orders:${env.BUILD_NUMBER} .
                    """
                }
                stage ('Push Docker Image to Registry') {
                    sh """
                    #!/bin/bash
                    export BLUEMIX_API_KEY=`cat /var/run/secrets/bluemix-api-key/api-key`
                    BX_SPACE=`cat /var/run/configs/bluemix-target/bluemix-space`
                    BX_API_ENDPOINT=`cat /var/run/configs/bluemix-target/bluemix-api-endpoint`
                    BX_REGISTRY=`cat /var/run/configs/bluemix-target/bluemix-registry`
                    BX_CR_NAMESPACE=`cat /var/run/configs/bluemix-target/bluemix-registry-namespace`

                    # Bluemix Login
                    # source /login.sh
                    bx login -a \${BX_API_ENDPOINT} -s \${BX_SPACE}

                    # initialize docker using container registry secret
                    bx cr login

                    docker push \${BX_REGISTRY}/\${BX_CR_NAMESPACE}/orders:${env.BUILD_NUMBER}
                    """
                }
            }
        }

        stage('Chart') {
            container('helm') {
                stage ('Install Chart') {
                    sh """
                    #!/bin/bash
                    BX_REGISTRY=`cat /var/run/configs/bluemix-target/bluemix-registry`
                    BX_CR_NAMESPACE=`cat /var/run/configs/bluemix-target/bluemix-namespace`

                    cd scripts
                    ./deploy.sh ${env.BUILD_NUMBER} \${BX_REGISTRY} \$BX_CR_NAMESPACE
                    """
                }
            }
        }

        stage('Cleanup') {
            container('kubectl') {
                stage ('Cleanup Helm Install Jobs') {
                    sh """
                    #!/bin/bash
                    cd scripts
                    ./kubecleanup.sh
                    """
                }
            }
        }
    }
}
