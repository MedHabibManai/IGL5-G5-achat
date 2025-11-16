// BOM Fix
// jenkins/stages/terraformPlan.groovy
def call() {
    stage('Terraform Plan') {
        // Check both env.DEPLOYMENT_MODE (set from commit message or params) and params.DEPLOYMENT_MODE (fallback)
        def deploymentMode = env.DEPLOYMENT_MODE ?: params.DEPLOYMENT_MODE
        if (!fileExists('terraform/main.tf') || deploymentMode == 'REUSE_INFRASTRUCTURE') {
            echo "Skipping Terraform Plan: configuration missing or deployment mode reuses infra (mode: ${deploymentMode})"
            return
        }

                echo '========================================='
                echo 'Stage 11: Terraform Plan'
                echo '========================================='

            withCredentials([file(credentialsId: "${AWS_CREDENTIAL_ID}", variable: 'AWS_CREDENTIALS_FILE')]) {
                dir(TERRAFORM_DIR) {
                    sh '''
                        echo "Setting up AWS credentials..."
                        mkdir -p ~/.aws
                        cp $AWS_CREDENTIALS_FILE ~/.aws/credentials
                        chmod 600 ~/.aws/credentials
                        
                        # Also export as environment variables for Terraform
                        # Terraform AWS provider prefers environment variables
                        export AWS_ACCESS_KEY_ID=$(grep aws_access_key_id "$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                        export AWS_SECRET_ACCESS_KEY=$(grep aws_secret_access_key "$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                        export AWS_SESSION_TOKEN=$(grep aws_session_token "$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ' || echo "")
                        export AWS_DEFAULT_REGION=us-east-1
                        
                        # Verify credentials are set
                        if [ -z "$AWS_ACCESS_KEY_ID" ] || [ -z "$AWS_SECRET_ACCESS_KEY" ]; then
                            echo "ERROR: Failed to parse AWS credentials from file"
                            exit 1
                        fi
                        
                        # Show first 10 chars of access key (POSIX-compatible)
                        ACCESS_KEY_PREFIX=$(echo "$AWS_ACCESS_KEY_ID" | cut -c1-10)
                        echo "AWS credentials configured (Access Key ID: ${ACCESS_KEY_PREFIX}...)"
                        if [ -n "$AWS_SESSION_TOKEN" ]; then
                            echo "Session token is set"
                        fi
                        
                        echo "Creating Terraform execution plan..."
                        
                        # Retry terraform plan up to 3 times with exponential backoff
                        MAX_RETRIES=3
                        RETRY_COUNT=0
                        SUCCESS=false
                        
                        while [ $RETRY_COUNT -lt $MAX_RETRIES ] && [ "$SUCCESS" = false ]; do
                            RETRY_COUNT=$((RETRY_COUNT + 1))
                            echo ""
                            echo "=========================================="
                            echo "Terraform Plan - Attempt $RETRY_COUNT of $MAX_RETRIES"
                            echo "=========================================="
                            
                            if terraform plan \
                              -var="docker_image=${TF_VAR_docker_image}" \
                              -out=tfplan \
                              -input=false; then
                                echo ""
                                echo "Terraform plan created successfully!"
                                SUCCESS=true
                            else
                                EXIT_CODE=$?
                                echo ""
                                echo "Terraform plan failed (exit code: $EXIT_CODE)"
                                
                                if [ $RETRY_COUNT -lt $MAX_RETRIES ]; then
                                    # Calculate delay: 10s, 20s, 40s (using case for sh compatibility)
                                    case $RETRY_COUNT in
                                        1) DELAY=10 ;;
                                        2) DELAY=20 ;;
                                        3) DELAY=40 ;;
                                        *) DELAY=10 ;;
                                    esac
                                    echo "Retrying in ${DELAY} seconds..."
                                    echo "(This may be a temporary network/TLS issue)"
                                    sleep $DELAY
                                else
                                    echo ""
                                    echo "Terraform plan failed after $MAX_RETRIES attempts"
                                    exit $EXIT_CODE
                                fi
                            fi
                        done

                        echo ""
                        echo "Plan saved to: tfplan"
                    '''
                }
            }

                echo 'Terraform plan created successfully'
                echo 'Review the plan above before applying'
    }
}
return this


