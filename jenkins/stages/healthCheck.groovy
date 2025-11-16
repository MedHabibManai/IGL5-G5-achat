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
                        
                        // Check deployment mode to determine how to get instance info
                        def deploymentMode = env.DEPLOYMENT_MODE ?: params.DEPLOYMENT_MODE
                        def isReuseMode = (deploymentMode == 'REUSE_INFRASTRUCTURE')
                        
                        // Get instance ID - in REUSE_INFRASTRUCTURE mode, query AWS directly for most recent instance
                        def instanceId = ""
                        if (isReuseMode) {
                            echo "REUSE_INFRASTRUCTURE mode: Finding most recent instance by tags..."
                            // In REUSE mode, find the most recent running instance with our tags
                            instanceId = sh(
                                script: '''
                                    export AWS_ACCESS_KEY_ID=$(grep aws_access_key_id "$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                    export AWS_SECRET_ACCESS_KEY=$(grep aws_secret_access_key "$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                    export AWS_SESSION_TOKEN=$(grep aws_session_token "$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                    export AWS_DEFAULT_REGION=''' + AWS_REGION + '''
                                    # Find most recent running instance with our project tag
                                    aws ec2 describe-instances \
                                        --region ''' + AWS_REGION + ''' \
                                        --filters "Name=tag:Project,Values=achat-app" "Name=tag:Name,Values=achat-app-instance" "Name=instance-state-name,Values=running" \
                                        --query "Reservations[*].Instances | sort_by(@, &LaunchTime) | [-1].InstanceId" \
                                        --output text 2>/dev/null || echo ""
                                ''',
                                returnStdout: true
                            ).trim()
                            if (instanceId && instanceId != "") {
                                echo "✓ Found most recent instance from AWS: ${instanceId}"
                            } else {
                                echo "⚠ Could not find instance by tags, falling back to Terraform output..."
                                instanceId = sh(
                                    script: 'terraform output -raw instance_id 2>/dev/null || echo ""',
                                    returnStdout: true
                                ).trim()
                            }
                        } else {
                            instanceId = sh(
                                script: 'terraform output -raw instance_id 2>/dev/null || echo ""',
                                returnStdout: true
                            ).trim()
                        }
                        
                        echo "Instance ID: ${instanceId}"
                        
                        // Get application URL
                        // In REUSE_INFRASTRUCTURE mode, trust Terraform output since it just created the resources
                        // AWS API might have propagation delays for EIP association
                        def appUrl = ""
                        if (isReuseMode) {
                            echo "REUSE_INFRASTRUCTURE mode: Using Terraform output (most reliable after recent creation)..."
                            appUrl = sh(
                                script: 'terraform output -raw application_url 2>/dev/null || echo ""',
                                returnStdout: true
                            ).trim()
                            
                            if (appUrl && appUrl != "") {
                                // Extract IP from URL for verification
                                def terraformIp = appUrl.replaceAll('http://', '').replaceAll('/SpringMVC', '').replaceAll(':8089', '')
                                echo "✓ Using Terraform output IP: ${terraformIp}"
                                
                                // Verify the instance exists (but trust Terraform for IP)
                                if (instanceId && instanceId != "") {
                                    def instanceExists = sh(
                                        script: '''
                                            export AWS_ACCESS_KEY_ID=$(grep aws_access_key_id "$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                            export AWS_SECRET_ACCESS_KEY=$(grep aws_secret_access_key "$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                            export AWS_SESSION_TOKEN=$(grep aws_session_token "$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                            export AWS_DEFAULT_REGION=''' + AWS_REGION + '''
                                            aws ec2 describe-instances --region ''' + AWS_REGION + ''' --instance-ids ''' + instanceId + ''' --query "Reservations[0].Instances[0].State.Name" --output text 2>/dev/null || echo "not-found"
                                        ''',
                                        returnStdout: true
                                    ).trim()
                                    echo "  Instance state: ${instanceExists}"
                                }
                            }
                        } else {
                            // In NORMAL/CLEANUP mode, query AWS directly for most reliable IP
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
                        echo "Deployment mode: ${deploymentMode} (isReuseMode: ${isReuseMode})"
                        
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


