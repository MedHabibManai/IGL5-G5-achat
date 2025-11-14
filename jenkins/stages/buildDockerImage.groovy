// BOM Fix
// jenkins/stages/buildDockerImage.groovy
def call() {
    stage('Build Docker Image') {
                echo '========================================='
                echo 'Stage 8: Building Docker Image'
                echo '========================================='

            // Build Docker image with retry logic for network issues
                echo "Building Docker image: ${DOCKER_IMAGE}"
                echo "JAR file: target/${PROJECT_NAME}-${PROJECT_VERSION}.jar"

                // Verify JAR exists
                sh "ls -lh target/${PROJECT_NAME}-${PROJECT_VERSION}.jar"

                // Pre-pull base image with retry logic to handle TLS timeouts
                def baseImage = 'eclipse-temurin:8-jre-alpine'
                def maxRetries = 3
                def pulled = false
                
                for (int i = 0; i < maxRetries && !pulled; i++) {
                    try {
                        if (i > 0) {
                            def delay = Math.pow(2.0, i as double) * 10
                            echo "Retry ${i + 1}/${maxRetries} - waiting ${delay}s before pulling base image..."
                            sleep(time: delay.toInteger(), unit: 'SECONDS')
                        }
                        
                        echo "Attempting to pull base image: ${baseImage}"
                        sh "docker pull ${baseImage}"
                        pulled = true
                        echo "Base image pulled successfully"
                    } catch (Exception e) {
                        if (i < maxRetries - 1) {
                            echo "Failed to pull base image: ${e.message}"
                        } else {
                            echo "Failed to pull base image after ${maxRetries} attempts, proceeding with build (may use cached image)"
                        }
                    }
                }

                // Build the image with retry logic
                def built = false
                
                for (int i = 0; i < maxRetries && !built; i++) {
                    try {
                        if (i > 0) {
                            def delay = Math.pow(2.0, i as double) * 10
                            echo "Retry ${i + 1}/${maxRetries} - waiting ${delay}s before building..."
                            sleep(time: delay.toInteger(), unit: 'SECONDS')
                        }
                        
                        sh """
                            docker build \
                              --build-arg JAR_FILE=target/${PROJECT_NAME}-${PROJECT_VERSION}.jar \
                              --build-arg BUILD_NUMBER=${BUILD_NUMBER} \
                              -t ${DOCKER_IMAGE} \
                              -t ${DOCKER_IMAGE_NAME}:latest \
                              .
                        """
                        built = true
                        
                        echo "Docker image built successfully!"
                        echo "  - ${DOCKER_IMAGE}"
                        echo "  - ${DOCKER_IMAGE_NAME}:latest"

                        // Show image details
                        sh "docker images | grep ${DOCKER_IMAGE_NAME}"
                    } catch (Exception e) {
                        if (i < maxRetries - 1) {
                            echo "Docker build failed: ${e.message}. Retrying..."
                        } else {
                            throw e
                        }
                    }
                }
    }
}
return this
