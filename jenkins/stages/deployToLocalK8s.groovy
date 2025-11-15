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
                    # For local K8s (Docker Desktop), we need NodePort instead of LoadBalancer
                    # Patch services to use NodePort for local access
                    echo "Configuring services for local Kubernetes (Docker Desktop)..."
                    
                    # Patch backend service to NodePort
                    kubectl patch svc achat-app -n achat-app -p '{"spec":{"type":"NodePort"}}' 2>/dev/null || echo "Service may not exist yet"
                    
                    # Patch frontend service to NodePort
                    kubectl patch svc achat-frontend -n achat-app -p '{"spec":{"type":"NodePort"}}' 2>/dev/null || echo "Service may not exist yet"
                    
                    # Update deployment image to use current build number
                    kubectl set image deployment/achat-app -n achat-app \\
                        achat-app=${DOCKER_REGISTRY}/habibmanai/${DOCKER_IMAGE_NAME}:${BUILD_NUMBER} \\
                        --record || echo "Deployment doesn't exist yet, applying manifests..."
                    
                    # Apply all manifests
                    kubectl apply -f k8s/
                    
                    # After applying, ensure services are NodePort (in case they were recreated as LoadBalancer)
                    kubectl patch svc achat-app -n achat-app -p '{"spec":{"type":"NodePort"}}' 2>/dev/null || true
                    kubectl patch svc achat-frontend -n achat-app -p '{"spec":{"type":"NodePort"}}' 2>/dev/null || true
                    
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
                    
                    # Get NodePort URLs for local access
                    echo ""
                    echo "=== Local Kubernetes Access URLs ==="
                    BACKEND_NODEPORT=\$(kubectl get svc achat-app -n achat-app -o jsonpath='{.spec.ports[0].nodePort}' 2>/dev/null || echo "")
                    FRONTEND_NODEPORT=\$(kubectl get svc achat-frontend -n achat-app -o jsonpath='{.spec.ports[0].nodePort}' 2>/dev/null || echo "")
                    
                    if [ -n "\$BACKEND_NODEPORT" ]; then
                        echo "✅ Backend NodePort: \$BACKEND_NODEPORT"
                        echo "   Access at: http://localhost:\$BACKEND_NODEPORT/SpringMVC"
                        echo "   Swagger: http://localhost:\$BACKEND_NODEPORT/SpringMVC/swagger-ui/index.html"
                    else
                        echo "⚠️  Backend NodePort not available yet"
                    fi
                    
                    if [ -n "\$FRONTEND_NODEPORT" ]; then
                        echo "✅ Frontend NodePort: \$FRONTEND_NODEPORT"
                        echo "   Access at: http://localhost:\$FRONTEND_NODEPORT"
                    else
                        echo "⚠️  Frontend NodePort not available yet"
                    fi
                    
                    echo ""
                    echo "Note: If NodePort doesn't work, use port-forward:"
                    echo "  kubectl port-forward svc/achat-app 8089:80 -n achat-app"
                    echo "  kubectl port-forward svc/achat-frontend 30080:80 -n achat-app"
                """
            }
            
                echo 'Application deployed to Local Kubernetes'
    }
}
return this


