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

                        # Retry terraform init up to 3 times
                        MAX_RETRIES=3
                        RETRY_COUNT=0

                        while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
                            echo "Attempt $((RETRY_COUNT + 1)) of $MAX_RETRIES..."

                            if terraform init -input=false -upgrade; then
                                echo "Terraform init successful!"
                                break
                            else
                                RETRY_COUNT=$((RETRY_COUNT + 1))
                                if [ $RETRY_COUNT -lt $MAX_RETRIES ]; then
                                    echo "Terraform init failed, retrying in 10 seconds..."
                                    sleep 10
                                else
                                    echo "Terraform init failed after $MAX_RETRIES attempts"
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


