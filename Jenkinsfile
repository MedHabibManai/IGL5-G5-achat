pipeline {
    agent any

    // ============================================================================
    // PIPELINE PARAMETERS - Control deployment behavior
    // ============================================================================
    parameters {
        choice(
            name: 'DEPLOY_ACTION',
            choices: ['deploy', 'redeploy', 'destroy', 'plan-only'],
            description: '''Deployment action:
• deploy: Update existing infrastructure (incremental, fast)
• redeploy: Destroy and recreate everything (clean slate)
• destroy: Cleanup all resources (no deployment)
• plan-only: Show what would change (dry-run)'''
        )

        string(
            name: 'DOCKER_IMAGE_TAG',
            defaultValue: '',
            description: 'Docker image tag to deploy (leave empty to use BUILD_NUMBER)'
        )

        booleanParam(
            name: 'SKIP_TESTS',
            defaultValue: false,
            description: 'Skip unit tests (faster builds for testing)'
        )

        booleanParam(
            name: 'SKIP_SONARQUBE',
            defaultValue: false,
            description: 'Skip SonarQube analysis (faster builds)'
        )

        booleanParam(
            name: 'FORCE_CLEANUP',
            defaultValue: false,
            description: 'Force cleanup of old VPCs before deployment (use if VPC limit reached)'
        )

        booleanParam(
            name: 'IMPORT_EXISTING_RESOURCES',
            defaultValue: true,
            description: 'Scan and import existing AWS resources into Terraform state (recommended for first deploy)'
        )

        choice(
            name: 'LOG_LEVEL',
            choices: ['INFO', 'DEBUG', 'VERBOSE'],
            description: 'Pipeline logging verbosity'
        )
    }

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
        DOCKER_IMAGE_TAG = "${params.DOCKER_IMAGE_TAG ?: BUILD_NUMBER}"
        DOCKER_IMAGE = "${DOCKER_IMAGE_NAME}:${DOCKER_IMAGE_TAG}"
        DOCKER_REGISTRY = 'docker.io'  // Docker Hub
        DOCKER_CREDENTIAL_ID = 'docker-hub-credentials'

        // AWS & Terraform (Phase 5)
        AWS_REGION = 'us-east-1'
        AWS_CREDENTIAL_ID = 'aws-sandbox-credentials'
        TERRAFORM_DIR = 'terraform'
        TF_VAR_docker_image = "${DOCKER_REGISTRY}/rayenslouma/${DOCKER_IMAGE}"
        TF_VAR_deploy_mode = 'eks'  // Deployment mode: 'ec2', 'k8s', or 'eks'

        // Terraform State Management
        // Store state in persistent Jenkins location (outside workspace)
        TERRAFORM_STATE_DIR = "/var/jenkins_home/terraform-states/${JOB_NAME}"
    }

    stages {
        // ============================================================================
        // TEMPORARY: TESTING EARLY STAGES ONLY
        // ============================================================================
        stage('Pipeline Mode') {
            steps {
                script {
                    echo '========================================='
                    echo 'TESTING MODE: Early Stages Only'
                    echo '========================================='
                    echo 'Testing stages 1-6:'
                    echo '  1. Checkout'
                    echo '  2. Build'
                    echo '  3. Unit Tests'
                    echo '  4. SonarQube Analysis'
                    echo '  5. Quality Gate'
                    echo ''
                    echo 'Skipping stages 7-15 (Nexus, Docker, AWS)'
                    echo '========================================='
                }
            }
        }
        stage('Initialize Pipeline') {
            steps {
                script {
                    echo '========================================='
                    echo 'Stage 0: Pipeline Initialization'
                    echo '========================================='
                    echo ''
                    echo 'PIPELINE PARAMETERS:'
                    echo "   Deploy Action:         ${params.DEPLOY_ACTION}"
                    echo "   Docker Tag:            ${env.DOCKER_IMAGE_TAG}"
                    echo "   Skip Tests:            ${params.SKIP_TESTS}"
                    echo "   Skip SonarQube:        ${params.SKIP_SONARQUBE}"
                    echo "   Force Cleanup:         ${params.FORCE_CLEANUP}"
                    echo "   Import Resources:      ${params.IMPORT_EXISTING_RESOURCES}"
                    echo "   Log Level:             ${params.LOG_LEVEL}"
                    echo ''
                    echo 'ENVIRONMENT:'
                    echo "   Build Number:     ${BUILD_NUMBER}"
                    echo "   Job Name:         ${JOB_NAME}"
                    echo "   AWS Region:       ${AWS_REGION}"
                    echo "   Docker Image:     ${TF_VAR_docker_image}"
                    echo "   Deploy Mode:      ${TF_VAR_deploy_mode} (${TF_VAR_deploy_mode == 'eks' ? 'AWS EKS' : (TF_VAR_deploy_mode == 'k8s' ? 'k3s on EC2' : 'Standalone EC2')})"
                    echo ''

                    // Set up Terraform state directory
                    sh """
                        mkdir -p ${TERRAFORM_STATE_DIR}
                        echo "Terraform state directory ready: ${TERRAFORM_STATE_DIR}"
                    """

                    // Validate parameters
                    if (params.DEPLOY_ACTION == 'destroy' && params.FORCE_CLEANUP) {
                        echo 'WARNING: Both destroy and force_cleanup are enabled'
                    }

                    echo ''
                    echo 'Pipeline initialized successfully!'
                    echo '========================================='
                }
            }
        }


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
            when {
                expression { return params.DEPLOY_ACTION != 'destroy' }
            }
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
            when {
                expression { return !params.SKIP_TESTS }
            }
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
            when {
                expression { return !params.SKIP_SONARQUBE }
            }
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
            when {
                expression { return !params.SKIP_SONARQUBE }
            }
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
            when {
                expression { return false }  // SKIP: Testing early stages only
            }
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
                expression { return false }  // SKIP: Testing early stages only
                allOf {
                    expression { return fileExists('Dockerfile') }
                    expression { return params.DEPLOY_ACTION != 'destroy' }
                }
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
                expression { return false }  // SKIP: Testing early stages only
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
                expression { return false }  // SKIP: Testing early stages only
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
                expression { return false }  // SKIP: Testing early stages only
                allOf {
                    expression { return fileExists('terraform/main.tf') }
                    expression {
                        return params.DEPLOY_ACTION == 'redeploy' ||
                               params.DEPLOY_ACTION == 'destroy' ||
                               params.FORCE_CLEANUP
                    }
                }
            }
            steps {
                script {
                    echo '========================================='
                    echo 'Stage 11: Cleanup Previous Deployment'
                    echo '========================================='
                    echo ''

                    // Try to use Terraform state if available
                    if (fileExists("${TERRAFORM_STATE_DIR}/terraform.tfstate")) {
                        echo '📦 Loading Terraform state for cleanup...'
                        sh """
                            cp -v ${TERRAFORM_STATE_DIR}/terraform.tfstate ${TERRAFORM_DIR}/terraform.tfstate
                            cp -v ${TERRAFORM_STATE_DIR}/terraform.tfstate.backup ${TERRAFORM_DIR}/terraform.tfstate.backup 2>/dev/null || true
                        """

                        echo ''
                        echo '🔥 Using Terraform destroy (state-based cleanup)...'
                        echo ''

                        dir(TERRAFORM_DIR) {
                            sh '''
                                export AWS_SHARED_CREDENTIALS_FILE=/var/jenkins_home/.aws/credentials
                                export AWS_CONFIG_FILE=/var/jenkins_home/.aws/config
                                export AWS_PAGER=""

                                # Initialize Terraform
                                terraform init -input=false

                                # Destroy all resources tracked in state
                                terraform destroy -auto-approve
                            '''
                        }

                        echo ''
                        echo '✅ Terraform destroy complete!'
                        echo ''

                        // Remove state files after successful destroy
                        sh """
                            rm -f ${TERRAFORM_STATE_DIR}/terraform.tfstate*
                            rm -f ${TERRAFORM_DIR}/terraform.tfstate*
                        """

                        echo '🗑️  State files removed'

                    } else {
                        echo '⚠️  No Terraform state found - skipping cleanup'
                        echo ''
                        echo 'ℹ️  AWS Academy does not allow VPC query operations'
                        echo 'ℹ️  If you need to cleanup, use AWS Console or set DEPLOY_ACTION=destroy with existing state'
                        echo ''
                    }
                }

                script {
                    // Fallback: AWS query-based cleanup if no state exists
                    // DISABLED: AWS Academy does not allow ec2:DescribeVpcs operation
                    if (false && !fileExists("${TERRAFORM_STATE_DIR}/terraform.tfstate")) {
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
                    } // End of if (!fileExists state) block

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

        stage('Import Existing AWS Resources') {
            when {
                expression { return false }  // SKIP: Testing early stages only
                allOf {
                    expression { return fileExists('terraform/main.tf') }
                    expression { return params.DEPLOY_ACTION == 'deploy' }
                    expression { return params.IMPORT_EXISTING_RESOURCES }
                    expression { return !fileExists("${TERRAFORM_STATE_DIR}/terraform.tfstate") }
                    // DISABLED: AWS Academy does not allow EC2 describe operations
                    expression { return false }
                }
            }
            steps {
                script {
                    echo '========================================='
                    echo 'Stage 11.5: Import Existing AWS Resources'
                    echo '========================================='
                    echo ''
                    echo '🔍 Scanning AWS for existing resources to import...'
                    echo ''
                }

                dir(TERRAFORM_DIR) {
                    script {
                        sh '''
                            export AWS_SHARED_CREDENTIALS_FILE=/var/jenkins_home/.aws/credentials
                            export AWS_CONFIG_FILE=/var/jenkins_home/.aws/config
                            export AWS_PAGER=""

                            echo "Initializing Terraform for import..."
                            terraform init -input=false
                            echo ""

                            # Function to safely import resource
                            import_resource() {
                                local resource_type=$1
                                local resource_name=$2
                                local resource_id=$3

                                echo "  Checking: $resource_type.$resource_name"

                                # Check if resource already in state
                                if terraform state show "$resource_type.$resource_name" >/dev/null 2>&1; then
                                    echo "    ✓ Already in state"
                                    return 0
                                fi

                                # Try to import
                                if [ -n "$resource_id" ]; then
                                    echo "    → Importing: $resource_id"
                                    if terraform import "$resource_type.$resource_name" "$resource_id" 2>&1 | grep -q "Successfully imported"; then
                                        echo "    ✅ Imported successfully"
                                        return 0
                                    else
                                        echo "    ⚠️  Import failed or resource doesn't exist"
                                        return 1
                                    fi
                                else
                                    echo "    ℹ️  No resource ID found - will be created"
                                    return 1
                                fi
                            }

                            echo "════════════════════════════════════════"
                            echo "1. Scanning for VPC..."
                            echo "════════════════════════════════════════"

                            VPC_ID=$(aws ec2 describe-vpcs \
                              --filters "Name=tag:Name,Values=achat-app-vpc" \
                              --query "Vpcs[0].VpcId" \
                              --output text 2>/dev/null)

                            if [ "$VPC_ID" != "None" ] && [ -n "$VPC_ID" ]; then
                                echo "Found VPC: $VPC_ID"
                                import_resource "aws_vpc" "main" "$VPC_ID"
                            else
                                echo "No existing VPC found - will create new one"
                            fi
                            echo ""

                            # Only continue if VPC was found
                            if [ "$VPC_ID" != "None" ] && [ -n "$VPC_ID" ]; then

                                echo "════════════════════════════════════════"
                                echo "2. Scanning for Subnets..."
                                echo "════════════════════════════════════════"

                                # Public Subnet
                                PUBLIC_SUBNET_ID=$(aws ec2 describe-subnets \
                                  --filters "Name=vpc-id,Values=$VPC_ID" "Name=tag:Name,Values=achat-app-public-subnet" \
                                  --query "Subnets[0].SubnetId" \
                                  --output text 2>/dev/null)

                                if [ "$PUBLIC_SUBNET_ID" != "None" ] && [ -n "$PUBLIC_SUBNET_ID" ]; then
                                    echo "Found Public Subnet: $PUBLIC_SUBNET_ID"
                                    import_resource "aws_subnet" "public" "$PUBLIC_SUBNET_ID"
                                fi

                                # Private Subnet
                                PRIVATE_SUBNET_ID=$(aws ec2 describe-subnets \
                                  --filters "Name=vpc-id,Values=$VPC_ID" "Name=tag:Name,Values=achat-app-private-subnet" \
                                  --query "Subnets[0].SubnetId" \
                                  --output text 2>/dev/null)

                                if [ "$PRIVATE_SUBNET_ID" != "None" ] && [ -n "$PRIVATE_SUBNET_ID" ]; then
                                    echo "Found Private Subnet: $PRIVATE_SUBNET_ID"
                                    import_resource "aws_subnet" "private" "$PRIVATE_SUBNET_ID"
                                fi
                                echo ""

                                echo "════════════════════════════════════════"
                                echo "3. Scanning for Internet Gateway..."
                                echo "════════════════════════════════════════"

                                IGW_ID=$(aws ec2 describe-internet-gateways \
                                  --filters "Name=attachment.vpc-id,Values=$VPC_ID" \
                                  --query "InternetGateways[0].InternetGatewayId" \
                                  --output text 2>/dev/null)

                                if [ "$IGW_ID" != "None" ] && [ -n "$IGW_ID" ]; then
                                    echo "Found Internet Gateway: $IGW_ID"
                                    import_resource "aws_internet_gateway" "main" "$IGW_ID"
                                fi
                                echo ""

                                echo "════════════════════════════════════════"
                                echo "4. Scanning for Route Tables..."
                                echo "════════════════════════════════════════"

                                # Public Route Table
                                PUBLIC_RT_ID=$(aws ec2 describe-route-tables \
                                  --filters "Name=vpc-id,Values=$VPC_ID" "Name=tag:Name,Values=achat-app-public-rt" \
                                  --query "RouteTables[0].RouteTableId" \
                                  --output text 2>/dev/null)

                                if [ "$PUBLIC_RT_ID" != "None" ] && [ -n "$PUBLIC_RT_ID" ]; then
                                    echo "Found Public Route Table: $PUBLIC_RT_ID"
                                    import_resource "aws_route_table" "public" "$PUBLIC_RT_ID"

                                    # Import route table association
                                    ASSOC_ID=$(aws ec2 describe-route-tables \
                                      --route-table-ids "$PUBLIC_RT_ID" \
                                      --query "RouteTables[0].Associations[?SubnetId=='$PUBLIC_SUBNET_ID'].RouteTableAssociationId" \
                                      --output text 2>/dev/null)

                                    if [ "$ASSOC_ID" != "None" ] && [ -n "$ASSOC_ID" ]; then
                                        echo "Found Route Table Association: $ASSOC_ID"
                                        import_resource "aws_route_table_association" "public" "$ASSOC_ID"
                                    fi
                                fi
                                echo ""

                                echo "════════════════════════════════════════"
                                echo "5. Scanning for Security Groups..."
                                echo "════════════════════════════════════════"

                                SG_ID=$(aws ec2 describe-security-groups \
                                  --filters "Name=vpc-id,Values=$VPC_ID" "Name=tag:Name,Values=achat-app-sg" \
                                  --query "SecurityGroups[0].GroupId" \
                                  --output text 2>/dev/null)

                                if [ "$SG_ID" != "None" ] && [ -n "$SG_ID" ]; then
                                    echo "Found Security Group: $SG_ID"
                                    import_resource "aws_security_group" "app_sg" "$SG_ID"
                                fi
                                echo ""

                                echo "════════════════════════════════════════"
                                echo "6. Scanning for EC2 Instances..."
                                echo "════════════════════════════════════════"

                                INSTANCE_ID=$(aws ec2 describe-instances \
                                  --filters "Name=vpc-id,Values=$VPC_ID" "Name=tag:Name,Values=achat-app-server" "Name=instance-state-name,Values=running,stopped" \
                                  --query "Reservations[0].Instances[0].InstanceId" \
                                  --output text 2>/dev/null)

                                if [ "$INSTANCE_ID" != "None" ] && [ -n "$INSTANCE_ID" ]; then
                                    echo "Found EC2 Instance: $INSTANCE_ID"
                                    import_resource "aws_instance" "app_server" "$INSTANCE_ID"
                                fi
                                echo ""

                                echo "════════════════════════════════════════"
                                echo "7. Scanning for EKS Clusters..."
                                echo "════════════════════════════════════════"

                                EKS_CLUSTERS=$(aws eks list-clusters --query "clusters" --output text 2>/dev/null)

                                if [ -n "$EKS_CLUSTERS" ]; then
                                    for cluster in $EKS_CLUSTERS; do
                                        echo "Found EKS Cluster: $cluster"
                                        # Note: Import command would be: terraform import aws_eks_cluster.name cluster-name
                                        echo "  ⚠️  EKS import requires manual configuration in terraform files"
                                    done
                                else
                                    echo "No EKS clusters found"
                                fi
                                echo ""

                                echo "════════════════════════════════════════"
                                echo "8. Scanning for RDS/DB Subnet Groups..."
                                echo "════════════════════════════════════════"

                                DB_SUBNET_GROUPS=$(aws rds describe-db-subnet-groups \
                                  --query "DBSubnetGroups[?VpcId=='$VPC_ID'].DBSubnetGroupName" \
                                  --output text 2>/dev/null)

                                if [ -n "$DB_SUBNET_GROUPS" ]; then
                                    for group in $DB_SUBNET_GROUPS; do
                                        echo "Found DB Subnet Group: $group"
                                        # Note: Import command would be: terraform import aws_db_subnet_group.name group-name
                                        echo "  ⚠️  DB Subnet Group import requires manual configuration in terraform files"
                                    done
                                else
                                    echo "No DB Subnet Groups found"
                                fi
                                echo ""

                            fi

                            echo "════════════════════════════════════════"
                            echo "✅ Resource Import Complete!"
                            echo "════════════════════════════════════════"
                            echo ""
                            echo "Current Terraform State:"
                            terraform state list || echo "No resources in state yet"
                            echo ""
                        '''
                    }
                }

                script {
                    echo ''
                    echo '💾 Saving imported state...'
                    sh """
                        mkdir -p ${TERRAFORM_STATE_DIR}
                        cp -v ${TERRAFORM_DIR}/terraform.tfstate ${TERRAFORM_STATE_DIR}/terraform.tfstate 2>/dev/null || true
                    """
                    echo '✅ Import stage complete!'
                }
            }
        }

        stage('Terraform Init') {
            when {
                expression { return false }  // SKIP: Testing early stages only
                allOf {
                    expression { return fileExists('terraform/main.tf') }
                    expression { return params.DEPLOY_ACTION != 'destroy' }
                }
            }
            steps {
                script {
                    echo '========================================='
                    echo 'Stage 12: Terraform Initialization'
                    echo '========================================='
                    echo ''

                    // Load existing Terraform state if it exists
                    if (fileExists("${TERRAFORM_STATE_DIR}/terraform.tfstate")) {
                        echo '📦 Loading existing Terraform state from persistent storage...'
                        sh """
                            cp -v ${TERRAFORM_STATE_DIR}/terraform.tfstate ${TERRAFORM_DIR}/terraform.tfstate
                            cp -v ${TERRAFORM_STATE_DIR}/terraform.tfstate.backup ${TERRAFORM_DIR}/terraform.tfstate.backup 2>/dev/null || true
                        """
                        echo '✅ Existing state loaded - Terraform will detect current AWS resources'
                    } else {
                        echo '🆕 No existing state found - This will be a fresh deployment'
                    }
                    echo ''
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
                expression { return false }  // SKIP: Testing early stages only
                allOf {
                    expression { return fileExists('terraform/main.tf') }
                    expression { return params.DEPLOY_ACTION != 'destroy' }
                }
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
                          -var="deploy_mode=${TF_VAR_deploy_mode}" \
                          -out=tfplan \
                          -input=false \
                          -refresh=false

                        echo ""
                        echo "Plan saved to: tfplan"
                        echo "Deployment mode: ${TF_VAR_deploy_mode}"
                        echo "Note: Using -refresh=false for AWS Academy compatibility"
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
                expression { return false }  // SKIP: Testing early stages only
                allOf {
                    expression { return fileExists('terraform/main.tf') }
                    expression {
                        return params.DEPLOY_ACTION == 'deploy' ||
                               params.DEPLOY_ACTION == 'redeploy'
                    }
                }
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
                    echo ''
                    echo '💾 Saving Terraform state to persistent storage...'

                    // Save state to persistent directory for next build
                    sh """
                        cp -v ${TERRAFORM_DIR}/terraform.tfstate ${TERRAFORM_STATE_DIR}/terraform.tfstate
                        cp -v ${TERRAFORM_DIR}/terraform.tfstate.backup ${TERRAFORM_STATE_DIR}/terraform.tfstate.backup 2>/dev/null || true
                        ls -lh ${TERRAFORM_STATE_DIR}/
                    """

                    echo '✅ State saved - Next build will use incremental updates'
                    echo ''
                }
            }
        }

        stage('Get AWS Deployment Info') {
            when {
                expression { return false }  // SKIP: Testing early stages only
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

        stage('Deploy Application to EKS') {
            when {
                expression { return false }  // SKIP: Testing early stages only
            }
            steps {
                script {
                    echo '========================================='
                    echo 'Stage 15: Deploy Application to EKS'
                    echo '========================================='
                }

                dir(TERRAFORM_DIR) {
                    script {
                        // Get EKS cluster name
                        def clusterName = sh(
                            script: 'terraform output -raw eks_cluster_id 2>/dev/null || echo ""',
                            returnStdout: true
                        ).trim()

                        if (clusterName && clusterName != "N/A") {
                            echo "EKS Cluster: ${clusterName}"
                            echo "Configuring kubectl for EKS..."

                            sh """
                                export AWS_SHARED_CREDENTIALS_FILE=/var/jenkins_home/.aws/credentials
                                export AWS_CONFIG_FILE=/var/jenkins_home/.aws/config
                                export AWS_PAGER=''

                                # Configure kubectl for EKS
                                aws eks update-kubeconfig --region ${AWS_REGION} --name ${clusterName}

                                # Verify connection
                                kubectl get nodes

                                echo ""
                                echo "EKS cluster configured successfully!"
                            """

                            echo ""
                            echo "Deploying application to EKS..."

                            // Create namespace
                            sh """
                                export AWS_SHARED_CREDENTIALS_FILE=/var/jenkins_home/.aws/credentials
                                export AWS_CONFIG_FILE=/var/jenkins_home/.aws/config
                                export AWS_PAGER=''
                                kubectl apply -f ../k8s/namespace.yaml
                            """

                            // Replace image placeholder in deployment
                            sh """
                                export AWS_SHARED_CREDENTIALS_FILE=/var/jenkins_home/.aws/credentials
                                export AWS_CONFIG_FILE=/var/jenkins_home/.aws/config
                                export AWS_PAGER=''
                                export DOCKER_IMAGE="${TF_VAR_docker_image}"
                                sed "s|\\\${DOCKER_IMAGE}|${TF_VAR_docker_image}|g" ../k8s/deployment.yaml | kubectl apply -f -
                            """

                            // Apply LoadBalancer service for EKS
                            sh """
                                export AWS_SHARED_CREDENTIALS_FILE=/var/jenkins_home/.aws/credentials
                                export AWS_CONFIG_FILE=/var/jenkins_home/.aws/config
                                export AWS_PAGER=''
                                kubectl apply -f ../k8s/service-eks.yaml
                            """

                            echo ""
                            echo "Waiting for deployment to be ready..."
                            sh """
                                export AWS_SHARED_CREDENTIALS_FILE=/var/jenkins_home/.aws/credentials
                                export AWS_CONFIG_FILE=/var/jenkins_home/.aws/config
                                export AWS_PAGER=''
                                kubectl wait --for=condition=available --timeout=300s deployment/achat-app -n achat-app || true
                            """

                            echo ""
                            echo "Getting deployment status..."
                            sh """
                                export AWS_SHARED_CREDENTIALS_FILE=/var/jenkins_home/.aws/credentials
                                export AWS_CONFIG_FILE=/var/jenkins_home/.aws/config
                                export AWS_PAGER=''
                                kubectl get all -n achat-app
                            """

                            echo ""
                            echo "Waiting for LoadBalancer to be provisioned..."
                            echo "This may take 2-3 minutes..."
                            sleep(30)

                            // Get LoadBalancer URL
                            def lbUrl = sh(
                                script: '''
                                    export AWS_SHARED_CREDENTIALS_FILE=/var/jenkins_home/.aws/credentials
                                    export AWS_CONFIG_FILE=/var/jenkins_home/.aws/config
                                    export AWS_PAGER=''
                                    kubectl get svc achat-app-service -n achat-app -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || echo ""
                                ''',
                                returnStdout: true
                            ).trim()

                            if (lbUrl) {
                                echo ""
                                echo "========================================="
                                echo "APPLICATION DEPLOYED TO EKS!"
                                echo "========================================="
                                echo "LoadBalancer URL: http://${lbUrl}"
                                echo "Main App: http://${lbUrl}/SpringMVC/"
                                echo "Health Check: http://${lbUrl}/SpringMVC/actuator/health"
                                echo "Swagger UI: http://${lbUrl}/SpringMVC/swagger-ui/"
                                echo "========================================="
                                echo ""
                                echo "Note: It may take a few minutes for the LoadBalancer DNS to propagate"
                                echo "      and for the application to be fully ready."
                            } else {
                                echo ""
                                echo "LoadBalancer is being provisioned..."
                                echo "Run this command to get the URL once ready:"
                                echo "kubectl get svc achat-app-service -n achat-app"
                            }
                        } else {
                            echo "EKS cluster not found. Skipping application deployment."
                        }
                    }
                }
            }
        }

        stage('Health Check AWS Deployment') {
            when {
                expression { return false }  // SKIP: Testing early stages only
            }
            steps {
                script {
                    echo '========================================='
                    echo 'Stage 16: Health Check (EC2/k3s)'
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
                            // For k8s deployments, wait longer for k3s installation and app deployment
                            def waitTime = env.TF_VAR_deploy_mode == 'k8s' ? 300 : 60
                            def retryCount = env.TF_VAR_deploy_mode == 'k8s' ? 10 : 5

                            echo "Waiting for application to start (${waitTime} seconds)..."
                            echo "Deploy mode: ${env.TF_VAR_deploy_mode}"
                            sleep(waitTime)

                            echo "Checking application health..."
                            // Application runs with context path /SpringMVC
                            def healthUrl = "${appUrl}/SpringMVC/actuator/health"

                            retry(retryCount) {
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

        stage('Health Check EKS Deployment') {
            when {
                expression { return false }  // SKIP: Testing early stages only
            }
            steps {
                script {
                    echo '========================================='
                    echo 'Stage 16: Health Check (EKS)'
                    echo '========================================='
                }

                script {
                    echo "Waiting for LoadBalancer and application to be ready (3 minutes)..."
                    sleep(180)

                    // Get LoadBalancer URL
                    def lbUrl = sh(
                        script: '''
                            export AWS_SHARED_CREDENTIALS_FILE=/var/jenkins_home/.aws/credentials
                            export AWS_CONFIG_FILE=/var/jenkins_home/.aws/config
                            export AWS_PAGER=''
                            kubectl get svc achat-app-service -n achat-app -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || echo ""
                        ''',
                        returnStdout: true
                    ).trim()

                    if (lbUrl) {
                        echo "LoadBalancer URL: ${lbUrl}"
                        def healthUrl = "http://${lbUrl}/SpringMVC/actuator/health"

                        echo "Checking application health..."
                        retry(10) {
                            sleep(15)
                            sh """
                                curl -f ${healthUrl} || exit 1
                            """
                        }

                        echo ""
                        echo "========================================="
                        echo "EKS APPLICATION IS HEALTHY!"
                        echo "========================================="
                        echo "Main App: http://${lbUrl}/SpringMVC/"
                        echo "Health Check: http://${lbUrl}/SpringMVC/actuator/health"
                        echo "Swagger UI: http://${lbUrl}/SpringMVC/swagger-ui/"
                        echo "========================================="
                    } else {
                        echo "Warning: Could not retrieve LoadBalancer URL"
                        echo "The LoadBalancer may still be provisioning."
                        echo "Run: kubectl get svc achat-app-service -n achat-app"
                    }
                }
            }
        }

        stage('Deploy to Kubernetes') {
            when {
                expression { return false }  // SKIP: Testing early stages only
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
                echo ''

                // Backup Terraform state before cleaning workspace
                if (fileExists("${TERRAFORM_DIR}/terraform.tfstate")) {
                    echo '💾 Backing up Terraform state before workspace cleanup...'
                    sh """
                        mkdir -p ${TERRAFORM_STATE_DIR}
                        cp -v ${TERRAFORM_DIR}/terraform.tfstate ${TERRAFORM_STATE_DIR}/terraform.tfstate
                        cp -v ${TERRAFORM_DIR}/terraform.tfstate.backup ${TERRAFORM_STATE_DIR}/terraform.tfstate.backup 2>/dev/null || true
                    """
                    echo '✅ State backed up to persistent storage'
                } else {
                    echo 'ℹ️  No Terraform state to backup'
                }
                echo ''
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

