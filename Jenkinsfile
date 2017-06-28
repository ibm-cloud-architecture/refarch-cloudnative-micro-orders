podTemplate(
    label: 'pod',
    containers: [
        containerTemplate(
            name: 'java',
            image: 'openjdk:8-jdk-alpine',
            alwaysPullImage: true,
            ttyEnabled: true,
            command: 'cat'
        ),
        containerTemplate(
            name: 'docker',
            image: 'ibmcase/bluemix-image-deploy:latest',
            alwaysPullImage: true,
            ttyEnabled: true
        ),
        containerTemplate(
            name: 'helm',
            image: 'ibmcase/helm:latest',
            alwaysPullImage: true,
            ttyEnabled: true,
            command: 'cat'
        )
    ],
    volumes: [
      hostPathVolume(
          hostPath: '/var/run/docker.sock',
          mountPath: '/var/run/docker.sock'
      ),
      secretVolume(
          secretName: 'bluemix-api-key',
          mountPath: '/var/run/secrets/bluemix-api-key'
      ),
      configMapVolume(
          configMapName: 'bluemix-target',
          mountPath: '/var/run/configs/bluemix-target'
      )
    ],
) {
    node ('pod') {
        try {
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

            container('docker') {
                stage ('Build Docker Image') {
                    sh """
                    #!/bin/bash
                    BX_REGISTRY=`cat /var/run/configs/bluemix-target/bluemix-registry`
                    BX_CR_NAMESPACE=`cat /var/run/configs/bluemix-target/bluemix-registry-namespace`

                    cd docker
                    docker build -t \${BX_REGISTRY}/\${BX_CR_NAMESPACE}/bluecompute-orders:${env.BUILD_NUMBER} .
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
                    bx login -a \${BX_API_ENDPOINT} -s \${BX_SPACE}

                    # initialize docker using container registry secret
                    bx cr login

                    docker push \${BX_REGISTRY}/\${BX_CR_NAMESPACE}/bluecompute-orders:${env.BUILD_NUMBER}

                    """
                }
            }
            container('helm') {
                stage ('Install Chart') {
                    sh """
                    #!/bin/bash
                    BX_REGISTRY=`cat /var/run/configs/bluemix-target/bluemix-registry`
                    BX_CR_NAMESPACE=`cat /var/run/configs/bluemix-target/bluemix-registry-namespace`
                    BX_ORG=`cat /var/run/configs/bluemix-target/bluemix-org`
                    BX_SPACE=`cat /var/run/configs/bluemix-target/bluemix-space`
                    CLUSTER_NAME=`cat /var/run/configs/bluemix-target/kube-cluster-name`
                    BX_API_KEY=`cat /var/run/secrets/bluemix-api-key/api-key`

                    # Init helm
                    helm init

                    # Install/Upgrade Chart
                    cd chart/orders

                    release=`helm list | grep orders | awk '{print \$1}' | head -1`

                    if [[ -z "\${release// }" ]]; then
                        echo "Installing bluecompute-orders chart for the first time"
                        time helm install \
                            --name orders \
                            . \
                            --debug \
                            --wait \
                            --timeout 600 \
                            --set image.tag=${env.BUILD_NUMBER} \
                            --set image.repository=\${BX_REGISTRY}/\${BX_CR_NAMESPACE}/bluecompute-orders \
                            --set hs256key.skipDelete=true \
                            --set mysql.skipDelete=true \
                            --set messagehub.skipDelete=true \
                            --set configMap.skipDelete=true \
                            --set secret.skipDelete=true \
                            --set configMap.bluemixOrg=\${BX_ORG} \
                            --set configMap.bluemixSpace=\${BX_SPACE} \
                            --set configMap.bluemixRegistryNamespace=\${BX_CR_NAMESPACE} \
                            --set configMap.kubeClusterName=\${CLUSTER_NAME} \
                            --set secret.apiKey=\${BX_API_KEY}

                    else
                        echo "Upgrading bluecompute-orders chart release"
                        time helm upgrade orders . \
                            --reuse-values \
                            --debug \
                            --wait \
                            --timeout 600 \
                            --set image.tag=${env.BUILD_NUMBER} \
                            --set image.repository=\${BX_REGISTRY}/\${BX_CR_NAMESPACE}/bluecompute-orders \
                            --set hs256key.skipDelete=true \
                            --set mysql.skipDelete=true \
                            --set messagehub.skipDelete=true \
                            --set configMap.skipDelete=true \
                            --set secret.skipDelete=true \
                            --set configMap.bluemixOrg=\${BX_ORG} \
                            --set configMap.bluemixSpace=\${BX_SPACE} \
                            --set configMap.bluemixRegistryNamespace=\${BX_CR_NAMESPACE} \
                            --set configMap.kubeClusterName=\${CLUSTER_NAME} \
                            --set secret.apiKey=\${BX_API_KEY}
                    fi
                    """
                }
            }
        } catch (exception) {
            container('docker') {
                stage ('Cleanup Bad Image') {
                    sh """
                    #!/bin/bash
                    set -x

                    export BLUEMIX_API_KEY=`cat /var/run/secrets/bluemix-api-key/api-key`
                    BX_SPACE=`cat /var/run/configs/bluemix-target/bluemix-space`
                    BX_API_ENDPOINT=`cat /var/run/configs/bluemix-target/bluemix-api-endpoint`
                    BX_REGISTRY=`cat /var/run/configs/bluemix-target/bluemix-registry`
                    BX_CR_NAMESPACE=`cat /var/run/configs/bluemix-target/bluemix-registry-namespace`

                    # Bluemix Login
                    bx login -a \${BX_API_ENDPOINT} -s \${BX_SPACE}

                    # initialize docker using container registry secret
                    bx cr login
                    bx cr image-rm \${BX_REGISTRY}/\${BX_CR_NAMESPACE}/bluecompute-orders:${env.BUILD_NUMBER}
                    """
                }
            }

        }
        container('docker') {
            stage ('Cleanup Old Images') {
                sh """
                #!/bin/bash
                set -x

                export BLUEMIX_API_KEY=`cat /var/run/secrets/bluemix-api-key/api-key`
                BX_SPACE=`cat /var/run/configs/bluemix-target/bluemix-space`
                BX_API_ENDPOINT=`cat /var/run/configs/bluemix-target/bluemix-api-endpoint`
                BX_REGISTRY=`cat /var/run/configs/bluemix-target/bluemix-registry`
                BX_CR_NAMESPACE=`cat /var/run/configs/bluemix-target/bluemix-registry-namespace`

                # Bluemix Login
                bx login -a \${BX_API_ENDPOINT} -s \${BX_SPACE}

                # initialize docker using container registry secret
                bx cr login

                # find out which images to delete -- keep last N images in the registry
                MAX_IMAGES_TO_KEEP=3
                set +e
                all_images=`bx cr image-list -q | grep \${BX_REGISTRY} | grep \${BX_CR_NAMESPACE} | grep bluecompute-orders`
                [ -z \$all_images ] && exit 0
                all_image_tags=`echo "\${all_images}" | awk -F: '{print \$2;}' | sort -n`
                total_num_images=`echo "\${all_images}" | wc -l | awk '{print \$1;}'`

                if [ \${total_num_images} -le \${MAX_IMAGES_TO_KEEP} ]; then
                    exit 0
                fi

                num_to_delete=\$((total_num_images - MAX_IMAGES_TO_KEEP))
                images_to_delete=`echo "\${all_image_tags}" | sort -n | head -n \${num_to_delete}`

                for i in \${images_to_delete}; do
                    # delete images smaller than current build
                    echo "Deleting \${BX_REGISTRY}/\${BX_CR_NAMESPACE}/bluecompute-orders:\${i} ..."
                    bx cr image-rm \${BX_REGISTRY}/\${BX_CR_NAMESPACE}/bluecompute-orders:\${i}
                done
                """
            }
        }
    }
}
