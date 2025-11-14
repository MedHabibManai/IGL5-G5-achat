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
                    
                    # Wait for rollout to complete with timeout
                    echo "Waiting for deployment rollout (timeout: 10 minutes)..."
                    if ! kubectl rollout status deployment/achat-app -n achat-app --timeout=10m; then
                        echo "ERROR: Deployment rollout failed or timed out"
                        echo "Checking deployment status..."
                        kubectl get deployment achat-app -n achat-app
                        echo ""
                        echo "Checking pod status..."
                        kubectl get pods -n achat-app -l app=achat-app
                        echo ""
                        echo "Checking recent events..."
                        kubectl get events -n achat-app --sort-by='.lastTimestamp' | tail -20
                        echo ""
                        echo "Checking pod logs for failed pods..."
                        for pod in \$(kubectl get pods -n achat-app -l app=achat-app -o jsonpath='{.items[?(@.status.phase!=\"Running\")].metadata.name}'); do
                            if [ -n "\$pod" ]; then
                                echo "=== Logs for pod: \$pod ==="
                                kubectl logs -n achat-app \$pod --tail=50 || true
                            fi
                        done
                        exit 1
                    fi
                    
                    echo "Deployment rollout completed successfully!"
                """
            }
            
                echo 'Application deployed to Kubernetes'
    }
}
return this


