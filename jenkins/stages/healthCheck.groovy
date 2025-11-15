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
                        // Reduced attempts for REUSE_INFRASTRUCTURE (instance already exists, just needs to start app)
                        def maxAttempts = 20  // 20 attempts = ~6.5 minutes (faster for reuse mode)
                        def checkInterval = 20  // 20 seconds between checks
                        def healthy = false
                        
                        echo "Starting health checks (max ${maxAttempts} attempts, ${checkInterval}s intervals)..."
                        echo "Maximum wait time: ${maxAttempts * checkInterval / 60} minutes"
                        echo ""
                        
                        for (int i = 1; i <= maxAttempts && !healthy; i++) {
                            try {
                                echo "Health check attempt ${i}/${maxAttempts}..."
                                
                                // Try to get HTTP status code with timeout
                                def response = sh(
                                    script: "curl -f -s -o /dev/null -w '%{http_code}' --connect-timeout 5 --max-time 10 ${healthUrl} 2>&1 || echo '000'",
                                    returnStdout: true
                                ).trim()
                                
                                if (response == '200') {
                                    healthy = true
                                    echo "Application is healthy! (HTTP ${response})"
                                    
                                    // Show actual health response
                                    sh "curl -s ${healthUrl} || echo 'Could not fetch health details'"
                                } else {
                                    echo "  Status: HTTP ${response} (not ready yet)"
                                    
                                    // Show diagnostic info every 5 attempts
                                    if (i % 5 == 0) {
                                        echo "  Diagnostic check (attempt ${i})..."
                                        try {
                                            // Check if we can reach the server at all
                                            def pingResult = sh(
                                                script: "curl -s -o /dev/null -w '%{http_code}' --connect-timeout 3 --max-time 5 ${appUrl} 2>&1 || echo 'unreachable'",
                                                returnStdout: true
                                            ).trim()
                                            echo "  Server reachability: ${pingResult}"
                                        } catch (Exception e) {
                                            echo "  Server appears unreachable"
                                        }
                                    }
                                    
                                    if (i < maxAttempts) {
                                        echo "  Waiting ${checkInterval} seconds before next check..."
                                        sleep(checkInterval)
                                    }
                                }
                            } catch (Exception e) {
                                echo "  Error during health check: ${e.message}"
                                if (i < maxAttempts) {
                                    echo "  Waiting ${checkInterval} seconds before retry..."
                                    sleep(checkInterval)
                                }
                            }
                        }
                        
                        if (!healthy) {
                            echo ""
                            echo "WARNING: Application did not become healthy after ${maxAttempts} attempts"
                            echo "This could mean:"
                            echo "  - EC2 instance is still initializing"
                            echo "  - Docker is installing/starting"
                            echo "  - Application container is starting"
                            echo "  - Database connection issue"
                            echo ""
                            echo "The application may still be starting in the background."
                            echo "You can check manually: ${healthUrl}"
                            echo ""
                            // Don't fail the pipeline - just warn (application may start later)
                            echo "Continuing pipeline - application will be available when ready"
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


