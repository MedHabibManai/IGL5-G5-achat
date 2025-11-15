// Final Deployment Summary
// jenkins/stages/finalSummary.groovy
def call() {
    stage('Final Deployment Summary') {
        echo '========================================='
        echo 'FINAL DEPLOYMENT SUMMARY'
        echo '========================================='
        echo ''
        
        // Collect all URLs
        def awsBackendUrl = ""
        def awsSwaggerUrl = ""
        def awsHealthUrl = ""
        def eksBackendUrl = ""
        def eksFrontendUrl = ""
        
        // Get AWS EC2 URLs from Terraform
        if (fileExists('terraform/main.tf')) {
            dir(TERRAFORM_DIR) {
                try {
                    awsBackendUrl = sh(
                        script: 'terraform output -raw application_url 2>/dev/null || echo ""',
                        returnStdout: true
                    ).trim()
                    
                    awsSwaggerUrl = sh(
                        script: 'terraform output -raw swagger_url 2>/dev/null || echo ""',
                        returnStdout: true
                    ).trim()
                    
                    awsHealthUrl = sh(
                        script: 'terraform output -raw health_check_url 2>/dev/null || echo ""',
                        returnStdout: true
                    ).trim()
                } catch (Exception e) {
                    echo "Could not get AWS URLs: ${e.message}"
                }
            }
        }
        
        // Get EKS URLs - use AWS credentials to configure kubectl (same as deployToEKS stage)
        withCredentials([file(credentialsId: "${AWS_CREDENTIAL_ID}", variable: 'AWS_CREDENTIALS_FILE')]) {
            dir(TERRAFORM_DIR) {
                try {
                    // Get EKS cluster name from Terraform
                    def eksClusterName = sh(
                        script: '''
                            # Parse and export AWS credentials
                            export AWS_ACCESS_KEY_ID=$(grep aws_access_key_id "$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                            export AWS_SECRET_ACCESS_KEY=$(grep aws_secret_access_key "$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                            export AWS_SESSION_TOKEN=$(grep aws_session_token "$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                            export AWS_DEFAULT_REGION=us-east-1
                            
                            # Try to get from Terraform output first
                            CLUSTER_NAME=$(terraform output -raw eks_cluster_name 2>/dev/null || echo "")
                            
                            # If not in Terraform state, use default name
                            if [ -z "$CLUSTER_NAME" ] || [ "$CLUSTER_NAME" = "" ]; then
                                CLUSTER_NAME="achat-app-eks-cluster"
                            fi
                            
                            echo "$CLUSTER_NAME"
                        ''',
                        returnStdout: true
                    ).trim()
                    
                    if (eksClusterName && eksClusterName != "") {
                        // Configure kubectl with AWS credentials
                        sh """
                            # Parse and export AWS credentials
                            export AWS_ACCESS_KEY_ID=\$(grep aws_access_key_id "\$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                            export AWS_SECRET_ACCESS_KEY=\$(grep aws_secret_access_key "\$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                            export AWS_SESSION_TOKEN=\$(grep aws_session_token "\$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                            export AWS_DEFAULT_REGION=us-east-1
                            
                            # Update kubeconfig
                            aws eks update-kubeconfig \\
                                --region ${AWS_REGION} \\
                                --name ${eksClusterName} 2>/dev/null || true
                        """
                        
                        // Get EKS service URLs
                        def eksBackend = sh(
                            script: '''
                                # Parse and export AWS credentials (needed for kubectl to work)
                                export AWS_ACCESS_KEY_ID=$(grep aws_access_key_id "$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                export AWS_SECRET_ACCESS_KEY=$(grep aws_secret_access_key "$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                export AWS_SESSION_TOKEN=$(grep aws_session_token "$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                export AWS_DEFAULT_REGION=us-east-1
                                
                                kubectl get svc achat-app -n achat-app -o jsonpath="{.status.loadBalancer.ingress[0].hostname}" 2>/dev/null || echo ""
                            ''',
                            returnStdout: true
                        ).trim()
                        
                        if (eksBackend && eksBackend != "" && eksBackend != "null" && eksBackend != "<none>") {
                            eksBackendUrl = "http://${eksBackend}/SpringMVC"
                        }
                        
                        def eksFrontend = sh(
                            script: '''
                                # Parse and export AWS credentials (needed for kubectl to work)
                                export AWS_ACCESS_KEY_ID=$(grep aws_access_key_id "$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                export AWS_SECRET_ACCESS_KEY=$(grep aws_secret_access_key "$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                export AWS_SESSION_TOKEN=$(grep aws_session_token "$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                export AWS_DEFAULT_REGION=us-east-1
                                
                                kubectl get svc achat-frontend -n achat-app -o jsonpath="{.status.loadBalancer.ingress[0].hostname}" 2>/dev/null || echo ""
                            ''',
                            returnStdout: true
                        ).trim()
                        
                        if (eksFrontend && eksFrontend != "" && eksFrontend != "null" && eksFrontend != "<none>") {
                            eksFrontendUrl = "http://${eksFrontend}"
                        }
                    }
                } catch (Exception e) {
                    echo "Could not get EKS URLs: ${e.message}"
                }
            }
        }
        
        
        // Print comprehensive summary
        echo ''
        echo '╔════════════════════════════════════════════════════════════════╗'
        echo '║           🚀 DEPLOYMENT COMPLETE - ACCESS YOUR APPS 🚀         ║'
        echo '╚════════════════════════════════════════════════════════════════╝'
        echo ''
        
        // AWS EC2 Backend
        echo '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━'
        echo '📍 AWS EC2 BACKEND (Production)'
        echo '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━'
        if (awsBackendUrl && awsBackendUrl != "") {
            echo "✅ Swagger UI (API Documentation):"
            if (awsSwaggerUrl && awsSwaggerUrl != "") {
                echo "   ${awsSwaggerUrl}"
            } else {
                echo "   ${awsBackendUrl}/swagger-ui/index.html"
            }
            echo ""
            if (awsHealthUrl && awsHealthUrl != "") {
                echo "✅ Health Check:"
                echo "   ${awsHealthUrl}"
                echo ""
            }
            echo "ℹ️  API Endpoints (use Swagger UI above to see all endpoints):"
            echo "   ${awsBackendUrl}/produit/retrieve-all-produits"
            echo "   ${awsBackendUrl}/stock/retrieve-all-stocks"
            echo "   (Note: /SpringMVC alone shows 404 - this is normal, use specific endpoints)"
            echo ""
        } else {
            echo "⚠️  AWS EC2 backend not deployed or not available"
            echo ""
        }
        
        // EKS Backend
        echo '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━'
        echo '📍 EKS BACKEND (Kubernetes on AWS)'
        echo '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━'
        if (eksBackendUrl && eksBackendUrl != "") {
            echo "✅ Swagger UI (API Documentation):"
            echo "   ${eksBackendUrl}/swagger-ui/index.html"
            echo ""
            echo "✅ Health Check:"
            echo "   ${eksBackendUrl}/actuator/health"
            echo ""
            echo "ℹ️  API Endpoints (use Swagger UI above to see all endpoints):"
            echo "   ${eksBackendUrl}/produit/retrieve-all-produits"
            echo "   ${eksBackendUrl}/stock/retrieve-all-stocks"
            echo "   (Note: /SpringMVC alone shows 404 - this is normal, use specific endpoints)"
            echo ""
        } else {
            echo "⚠️  EKS backend not deployed or LoadBalancer pending"
            echo "   (May take 2-5 minutes for LoadBalancer to provision)"
            echo ""
        }
        
        // EKS Frontend
        echo '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━'
        echo '📍 EKS FRONTEND (React App on Kubernetes)'
        echo '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━'
        if (eksFrontendUrl && eksFrontendUrl != "") {
            echo "✅ Frontend Application:"
            echo "   ${eksFrontendUrl}"
            echo ""
        } else {
            echo "⚠️  EKS frontend not deployed or LoadBalancer pending"
            echo "   (May take 2-5 minutes for LoadBalancer to provision)"
            echo ""
        }
        
        // Quick Test Commands
        echo '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━'
        echo '🧪 QUICK TEST COMMANDS'
        echo '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━'
        if (awsHealthUrl && awsHealthUrl != "") {
            echo "Test AWS Backend Health:"
            echo "   curl ${awsHealthUrl}"
            echo ""
        }
        if (eksBackendUrl && eksBackendUrl != "") {
            echo "Test EKS Backend Health:"
            echo "   curl ${eksBackendUrl}/actuator/health"
            echo ""
        }
        
        echo '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━'
        echo '📝 NOTES'
        echo '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━'
        echo '• LoadBalancer URLs may take 2-5 minutes to become active'
        echo '• If a URL shows "Pending", wait a few minutes and refresh'
        echo '• Check pod status: kubectl get pods -n achat-app'
        echo '• Check service status: kubectl get svc -n achat-app'
        echo ''
        echo '╔════════════════════════════════════════════════════════════════╗'
        echo '║                    Deployment Summary Complete                  ║'
        echo '╚════════════════════════════════════════════════════════════════╝'
        echo ''
    }
}
return this

