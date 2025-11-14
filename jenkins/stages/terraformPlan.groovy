// BOM Fix
// jenkins/stages/terraformPlan.groovy
def call() {
    stage('Terraform Plan') {
        if (!fileExists('terraform/main.tf') || params.DEPLOYMENT_MODE == 'REUSE_INFRASTRUCTURE') {
            echo 'Skipping Terraform Plan: configuration missing or deployment mode reuses infra'
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


