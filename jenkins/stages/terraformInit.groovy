// BOM Fix
// jenkins/stages/terraformInit.groovy
def call() {
    stage('Terraform Init') {
        if (!fileExists('terraform/main.tf') || params.DEPLOYMENT_MODE == 'REUSE_INFRASTRUCTURE') {
            echo 'Skipping Terraform Init: configuration missing or deployment mode reuses infra'
            return
        }

                echo '========================================='
                echo 'Stage 10: Terraform Initialization'
                echo '========================================='

            withCredentials([file(credentialsId: "${AWS_CREDENTIAL_ID}", variable: 'AWS_CREDENTIALS_FILE')]) {
                dir(TERRAFORM_DIR) {
                    sh '''
                        echo "Setting up AWS credentials..."
                        mkdir -p ~/.aws
                        cp $AWS_CREDENTIALS_FILE ~/.aws/credentials
                        chmod 600 ~/.aws/credentials
                        
                        echo "Initializing Terraform..."

                        # Retry terraform init with exponential backoff
                        # Network issues with Terraform registry can be transient
                        MAX_RETRIES=7
                        RETRY_COUNT=0
                        INIT_SUCCESS=false

                        while [ $RETRY_COUNT -lt $MAX_RETRIES ] && [ "$INIT_SUCCESS" != "true" ]; do
                            RETRY_COUNT=$((RETRY_COUNT + 1))
                            echo ""
                            echo "=========================================="
                            echo "Attempt $RETRY_COUNT of $MAX_RETRIES..."
                            echo "=========================================="
                            
                            # On first attempt, try with upgrade. On retries, skip upgrade to use cache if available
                            if [ $RETRY_COUNT -eq 1 ]; then
                                INIT_CMD="terraform init -input=false -upgrade"
                            else
                                INIT_CMD="terraform init -input=false"
                                echo "Skipping -upgrade flag to use cached providers if available"
                            fi
                            
                            if $INIT_CMD; then
                                echo ""
                                echo "✅ Terraform init successful!"
                                INIT_SUCCESS=true
                            else
                                if [ $RETRY_COUNT -lt $MAX_RETRIES ]; then
                                    # Exponential backoff: 15s, 30s, 60s, 120s, 180s, 240s
                                    WAIT_TIME=$((15 * (2 ** (RETRY_COUNT - 1))))
                                    if [ $WAIT_TIME -gt 240 ]; then
                                        WAIT_TIME=240
                                    fi
                                    echo ""
                                    echo "⚠️  Terraform init failed (likely network timeout)"
                                    echo "    Waiting ${WAIT_TIME} seconds before retry..."
                                    echo "    This is attempt $RETRY_COUNT of $MAX_RETRIES"
                                    sleep $WAIT_TIME
                                else
                                    echo ""
                                    echo "❌ Terraform init failed after $MAX_RETRIES attempts"
                                    echo "    This is likely a network connectivity issue from Jenkins to Terraform registry"
                                    echo "    Please check:"
                                    echo "    1. Jenkins server network connectivity"
                                    echo "    2. Firewall/proxy settings"
                                    echo "    3. Terraform registry status (registry.terraform.io)"
                                    exit 1
                                fi
                            fi
                        done

                        echo ""
                        echo "Terraform version:"
                        terraform version

                        echo ""
                        echo "AWS CLI version:"
                        /usr/local/bin/aws --version

                        echo ""
                        echo "Verifying AWS credentials (non-fatal)..."
                        if /usr/local/bin/aws sts get-caller-identity 2>&1; then
                            echo "AWS credentials verified successfully"
                        else
                            echo "WARNING: AWS credential verification failed, but terraform init succeeded"
                            echo "This is non-fatal - credentials will be verified again during terraform plan/apply"
                        fi
                    '''
                }
            }

                echo 'Terraform initialized successfully'
    }
}
return this


