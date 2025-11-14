// BOM Fix
// jenkins/stages/healthCheck.groovy
def call() {
    stage('Health Check AWS Deployment') {
        if (!fileExists('terraform/main.tf')) {
            echo 'Skipping health check: terraform configuration not found'
            return
        }

                echo '========================================='
                echo 'Stage 14: Health Check'
                echo '========================================='

            dir(TERRAFORM_DIR) {
                    // Get application URL from Terraform output
                    def appUrl = sh(
                        script: 'terraform output -raw application_url 2>/dev/null || echo ""',
                        returnStdout: true
                    ).trim()

                    if (appUrl) {
                        def healthUrl = "${appUrl}/actuator/health"
                        echo "Application URL: ${appUrl}"
                        echo "Health Check URL: ${healthUrl}"
                        echo ""
                        echo "Waiting for application to start..."
                        echo "This includes time for:"
                        echo "  - EC2 instance initialization"
                        echo "  - Docker installation and startup"
                        echo "  - Database connectivity"
                        echo "  - Application container startup"
                        echo ""
                        
                        // Initial wait for EC2 instance to initialize
                        echo "Initial wait: 60 seconds for EC2 instance boot..."
                        sleep(60)
                        
                        // Continuous health check with retries
                        def maxAttempts = 30  // 30 attempts
                        def checkInterval = 20  // 20 seconds between checks = 10 minutes total
                        def healthy = false
                        
                        echo "Starting health checks (max ${maxAttempts} attempts, ${checkInterval}s intervals)..."
                        echo "Maximum wait time: ${maxAttempts * checkInterval / 60} minutes"
                        echo ""
                        
                        for (int i = 1; i <= maxAttempts && !healthy; i++) {
                            try {
                                echo "Health check attempt ${i}/${maxAttempts}..."
                                
                                def response = sh(
                                    script: "curl -f -s -o /dev/null -w '%{http_code}' ${healthUrl} || echo '000'",
                                    returnStdout: true
                                ).trim()
                                
                                if (response == '200') {
                                    healthy = true
                                    echo "Application is healthy! (HTTP ${response})"
                                    
                                    // Show actual health response
                                    sh "curl -s ${healthUrl} || echo 'Could not fetch health details'"
                                } else {
                                    echo "  Status: HTTP ${response} (not ready yet)"
                                    if (i < maxAttempts) {
                                        echo "  Waiting ${checkInterval} seconds before next check..."
                                        sleep(checkInterval)
                                    }
                                }
                            } catch (Exception e) {
                                echo "  Connection failed: ${e.message}"
                                if (i < maxAttempts) {
                                    echo "  Waiting ${checkInterval} seconds before retry..."
                                    sleep(checkInterval)
                                }
                            }
                        }
                        
                        if (!healthy) {
                            error("Application failed to become healthy after ${maxAttempts} attempts (${maxAttempts * checkInterval / 60} minutes)")
                        }
                        
                        echo ""
                        echo "-----------------"
                    } else {
                        echo "Could not retrieve application URL"
                    
                    }
            }

    }
}
return this


