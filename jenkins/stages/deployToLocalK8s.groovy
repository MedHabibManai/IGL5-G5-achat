// BOM Fix
// jenkins/stages/deployToLocalK8s.groovy
def call() {
    stage('Deploy to Local Kubernetes') {
        if (!fileExists('k8s/deployment.yaml')) {
            echo 'Skipping local Kubernetes deploy: k8s manifests not found'
            return
        }

                echo '========================================='
                echo 'Stage 14: Deploying to Local Kubernetes (Docker Desktop)'
                echo '========================================='
            
            // Deploy to Kubernetes
            withKubeConfig([credentialsId: 'kubeconfig-credentials']) {
                sh """
                    # Update deployment image to use current build number
                    kubectl set image deployment/achat-app -n achat-app \\
                        achat-app=${DOCKER_REGISTRY}/habibmanai/${DOCKER_IMAGE_NAME}:${BUILD_NUMBER} \\
                        --record || echo "Deployment doesn't exist yet, applying manifests..."
                    
                    # Apply all manifests
                    kubectl apply -f k8s/
                    
                    # Wait for rollout to complete
                    kubectl rollout status deployment/achat-app -n achat-app
                """
            }
            
                echo 'Application deployed to Kubernetes'
    }
}
return this


