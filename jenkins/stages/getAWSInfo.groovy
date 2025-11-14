// BOM Fix
// jenkins/stages/getAWSInfo.groovy
def call() {
    stage('Get AWS Deployment Info') {
        when {
            expression { return fileExists('terraform/main.tf') }
        }
        steps {
            script {
                echo '========================================='
                echo 'Stage 13: AWS Deployment Information'
                echo '========================================='
            }

            dir(TERRAFORM_DIR) {
                script {
                    // Get outputs from Terraform
                    def outputs = sh(
                        script: 'terraform output -json',
                        returnStdout: true
                    ).trim()

                    echo "Terraform Outputs:"
                    echo outputs

                    // Extract key information using shell commands
                    def appUrl = sh(
                        script: "terraform output -raw application_url 2>/dev/null || echo 'N/A'",
                        returnStdout: true
                    ).trim()

                    def healthCheckUrl = sh(
                        script: "terraform output -raw health_check_url 2>/dev/null || echo 'N/A'",
                        returnStdout: true
                    ).trim()

                    def publicIp = sh(
                        script: "terraform output -raw public_ip 2>/dev/null || echo 'N/A'",
                        returnStdout: true
                    ).trim()

                    def instanceId = sh(
                        script: "terraform output -raw instance_id 2>/dev/null || echo 'N/A'",
                        returnStdout: true
                    ).trim()

                    if (appUrl != 'N/A') {
                        echo ""
                        echo "========================================="
                        echo "APPLICATION DEPLOYED SUCCESSFULLY!"
                        echo "========================================="
                        echo "Application URL: ${appUrl}"
                        echo "Health Check: ${healthCheckUrl}"
                        echo "Public IP: ${publicIp}"
                        echo "Instance ID: ${instanceId}"
                        echo "========================================="
                    
                    }
                }
            }

        }
    }
}
return this
