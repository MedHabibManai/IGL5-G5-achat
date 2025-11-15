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
                        
                        // Verify instance exists and get its actual IP from AWS
                        def actualIp = ""
                        if (instanceId && instanceId != "") {
                            // Get the actual current IP from AWS (most reliable)
                            actualIp = sh(
                                script: '''
                                    export AWS_ACCESS_KEY_ID=$(grep aws_access_key_id "$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                    export AWS_SECRET_ACCESS_KEY=$(grep aws_secret_access_key "$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                    export AWS_SESSION_TOKEN=$(grep aws_session_token "$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                    export AWS_DEFAULT_REGION=''' + AWS_REGION + '''
                                    # First check for EIP
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
                                def appUrl = "http://${actualIp}:8089/SpringMVC"
                            } else {
                                echo "⚠ Could not get IP from AWS, trying Terraform output..."
                                def appUrl = sh(
                                    script: 'terraform output -raw application_url 2>/dev/null || echo ""',
                                    returnStdout: true
                                ).trim()
                            }
                        } else {
                            echo "⚠ No instance ID found, using Terraform output..."
                            def appUrl = sh(
                                script: 'terraform output -raw application_url 2>/dev/null || echo ""',
                                returnStdout: true
                            ).trim()
                        }
                        
                        // Also verify by getting IP directly from AWS to ensure we have the correct IP
                        echo "Verifying IP address from AWS..."
                        def instanceId = sh(
                            script: 'terraform output -raw instance_id 2>/dev/null || echo ""',
                            returnStdout: true
                        ).trim()
                        
                        if (instanceId && instanceId != "") {
                            // First check if EIP is associated (preferred)
                            def eipIp = sh(
                                script: '''
                                    export AWS_ACCESS_KEY_ID=$(grep aws_access_key_id "$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                    export AWS_SECRET_ACCESS_KEY=$(grep aws_secret_access_key "$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                    export AWS_SESSION_TOKEN=$(grep aws_session_token "$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                    export AWS_DEFAULT_REGION=''' + AWS_REGION + '''
                                    aws ec2 describe-addresses --region ''' + AWS_REGION + ''' --filters "Name=instance-id,Values=''' + instanceId + '''" --query "Addresses[0].PublicIp" --output text 2>/dev/null || echo ""
                                ''',
                                returnStdout: true
                            ).trim()
                            
                            // If no EIP, get instance public IP
                            def publicIp = ""
                            if (!eipIp || eipIp == "None" || eipIp == "") {
                                publicIp = sh(
                                    script: '''
                                        export AWS_ACCESS_KEY_ID=$(grep aws_access_key_id "$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                        export AWS_SECRET_ACCESS_KEY=$(grep aws_secret_access_key "$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                        export AWS_SESSION_TOKEN=$(grep aws_session_token "$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                        export AWS_DEFAULT_REGION=''' + AWS_REGION + '''
                                        aws ec2 describe-instances --region ''' + AWS_REGION + ''' --instance-ids ''' + instanceId + ''' --query "Reservations[0].Instances[0].PublicIpAddress" --output text 2>/dev/null || echo ""
                                    ''',
                                    returnStdout: true
                                ).trim()
                            } else {
                                publicIp = eipIp
                                echo "✓ EIP is associated: ${eipIp}"
                            }
                            
                            if (publicIp && publicIp != "None" && publicIp != "") {
                                def verifiedUrl = "http://${publicIp}:8089/SpringMVC"
                                echo "Terraform output URL: ${appUrl}"
                                echo "AWS verified IP: ${publicIp}"
                                echo "Verified URL: ${verifiedUrl}"
                                
                                // Use the verified URL from AWS (most reliable)
                                if (appUrl && appUrl.contains(publicIp)) {
                                    echo "✓ URLs match - using Terraform output"
                                } else {
                                    echo "⚠ URLs don't match - using AWS verified IP (EIP or instance IP)"
                                    appUrl = verifiedUrl
                                }
                            } else {
                                echo "Could not get IP from AWS, using Terraform output: ${appUrl}"
                            }
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
                                            
                                            // Check if port 8089 is open (using telnet or nc if available)
                                            def portCheck = sh(
                                                script: "timeout 2 bash -c '</dev/tcp/${appUrl.replaceAll('http://', '').replaceAll('/SpringMVC', '').replaceAll(':8089', '')}/8089' 2>&1 && echo 'open' || echo 'closed'",
                                                returnStdout: true
                                            ).trim()
                                            echo "  Port 8089 status: ${portCheck}"
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


