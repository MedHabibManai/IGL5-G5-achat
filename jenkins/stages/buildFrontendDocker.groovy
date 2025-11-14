// BOM Fix
// jenkins/stages/buildFrontendDocker.groovy
def call() {
    stage('Build Frontend Docker Image') {
        if (!fileExists('frontend/Dockerfile')) {
            echo 'Skipping frontend Docker build: frontend/Dockerfile not found'
            return
        }

                echo '========================================='
                echo 'Stage 11: Building Frontend Docker Image'
                echo '========================================='
            
            dir('frontend') {
                    // Build Docker image with continuous retry on failure (15s delay)
                    sh """
                        attempt=0
                        while true; do
                          attempt=\$((attempt + 1))
                          echo "Frontend docker build attempt \${attempt}..."
                          if docker build -t ${DOCKER_REGISTRY}/habibmanai/achat-frontend:${BUILD_NUMBER} .; then
                            docker tag ${DOCKER_REGISTRY}/habibmanai/achat-frontend:${BUILD_NUMBER} ${DOCKER_REGISTRY}/habibmanai/achat-frontend:latest
                            echo "Frontend docker build succeeded on attempt \${attempt}"
                            break
                          else
                            echo "Frontend docker build failed on attempt \${attempt} retrying after 15s (likely network/TLS error)"
                            sleep 15
                          fi
                        done
                    """
            }
            
                echo 'Frontend Docker image built successfully'
    }
}
return this


