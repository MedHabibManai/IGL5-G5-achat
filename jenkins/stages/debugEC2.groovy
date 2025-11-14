// BOM Fix
// jenkins/stages/debugEC2.groovy
def call() {
    stage('Debug EC2 Instance') {
        when {
        expression { return fileExists('terraform/main.tf') }
        }
        echo '========================================='
        echo 'Stage 13.5: Debug EC2 Instance'
        echo '========================================='
        withCredentials([file(credentialsId: "${AWS_CREDENTIAL_ID}", variable: 'AWS_CREDENTIALS_FILE')]) {
        dir(TERRAFORM_DIR) {
        sh '''
        echo "Setting up AWS credentials..."
        mkdir -p ~/.aws
        cp $AWS_CREDENTIALS_FILE ~/.aws/credentials
        chmod 600 ~/.aws/credentials
        echo "======================================"
        echo "Fetching EC2 Instance Information"
        echo "======================================"
        # Get instance ID from Terraform output
        INSTANCE_ID=$(terraform output -raw instance_id 2>/dev/null || echo "")
        if [ -n "$INSTANCE_ID" ]; then
        echo "Instance ID: $INSTANCE_ID"
        echo ""
        # Wait for instance to initialize
        echo "Waiting for instance to initialize (60 seconds)..."
        sleep 60
        # Get instance status
        echo "Instance Status:"
        aws ec2 describe-instance-status --region ${AWS_REGION} --instance-ids $INSTANCE_ID || echo "Status not available yet"
        echo ""
        # Try to get console output multiple times
        echo "======================================"
        echo "EC2 Console Output (Last 100 lines):"
        echo "======================================"
        MAX_ATTEMPTS=3
        for i in $(seq 1 $MAX_ATTEMPTS); do
        echo "Attempt $i of $MAX_ATTEMPTS to fetch console output..."
        CONSOLE_OUTPUT=$(aws ec2 get-console-output --region ${AWS_REGION} --instance-id $INSTANCE_ID --output text 2>/dev/null | tail -n 100)
        if [ -n "$CONSOLE_OUTPUT" ] && [ "$CONSOLE_OUTPUT" != "$INSTANCE_ID" ]; then
        echo "$CONSOLE_OUTPUT"
        break
        else
        echo "Console output not available yet, waiting 30 seconds..."
        sleep 30
        fi
        done
        if [ -z "$CONSOLE_OUTPUT" ] || [ "$CONSOLE_OUTPUT" = "$INSTANCE_ID" ]; then
        echo "WARNING: Console output is still not available after $MAX_ATTEMPTS attempts"
        echo "This is normal for new instances. Logs will be available in a few minutes."
        fi
        echo ""
        echo "======================================"
        else
        echo "Could not retrieve instance ID from Terraform"
        fi
        '''
        }
    }
}
return this