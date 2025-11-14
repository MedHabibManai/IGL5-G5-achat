// BOM Fix
// jenkins/stages/pushFrontendDocker.groovy
def call() {
    stage('Push Frontend Docker Image') {
        when {
            expression { return fileExists('frontend/Dockerfile') }
        }
        steps {
            script {
                echo '========================================='
                echo 'Stage 12: Pushing Frontend Image to Docker Hub'
                echo '========================================='
            }
            
            script {
                // Push to Docker Hub
                withCredentials([usernamePassword(
                    credentialsId: "${DOCKER_CREDENTIAL_ID}",
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    // Infinite retry for Docker login with TLS timeout handling
                    def loginSuccess = false
                    
                    while (!loginSuccess) {
                        try {
                            echo "Attempting Docker login..."
                            sh '''
                                while true; do
                                    if echo "${DOCKER_PASS}" | docker login -u "${DOCKER_USER}" --password-stdin $DOCKER_REGISTRY; then
                                        echo "Docker login successful!"
                                        break
                                    else
                                        echo "Docker login failed, retrying in 15 seconds..."
                                        sleep 15
                                    fi
                                done
                            '''
                            loginSuccess = true
                        } catch (Exception e) {
                            echo "Docker login error: ${e.message}. Retrying in 15 seconds..."
                            sleep(15)
                        }
                    }
                    
                    // Infinite retry for Docker push with TLS timeout handling
                    def pushSuccess = false
                    
                    while (!pushSuccess) {
                        try {
                            echo "Attempting to push frontend images..."
                            sh '''
                                while true; do
                                    if docker push $DOCKER_REGISTRY/habibmanai/achat-frontend:$BUILD_NUMBER; then
                                        echo "Build number tag pushed successfully!"
                                        break
                                    else
                                        echo "Push failed, retrying in 15 seconds..."
                                        sleep 15
                                    fi
                                done
                                
                                while true; do
                                    if docker push $DOCKER_REGISTRY/habibmanai/achat-frontend:latest; then
                                        echo "Latest tag pushed successfully!"
                                        break
                                    else
                                        echo "Push failed, retrying in 15 seconds..."
                                        sleep 15
                                    fi
                                done
                            '''
                            pushSuccess = true
                            echo "Frontend Docker images pushed successfully!"
                        } catch (Exception e) {
                            echo "Docker push error: ${e.message}. Retrying in 15 seconds..."
                            sleep(15)
                        }
                    }
                }
            }
            
            script {
                echo 'Frontend image pushed to Docker Hub'
                echo "Image: ${DOCKER_REGISTRY}/habibmanai/achat-frontend:${BUILD_NUMBER}"
            }
        }
    }
}
return this
