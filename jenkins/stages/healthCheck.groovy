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

            withCredentials([file(credentialsId: "${AWS_CREDENTIAL_ID}", variable: 'AWS_CREDENTIALS_FILE')]) {
                dir(TERRAFORM_DIR) {
                        // Refresh Terraform state first to get latest EC2 instance info
                        // This is important in REUSE_INFRASTRUCTURE mode where instance was just recreated
                        echo "Refreshing Terraform state to get latest instance information..."
                        sh '''
                            # Setup AWS credentials for terraform refresh
                            export AWS_ACCESS_KEY_ID=$(grep aws_access_key_id "$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                            export AWS_SECRET_ACCESS_KEY=$(grep aws_secret_access_key "$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                            export AWS_SESSION_TOKEN=$(grep aws_session_token "$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                            export AWS_DEFAULT_REGION=''' + AWS_REGION + '''
                            terraform refresh -input=false 2>&1 || echo "Refresh completed (warnings OK)"
                        '''
                        
                        // Get instance ID first to verify it exists
                        def instanceId = sh(
                            script: 'terraform output -raw instance_id 2>/dev/null || echo ""',
                            returnStdout: true
                        ).trim()
                        
                        echo "Terraform instance ID: ${instanceId}"
                        
                        // Get application URL - always query AWS directly for most reliable IP
                        def appUrl = ""
                        if (instanceId && instanceId != "") {
                            echo "Verifying IP address directly from AWS (most reliable)..."
                            // Get the actual current IP from AWS (most reliable)
                            def actualIp = sh(
                                script: '''
                                    export AWS_ACCESS_KEY_ID=$(grep aws_access_key_id "$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                    export AWS_SECRET_ACCESS_KEY=$(grep aws_secret_access_key "$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                    export AWS_SESSION_TOKEN=$(grep aws_session_token "$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                    export AWS_DEFAULT_REGION=''' + AWS_REGION + '''
                                    # First check for EIP (preferred)
                                    EIP_IP=$(aws ec2 describe-addresses --region ''' + AWS_REGION + ''' --filters "Name=instance-id,Values=''' + instanceId + '''" --query "Addresses[0].PublicIp" --output text 2>/dev/null || echo "")
                                    if [ -n "$EIP_IP" ] && [ "$EIP_IP" != "None" ] && [ "$EIP_IP" != "" ]; then
                                        echo "$EIP_IP"
                                    else
                                        # Fallback to instance public IP
                                        aws ec2 describe-instances --region ''' + AWS_REGION + ''' --instance-ids ''' + instanceId + ''' --query "Reservations[0].Instances[0].PublicIpAddress" --output text 2>/dev/null || echo ""
                                    fi
                                ''',
                                returnStdout: true
                            ).trim()
                            
                            if (actualIp && actualIp != "None" && actualIp != "") {
                                echo "✓ Verified IP from AWS: ${actualIp}"
                                appUrl = "http://${actualIp}:8089/SpringMVC"
                                
                                // Also get Terraform output for comparison
                                def terraformUrl = sh(
                                    script: 'terraform output -raw application_url 2>/dev/null || echo ""',
                                    returnStdout: true
                                ).trim()
                                
                                if (terraformUrl && terraformUrl.contains(actualIp)) {
                                    echo "✓ Terraform output matches AWS IP"
                                } else {
                                    echo "⚠ Terraform output (${terraformUrl}) doesn't match AWS IP (${actualIp})"
                                    echo "  Using AWS verified IP: ${appUrl}"
                                }
                            } else {
                                echo "⚠ Could not get IP from AWS, falling back to Terraform output..."
                                appUrl = sh(
                                    script: 'terraform output -raw application_url 2>/dev/null || echo ""',
                                    returnStdout: true
                                ).trim()
                            }
                        } else {
                            echo "⚠ No instance ID found, using Terraform output..."
                            appUrl = sh(
                                script: 'terraform output -raw application_url 2>/dev/null || echo ""',
                                returnStdout: true
                            ).trim()
                        }

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
                        
                        // Check if this is REUSE_INFRASTRUCTURE mode (instance was just recreated)
                        def isReuseMode = false
                        try {
                            // Access params.DEPLOYMENT_MODE from pipeline context (same way other stages do)
                            isReuseMode = (params.DEPLOYMENT_MODE == 'REUSE_INFRASTRUCTURE')
                            echo "Deployment mode: ${params.DEPLOYMENT_MODE} (isReuseMode: ${isReuseMode})"
                        } catch (Exception e) {
                            echo "Could not determine deployment mode, assuming NORMAL: ${e.message}"
                            isReuseMode = false
                        }
                        
                        // Initial wait for EC2 instance to initialize
                        // In REUSE_INFRASTRUCTURE mode, instance was just created, so user-data script needs time
                        if (isReuseMode) {
                            echo "REUSE_INFRASTRUCTURE mode detected - instance was just recreated"
                            echo "User-data script needs time to:"
                            echo "  - Update system packages (~30 seconds)"
                            echo "  - Install Docker (~1-2 minutes)"
                            echo "  - Wait for RDS connectivity (~30 seconds)"
                            echo "  - Pull Docker image (~1-2 minutes)"
                            echo "  - Start application container (~30 seconds)"
                            echo ""
                            echo "Initial wait: 4 minutes for user-data script to complete..."
                            sleep(240)  // 4 minutes for user-data script
                        } else {
                            echo "Initial wait: 60 seconds for EC2 instance boot..."
                            sleep(60)
                        }
                        
                        // Continuous health check with retries
                        // In REUSE_INFRASTRUCTURE, we already waited 4 minutes, so reduce total attempts
                        def maxAttempts = isReuseMode ? 15 : 20  // 15 attempts = ~5 minutes after initial wait
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
                                    
                                    // Show diagnostic info every 3 attempts (more frequent for troubleshooting)
                                    if (i % 3 == 0) {
                                        echo "  Diagnostic check (attempt ${i})..."
                                        try {
                                            // Extract IP from URL for port check
                                            def ipAddress = appUrl.replaceAll('http://', '').replaceAll('/SpringMVC', '').replaceAll(':8089', '')
                                            
                                            // Check if we can reach the server at all
                                            def pingResult = sh(
                                                script: "curl -s -o /dev/null -w '%{http_code}' --connect-timeout 3 --max-time 5 ${appUrl} 2>&1 || echo 'unreachable'",
                                                returnStdout: true
                                            ).trim()
                                            echo "  Server reachability: ${pingResult}"
                                            
                                            // Check if port 8089 is open
                                            def portCheck = sh(
                                                script: "timeout 2 bash -c '</dev/tcp/${ipAddress}/8089' 2>&1 && echo 'open' || echo 'closed'",
                                                returnStdout: true
                                            ).trim()
                                            echo "  Port 8089 status: ${portCheck}"
                                            
                                            // In REUSE_INFRASTRUCTURE mode, provide more context
                                            if (isReuseMode && portCheck == 'closed') {
                                                echo "  ℹ️  In REUSE_INFRASTRUCTURE mode, user-data script may still be running"
                                                echo "     This is normal - Docker installation and container startup takes 3-5 minutes"
                                            }
                                        } catch (Exception e) {
                                            echo "  Server appears unreachable: ${e.message}"
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
}
return this


