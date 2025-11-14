// BOM Fix
// jenkins/stages/deployToEKS.groovy
def call() {
    stage('Deploy to EKS') {
        if (!fileExists('k8s/deployment.yaml')) {
            echo 'Skipping EKS deploy: k8s manifests not found'
            return
        }
                echo '========================================='
                echo 'Stage 13: Deploying to AWS EKS'
                echo '========================================='
            
            // Configure kubectl for EKS - using withCredentials instead of withAWS
            withCredentials([file(credentialsId: "${AWS_CREDENTIAL_ID}", variable: 'AWS_CREDENTIALS_FILE')]) {
                dir("${TERRAFORM_DIR}") {
                        // Get EKS cluster name from Terraform
                        def eksClusterName = sh(
                            script: '''
                                # Parse and export AWS credentials
                                export AWS_ACCESS_KEY_ID=$(grep aws_access_key_id "$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                export AWS_SECRET_ACCESS_KEY=$(grep aws_secret_access_key "$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                export AWS_SESSION_TOKEN=$(grep aws_session_token "$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                export AWS_DEFAULT_REGION=us-east-1
                                
                                terraform output -raw eks_cluster_name 2>/dev/null || echo ""
                            ''',
                            returnStdout: true
                        ).trim()
                        
                        if (eksClusterName) {
                            echo "Configuring kubectl for EKS cluster: ${eksClusterName}"
                            
                            // Update kubeconfig and deploy
                            sh """
                                # Parse and export AWS credentials
                                export AWS_ACCESS_KEY_ID=\$(grep aws_access_key_id "\$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                export AWS_SECRET_ACCESS_KEY=\$(grep aws_secret_access_key "\$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                export AWS_SESSION_TOKEN=\$(grep aws_session_token "\$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                export AWS_DEFAULT_REGION=us-east-1
                                
                                echo "AWS credentials configured from file"
                                
                                # Update kubeconfig
                                aws eks update-kubeconfig \\
                                    --region ${AWS_REGION} \\
                                    --name ${eksClusterName}
                            """
                            
                            // Get RDS endpoint from Terraform
                            def rdsEndpoint = sh(
                                script: '''
                                    # Parse and export AWS credentials
                                    export AWS_ACCESS_KEY_ID=$(grep aws_access_key_id "$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                    export AWS_SECRET_ACCESS_KEY=$(grep aws_secret_access_key "$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                    export AWS_SESSION_TOKEN=$(grep aws_session_token "$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                    export AWS_DEFAULT_REGION=us-east-1
                                    
                                    terraform output -raw rds_endpoint 2>/dev/null || echo ""
                                ''',
                                returnStdout: true
                            ).trim()
                            
                            if (rdsEndpoint) {
                                echo "RDS Endpoint: ${rdsEndpoint}"
                                echo "Updating ConfigMap with RDS endpoint..."
                            } else {
                                echo "WARNING: Could not retrieve RDS endpoint!"
                            }
                            
                            // Deploy to EKS
                            sh """
                                # Helper function for kubectl with retry
                                kubectl_retry() {
                                    local cmd="\$@"
                                    local attempt=0
                                    while true; do
                                        attempt=\$((attempt + 1))
                                        echo "Attempt \$attempt: \$cmd"
                                        if eval "\$cmd"; then
                                            echo "Success"
                                            return 0
                                        else
                                            echo "Failed (TLS timeout likely), retrying in 15s..."
                                            sleep 15
                                        fi
                                    done
                                }
                                
                                # Create namespace first
                                echo "Creating namespace..."
                                kubectl_retry kubectl apply -f ../k8s/namespace.yaml
                                
                                # Apply secrets and configmaps
                                echo "Applying secrets and configmaps..."
                                kubectl_retry kubectl apply -f ../k8s/secret.yaml
                                
                                # Update ConfigMap with actual RDS endpoint
                                if [ -n "${rdsEndpoint}" ]; then
                                    echo "Updating ConfigMap with RDS endpoint: ${rdsEndpoint}"
                                    cat ../k8s/configmap.yaml | \\
                                        sed "s|RDS_ENDPOINT_PLACEHOLDER|${rdsEndpoint}|g" | \\
                                        kubectl apply -f -
                                else
                                    echo "WARNING: Applying ConfigMap without RDS endpoint update!"
                                    kubectl_retry kubectl apply -f ../k8s/configmap.yaml
                                fi
                                
                                # Apply all other Kubernetes manifests (deployments, services, etc.)
                                echo "Applying deployments and services..."
                                kubectl_retry kubectl apply -f ../k8s/deployment.yaml
                                kubectl_retry kubectl apply -f ../k8s/frontend-deployment.yaml
                                kubectl_retry kubectl apply -f ../k8s/service.yaml
                                kubectl_retry kubectl apply -f ../k8s/hpa.yaml
                                
                                # Now update deployment images to specific build versions
                                echo "Updating deployment images to build ${BUILD_NUMBER}..."
                                kubectl_retry kubectl set image deployment/achat-app -n achat-app \\
                                    achat-app=${DOCKER_REGISTRY}/habibmanai/${DOCKER_IMAGE_NAME}:${BUILD_NUMBER} \\
                                    --record
                                
                                kubectl_retry kubectl set image deployment/achat-frontend -n achat-app \\
                                    frontend=${DOCKER_REGISTRY}/habibmanai/achat-frontend:${BUILD_NUMBER} \\
                                    --record
                                
                                # Wait for backend rollout
                                echo "Waiting for backend deployment..."
                                kubectl_retry kubectl rollout status deployment/achat-app -n achat-app --timeout=5m
                                
                                # Wait for frontend rollout
                                echo "Waiting for frontend deployment..."
                                kubectl_retry kubectl rollout status deployment/achat-frontend -n achat-app --timeout=5m
                                
                                # Get service endpoints
                                echo ""
                                echo "=== EKS Services ==="
                                kubectl_retry kubectl get svc -n achat-app
                                
                                # Get Load Balancer URLs
                                echo ""
                                echo "=== Frontend LoadBalancer URL ==="
                                FRONTEND_URL=\$(kubectl get svc achat-frontend -n achat-app -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || echo "Pending...")
                                echo "Frontend will be accessible at: http://\${FRONTEND_URL}"
                                
                                echo ""
                                echo "=== Backend LoadBalancer URL ==="
                                BACKEND_URL=\$(kubectl get svc achat-app -n achat-app -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || echo "Pending...")
                                echo "Backend will be accessible at: http://\${BACKEND_URL}/SpringMVC"
                            """
                            
                            echo 'Application deployed to EKS successfully!'
                        } else {
                            echo 'EKS cluster not found. Skipping EKS deployment.'
                            echo 'Run terraform apply to create EKS cluster first.'
                        }
                }
            }

    }
}
return this
