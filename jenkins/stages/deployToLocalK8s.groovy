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
                    
                    # Wait for rollout with shorter timeout (2 minutes) and non-blocking check
                    echo "Waiting for deployment rollout (timeout: 2 minutes, will continue if still progressing)..."
                    if ! timeout 120 kubectl rollout status deployment/achat-app -n achat-app --timeout=2m 2>&1; then
                        echo "Rollout check completed or timed out, verifying status..."
                        # Quick status check
                        ROLLOUT_STATUS=\$(kubectl get deployment achat-app -n achat-app -o jsonpath='{.status.conditions[?(@.type==\"Progressing\")].status}' 2>/dev/null || echo "Unknown")
                        READY_REPLICAS=\$(kubectl get deployment achat-app -n achat-app -o jsonpath='{.status.readyReplicas}' 2>/dev/null || echo "0")
                        DESIRED_REPLICAS=\$(kubectl get deployment achat-app -n achat-app -o jsonpath='{.spec.replicas}' 2>/dev/null || echo "2")
                        
                        if [ "\$ROLLOUT_STATUS" = "True" ] || [ "\$READY_REPLICAS" -gt "0" ]; then
                            echo "Deployment is progressing (Ready: \$READY_REPLICAS/\$DESIRED_REPLICAS)"
                            echo "Continuing - deployment will complete in background..."
                        else
                            echo "WARNING: Deployment may have issues, checking status..."
                            kubectl get deployment achat-app -n achat-app || true
                            kubectl get pods -n achat-app -l app=achat-app --no-headers | head -3 || true
                            echo ""
                            echo "Checking recent events..."
                            kubectl get events -n achat-app --sort-by='.lastTimestamp' | tail -10 || true
                            echo ""
                            echo "Continuing - check pod status manually if needed"
                        fi
                    else
                        echo "Deployment rollout completed successfully!"
                    fi
                    
                    echo ""
                    echo "Deployment initiated - pods will continue starting in background"
                    echo "You can check status with: kubectl get pods -n achat-app"
                """
            }
            
                echo 'Application deployed to Kubernetes'
    }
}
return this


