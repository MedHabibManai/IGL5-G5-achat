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
        
        // Get EKS URLs - check if EKS cluster exists first
        try {
            // Check if we're connected to EKS (not local K8s)
            def clusterInfo = sh(
                script: 'kubectl config current-context 2>/dev/null || echo ""',
                returnStdout: true
            ).trim()
            
            if (clusterInfo && clusterInfo.contains("eks") || clusterInfo.contains("achat-app-eks")) {
                withKubeConfig([credentialsId: 'kubeconfig-credentials']) {
                    def eksBackend = sh(
                        script: 'kubectl get svc achat-app -n achat-app -o jsonpath="{.status.loadBalancer.ingress[0].hostname}" 2>/dev/null || echo ""',
                        returnStdout: true
                    ).trim()
                    
                    if (eksBackend && eksBackend != "" && eksBackend != "null" && eksBackend != "<none>") {
                        eksBackendUrl = "http://${eksBackend}/SpringMVC"
                    }
                    
                    def eksFrontend = sh(
                        script: 'kubectl get svc achat-frontend -n achat-app -o jsonpath="{.status.loadBalancer.ingress[0].hostname}" 2>/dev/null || echo ""',
                        returnStdout: true
                    ).trim()
                    
                    if (eksFrontend && eksFrontend != "" && eksFrontend != "null" && eksFrontend != "<none>") {
                        eksFrontendUrl = "http://${eksFrontend}"
                    }
                }
            }
        } catch (Exception e) {
            echo "Could not get EKS URLs: ${e.message}"
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
        
        // Local Kubernetes
        echo '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━'
        echo '📍 LOCAL KUBERNETES (Docker Desktop)'
        echo '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━'
        if (localK8sBackendPort && localK8sBackendPort != "" && localK8sBackendPort != "null") {
            echo "✅ Backend (NodePort):"
            echo "   ${localK8sBackendUrl}"
            echo "   Swagger UI: http://localhost:${localK8sBackendPort}/SpringMVC/swagger-ui/index.html"
            echo ""
        } else {
            echo "⚠️  Backend NodePort not available"
            echo "   Use port-forward: kubectl port-forward svc/achat-app 8089:80 -n achat-app"
            echo "   Then access: http://localhost:8089/SpringMVC"
            echo ""
        }
        
        if (localK8sFrontendPort && localK8sFrontendPort != "" && localK8sFrontendPort != "null") {
            echo "✅ Frontend (NodePort):"
            echo "   ${localK8sFrontendUrl}"
            echo ""
        } else {
            echo "⚠️  Frontend NodePort not available"
            echo "   Use port-forward: kubectl port-forward svc/achat-frontend 30080:80 -n achat-app"
            echo "   Then access: http://localhost:30080"
            echo ""
        }
        
        echo "ℹ️  Services are configured as NodePort for local access"
        echo "   Check service status: kubectl get svc -n achat-app"
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

