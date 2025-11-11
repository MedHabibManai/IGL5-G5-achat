pipeline {
    agent any

    tools {
        maven 'Maven-3.8.6'  // Configure this in Jenkins Global Tool Configuration
        jdk 'JDK-8'          // Configure this in Jenkins Global Tool Configuration
    }
    
    environment {
        // Maven settings
        MAVEN_OPTS = '-Xmx1024m'
        
        // Project information from pom.xml
        PROJECT_NAME = 'achat'
        PROJECT_VERSION = '1.0'
        
        // Artifact naming
        ARTIFACT_NAME = "${PROJECT_NAME}-${PROJECT_VERSION}-${BUILD_NUMBER}.jar"
        
        // SonarQube (Phase 2)
        SONAR_HOST_URL = 'http://sonarqube-server:9000'
        SONAR_PROJECT_KEY = 'achat'
        SONAR_PROJECT_NAME = 'Achat Application'
        
        // Nexus (Phase 3)
        NEXUS_URL = 'nexus-repository:8081'
        NEXUS_REPOSITORY = 'maven-releases'
        NEXUS_CREDENTIAL_ID = 'nexus-credentials'
        
        // Docker (Phase 4)
        DOCKER_IMAGE_NAME = 'achat-app'
        DOCKER_IMAGE_TAG = "${BUILD_NUMBER}"
        DOCKER_IMAGE = "${DOCKER_IMAGE_NAME}:${DOCKER_IMAGE_TAG}"
        DOCKER_REGISTRY = 'docker.io'  // Docker Hub
        DOCKER_CREDENTIAL_ID = 'docker-hub-credentials'

        // AWS & Terraform (Phase 5)
        AWS_REGION = 'us-east-1'
        AWS_CREDENTIAL_ID = 'aws-sandbox-credentials'
        TERRAFORM_DIR = 'terraform'
        TF_VAR_docker_image = "${DOCKER_REGISTRY}/rayenslouma/${DOCKER_IMAGE}"
    }
    
    stages {
        stage('Checkout') {
            steps {
                script {
                    echo '========================================='
                    echo 'Stage 1: Checking out code from GitHub'
                    echo '========================================='
                }
                
                // Checkout code from GitHub
                checkout scm
                
                script {
                    echo "âœ“ Successfully checked out branch: ${env.GIT_BRANCH}"
                    echo "âœ“ Commit: ${env.GIT_COMMIT}"
                }
            }
        }
        
        stage('Build') {
            steps {
                script {
                    echo '========================================='
                    echo 'Stage 2: Building the application'
                    echo '========================================='
                }
                
                // Clean and compile the project
                sh 'mvn clean compile'
                
                script {
                    echo 'âœ“ Build completed successfully'
                }
            }
        }
        
        stage('Unit Tests') {
            steps {
                script {
                    echo '========================================='
                    echo 'Stage 3: Running Unit Tests'
                    echo '========================================='
                }
                
                // Run tests
                sh 'mvn test'
                
                script {
                    echo 'âœ“ All unit tests passed'
                }
            }
            
            post {
                always {
                    // Publish JUnit test results
                    junit '**/target/surefire-reports/*.xml'
                    
                    script {
                        echo 'âœ“ Test results published'
                    }
                }
            }
        }
        
        stage('Package') {
            steps {
                script {
                    echo '========================================='
                    echo 'Stage 4: Packaging the application'
                    echo '========================================='
                }
                
                // Package the application
                sh 'mvn package -DskipTests'
                
                script {
                    echo "âœ“ Application packaged: ${ARTIFACT_NAME}"
                }
            }
            
            post {
                success {
                    // Archive the artifacts
                    archiveArtifacts artifacts: '**/target/*.jar', fingerprint: true
                    
                    script {
                        echo 'âœ“ Artifacts archived successfully'
                    }
                }
            }
        }
        
        stage('Code Quality Analysis - SonarQube') {
            steps {
                script {
                    echo '========================================='
                    echo 'Stage 5: SonarQube Code Quality Analysis'
                    echo '========================================='
                }

                // Run SonarQube analysis
                withSonarQubeEnv('SonarQube') {
                    sh '''
                        mvn sonar:sonar \
                          -Dsonar.projectKey=${SONAR_PROJECT_KEY} \
                          -Dsonar.projectName="${SONAR_PROJECT_NAME}" \
                          -Dsonar.host.url=${SONAR_HOST_URL} \
                          -Dsonar.java.binaries=target/classes
                    '''
                }

                script {
                    echo 'âœ“ SonarQube analysis completed'
                    echo "âœ“ View results at: ${SONAR_HOST_URL}/dashboard?id=${SONAR_PROJECT_KEY}"
                }
            }
        }
        
        stage('Quality Gate') {
            steps {
                script {
                    echo '========================================='
                    echo 'Stage 6: Waiting for Quality Gate'
                    echo '========================================='
                }

                // Wait for SonarQube Quality Gate result
                timeout(time: 5, unit: 'MINUTES') {
                    script {
                        def qg = waitForQualityGate()
                        if (qg.status != 'OK') {
                            echo "âš  Quality Gate status: ${qg.status}"
                            echo "âš  Pipeline will continue but code quality needs attention"
                        } else {
                            echo 'âœ“ Quality Gate passed!'
                        }
                    }
                }
            }
        }
        
        stage('Deploy to Nexus') {
            steps {
                script {
                    echo '========================================='
                    echo 'Stage 7: Deploying artifacts to Nexus'
                    echo '========================================='
                }

                // Create Maven settings.xml with Nexus credentials
                withCredentials([usernamePassword(credentialsId: "${NEXUS_CREDENTIAL_ID}",
                                                  usernameVariable: 'NEXUS_USER',
                                                  passwordVariable: 'NEXUS_PASS')]) {
                    sh '''
                        # Create settings.xml with Nexus credentials
                        cat > settings.xml << EOF
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
  <servers>
    <server>
      <id>nexus-releases</id>
      <username>${NEXUS_USER}</username>
      <password>${NEXUS_PASS}</password>
    </server>
    <server>
      <id>nexus-snapshots</id>
      <username>${NEXUS_USER}</username>
      <password>${NEXUS_PASS}</password>
    </server>
  </servers>
</settings>
EOF

                        # Deploy to Nexus
                        mvn deploy -DskipTests -s settings.xml
                    '''
                }

                script {
                    echo 'âœ“ Artifacts deployed to Nexus successfully'
                    echo "âœ“ View artifacts at: http://localhost:8081/#browse/browse:maven-releases"
                }
            }
        }
        
        stage('Build Docker Image') {
            when {
                expression { return fileExists('Dockerfile') }
            }
            steps {
                script {
                    echo '========================================='
                    echo 'Stage 8: Building Docker Image'
                    echo '========================================='
                }

                // Build Docker image
                script {
                    echo "Building Docker image: ${DOCKER_IMAGE}"
                    echo "JAR file: target/${PROJECT_NAME}-${PROJECT_VERSION}.jar"

                    // Verify JAR exists
                    sh "ls -lh target/${PROJECT_NAME}-${PROJECT_VERSION}.jar"

                    // Build the image
                    sh """
                        docker build \
                          --build-arg JAR_FILE=target/${PROJECT_NAME}-${PROJECT_VERSION}.jar \
                          --build-arg BUILD_NUMBER=${BUILD_NUMBER} \
                          -t ${DOCKER_IMAGE} \
                          -t ${DOCKER_IMAGE_NAME}:latest \
                          .
                    """

                    echo "âœ“ Docker image built successfully!"
                    echo "  - ${DOCKER_IMAGE}"
                    echo "  - ${DOCKER_IMAGE_NAME}:latest"

                    // Show image details
                    sh "docker images | grep ${DOCKER_IMAGE_NAME}"
                }
            }
        }
        
        stage('Push Docker Image') {
            when {
                expression { return fileExists('Dockerfile') }
            }
            steps {
                script {
                    echo '========================================='
                    echo 'Stage 9: Pushing Docker Image to Registry'
                    echo '========================================='
                }

                // Push to Docker registry
                withCredentials([usernamePassword(credentialsId: "${DOCKER_CREDENTIAL_ID}",
                                                  usernameVariable: 'DOCKER_USER',
                                                  passwordVariable: 'DOCKER_PASS')]) {
                    script {
                        echo "Logging in to Docker Hub as ${DOCKER_USER}..."

                        sh '''
                            echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin
                        '''

                        echo "Tagging images for Docker Hub..."

                        // Tag with username prefix for Docker Hub
                        sh """
                            docker tag ${DOCKER_IMAGE} ${DOCKER_USER}/${DOCKER_IMAGE}
                            docker tag ${DOCKER_IMAGE_NAME}:latest ${DOCKER_USER}/${DOCKER_IMAGE_NAME}:latest
                        """

                        echo "Pushing images to Docker Hub..."

                        sh """
                            docker push ${DOCKER_USER}/${DOCKER_IMAGE}
                            docker push ${DOCKER_USER}/${DOCKER_IMAGE_NAME}:latest
                        """

                        echo "âœ“ Docker images pushed successfully!"
                        echo "  - ${DOCKER_USER}/${DOCKER_IMAGE}"
                        echo "  - ${DOCKER_USER}/${DOCKER_IMAGE_NAME}:latest"
                        echo ""
                        echo "View on Docker Hub: https://hub.docker.com/r/${DOCKER_USER}/${DOCKER_IMAGE_NAME}"
                    }
                }
            }
        }

        stage('Verify AWS Credentials') {
            when {
                expression { return fileExists('terraform/main.tf') }
            }
            steps {
                script {
                    echo '========================================='
                    echo 'Stage 10: Verify AWS Credentials'
                    echo '========================================='

                    // Check if AWS credentials file exists
                    def awsCredsFile = '/var/jenkins_home/.aws/credentials'
                    def awsConfigFile = '/var/jenkins_home/.aws/config'

                    echo "Checking AWS credentials..."

                    // Test current credentials
                    def credentialsValid = sh(
                        script: '''
                            export AWS_SHARED_CREDENTIALS_FILE=/var/jenkins_home/.aws/credentials
                            export AWS_CONFIG_FILE=/var/jenkins_home/.aws/config
                            aws sts get-caller-identity > /dev/null 2>&1
                            echo $?
                        ''',
                        returnStdout: true
                    ).trim()

                    if (credentialsValid == '0') {
                        echo "✓ AWS credentials are valid!"

                        // Display current identity
                        sh '''
                            export AWS_SHARED_CREDENTIALS_FILE=/var/jenkins_home/.aws/credentials
                            export AWS_CONFIG_FILE=/var/jenkins_home/.aws/config
                            echo ""
                            echo "Current AWS Identity:"
                            aws sts get-caller-identity
                            echo ""
                        '''

                        // Check session expiration for temporary credentials
                        def hasSessionToken = sh(
                            script: 'grep -q "aws_session_token" /var/jenkins_home/.aws/credentials && echo "true" || echo "false"',
                            returnStdout: true
                        ).trim()

                        if (hasSessionToken == 'true') {
                            echo "⚠ Using temporary AWS Academy credentials"
                            echo "  These credentials expire after a few hours"
                            echo "  If deployment fails, update credentials from AWS Academy"
                        }
                    } else {
                        echo "✗ AWS credentials are invalid or expired!"
                        echo ""
                        echo "========================================="
                        echo "ACTION REQUIRED: Update AWS Credentials"
                        echo "========================================="
                        echo ""
                        echo "1. Go to AWS Academy Learner Lab"
                        echo "2. Click 'AWS Details' → 'Show' under 'AWS CLI'"
                        echo "3. Copy the credentials"
                        echo "4. Update the credentials file on Jenkins:"
                        echo ""
                        echo "   docker exec -it jenkins-cicd bash"
                        echo "   vi ~/.aws/credentials"
                        echo ""
                        echo "   Paste the credentials in this format:"
                        echo "   [default]"
                        echo "   aws_access_key_id=YOUR_ACCESS_KEY"
                        echo "   aws_secret_access_key=YOUR_SECRET_KEY"
                        echo "   aws_session_token=YOUR_SESSION_TOKEN"
                        echo ""
                        echo "5. Save and run the pipeline again"
                        echo ""
                        echo "========================================="

                        error("AWS credentials are invalid or expired. Please update them.")
                    }
                }
            }
        }

        stage('Cleanup Previous Deployment') {
            when {
                expression { return fileExists('terraform/main.tf') }
            }
            steps {
                script {
                    echo '========================================='
                    echo 'Stage 11: Cleanup Previous Deployment'
                    echo '========================================='
                    echo ''
                    echo '🧹 Searching for existing Terraform-managed VPCs...'
                }

                script {
                    // Find all VPCs with Name tag = "achat-app-vpc"
                    def vpcIds = sh(
                        script: '''
                            export AWS_SHARED_CREDENTIALS_FILE=/var/jenkins_home/.aws/credentials
                            export AWS_CONFIG_FILE=/var/jenkins_home/.aws/config
                            export AWS_PAGER=""

                            aws ec2 describe-vpcs \
                              --filters "Name=tag:Name,Values=achat-app-vpc" \
                              --query "Vpcs[*].VpcId" \
                              --output text
                        ''',
                        returnStdout: true
                    ).trim()

                    if (vpcIds) {
                        echo "⚠️  Found existing Terraform VPCs: ${vpcIds}"
                        echo ""
                        echo "Deleting all Terraform-managed resources..."
                        echo ""

                        // Delete each VPC and its dependencies
                        vpcIds.split().each { vpcId ->
                            echo "─────────────────────────────────────────"
                            echo "Deleting VPC: ${vpcId}"
                            echo ""

                            sh """
                                export AWS_SHARED_CREDENTIALS_FILE=/var/jenkins_home/.aws/credentials
                                export AWS_CONFIG_FILE=/var/jenkins_home/.aws/config
                                export AWS_PAGER=""

                                # 1. Terminate EC2 instances
                                echo "  1. Checking for EC2 instances..."
                                INSTANCE_IDS=\$(aws ec2 describe-instances \
                                  --filters "Name=vpc-id,Values=${vpcId}" "Name=instance-state-name,Values=running,stopped,stopping" \
                                  --query "Reservations[*].Instances[*].InstanceId" \
                                  --output text)

                                if [ -n "\$INSTANCE_IDS" ]; then
                                  echo "     Terminating instances: \$INSTANCE_IDS"
                                  aws ec2 terminate-instances --instance-ids \$INSTANCE_IDS > /dev/null
                                  echo "     Waiting for instances to terminate..."
                                  aws ec2 wait instance-terminated --instance-ids \$INSTANCE_IDS || true
                                  echo "     ✓ Instances terminated"
                                else
                                  echo "     No instances found"
                                fi

                                # 2. Delete security groups (except default)
                                echo "  2. Deleting security groups..."
                                SG_IDS=\$(aws ec2 describe-security-groups \
                                  --filters "Name=vpc-id,Values=${vpcId}" \
                                  --query "SecurityGroups[?GroupName!='default'].GroupId" \
                                  --output text)

                                if [ -n "\$SG_IDS" ]; then
                                  for sg_id in \$SG_IDS; do
                                    echo "     Deleting SG: \$sg_id"
                                    aws ec2 delete-security-group --group-id \$sg_id 2>/dev/null || echo "     (already deleted or in use)"
                                  done
                                else
                                  echo "     No security groups to delete"
                                fi

                                # 3. Delete subnets
                                echo "  3. Deleting subnets..."
                                SUBNET_IDS=\$(aws ec2 describe-subnets \
                                  --filters "Name=vpc-id,Values=${vpcId}" \
                                  --query "Subnets[*].SubnetId" \
                                  --output text)

                                if [ -n "\$SUBNET_IDS" ]; then
                                  for subnet_id in \$SUBNET_IDS; do
                                    echo "     Deleting subnet: \$subnet_id"
                                    aws ec2 delete-subnet --subnet-id \$subnet_id 2>/dev/null || echo "     (already deleted)"
                                  done
                                else
                                  echo "     No subnets to delete"
                                fi

                                # 4. Detach and delete internet gateways
                                echo "  4. Deleting internet gateways..."
                                IGW_IDS=\$(aws ec2 describe-internet-gateways \
                                  --filters "Name=attachment.vpc-id,Values=${vpcId}" \
                                  --query "InternetGateways[*].InternetGatewayId" \
                                  --output text)

                                if [ -n "\$IGW_IDS" ]; then
                                  for igw_id in \$IGW_IDS; do
                                    echo "     Detaching IGW: \$igw_id"
                                    aws ec2 detach-internet-gateway --internet-gateway-id \$igw_id --vpc-id ${vpcId} 2>/dev/null || echo "     (already detached)"
                                    echo "     Deleting IGW: \$igw_id"
                                    aws ec2 delete-internet-gateway --internet-gateway-id \$igw_id 2>/dev/null || echo "     (already deleted)"
                                  done
                                else
                                  echo "     No internet gateways to delete"
                                fi

                                # 5. Delete route tables (except main)
                                echo "  5. Deleting route tables..."
                                RT_IDS=\$(aws ec2 describe-route-tables \
                                  --filters "Name=vpc-id,Values=${vpcId}" \
                                  --query 'RouteTables[?Associations[0].Main==`false`].RouteTableId' \
                                  --output text)

                                if [ -n "\$RT_IDS" ]; then
                                  for rt_id in \$RT_IDS; do
                                    echo "     Deleting route table: \$rt_id"
                                    aws ec2 delete-route-table --route-table-id \$rt_id 2>/dev/null || echo "     (already deleted)"
                                  done
                                else
                                  echo "     No route tables to delete"
                                fi

                                # 6. Delete VPC
                                echo "  6. Deleting VPC..."
                                aws ec2 delete-vpc --vpc-id ${vpcId} && echo "     ✓ VPC deleted!" || echo "     ⚠️  VPC deletion failed (may have dependencies)"
                                echo ""
                            """
                        }

                        echo "════════════════════════════════════════"
                        echo "✅ Cleanup complete!"
                        echo "════════════════════════════════════════"
                    } else {
                        echo "✓ No existing Terraform VPCs found - starting fresh deployment"
                    }

                    echo ""
                }

                dir(TERRAFORM_DIR) {
                    script {
                        echo "Cleaning up local Terraform files..."
                        sh '''
                            rm -f terraform.tfstate*
                            rm -f tfplan
                            rm -rf .terraform/
                        '''
                        echo "✓ Local cleanup complete!"
                    }
                }
            }
        }

        stage('Terraform Init') {
            when {
                expression { return fileExists('terraform/main.tf') }
            }
            steps {
                script {
                    echo '========================================='
                    echo 'Stage 12: Terraform Initialization'
                    echo '========================================='
                }

                dir(TERRAFORM_DIR) {
                    sh '''
                        # Set AWS credentials path
                        export AWS_SHARED_CREDENTIALS_FILE=/var/jenkins_home/.aws/credentials
                        export AWS_CONFIG_FILE=/var/jenkins_home/.aws/config

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
                        aws --version

                        echo ""
                        echo "AWS Account:"
                        aws sts get-caller-identity
                    '''
                }

                script {
                    echo 'Terraform initialized successfully'
                }
            }
        }

        stage('Terraform Plan') {
            when {
                expression { return fileExists('terraform/main.tf') }
            }
            steps {
                script {
                    echo '========================================='
                    echo 'Stage 13: Terraform Plan'
                    echo '========================================='
                }

                dir(TERRAFORM_DIR) {
                    sh '''
                        # Set AWS credentials path
                        export AWS_SHARED_CREDENTIALS_FILE=/var/jenkins_home/.aws/credentials
                        export AWS_CONFIG_FILE=/var/jenkins_home/.aws/config

                        echo "Creating Terraform execution plan..."
                        terraform plan \
                          -var="docker_image=${TF_VAR_docker_image}" \
                          -out=tfplan \
                          -input=false

                        echo ""
                        echo "Plan saved to: tfplan"
                    '''
                }

                script {
                    echo 'Terraform plan created successfully'
                    echo 'Review the plan above before applying'
                }
            }
        }

        stage('Terraform Apply') {
            when {
                expression { return fileExists('terraform/main.tf') }
            }
            steps {
                script {
                    echo '========================================='
                    echo 'Stage 14: Terraform Apply (Deploy to AWS)'
                    echo '========================================='
                }

                // Ask for approval before deploying to AWS (optional)
                // Uncomment the following lines to require manual approval
                // input {
                //     message "Deploy to AWS?"
                //     ok "Deploy"
                // }

                dir(TERRAFORM_DIR) {
                    sh '''
                        # Set AWS credentials path
                        export AWS_SHARED_CREDENTIALS_FILE=/var/jenkins_home/.aws/credentials
                        export AWS_CONFIG_FILE=/var/jenkins_home/.aws/config

                        echo "Applying Terraform plan..."
                        terraform apply -auto-approve tfplan

                        echo ""
                        echo "Deployment complete!"
                    '''
                }

                script {
                    echo 'Infrastructure deployed to AWS successfully'
                }
            }
        }

        stage('Get AWS Deployment Info') {
            when {
                expression { return fileExists('terraform/main.tf') }
            }
            steps {
                script {
                    echo '========================================='
                    echo 'Stage 15: AWS Deployment Information'
                    echo '========================================='
                }

                dir(TERRAFORM_DIR) {
                    script {
                        // Display deployment summary from Terraform
                        def summary = sh(
                            script: 'terraform output -raw deployment_summary 2>/dev/null || echo "Deployment completed"',
                            returnStdout: true
                        ).trim()

                        echo ""
                        echo summary
                        echo ""

                        // Get key outputs
                        def appUrl = sh(
                            script: 'terraform output -raw application_url 2>/dev/null || echo ""',
                            returnStdout: true
                        ).trim()

                        def publicIp = sh(
                            script: 'terraform output -raw public_ip 2>/dev/null || echo ""',
                            returnStdout: true
                        ).trim()

                        def healthUrl = sh(
                            script: 'terraform output -raw health_check_url 2>/dev/null || echo ""',
                            returnStdout: true
                        ).trim()

                        if (appUrl) {
                            echo ""
                            echo "========================================="
                            echo "APPLICATION DEPLOYED SUCCESSFULLY!"
                            echo "========================================="
                            echo "Application URL: ${appUrl}"

                            if (publicIp) {
                                echo "Public IP: ${publicIp}"
                            }

                            if (healthUrl) {
                                echo "Health Check: ${healthUrl}"
                            }

                            echo "========================================="
                            echo ""
                        }
                    }
                }
            }
        }

        stage('Health Check AWS Deployment') {
            when {
                expression { return fileExists('terraform/main.tf') }
            }
            steps {
                script {
                    echo '========================================='
                    echo 'Stage 16: Health Check'
                    echo '========================================='
                }

                dir(TERRAFORM_DIR) {
                    script {
                        // Get application URL from Terraform output
                        def appUrl = sh(
                            script: 'terraform output -raw application_url 2>/dev/null || echo ""',
                            returnStdout: true
                        ).trim()

                        if (appUrl) {
                            echo "Waiting for application to start (60 seconds)..."
                            sleep(60)

                            echo "Checking application health..."
                            def healthUrl = "${appUrl}/actuator/health"

                            retry(5) {
                                sleep(10)
                                sh """
                                    curl -f ${healthUrl} || exit 1
                                """
                            }

                            echo ""
                            echo "Application is healthy and responding!"
                        } else {
                            echo "Could not retrieve application URL"
                        }
                    }
                }
            }
        }

        stage('Deploy to Kubernetes') {
            when {
                branch 'main'
                expression { return fileExists('k8s/deployment.yaml') }
            }
            steps {
                script {
                    echo '========================================='
                    echo 'Stage 10: Deploying to Kubernetes'
                    echo '========================================='
                    echo 'Note: This stage requires Phase 5 setup'
                }
                
                // Deploy to Kubernetes
                withKubeConfig([credentialsId: 'kubeconfig-credentials']) {
                    sh '''
                        kubectl apply -f k8s/
                        kubectl rollout status deployment/achat-app
                    '''
                }
                
                script {
                    echo 'âœ“ Application deployed to Kubernetes'
                }
            }
        }
    }
    
    post {
        always {
            script {
                echo '========================================='
                echo 'Pipeline Execution Summary'
                echo '========================================='
                echo "Build Number: ${BUILD_NUMBER}"
                echo "Build Status: ${currentBuild.currentResult}"
                echo "Duration: ${currentBuild.durationString}"
            }
            
            // Clean workspace
            cleanWs()
        }
        
        success {
            script {
                echo 'âœ“âœ“âœ“ Pipeline completed successfully! âœ“âœ“âœ“'
            }
            
            // Send notification (optional - requires email plugin)
            // emailext (
            //     subject: "SUCCESS: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
            //     body: "Good news! The build ${env.BUILD_NUMBER} completed successfully.",
            //     to: 'your-email@example.com'
            // )
        }
        
        failure {
            script {
                echo '✗✗✗ Pipeline failed! ✗✗✗'
            }
            
            // Send notification (optional - requires email plugin)
            // emailext (
            //     subject: "FAILURE: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
            //     body: "Build ${env.BUILD_NUMBER} failed. Please check the console output.",
            //     to: 'your-email@example.com'
            // )
        }
        
        unstable {
            script {
                echo 'âš  Pipeline is unstable âš '
            }
        }
    }
}

