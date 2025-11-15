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
        def localK8sBackendUrl = ""
        def localK8sFrontendUrl = ""
        
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
        
        // Get EKS URLs
        try {
            withKubeConfig([credentialsId: 'kubeconfig-credentials']) {
                def eksBackend = sh(
                    script: 'kubectl get svc achat-app -n achat-app -o jsonpath="{.status.loadBalancer.ingress[0].hostname}" 2>/dev/null || echo ""',
                    returnStdout: true
                ).trim()
                
                if (eksBackend && eksBackend != "" && eksBackend != "null") {
                    eksBackendUrl = "http://${eksBackend}/SpringMVC"
                }
                
                def eksFrontend = sh(
                    script: 'kubectl get svc achat-frontend -n achat-app -o jsonpath="{.status.loadBalancer.ingress[0].hostname}" 2>/dev/null || echo ""',
                    returnStdout: true
                ).trim()
                
                if (eksFrontend && eksFrontend != "" && eksFrontend != "null") {
                    eksFrontendUrl = "http://${eksFrontend}"
                }
            }
        } catch (Exception e) {
            echo "Could not get EKS URLs: ${e.message}"
        }
        
        // Local K8s URLs (Docker Desktop)
        localK8sBackendUrl = "http://localhost/SpringMVC"
        localK8sFrontendUrl = "http://localhost:30080"
        
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
            echo "✅ Main Application:"
            echo "   ${awsBackendUrl}"
            echo ""
            if (awsSwaggerUrl && awsSwaggerUrl != "") {
                echo "✅ Swagger UI:"
                echo "   ${awsSwaggerUrl}"
                echo ""
            }
            if (awsHealthUrl && awsHealthUrl != "") {
                echo "✅ Health Check:"
                echo "   ${awsHealthUrl}"
                echo ""
            }
        } else {
            echo "⚠️  AWS EC2 backend not deployed or not available"
            echo ""
        }
        
        // EKS Backend
        echo '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━'
        echo '📍 EKS BACKEND (Kubernetes on AWS)'
        echo '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━'
        if (eksBackendUrl && eksBackendUrl != "") {
            echo "✅ Backend API:"
            echo "   ${eksBackendUrl}"
            echo ""
            echo "✅ Swagger UI:"
            echo "   ${eksBackendUrl}/swagger-ui/index.html"
            echo ""
            echo "✅ Health Check:"
            echo "   ${eksBackendUrl}/actuator/health"
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
        
        // Local Kubernetes
        echo '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━'
        echo '📍 LOCAL KUBERNETES (Docker Desktop)'
        echo '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━'
        echo "✅ Backend API:"
        echo "   ${localK8sBackendUrl}"
        echo ""
        echo "✅ Frontend Application:"
        echo "   ${localK8sFrontendUrl}"
        echo ""
        
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

