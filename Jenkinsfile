pipeline {
    agent any

    // Disable automatic checkout to use our custom retry logic instead
    options {
        skipDefaultCheckout()
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 2, unit: 'HOURS')
    }

    // Parameters to control pipeline behavior
    parameters {
        choice(
            name: 'DEPLOYMENT_MODE',
            choices: ['NORMAL', 'CLEANUP_AND_DEPLOY', 'REUSE_INFRASTRUCTURE'],
            description: '''Deployment mode:
            ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¢ NORMAL: Deploy fresh infrastructure (may fail if VPC limit reached)
            ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¢ CLEANUP_AND_DEPLOY: Destroy old resources first, then deploy new ones
            ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¢ REUSE_INFRASTRUCTURE: Keep VPC/RDS, only recreate EC2 instance (fastest for testing)'''
        )
    }

    // Removed Jenkins auto-install tools block to avoid external TLS download failures.
    // Use project Maven Wrapper (mvnw) and container's existing JDK instead of downloading.
    // tools {
    //     maven 'maven-3.8.6'  // Will be auto-installed
    //     jdk 'jdk-8'          // Will be auto-installed
    // }
    
    environment {
        // Git settings to avoid TLS issues
        GIT_SSL_NO_VERIFY = 'true'
        
        // Docker client timeout settings to handle network issues
        DOCKER_CLIENT_TIMEOUT = '300'
        COMPOSE_HTTP_TIMEOUT = '300'
        
        // Maven settings
        MAVEN_OPTS = '-Xmx1024m'
        
        // Project information from pom.xml
        PROJECT_NAME = 'achat'
        PROJECT_VERSION = '1.0-SNAPSHOT'
        
        // Artifact naming
        ARTIFACT_NAME = "${PROJECT_NAME}-${PROJECT_VERSION}-${BUILD_NUMBER}.jar"
        
        // SonarQube (Phase 2)
        SONAR_HOST_URL = 'http://sonarqube-server:9000'
        SONAR_PROJECT_KEY = 'achat'
        SONAR_PROJECT_NAME = 'Achat Application'
        
        // Nexus (Phase 3)
        NEXUS_URL = 'nexus-repository:8081'
        NEXUS_REPOSITORY = 'maven-snapshots'
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
        DOCKER_HUB_USER = '' // Will be set from credentials
        TF_VAR_docker_image = "${DOCKER_REGISTRY}/habibmanai/${DOCKER_IMAGE}"
    }
    
    stages {
        stage('Checkout') {
            steps {
                script {
                    echo '========================================='
                    echo 'Stage 1: Checking out code from GitHub'
                    echo '========================================='
                }

            // Checkout code from GitHub with retry logic and exponential backoff
            script {
                def maxRetries = 5
                def retryCount = 0
                def success = false
                
                while (retryCount < maxRetries && !success) {
                    try {
                        retryCount++
                        if (retryCount > 1) {
                            def waitTime = (int)(Math.pow(2, retryCount - 1) * 10) // 10s, 20s, 40s, 80s
                            echo "Retry attempt ${retryCount}/${maxRetries} after ${waitTime}s wait..."
                            sleep(time: waitTime, unit: 'SECONDS')
                        }
                        
                        checkout([
                            $class: 'GitSCM',
                            branches: [[name: '*/MohamedHabibManai-GL5-G5-Produit']],
                            extensions: [
                                [$class: 'CloneOption', timeout: 60, noTags: true, shallow: true, depth: 1],
                                [$class: 'CheckoutOption', timeout: 60]
                            ],
                            userRemoteConfigs: [[url: 'https://github.com/MedHabibManai/IGL5-G5-achat.git']]
                        ])
                        success = true
                    } catch (Exception e) {
                        if (retryCount >= maxRetries) {
                            throw e
                        }
                        echo "Checkout failed: ${e.message}. Retrying..."
                    }
                }
            }
            
            script {
                    echo " Successfully checked out branch: ${env.GIT_BRANCH}"
                    echo " Commit: ${env.GIT_COMMIT}"
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
                
                // Use system Maven (wrapper files missing)
                sh 'mvn clean compile'
                
                script {
                    echo 'ÃƒÆ’Ã‚Â¢Ãƒâ€¦Ã¢â‚¬Å“ÃƒÂ¢Ã¢â€šÂ¬Ã…â€œ Build completed successfully'
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
                    echo 'ÃƒÆ’Ã‚Â¢Ãƒâ€¦Ã¢â‚¬Å“ÃƒÂ¢Ã¢â€šÂ¬Ã…â€œ All unit tests passed'
                }
            }
            
            post {
                always {
                    // Publish JUnit test results
                    junit '**/target/surefire-reports/*.xml'
                    
                    script {
                        echo 'ÃƒÆ’Ã‚Â¢Ãƒâ€¦Ã¢â‚¬Å“ÃƒÂ¢Ã¢â€šÂ¬Ã…â€œ Test results published'
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
                    echo "ÃƒÆ’Ã‚Â¢Ãƒâ€¦Ã¢â‚¬Å“ÃƒÂ¢Ã¢â€šÂ¬Ã…â€œ Application packaged: ${ARTIFACT_NAME}"
                }
            }
            
            post {
                success {
                    // Archive the artifacts
                    archiveArtifacts artifacts: '**/target/*.jar', fingerprint: true
                    
                    script {
                        echo 'ÃƒÆ’Ã‚Â¢Ãƒâ€¦Ã¢â‚¬Å“ÃƒÂ¢Ã¢â€šÂ¬Ã…â€œ Artifacts archived successfully'
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

                        export MAVEN_OPTS="--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.io=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED"
                        mvn sonar:sonar \
                          -Dsonar.projectKey=${SONAR_PROJECT_KEY} \
                          -Dsonar.projectName="${SONAR_PROJECT_NAME}" \
                          -Dsonar.host.url=${SONAR_HOST_URL} \
                          -Dsonar.java.binaries=target/classes
                    '''
                }

                script {
                    echo 'ÃƒÆ’Ã‚Â¢Ãƒâ€¦Ã¢â‚¬Å“ÃƒÂ¢Ã¢â€šÂ¬Ã…â€œ SonarQube analysis completed'
                    echo "ÃƒÆ’Ã‚Â¢Ãƒâ€¦Ã¢â‚¬Å“ÃƒÂ¢Ã¢â€šÂ¬Ã…â€œ View results at: ${SONAR_HOST_URL}/dashboard?id=${SONAR_PROJECT_KEY}"
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
                            echo "ÃƒÆ’Ã‚Â¢Ãƒâ€¦Ã‚Â¡Ãƒâ€šÃ‚Â  Quality Gate status: ${qg.status}"
                            echo "ÃƒÆ’Ã‚Â¢Ãƒâ€¦Ã‚Â¡Ãƒâ€šÃ‚Â  Pipeline will continue but code quality needs attention"
                        } else {
                            echo 'ÃƒÆ’Ã‚Â¢Ãƒâ€¦Ã¢â‚¬Å“ÃƒÂ¢Ã¢â€šÂ¬Ã…â€œ Quality Gate passed!'
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
                    echo 'ÃƒÆ’Ã‚Â¢Ãƒâ€¦Ã¢â‚¬Å“ÃƒÂ¢Ã¢â€šÂ¬Ã…â€œ Artifacts deployed to Nexus successfully'
                    echo "ÃƒÆ’Ã‚Â¢Ãƒâ€¦Ã¢â‚¬Å“ÃƒÂ¢Ã¢â€šÂ¬Ã…â€œ View artifacts at: http://localhost:8081/#browse/browse:maven-releases"
                }
            }
        }
        
        stage('Build Docker Image') {
            steps {
                script {
                    echo '========================================='
                    echo 'Stage 8: Building Docker Image'
                    echo '========================================='
                }

                // Build Docker image with retry logic for network issues
                script {
                    echo "Building Docker image: ${DOCKER_IMAGE}"
                    echo "JAR file: target/${PROJECT_NAME}-${PROJECT_VERSION}.jar"

                    // Verify JAR exists
                    sh "ls -lh target/${PROJECT_NAME}-${PROJECT_VERSION}.jar"

                    // Pre-pull base image with retry logic to handle TLS timeouts
                    def baseImage = 'eclipse-temurin:8-jre-alpine'
                    def maxRetries = 3
                    def pulled = false
                    
                    for (int i = 0; i < maxRetries && !pulled; i++) {
                        try {
                            if (i > 0) {
                                def delay = Math.pow(2.0, i as double) * 10
                                echo "Retry ${i + 1}/${maxRetries} - waiting ${delay}s before pulling base image..."
                                sleep(time: delay.toInteger(), unit: 'SECONDS')
                            }
                            
                            echo "Attempting to pull base image: ${baseImage}"
                            sh "docker pull ${baseImage}"
                            pulled = true
                            echo "? Base image pulled successfully"
                        } catch (Exception e) {
                            if (i < maxRetries - 1) {
                                echo "Failed to pull base image: ${e.message}"
                            } else {
                                echo "? Failed to pull base image after ${maxRetries} attempts, proceeding with build (may use cached image)"
                            }
                        }
                    }

                    // Build the image with retry logic
                    def built = false
                    
                    for (int i = 0; i < maxRetries && !built; i++) {
                        try {
                            if (i > 0) {
                                def delay = Math.pow(2.0, i as double) * 10
                                echo "Retry ${i + 1}/${maxRetries} - waiting ${delay}s before building..."
                                sleep(time: delay.toInteger(), unit: 'SECONDS')
                            }
                            
                            sh """
                                docker build \
                                  --build-arg JAR_FILE=target/${PROJECT_NAME}-${PROJECT_VERSION}.jar \
                                  --build-arg BUILD_NUMBER=${BUILD_NUMBER} \
                                  -t ${DOCKER_IMAGE} \
                                  -t ${DOCKER_IMAGE_NAME}:latest \
                                  .
                            """
                            built = true
                            
                            echo "? Docker image built successfully!"
                            echo "  - ${DOCKER_IMAGE}"
                            echo "  - ${DOCKER_IMAGE_NAME}:latest"

                            // Show image details
                            sh "docker images | grep ${DOCKER_IMAGE_NAME}"
                        } catch (Exception e) {
                            if (i < maxRetries - 1) {
                                echo "Docker build failed: ${e.message}. Retrying..."
                            } else {
                                throw e
                            }
                        }
                    }
                }
            }
        }
        
        stage('Push Docker Image') {
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

                        // Infinite retry for Docker login (handles TLS handshake timeouts)
                        def loginSuccess = false
                        
                        while (!loginSuccess) {
                            try {
                                sh '''
                                    attempt=0
                                    while true; do
                                        attempt=$((attempt + 1))
                                        echo "Attempting Docker login (attempt $attempt)..."
                                        
                                        if echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin; then
                                            echo "V Docker login successful!"
                                            break
                                        else
                                            echo "? Login failed (likely network/TLS error), retrying in 15s..."
                                            sleep 15
                                        fi
                                    done
                                '''
                                
                                loginSuccess = true
                                echo "V Docker login completed!"
                            } catch (Exception e) {
                                echo "Unexpected error in login block: ${e.message}"
                                echo "Retrying entire login sequence in 15s..."
                                sleep(15)
                            }
                        }

                        echo "Tagging images for Docker Hub..."

                        // Tag with username prefix for Docker Hub
                        sh """
                            docker tag ${DOCKER_IMAGE} ${DOCKER_USER}/${DOCKER_IMAGE}
                            docker tag ${DOCKER_IMAGE_NAME}:latest ${DOCKER_USER}/${DOCKER_IMAGE_NAME}:latest
                        """

                        echo "Pushing images to Docker Hub..."


                        // Infinite retry for Docker push (handles TLS handshake timeouts)
                        def pushSuccess = false
                        
                        while (!pushSuccess) {
                            try {
                                // Push with BUILD_NUMBER tag using infinite retry in shell
                                sh """
                                    attempt=0
                                    while true; do
                                        attempt=\$((attempt + 1))
                                        echo "Attempting to push ${DOCKER_USER}/${DOCKER_IMAGE} (attempt \$attempt)..."
                                        
                                        if docker push ${DOCKER_USER}/${DOCKER_IMAGE}; then
                                            echo "✓ Successfully pushed ${DOCKER_USER}/${DOCKER_IMAGE}"
                                            break
                                        else
                                            echo "✗ Push failed (likely network/TLS error), retrying in 15s..."
                                            sleep 15
                                        fi
                                    done
                                """
                                
                                // Push with latest tag using infinite retry in shell
                                sh """
                                    attempt=0
                                    while true; do
                                        attempt=\$((attempt + 1))
                                        echo "Attempting to push ${DOCKER_USER}/${DOCKER_IMAGE_NAME}:latest (attempt \$attempt)..."
                                        
                                        if docker push ${DOCKER_USER}/${DOCKER_IMAGE_NAME}:latest; then
                                            echo "✓ Successfully pushed ${DOCKER_USER}/${DOCKER_IMAGE_NAME}:latest"
                                            break
                                        else
                                            echo "✗ Push failed (likely network/TLS error), retrying in 15s..."
                                            sleep 15
                                        fi
                                    done
                                """
                                
                                pushSuccess = true
                                echo "✓ Docker push successful!"
                            } catch (Exception e) {
                                echo "Unexpected error in push block: ${e.message}"
                                echo "Retrying entire push sequence in 15s..."
                                sleep(15)
                            }
                        }

                        echo "✓ Docker images pushed successfully!"
                        echo "  - ${DOCKER_USER}/${DOCKER_IMAGE}"
                        echo "  - ${DOCKER_USER}/${DOCKER_IMAGE_NAME}:latest"
                        echo ""
                        echo "View on Docker Hub: https://hub.docker.com/r/${DOCKER_USER}/${DOCKER_IMAGE_NAME}"
                    }
                }
            }
        }

        stage('Cleanup AWS Infrastructure') {
            when {
                allOf {
                    expression { return params.DEPLOYMENT_MODE == 'CLEANUP_AND_DEPLOY' }
                    expression { return fileExists('terraform/main.tf') }
                }
            }
            steps {
                script {
                    echo '========================================='
                    echo 'Stage 9.5: Cleaning Up Old AWS Infrastructure'
                    echo '========================================='
                    echo 'WARNING: This will destroy ALL AWS resources'
                    echo 'Method: Terraform Destroy (handles all resources including EKS)'
                }

                withCredentials([file(credentialsId: "${AWS_CREDENTIAL_ID}", variable: 'AWS_CREDENTIALS_FILE')]) {
                    dir(TERRAFORM_DIR) {
                        sh '''
                            echo "Setting up AWS credentials..."
                            mkdir -p ~/.aws
                            cp $AWS_CREDENTIALS_FILE ~/.aws/credentials
                            chmod 600 ~/.aws/credentials
                            
                            echo "======================================"
                            echo "Using Terraform Destroy for Clean Removal"
                            echo "======================================"
                            echo ""
                            
                            # Check if Terraform state exists
                            if [ -f "terraform.tfstate" ]; then
                                echo "ÃƒÂ¢Ã…â€œÃ¢â‚¬Å“ Terraform state found. Using terraform destroy..."
                                echo ""
                                
                                # Initialize Terraform
                                terraform init -upgrade
                                
                                # Destroy all resources
                                echo "Destroying all infrastructure (this may take 10-15 minutes for EKS)..."
                                terraform destroy -auto-approve \
                                    -var="docker_image=${DOCKER_REGISTRY}/${DOCKER_HUB_USER}/${DOCKER_IMAGE_NAME}:${BUILD_NUMBER}"
                                
                                echo ""
                                echo "ÃƒÂ¢Ã…â€œÃ¢â‚¬Å“ Terraform destroy completed successfully"
                            else
                                echo "ÃƒÂ¢Ã…Â¡Ã‚Â  No Terraform state found. Using manual cleanup..."
                                echo ""
                            
                                echo "======================================"
                                echo "Manual Cleanup (fallback method)"
                                echo "======================================"
                            
                                # Function to safely delete resources
                                safe_delete() {
                                eval "$1" 2>/dev/null || echo "  (already deleted or not found)"
                                }
                            
                                # Step 0: Delete EKS Clusters first (if any exist)
                                echo ""
                                echo "Step 0: Deleting EKS Clusters..."
                                EKS_CLUSTERS=$(aws eks list-clusters \
                                --region ${AWS_REGION} \
                                --query 'clusters[?contains(@, `achat-app`) == `true`]' \
                                --output text 2>/dev/null || echo "")
                            
                                if [ -n "$EKS_CLUSTERS" ]; then
                                for cluster_name in $EKS_CLUSTERS; do
                                    echo "  Found EKS cluster: $cluster_name"
                                    
                                    # Check cluster status
                                    CLUSTER_STATUS=$(aws eks describe-cluster \
                                        --region ${AWS_REGION} \
                                        --name $cluster_name \
                                        --query 'cluster.status' \
                                        --output text 2>/dev/null || echo "NOT_FOUND")
                                    echo "    Current status: $CLUSTER_STATUS"
                                    
                                    if [ "$CLUSTER_STATUS" = "DELETING" ]; then
                                        echo "    Cluster is already being deleted, waiting..."
                                        aws eks wait cluster-deleted --region ${AWS_REGION} --name $cluster_name 2>&1 || echo "      Wait completed or timed out"
                                        continue
                                    fi
                                    
                                    if [ "$CLUSTER_STATUS" = "NOT_FOUND" ]; then
                                        echo "    Cluster not found, skipping"
                                        continue
                                    fi
                                    
                                    # Delete node groups first
                                    echo "    Checking for node groups..."
                                    NODE_GROUPS=$(aws eks list-nodegroups \
                                        --region ${AWS_REGION} \
                                        --cluster-name $cluster_name \
                                        --query 'nodegroups[]' \
                                        --output text 2>/dev/null || echo "")
                                    
                                    if [ -n "$NODE_GROUPS" ] && [ "$NODE_GROUPS" != "None" ]; then
                                        echo "    Found node groups: $NODE_GROUPS"
                                        for node_group in $NODE_GROUPS; do
                                            echo "      Deleting node group: $node_group"
                                            aws eks delete-nodegroup \
                                                --region ${AWS_REGION} \
                                                --cluster-name $cluster_name \
                                                --nodegroup-name $node_group 2>&1 | grep -v "ResourceNotFoundException" || true
                                        done
                                        
                                        # Wait for node groups to delete
                                        echo "      Waiting for node groups to delete (this takes 3-5 minutes)..."
                                        for node_group in $NODE_GROUPS; do
                                            echo "        Waiting for node group: $node_group"
                                            aws eks wait nodegroup-deleted \
                                                --region ${AWS_REGION} \
                                                --cluster-name $cluster_name \
                                                --nodegroup-name $node_group 2>&1 | head -5 || echo "        Node group deleted or not found"
                                        done
                                        echo "      ÃƒÂ¢Ã…â€œÃ¢â‚¬Å“ All node groups deleted"
                                    else
                                        echo "    No node groups found or already deleted"
                                    fi
                                    
                                    # Delete the cluster
                                    echo "    Deleting EKS cluster: $cluster_name"
                                    DELETE_OUTPUT=$(aws eks delete-cluster --region ${AWS_REGION} --name $cluster_name 2>&1)
                                    if echo "$DELETE_OUTPUT" | grep -q "ResourceNotFoundException"; then
                                        echo "      Cluster already deleted"
                                    elif echo "$DELETE_OUTPUT" | grep -q "error"; then
                                        echo "      Error deleting cluster: $DELETE_OUTPUT"
                                    else
                                        echo "      Delete command sent successfully"
                                        
                                        # Wait for cluster deletion with timeout
                                        echo "      Waiting for cluster to delete (this takes 5-10 minutes)..."
                                        WAIT_START=$(date +%s)
                                        TIMEOUT=900  # 15 minutes timeout
                                        
                                        while true; do
                                            CURRENT_STATUS=$(aws eks describe-cluster \
                                                --region ${AWS_REGION} \
                                                --name $cluster_name \
                                                --query 'cluster.status' \
                                                --output text 2>/dev/null || echo "DELETED")
                                            
                                            if [ "$CURRENT_STATUS" = "DELETED" ] || echo "$CURRENT_STATUS" | grep -q "ResourceNotFoundException"; then
                                                echo "      ÃƒÂ¢Ã…â€œÃ¢â‚¬Å“ Cluster deleted successfully"
                                                break
                                            fi
                                            
                                            ELAPSED=$(($(date +%s) - WAIT_START))
                                            if [ $ELAPSED -gt $TIMEOUT ]; then
                                                echo "      ÃƒÂ¢Ã…Â¡Ã‚Â  Timeout waiting for cluster deletion (${TIMEOUT}s)"
                                                echo "      Current status: $CURRENT_STATUS"
                                                break
                                            fi
                                            
                                            echo "        Status: $CURRENT_STATUS (${ELAPSED}s elapsed)"
                                            sleep 30
                                        done
                                    fi
                                    
                                    echo "    ÃƒÂ¢Ã…â€œÃ¢â‚¬Å“ EKS cluster $cluster_name processed"
                                done
                                else
                                echo "  No EKS clusters found"
                                fi
                            
                                # Step 0.5: Delete RDS Instances FIRST (before DB subnet groups)
                                echo ""
                                echo "Step 0.5: Deleting RDS Instances..."
                            
                                # Find all achat-app RDS instances
                                echo "  Checking for achat-app RDS instances..."
                                RDS_INSTANCES=$(aws rds describe-db-instances \
                                --region ${AWS_REGION} \
                                --query 'DBInstances[?contains(DBInstanceIdentifier, `achat-app`) == `true`].DBInstanceIdentifier' \
                                --output text 2>/dev/null || echo "")
                            
                                if [ -n "$RDS_INSTANCES" ]; then
                                echo "    Found RDS instances: $RDS_INSTANCES"
                                
                                # Delete each RDS instance
                                for db_id in $RDS_INSTANCES; do
                                    echo "    Deleting RDS instance: $db_id"
                                    DELETE_RDS_OUTPUT=$(aws rds delete-db-instance \
                                        --region ${AWS_REGION} \
                                        --db-instance-identifier $db_id \
                                        --skip-final-snapshot 2>&1)
                                    
                                    if echo "$DELETE_RDS_OUTPUT" | grep -q "DBInstanceNotFound"; then
                                        echo "      RDS instance already deleted"
                                    elif echo "$DELETE_RDS_OUTPUT" | grep -qi "error"; then
                                        echo "      Error deleting RDS instance: $DELETE_RDS_OUTPUT"
                                    else
                                        echo "      Delete command sent successfully"
                                    fi
                                done
                                
                                # Wait for RDS instances to be fully deleted
                                echo "    Waiting for RDS instances to delete (this may take 5-10 minutes)..."
                                for db_id in $RDS_INSTANCES; do
                                    echo "      Waiting for RDS instance: $db_id"
                                    WAIT_COUNT=0
                                    MAX_WAIT=30  # 30 * 30 seconds = 15 minutes max
                                    
                                    while [ $WAIT_COUNT -lt $MAX_WAIT ]; do
                                        # Check if instance exists
                                        DB_CHECK=$(aws rds describe-db-instances \
                                            --region ${AWS_REGION} \
                                            --db-instance-identifier $db_id 2>&1)
                                        
                                        if echo "$DB_CHECK" | grep -q "DBInstanceNotFound"; then
                                            echo "        ÃƒÂ¢Ã…â€œÃ¢â‚¬Å“ RDS instance $db_id fully deleted (not found)"
                                            break
                                        fi
                                        
                                        # Get status if instance still exists
                                        DB_STATUS=$(echo "$DB_CHECK" | grep -o '"DBInstanceStatus": "[^"]*"' | cut -d'"' -f4 || echo "unknown")
                                        
                                        if [ "$DB_STATUS" = "unknown" ] || [ -z "$DB_STATUS" ]; then
                                            echo "        ÃƒÂ¢Ã…â€œÃ¢â‚¬Å“ RDS instance $db_id fully deleted (no status)"
                                            break
                                        else
                                            echo "        Status: $DB_STATUS (attempt $WAIT_COUNT/$MAX_WAIT, waiting 30s...)"
                                            sleep 30
                                            WAIT_COUNT=$((WAIT_COUNT + 1))
                                        fi
                                    done
                                    
                                    if [ $WAIT_COUNT -ge $MAX_WAIT ]; then
                                        echo "        WARNING: Timeout waiting for $db_id deletion, but continuing..."
                                    fi
                                done
                                echo "    ÃƒÂ¢Ã…â€œÃ¢â‚¬Å“ All RDS instances processed"
                                else
                                echo "    No RDS instances found"
                                fi
                            
                                # Step 0.6: Delete DB Subnet Groups (AFTER RDS instances are deleted)
                                echo ""
                                echo "Step 0.6: Deleting DB Subnet Groups..."
                            
                                # Verify no RDS instances exist before deleting subnet groups
                                echo "  Double-checking that all RDS instances are deleted..."
                                REMAINING_RDS=$(aws rds describe-db-instances \
                                --region ${AWS_REGION} \
                                --query 'DBInstances[?contains(DBInstanceIdentifier, `achat-app`) == `true`].DBInstanceIdentifier' \
                                --output text 2>/dev/null || echo "")
                            
                                if [ -n "$REMAINING_RDS" ]; then
                                echo "    WARNING: Found remaining RDS instances: $REMAINING_RDS"
                                echo "    Waiting additional time for RDS deletion..."
                                
                                EXTRA_WAIT=0
                                while [ $EXTRA_WAIT -lt 10 ] && [ -n "$REMAINING_RDS" ]; do
                                    echo "      Extra wait attempt $EXTRA_WAIT/10 (60s)..."
                                    sleep 60
                                    REMAINING_RDS=$(aws rds describe-db-instances \
                                        --region ${AWS_REGION} \
                                        --query 'DBInstances[?contains(DBInstanceIdentifier, `achat-app`) == `true`].DBInstanceIdentifier' \
                                        --output text 2>/dev/null || echo "")
                                    EXTRA_WAIT=$((EXTRA_WAIT + 1))
                                done
                                
                                if [ -n "$REMAINING_RDS" ]; then
                                    echo "    ERROR: RDS instances still exist after extended wait: $REMAINING_RDS"
                                    echo "    Cannot safely delete DB subnet groups. Skipping subnet group deletion."
                                    echo "    Please manually delete these RDS instances and subnet groups later."
                                else
                                    echo "    ÃƒÂ¢Ã…â€œÃ¢â‚¬Å“ All RDS instances confirmed deleted"
                                fi
                                else
                                echo "    ÃƒÂ¢Ã…â€œÃ¢â‚¬Å“ No RDS instances found - safe to delete subnet groups"
                                fi
                            
                                # Only proceed with subnet group deletion if no RDS instances remain
                                if [ -z "$REMAINING_RDS" ]; then
                                # Delete by specific name first (Terraform-managed)
                                echo "  Checking for Terraform-managed DB subnet group: achat-app-db-subnet-group"
                                DB_SG_CHECK=$(aws rds describe-db-subnet-groups \
                                --region ${AWS_REGION} \
                                --db-subnet-group-name achat-app-db-subnet-group \
                                --query 'DBSubnetGroups[0].DBSubnetGroupName' \
                                --output text 2>/dev/null || echo "NOT_FOUND")
                            
                                if [ "$DB_SG_CHECK" != "NOT_FOUND" ] && [ -n "$DB_SG_CHECK" ]; then
                                echo "    Found DB subnet group: $DB_SG_CHECK"
                                DELETE_SG_OUTPUT=$(aws rds delete-db-subnet-group \
                                    --region ${AWS_REGION} \
                                    --db-subnet-group-name achat-app-db-subnet-group 2>&1)
                                
                                if echo "$DELETE_SG_OUTPUT" | grep -q "DBSubnetGroupNotFoundFault"; then
                                    echo "      DB subnet group already deleted"
                                elif echo "$DELETE_SG_OUTPUT" | grep -qi "error"; then
                                    echo "      Error deleting DB subnet group: $DELETE_SG_OUTPUT"
                                else
                                    echo "      ÃƒÂ¢Ã…â€œÃ¢â‚¬Å“ DB subnet group deleted successfully"
                                fi
                                else
                                echo "    DB subnet group not found (already deleted)"
                                fi
                            
                                # Also check for any other achat-app related DB subnet groups
                                echo "  Checking for other achat-app DB subnet groups..."
                                ALL_DB_SUBNET_GROUPS=$(aws rds describe-db-subnet-groups \
                                --region ${AWS_REGION} \
                                --query 'DBSubnetGroups[?contains(DBSubnetGroupName, `achat-app`) == `true`].DBSubnetGroupName' \
                                --output text 2>/dev/null || echo "")
                            
                                if [ -n "$ALL_DB_SUBNET_GROUPS" ]; then
                                for db_sg_name in $ALL_DB_SUBNET_GROUPS; do
                                    echo "    Found additional DB subnet group: $db_sg_name"
                                    aws rds delete-db-subnet-group \
                                        --region ${AWS_REGION} \
                                        --db-subnet-group-name $db_sg_name 2>&1 | grep -v "DBSubnetGroupNotFoundFault" || true
                                    echo "      ÃƒÂ¢Ã…â€œÃ¢â‚¬Å“ Processed DB subnet group: $db_sg_name"
                                done
                                else
                                echo "    No additional DB subnet groups found"
                                fi
                            
                                echo "  ÃƒÂ¢Ã…â€œÃ¢â‚¬Å“ DB subnet group cleanup completed"
                                else
                                echo "  ÃƒÂ¢Ã…Â¡Ã‚Â  Skipping DB subnet group deletion due to remaining RDS instances"
                                fi
                            
                                # Find VPCs with name "achat-app-vpc" (regardless of tags)
                                echo ""
                                echo "Step 1: Finding VPCs named 'achat-app-vpc'..."
                                VPC_IDS=$(aws ec2 describe-vpcs \
                                --region ${AWS_REGION} \
                                --filters "Name=tag:Name,Values=achat-app-vpc" \
                                --query "Vpcs[].VpcId" \
                                --output text 2>/dev/null || echo "")
                            
                                if [ -z "$VPC_IDS" ]; then
                                echo "  No VPCs named 'achat-app-vpc' found. Nothing to clean up."
                                else
                                echo "  Found VPCs to delete: $VPC_IDS"
                                
                                for vpc_id in $VPC_IDS; do
                                    echo ""
                                    echo "=========================================="
                                    echo "Processing VPC: $vpc_id"
                                    echo "=========================================="
                                    
                                    # 1. Terminate EC2 instances in this VPC
                                    echo ""
                                    echo "  1. Terminating EC2 instances in VPC..."
                                    INSTANCE_IDS=$(aws ec2 describe-instances \
                                        --region ${AWS_REGION} \
                                        --filters "Name=vpc-id,Values=$vpc_id" "Name=instance-state-name,Values=running,stopped,stopping" \
                                        --query "Reservations[].Instances[].InstanceId" \
                                        --output text 2>/dev/null || echo "")
                                    
                                    if [ -n "$INSTANCE_IDS" ]; then
                                        echo "    Found instances: $INSTANCE_IDS"
                                        safe_delete "aws ec2 terminate-instances --region ${AWS_REGION} --instance-ids $INSTANCE_IDS"
                                        echo "    Waiting for instances to terminate..."
                                        aws ec2 wait instance-terminated --region ${AWS_REGION} --instance-ids $INSTANCE_IDS 2>/dev/null || sleep 30
                                    else
                                        echo "    No instances found"
                                    fi
                                    
                                    # 2. Delete RDS Instances in VPC
                                    echo ""
                                    echo "  2. Deleting RDS Instances..."
                                    DB_INSTANCES=$(aws rds describe-db-instances \
                                        --region ${AWS_REGION} \
                                        --query 'DBInstances[?DBSubnetGroup.VpcId==`'"$vpc_id"'`].DBInstanceIdentifier' \
                                        --output text 2>/dev/null || echo "")
                                    
                                    if [ -n "$DB_INSTANCES" ]; then
                                        for db_id in $DB_INSTANCES; do
                                            echo "    Deleting RDS instance: $db_id"
                                            safe_delete "aws rds delete-db-instance --region ${AWS_REGION} --db-instance-identifier $db_id --skip-final-snapshot"
                                        done
                                        echo "    Waiting for RDS instances to delete (this may take 5-10 minutes)..."
                                        echo "    Checking deletion status every 30 seconds..."
                                        for db_id in $DB_INSTANCES; do
                                            WAIT_COUNT=0
                                            MAX_WAIT=20  # 20 * 30 seconds = 10 minutes max
                                            while [ \$WAIT_COUNT -lt \$MAX_WAIT ]; do
                                                DB_STATUS=\$(aws rds describe-db-instances \
                                                    --region ${AWS_REGION} \
                                                    --db-instance-identifier \$db_id \
                                                    --query 'DBInstances[0].DBInstanceStatus' \
                                                    --output text 2>/dev/null || echo "deleted")
                                                
                                                if [ "\$DB_STATUS" = "deleted" ] || [ "\$DB_STATUS" = "None" ]; then
                                                    echo "      ÃƒÂ¢Ã…â€œÃ¢â‚¬Å“ RDS instance \$db_id fully deleted"
                                                    break
                                                else
                                                    echo "      Status: \$DB_STATUS (waiting...)"
                                                    sleep 30
                                                    WAIT_COUNT=\$((WAIT_COUNT + 1))
                                                fi
                                            done
                                        done
                                    else
                                        echo "    No RDS instances found"
                                    fi
                                    
                                    # 2.5. Delete DB Subnet Groups (only after RDS is fully deleted)
                                    echo ""
                                    echo "  2.5. Deleting DB Subnet Groups..."
                                    DB_SUBNET_GROUPS=$(aws rds describe-db-subnet-groups \
                                        --region ${AWS_REGION} \
                                        --query 'DBSubnetGroups[?VpcId==`'"$vpc_id"'`].DBSubnetGroupName' \
                                        --output text 2>/dev/null || echo "")
                                    
                                    if [ -n "$DB_SUBNET_GROUPS" ]; then
                                        for db_subnet_group in $DB_SUBNET_GROUPS; do
                                            echo "    Deleting DB subnet group: $db_subnet_group"
                                            safe_delete "aws rds delete-db-subnet-group --region ${AWS_REGION} --db-subnet-group-name $db_subnet_group"
                                        done
                                    else
                                        echo "    No DB subnet groups found"
                                    fi
                                    
                                    # 3. Delete NAT Gateways
                                    echo ""
                                    echo "  3. Deleting NAT Gateways..."
                                    NAT_GW_IDS=$(aws ec2 describe-nat-gateways \
                                        --region ${AWS_REGION} \
                                        --filter "Name=vpc-id,Values=$vpc_id" "Name=state,Values=available" \
                                        --query "NatGateways[].NatGatewayId" \
                                        --output text 2>/dev/null || echo "")
                                    
                                    if [ -n "$NAT_GW_IDS" ]; then
                                        for nat_id in $NAT_GW_IDS; do
                                            echo "    Deleting NAT Gateway: $nat_id"
                                            safe_delete "aws ec2 delete-nat-gateway --region ${AWS_REGION} --nat-gateway-id $nat_id"
                                        done
                                        echo "    Waiting for NAT Gateways to delete..."
                                        sleep 30
                                    else
                                        echo "    No NAT Gateways found"
                                    fi
                                    
                                    # 4. Release Elastic IPs
                                    echo ""
                                    echo "  4. Releasing Elastic IPs..."
                                    ALLOCATION_IDS=$(aws ec2 describe-addresses \
                                        --region ${AWS_REGION} \
                                        --filters "Name=domain,Values=vpc" \
                                        --query "Addresses[].AllocationId" \
                                        --output text 2>/dev/null || echo "")
                                    
                                    if [ -n "$ALLOCATION_IDS" ]; then
                                        for alloc_id in $ALLOCATION_IDS; do
                                            echo "    Releasing $alloc_id"
                                            safe_delete "aws ec2 release-address --region ${AWS_REGION} --allocation-id $alloc_id"
                                        done
                                    else
                                        echo "    No Elastic IPs found"
                                    fi
                                    
                                    # 5. Detach and delete Internet Gateways
                                    echo ""
                                    echo "  5. Deleting Internet Gateways..."
                                    IGW_IDS=$(aws ec2 describe-internet-gateways \
                                        --region ${AWS_REGION} \
                                        --filters "Name=attachment.vpc-id,Values=$vpc_id" \
                                        --query "InternetGateways[].InternetGatewayId" \
                                        --output text 2>/dev/null || echo "")
                                    
                                    if [ -n "$IGW_IDS" ]; then
                                        for igw_id in $IGW_IDS; do
                                            echo "    Detaching and deleting IGW: $igw_id"
                                            safe_delete "aws ec2 detach-internet-gateway --region ${AWS_REGION} --internet-gateway-id $igw_id --vpc-id $vpc_id"
                                            safe_delete "aws ec2 delete-internet-gateway --region ${AWS_REGION} --internet-gateway-id $igw_id"
                                        done
                                    else
                                        echo "    No Internet Gateways found"
                                    fi
                                    
                                    # 6. Delete Subnets
                                    echo ""
                                    echo "  6. Deleting Subnets..."
                                    SUBNET_IDS=$(aws ec2 describe-subnets \
                                        --region ${AWS_REGION} \
                                        --filters "Name=vpc-id,Values=$vpc_id" \
                                        --query "Subnets[].SubnetId" \
                                        --output text 2>/dev/null || echo "")
                                    
                                    if [ -n "$SUBNET_IDS" ]; then
                                        for subnet_id in $SUBNET_IDS; do
                                            echo "    Deleting subnet: $subnet_id"
                                            safe_delete "aws ec2 delete-subnet --region ${AWS_REGION} --subnet-id $subnet_id"
                                        done
                                    else
                                        echo "    No subnets found"
                                    fi
                                    
                                    # 7. Delete custom Route Tables (skip main route table)
                                    echo ""
                                    echo "  7. Deleting Route Tables..."
                                    RTB_IDS=$(aws ec2 describe-route-tables \
                                        --region ${AWS_REGION} \
                                        --filters "Name=vpc-id,Values=$vpc_id" \
                                        --query 'RouteTables[?Associations[0].Main!=`true`].RouteTableId' \
                                        --output text 2>/dev/null || echo "")
                                    
                                    if [ -n "$RTB_IDS" ]; then
                                        for rtb_id in $RTB_IDS; do
                                            echo "    Deleting route table: $rtb_id"
                                            safe_delete "aws ec2 delete-route-table --region ${AWS_REGION} --route-table-id $rtb_id"
                                        done
                                    else
                                        echo "    No custom route tables found"
                                    fi
                                    
                                    # 8. Delete Security Groups (skip default)
                                    echo ""
                                    echo "  8. Deleting Security Groups..."
                                    SG_IDS=$(aws ec2 describe-security-groups \
                                        --region ${AWS_REGION} \
                                        --filters "Name=vpc-id,Values=$vpc_id" \
                                        --query 'SecurityGroups[?GroupName!=`default`].GroupId' \
                                        --output text 2>/dev/null || echo "")
                                    
                                    if [ -n "$SG_IDS" ]; then
                                        for sg_id in $SG_IDS; do
                                            echo "    Deleting security group: $sg_id"
                                            safe_delete "aws ec2 delete-security-group --region ${AWS_REGION} --group-id $sg_id"
                                        done
                                    else
                                        echo "    No custom security groups found"
                                    fi
                                    
                                    # 9. Finally, delete the VPC
                                    echo ""
                                    echo "  9. Deleting VPC..."
                                    echo "    Deleting VPC: $vpc_id"
                                    if aws ec2 delete-vpc --region ${AWS_REGION} --vpc-id $vpc_id 2>/dev/null; then
                                        echo "    ÃƒÂ¢Ã…â€œÃ¢â‚¬Å“ VPC $vpc_id deleted successfully"
                                    else
                                        echo "    ÃƒÂ¢Ã…â€œÃ¢â‚¬â€ Failed to delete VPC $vpc_id (may have dependencies)"
                                    fi
                                    
                                    echo "=========================================="
                                done
                                fi
                            
                                echo ""
                                echo "======================================"
                                echo "ÃƒÂ¢Ã…â€œÃ¢â‚¬Å“ Cleanup completed successfully"
                                echo "======================================"
                            fi
                        '''
                    }
                }

                script {
                    echo 'AWS infrastructure cleanup completed'
                    echo 'Ready for fresh deployment'
                }
            }
        }

        stage('Refresh EC2 Instance Only') {
            when {
                allOf {
                    expression { return params.DEPLOYMENT_MODE == 'REUSE_INFRASTRUCTURE' }
                    expression { return fileExists('terraform/main.tf') }
                }
            }
            steps {
                script {
                    echo '========================================='
                    echo 'Stage 9.6: Refreshing EC2 Instance Only'
                    echo '========================================='
                    echo 'Mode: REUSE_INFRASTRUCTURE'
                    echo 'This will keep VPC, RDS, and other resources'
                    echo 'Only the EC2 instance will be recreated with new user-data'
                }

                withCredentials([file(credentialsId: "${AWS_CREDENTIAL_ID}", variable: 'AWS_CREDENTIALS_FILE')]) {
                    dir(TERRAFORM_DIR) {
                        sh '''
                            echo "Setting up AWS credentials..."
                            mkdir -p ~/.aws
                            cp $AWS_CREDENTIALS_FILE ~/.aws/credentials
                            chmod 600 ~/.aws/credentials
                            
                            echo "======================================"
                            echo "Importing existing infrastructure state..."
                            echo "======================================"
                            
                            # Initialize Terraform first
                            terraform init -input=false
                            
                            # Import existing resources (ignore errors if already in state)
                            echo "Importing VPC and network resources..."
                            VPC_ID=$(aws ec2 describe-vpcs --region ${AWS_REGION} --filters "Name=tag:Name,Values=achat-app-vpc" --query "Vpcs[0].VpcId" --output text 2>/dev/null || echo "")
                            
                            if [ -n "$VPC_ID" ] && [ "$VPC_ID" != "None" ]; then
                                echo "Found existing VPC: $VPC_ID"
                                terraform import -var="docker_image=${TF_VAR_docker_image}" aws_vpc.main $VPC_ID 2>/dev/null || echo "  (already in state)"
                                
                                # Import Internet Gateway
                                IGW_ID=$(aws ec2 describe-internet-gateways --region ${AWS_REGION} --filters "Name=attachment.vpc-id,Values=$VPC_ID" --query "InternetGateways[0].InternetGatewayId" --output text 2>/dev/null || echo "")
                                if [ -n "$IGW_ID" ] && [ "$IGW_ID" != "None" ]; then
                                    echo "Importing IGW: $IGW_ID"
                                    terraform import -var="docker_image=${TF_VAR_docker_image}" aws_internet_gateway.main $IGW_ID 2>/dev/null || echo "  (already in state)"
                                fi
                                
                                # Import public subnet
                                PUBLIC_SUBNET_ID=$(aws ec2 describe-subnets --region ${AWS_REGION} --filters "Name=vpc-id,Values=$VPC_ID" "Name=tag:Name,Values=achat-app-public-subnet" --query "Subnets[0].SubnetId" --output text 2>/dev/null || echo "")
                                if [ -n "$PUBLIC_SUBNET_ID" ] && [ "$PUBLIC_SUBNET_ID" != "None" ]; then
                                    echo "Importing public subnet: $PUBLIC_SUBNET_ID"
                                    terraform import -var="docker_image=${TF_VAR_docker_image}" aws_subnet.public $PUBLIC_SUBNET_ID 2>/dev/null || echo "  (already in state)"
                                fi
                                
                                # Import private subnet
                                PRIVATE_SUBNET_ID=$(aws ec2 describe-subnets --region ${AWS_REGION} --filters "Name=vpc-id,Values=$VPC_ID" "Name=tag:Name,Values=achat-app-private-subnet" --query "Subnets[0].SubnetId" --output text 2>/dev/null || echo "")
                                if [ -n "$PRIVATE_SUBNET_ID" ] && [ "$PRIVATE_SUBNET_ID" != "None" ]; then
                                    echo "Importing private subnet: $PRIVATE_SUBNET_ID"
                                    terraform import -var="docker_image=${TF_VAR_docker_image}" aws_subnet.private $PRIVATE_SUBNET_ID 2>/dev/null || echo "  (already in state)"
                                fi
                                
                                # Import route table
                                RTB_ID=$(aws ec2 describe-route-tables --region ${AWS_REGION} --filters "Name=vpc-id,Values=$VPC_ID" "Name=tag:Name,Values=achat-app-public-rt" --query "RouteTables[0].RouteTableId" --output text 2>/dev/null || echo "")
                                if [ -n "$RTB_ID" ] && [ "$RTB_ID" != "None" ]; then
                                    echo "Importing route table: $RTB_ID"
                                    terraform import -var="docker_image=${TF_VAR_docker_image}" aws_route_table.public $RTB_ID 2>/dev/null || echo "  (already in state)"
                                    
                                    # Import route table association
                                    if [ -n "$PUBLIC_SUBNET_ID" ]; then
                                        RTB_ASSOC_ID=$(aws ec2 describe-route-tables --region ${AWS_REGION} --route-table-id $RTB_ID --query "RouteTables[0].Associations[?SubnetId=='$PUBLIC_SUBNET_ID'].RouteTableAssociationId | [0]" --output text 2>/dev/null || echo "")
                                        if [ -n "$RTB_ASSOC_ID" ] && [ "$RTB_ASSOC_ID" != "None" ]; then
                                            echo "Importing route table association: $RTB_ASSOC_ID"
                                            terraform import -var="docker_image=${TF_VAR_docker_image}" aws_route_table_association.public $RTB_ASSOC_ID 2>/dev/null || echo "  (already in state)"
                                        fi
                                    fi
                                fi
                                
                                # Import security groups
                                APP_SG_ID=$(aws ec2 describe-security-groups --region ${AWS_REGION} --filters "Name=vpc-id,Values=$VPC_ID" "Name=group-name,Values=achat-app-app-sg" --query "SecurityGroups[0].GroupId" --output text 2>/dev/null || echo "")
                                if [ -n "$APP_SG_ID" ] && [ "$APP_SG_ID" != "None" ]; then
                                    echo "Importing app security group: $APP_SG_ID"
                                    terraform import -var="docker_image=${TF_VAR_docker_image}" aws_security_group.app $APP_SG_ID 2>/dev/null || echo "  (already in state)"
                                fi
                                
                                RDS_SG_ID=$(aws ec2 describe-security-groups --region ${AWS_REGION} --filters "Name=vpc-id,Values=$VPC_ID" "Name=group-name,Values=achat-app-rds-sg" --query "SecurityGroups[0].GroupId" --output text 2>/dev/null || echo "")
                                if [ -n "$RDS_SG_ID" ] && [ "$RDS_SG_ID" != "None" ]; then
                                    echo "Importing RDS security group: $RDS_SG_ID"
                                    terraform import -var="docker_image=${TF_VAR_docker_image}" aws_security_group.rds $RDS_SG_ID 2>/dev/null || echo "  (already in state)"
                                fi
                                
                                # Import RDS
                                DB_ID=$(aws rds describe-db-instances --region ${AWS_REGION} --query "DBInstances[?DBName=='achatdb'].DBInstanceIdentifier | [0]" --output text 2>/dev/null || echo "")
                                if [ -n "$DB_ID" ] && [ "$DB_ID" != "None" ]; then
                                    echo "Importing RDS instance: $DB_ID"
                                    terraform import -var="docker_image=${TF_VAR_docker_image}" aws_db_instance.mysql $DB_ID 2>/dev/null || echo "  (already in state)"
                                fi
                                
                                # Import DB subnet group
                                DB_SUBNET_GROUP=$(aws rds describe-db-subnet-groups --region ${AWS_REGION} --query "DBSubnetGroups[?DBSubnetGroupName=='achat-app-db-subnet-group'].DBSubnetGroupName | [0]" --output text 2>/dev/null || echo "")
                                if [ -n "$DB_SUBNET_GROUP" ] && [ "$DB_SUBNET_GROUP" != "None" ]; then
                                    echo "Importing DB subnet group: $DB_SUBNET_GROUP"
                                    terraform import -var="docker_image=${TF_VAR_docker_image}" aws_db_subnet_group.main $DB_SUBNET_GROUP 2>/dev/null || echo "  (already in state)"
                                fi
                            fi
                            
                            echo ""
                            echo "======================================"
                            echo "Destroying only EC2 instance..."
                            echo "======================================"
                            
                            # Destroy only EC2 and EIP
                            terraform destroy -auto-approve \
                              -target=aws_eip_association.app \
                              -target=aws_eip.app \
                              -target=aws_instance.app \
                              -var="docker_image=${TF_VAR_docker_image}" 2>/dev/null || echo "EC2 resources don't exist yet"
                            
                            echo ""
                            echo "======================================"
                            echo "Creating new EC2 instance..."
                            echo "======================================"
                            
                            # Apply full configuration (will reuse existing VPC/RDS, create new EC2)
                            terraform apply -auto-approve \
                              -var="docker_image=${TF_VAR_docker_image}"
                            
                            echo "======================================"
                            echo "ÃƒÂ¢Ã…â€œÃ¢â‚¬Å“ EC2 instance refreshed successfully"
                            echo "======================================"
                        '''
                    }
                }

                script {
                    echo 'EC2 instance recreated with new configuration'
                    echo 'VPC and RDS remain unchanged (faster deployment!)'
                }
            }
        }

        stage('Terraform Init') {
            when {
                expression { 
                    return fileExists('terraform/main.tf') && params.DEPLOYMENT_MODE != 'REUSE_INFRASTRUCTURE'
                }
            }
            steps {
                script {
                    echo '========================================='
                    echo 'Stage 10: Terraform Initialization'
                    echo '========================================='
                }

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
                            echo "AWS Account:"
                            /usr/local/bin/aws sts get-caller-identity
                        '''
                    }
                }

                script {
                    echo 'Terraform initialized successfully'
                }
            }
        }

        stage('Pre-Terraform Validation') {
            when {
                expression { 
                    return fileExists('terraform/main.tf') && params.DEPLOYMENT_MODE != 'REUSE_INFRASTRUCTURE'
                }
            }
            steps {
                script {
                    echo '========================================='
                    echo 'Stage 10.5: Pre-Terraform Validation'
                    echo '========================================='
                    echo 'Checking for resources that could cause conflicts...'
                }

                withCredentials([file(credentialsId: "${AWS_CREDENTIAL_ID}", variable: 'AWS_CREDENTIALS_FILE')]) {
                    dir(TERRAFORM_DIR) {
                        sh '''
                            echo "Setting up AWS credentials..."
                            mkdir -p ~/.aws
                            cp $AWS_CREDENTIALS_FILE ~/.aws/credentials
                            chmod 600 ~/.aws/credentials
                            
                            AWS_REGION="us-east-1"
                            
                            echo ""
                            echo "=========================================="
                            echo "Pre-Check 1: Verifying EKS Cluster Status"
                            echo "=========================================="
                            
                            CLUSTER_NAME="achat-app-eks-cluster"
                            
                            # Check if cluster exists
                            CLUSTER_STATUS=$(aws eks describe-cluster \
                                --region ${AWS_REGION} \
                                --name ${CLUSTER_NAME} \
                                --query 'cluster.status' \
                                --output text 2>/dev/null || echo "NOT_FOUND")
                            
                            echo "  Cluster status: $CLUSTER_STATUS"
                            
                            if [ "$CLUSTER_STATUS" = "ACTIVE" ]; then
                                echo "  ÃƒÂ¢Ã‚ÂÃ…â€™ ERROR: EKS cluster '${CLUSTER_NAME}' already exists!"
                                echo "  This will cause Terraform to fail with 'cluster already exists' error."
                                echo "  Please run CLEANUP_AND_DEPLOY mode to delete existing resources first."
                                exit 1
                            elif [ "$CLUSTER_STATUS" = "CREATING" ]; then
                                echo "  ÃƒÂ¢Ã‚ÂÃ…â€™ ERROR: EKS cluster '${CLUSTER_NAME}' is currently CREATING!"
                                echo "  Please wait for creation to complete or delete it manually."
                                exit 1
                            elif [ "$CLUSTER_STATUS" = "DELETING" ]; then
                                echo "  ÃƒÂ¢Ã…Â¡Ã‚Â  WARNING: EKS cluster '${CLUSTER_NAME}' is currently DELETING."
                                echo "  This is expected if cleanup just ran. Terraform Apply will wait for deletion."
                            elif [ "$CLUSTER_STATUS" = "NOT_FOUND" ]; then
                                echo "  ÃƒÂ¢Ã…â€œÃ¢â‚¬Å“ EKS cluster does not exist - ready to create"
                            else
                                echo "  ÃƒÂ¢Ã…Â¡Ã‚Â  Unexpected cluster status: $CLUSTER_STATUS"
                            fi
                            
                            echo ""
                            echo "=========================================="
                            echo "Pre-Check 2: Verifying DB Subnet Group"
                            echo "=========================================="
                            
                            DB_SUBNET_GROUP_NAME="achat-app-db-subnet-group"
                            
                            # Check if DB subnet group exists
                            DB_SG_EXISTS=$(aws rds describe-db-subnet-groups \
                                --region ${AWS_REGION} \
                                --db-subnet-group-name ${DB_SUBNET_GROUP_NAME} \
                                --query 'DBSubnetGroups[0].DBSubnetGroupName' \
                                --output text 2>/dev/null || echo "NOT_FOUND")
                            
                            if [ "$DB_SG_EXISTS" != "NOT_FOUND" ] && [ -n "$DB_SG_EXISTS" ]; then
                                echo "  ÃƒÂ¢Ã‚ÂÃ…â€™ ERROR: DB subnet group '${DB_SUBNET_GROUP_NAME}' already exists!"
                                echo "  This will cause Terraform to fail with 'DBSubnetGroupAlreadyExists' error."
                                echo "  Please run CLEANUP_AND_DEPLOY mode to delete existing resources first."
                                echo ""
                                echo "  Or delete manually with:"
                                echo "    aws rds delete-db-subnet-group --db-subnet-group-name ${DB_SUBNET_GROUP_NAME} --region ${AWS_REGION}"
                                exit 1
                            else
                                echo "  ÃƒÂ¢Ã…â€œÃ¢â‚¬Å“ DB subnet group does not exist - ready to create"
                            fi
                            
                            echo ""
                            echo "=========================================="
                            echo "Pre-Check 3: Checking for RDS Instances"
                            echo "=========================================="
                            
                            # Check for any achat-app RDS instances
                            RDS_INSTANCES=$(aws rds describe-db-instances \
                                --region ${AWS_REGION} \
                                --query 'DBInstances[?contains(DBInstanceIdentifier, `achat-app`) == `true`].DBInstanceIdentifier' \
                                --output text 2>/dev/null || echo "")
                            
                            if [ -n "$RDS_INSTANCES" ]; then
                                echo "  ÃƒÂ¢Ã…Â¡Ã‚Â  WARNING: Found existing RDS instances: $RDS_INSTANCES"
                                for rds_id in $RDS_INSTANCES; do
                                    RDS_STATUS=$(aws rds describe-db-instances \
                                        --region ${AWS_REGION} \
                                        --db-instance-identifier $rds_id \
                                        --query 'DBInstances[0].DBInstanceStatus' \
                                        --output text 2>/dev/null || echo "unknown")
                                    echo "    - $rds_id: $RDS_STATUS"
                                done
                                
                                # Only fail if RDS is in a problematic state
                                if echo "$RDS_STATUS" | grep -qE "available|creating|backing-up"; then
                                    echo "  ÃƒÂ¢Ã‚ÂÃ…â€™ ERROR: Active RDS instance(s) found!"
                                    echo "  Please run CLEANUP_AND_DEPLOY mode to delete existing resources first."
                                    exit 1
                                fi
                            else
                                echo "  ÃƒÂ¢Ã…â€œÃ¢â‚¬Å“ No RDS instances found - ready to create"
                            fi
                            
                            echo ""
                            echo "=========================================="
                            echo "ÃƒÂ¢Ã…â€œÃ¢â‚¬Å“ All Pre-Checks Passed!"
                            echo "=========================================="
                            echo "  ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¢ EKS cluster: Ready"
                            echo "  ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¢ DB subnet group: Ready"
                            echo "  ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¢ RDS instances: Ready"
                            echo ""
                            echo "Proceeding with Terraform Plan..."
                        '''
                    }
                }

                script {
                    echo 'Pre-Terraform validation completed successfully'
                }
            }
        }

        stage('Terraform Plan') {
            when {
                expression { 
                    return fileExists('terraform/main.tf') && params.DEPLOYMENT_MODE != 'REUSE_INFRASTRUCTURE'
                }
            }
            steps {
                script {
                    echo '========================================='
                    echo 'Stage 11: Terraform Plan'
                    echo '========================================='
                }

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
                                    echo "ÃƒÂ¢Ã…â€œÃ¢â‚¬Å“ Terraform plan created successfully!"
                                    SUCCESS=true
                                else
                                    EXIT_CODE=$?
                                    echo ""
                                    echo "ÃƒÂ¢Ã…â€œÃ¢â‚¬â€ Terraform plan failed (exit code: $EXIT_CODE)"
                                    
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
                                        echo "ÃƒÂ¢Ã…â€œÃ¢â‚¬â€ÃƒÂ¢Ã…â€œÃ¢â‚¬â€ÃƒÂ¢Ã…â€œÃ¢â‚¬â€ Terraform plan failed after $MAX_RETRIES attempts ÃƒÂ¢Ã…â€œÃ¢â‚¬â€ÃƒÂ¢Ã…â€œÃ¢â‚¬â€ÃƒÂ¢Ã…â€œÃ¢â‚¬â€"
                                        exit $EXIT_CODE
                                    fi
                                fi
                            done

                            echo ""
                            echo "Plan saved to: tfplan"
                        '''
                    }
                }

                script {
                    echo 'Terraform plan created successfully'
                    echo 'Review the plan above before applying'
                }
            }
        }

        stage('Terraform Apply') {
            when {
                expression { 
                    return fileExists('terraform/main.tf') && params.DEPLOYMENT_MODE != 'REUSE_INFRASTRUCTURE'
                }
            }
            steps {
                script {
                    echo '========================================='
                    echo 'Stage 12: Terraform Apply (Deploy to AWS)'
                    echo '========================================='
                }

                // Ask for approval before deploying to AWS (optional)
                // Uncomment the following lines to require manual approval
                // input {
                //     message "Deploy to AWS?"
                //     ok "Deploy"
                // }

                withCredentials([file(credentialsId: "${AWS_CREDENTIAL_ID}", variable: 'AWS_CREDENTIALS_FILE')]) {
                    dir(TERRAFORM_DIR) {
                        sh '''
                            echo "Setting up AWS credentials..."
                            mkdir -p ~/.aws
                            cp $AWS_CREDENTIALS_FILE ~/.aws/credentials
                            chmod 600 ~/.aws/credentials
                            
                            # Pre-check: Wait for any existing EKS cluster to be fully deleted
                            echo ""
                            echo "=========================================="
                            echo "Pre-Check: Verifying EKS cluster name availability"
                            echo "=========================================="
                            
                            CLUSTER_NAME="achat-app-eks-cluster"
                            CLUSTER_STATUS=$(aws eks describe-cluster \
                                --region ${AWS_REGION} \
                                --name $CLUSTER_NAME \
                                --query 'cluster.status' \
                                --output text 2>/dev/null || echo "NOT_FOUND")
                            
                            if [ "$CLUSTER_STATUS" != "NOT_FOUND" ]; then
                                echo "ÃƒÂ¢Ã…Â¡Ã‚Â  Found existing cluster: $CLUSTER_NAME"
                                echo "  Current status: $CLUSTER_STATUS"
                                
                                if [ "$CLUSTER_STATUS" = "DELETING" ]; then
                                    echo ""
                                    echo "Cluster is being deleted. Waiting for deletion to complete..."
                                    echo "This may take 5-10 minutes..."
                                    echo ""
                                    
                                    WAIT_START=$(date +%s)
                                    MAX_WAIT=900  # 15 minutes
                                    
                                    while true; do
                                        sleep 30
                                        
                                        CURRENT_STATUS=$(aws eks describe-cluster \
                                            --region ${AWS_REGION} \
                                            --name $CLUSTER_NAME \
                                            --query 'cluster.status' \
                                            --output text 2>/dev/null || echo "DELETED")
                                        
                                        if [ "$CURRENT_STATUS" = "DELETED" ] || echo "$CURRENT_STATUS" | grep -q "ResourceNotFoundException"; then
                                            echo "ÃƒÂ¢Ã…â€œÃ¢â‚¬Å“ Cluster $CLUSTER_NAME is now fully deleted"
                                            break
                                        fi
                                        
                                        ELAPSED=$(($(date +%s) - WAIT_START))
                                        if [ $ELAPSED -gt $MAX_WAIT ]; then
                                            echo "ÃƒÂ¢Ã…â€œÃ¢â‚¬â€ Timeout waiting for cluster deletion (${MAX_WAIT}s)"
                                            echo "  Current status: $CURRENT_STATUS"
                                            exit 1
                                        fi
                                        
                                        echo "  Status: $CURRENT_STATUS (waited ${ELAPSED}s / ${MAX_WAIT}s)"
                                    done
                                    echo ""
                                elif [ "$CLUSTER_STATUS" = "ACTIVE" ]; then
                                    echo "ÃƒÂ¢Ã…â€œÃ¢â‚¬â€ ERROR: Cluster $CLUSTER_NAME is still ACTIVE!"
                                    echo "  This should have been deleted by the cleanup stage."
                                    echo "  Please run with CLEANUP_AND_DEPLOY mode or manually delete the cluster."
                                    exit 1
                                else
                                    echo "ÃƒÂ¢Ã…Â¡Ã‚Â  Unexpected cluster status: $CLUSTER_STATUS"
                                    echo "  Proceeding anyway..."
                                fi
                            else
                                echo "ÃƒÂ¢Ã…â€œÃ¢â‚¬Å“ No existing cluster found - name is available"
                            fi
                            
                            echo ""
                            echo "=========================================="
                            echo "Applying Terraform plan..."
                            echo "=========================================="
                            
                            # Retry terraform apply up to 3 times with exponential backoff
                            MAX_RETRIES=3
                            RETRY_COUNT=0
                            SUCCESS=false
                            
                            while [ $RETRY_COUNT -lt $MAX_RETRIES ] && [ "$SUCCESS" = false ]; do
                                RETRY_COUNT=$((RETRY_COUNT + 1))
                                echo ""
                                echo "=========================================="
                                echo "Terraform Apply - Attempt $RETRY_COUNT of $MAX_RETRIES"
                                echo "=========================================="
                                
                                if terraform apply -auto-approve tfplan; then
                                    echo ""
                                    echo "ÃƒÂ¢Ã…â€œÃ¢â‚¬Å“ Terraform apply completed successfully!"
                                    SUCCESS=true
                                else
                                    EXIT_CODE=$?
                                    echo ""
                                    echo "ÃƒÂ¢Ã…â€œÃ¢â‚¬â€ Terraform apply failed (exit code: $EXIT_CODE)"
                                    
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
                                        echo ""
                                        echo "Note: Will need to recreate plan after retry..."
                                        sleep $DELAY
                                        
                                        # Recreate plan for retry (plan file may be consumed)
                                        echo "Recreating Terraform plan for retry..."
                                        if ! terraform plan \
                                          -var="docker_image=${TF_VAR_docker_image}" \
                                          -out=tfplan \
                                          -input=false; then
                                            echo "ÃƒÂ¢Ã…â€œÃ¢â‚¬â€ Failed to recreate plan for retry"
                                            exit 1
                                        fi
                                    else
                                        echo ""
                                        echo "ÃƒÂ¢Ã…â€œÃ¢â‚¬â€ÃƒÂ¢Ã…â€œÃ¢â‚¬â€ÃƒÂ¢Ã…â€œÃ¢â‚¬â€ Terraform apply failed after $MAX_RETRIES attempts ÃƒÂ¢Ã…â€œÃ¢â‚¬â€ÃƒÂ¢Ã…â€œÃ¢â‚¬â€ÃƒÂ¢Ã…â€œÃ¢â‚¬â€"
                                        exit $EXIT_CODE
                                    fi
                                fi
                            done

                            echo ""
                            echo "Deployment complete!"
                        '''
                    }
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

        stage('Debug EC2 Instance') {
            when {
                expression { return fileExists('terraform/main.tf') }
            }
            steps {
                script {
                    echo '========================================='
                    echo 'Stage 13.5: Debug EC2 Instance'
                    echo '========================================='
                }

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
        }

        stage('Health Check AWS Deployment') {
            when {
                expression { return fileExists('terraform/main.tf') }
            }
            steps {
                script {
                    echo '========================================='
                    echo 'Stage 14: Health Check'
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
                            def maxAttempts = 30  // 30 attempts
                            def checkInterval = 20  // 20 seconds between checks = 10 minutes total
                            def healthy = false
                            
                            echo "Starting health checks (max ${maxAttempts} attempts, ${checkInterval}s intervals)..."
                            echo "Maximum wait time: ${maxAttempts * checkInterval / 60} minutes"
                            echo ""
                            
                            for (int i = 1; i <= maxAttempts && !healthy; i++) {
                                try {
                                    echo "Health check attempt ${i}/${maxAttempts}..."
                                    
                                    def response = sh(
                                        script: "curl -f -s -o /dev/null -w '%{http_code}' ${healthUrl} || echo '000'",
                                        returnStdout: true
                                    ).trim()
                                    
                                    if (response == '200') {
                                        healthy = true
                                        echo "ÃƒÂ¢Ã…â€œÃ¢â‚¬Å“ Application is healthy! (HTTP ${response})"
                                        
                                        // Show actual health response
                                        sh "curl -s ${healthUrl} || echo 'Could not fetch health details'"
                                    } else {
                                        echo "  Status: HTTP ${response} (not ready yet)"
                                        if (i < maxAttempts) {
                                            echo "  Waiting ${checkInterval} seconds before next check..."
                                            sleep(checkInterval)
                                        }
                                    }
                                } catch (Exception e) {
                                    echo "  Connection failed: ${e.message}"
                                    if (i < maxAttempts) {
                                        echo "  Waiting ${checkInterval} seconds before retry..."
                                        sleep(checkInterval)
                                    }
                                }
                            }
                            
                            if (!healthy) {
                                error("Application failed to become healthy after ${maxAttempts} attempts (${maxAttempts * checkInterval / 60} minutes)")
                            }
                            
                            echo ""
                            echo "ÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚Â"
                            echo "ÃƒÂ¢Ã…â€œÃ¢â‚¬Å“ Application is healthy and responding!"
                            echo "ÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚Â"
                        } else {
                            echo "Could not retrieve application URL"
                        }
                    }
                }
            }
        }

        stage('Build Frontend') {
            when {
                expression { return fileExists('frontend/package.json') }
            }
            steps {
                script {
                    echo '========================================='
                    echo 'Stage 10: Building Frontend Application'
                    echo '========================================='
                }
                
                dir('frontend') {
                    script {
                        // Get backend URL from Terraform output
                        def backendUrl = ''
                        dir("../${TERRAFORM_DIR}") {
                            backendUrl = sh(
                                script: 'terraform output -raw application_url 2>/dev/null || echo ""',
                                returnStdout: true
                            ).trim()
                        }
                        
                        if (backendUrl) {
                            echo "Backend URL: ${backendUrl}"
                            
                            // Create .env.production file with correct backend URL
                            sh """
                                echo "REACT_APP_API_URL=${backendUrl}" > .env.production
                                cat .env.production
                            """
                        } else {
                            echo "Warning: Could not get backend URL from Terraform"
                        }
                        
                        // Install dependencies and build
                        sh '''
                            npm install
                            npm run build
                        '''
                    }
                }
                
                script {
                    echo 'ÃƒÂ¢Ã…â€œÃ¢â‚¬Å“ Frontend built successfully'
                }
            }
        }

        stage('Build Frontend Docker Image') {
            when {
                expression { return fileExists('frontend/Dockerfile') }
            }
            steps {
                script {
                    echo '========================================='
                    echo 'Stage 11: Building Frontend Docker Image'
                    echo '========================================='
                }
                
                dir('frontend') {
                    script {
                        // Build Docker image with continuous retry on failure (15s delay)
                        sh """
                            attempt=0
                            while true; do
                              attempt=\$((attempt + 1))
                              echo "Frontend docker build attempt \${attempt}..."
                              if docker build -t ${DOCKER_REGISTRY}/habibmanai/achat-frontend:${BUILD_NUMBER} .; then
                                docker tag ${DOCKER_REGISTRY}/habibmanai/achat-frontend:${BUILD_NUMBER} ${DOCKER_REGISTRY}/habibmanai/achat-frontend:latest
                                echo "ÃƒÂ¢Ã…â€œÃ¢â‚¬Å“ Frontend docker build succeeded on attempt \${attempt}"
                                break
                              else
                                echo "Frontend docker build failed on attempt \${attempt} ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â retrying after 15s (likely network/TLS error)"
                                sleep 15
                              fi
                            done
                        """
                    }
                }
                
                script {
                    echo 'ÃƒÂ¢Ã…â€œÃ¢â‚¬Å“ Frontend Docker image built successfully'
                }
            }
        }

        stage('Push Frontend Docker Image') {
            when {
                expression { return fileExists('frontend/Dockerfile') }
            }
            steps {
                script {
                    echo '========================================='
                    echo 'Stage 12: Pushing Frontend Image to Docker Hub'
                    echo '========================================='
                }
                
                script {
                    // Push to Docker Hub
                    withCredentials([usernamePassword(
                        credentialsId: "${DOCKER_CREDENTIAL_ID}",
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )]) {
                        // Retry Docker login with exponential backoff
                        def maxRetries = 3
                        def retryDelay = 10
                        def loginSuccess = false
                        
                        for (int i = 0; i < maxRetries && !loginSuccess; i++) {
                            try {
                                if (i > 0) {
                                    echo "Retry login attempt ${i + 1}/${maxRetries} after ${retryDelay}s delay..."
                                    sleep(retryDelay)
                                    retryDelay *= 2
                                }
                                
                                sh """
                                    echo "\${DOCKER_PASS}" | docker login -u "\${DOCKER_USER}" --password-stdin ${DOCKER_REGISTRY}
                                """
                                
                                loginSuccess = true
                                echo "ÃƒÂ¢Ã…â€œÃ¢â‚¬Å“ Docker login successful!"
                            } catch (Exception e) {
                                echo "Docker login attempt ${i + 1} failed: ${e.message}"
                                if (i == maxRetries - 1) {
                                    error("Failed to login to Docker Hub after ${maxRetries} attempts")
                                }
                            }
                        }
                        
                        // Retry Docker push with exponential backoff
                        maxRetries = 3
                        retryDelay = 10
                        def pushSuccess = false
                        
                        for (int i = 0; i < maxRetries && !pushSuccess; i++) {
                            try {
                                if (i > 0) {
                                    echo "Retry push attempt ${i + 1}/${maxRetries} after ${retryDelay}s delay..."
                                    sleep(retryDelay)
                                    retryDelay *= 2
                                }
                                
                                sh """
                                    docker push ${DOCKER_REGISTRY}/habibmanai/achat-frontend:${BUILD_NUMBER}
                                    docker push ${DOCKER_REGISTRY}/habibmanai/achat-frontend:latest
                                """
                                
                                pushSuccess = true
                                echo "ÃƒÂ¢Ã…â€œÃ¢â‚¬Å“ Frontend Docker push successful!"
                            } catch (Exception e) {
                                echo "Frontend push attempt ${i + 1} failed: ${e.message}"
                                if (i == maxRetries - 1) {
                                    error("Failed to push frontend to Docker Hub after ${maxRetries} attempts")
                                }
                            }
                        }
                    }
                }
                
                script {
                    echo 'ÃƒÂ¢Ã…â€œÃ¢â‚¬Å“ Frontend image pushed to Docker Hub'
                    echo "Image: ${DOCKER_REGISTRY}/habibmanai/achat-frontend:${BUILD_NUMBER}"
                }
            }
        }

        stage('Deploy to EKS') {
            when {
                expression { return fileExists('k8s/deployment.yaml') }
            }
            steps {
                script {
                    echo '========================================='
                    echo 'Stage 13: Deploying to AWS EKS'
                    echo '========================================='
                }
                
                // Configure kubectl for EKS - using withCredentials instead of withAWS
                withCredentials([file(credentialsId: "${AWS_CREDENTIAL_ID}", variable: 'AWS_CREDENTIALS_FILE')]) {
                    dir("${TERRAFORM_DIR}") {
                        script {
                            // Get EKS cluster name from Terraform
                            def eksClusterName = sh(
                                script: '''
                                    # Parse and export AWS credentials
                                    export AWS_ACCESS_KEY_ID=$(grep aws_access_key_id "$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                    export AWS_SECRET_ACCESS_KEY=$(grep aws_secret_access_key "$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                    export AWS_SESSION_TOKEN=$(grep aws_session_token "$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                    export AWS_DEFAULT_REGION=us-east-1
                                    
                                    terraform output -raw eks_cluster_name 2>/dev/null || echo ""
                                ''',
                                returnStdout: true
                            ).trim()
                            
                            if (eksClusterName) {
                                echo "Configuring kubectl for EKS cluster: ${eksClusterName}"
                                
                                // Update kubeconfig and deploy
                                sh """
                                    # Parse and export AWS credentials
                                    export AWS_ACCESS_KEY_ID=\$(grep aws_access_key_id "\$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                    export AWS_SECRET_ACCESS_KEY=\$(grep aws_secret_access_key "\$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                    export AWS_SESSION_TOKEN=\$(grep aws_session_token "\$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                    export AWS_DEFAULT_REGION=us-east-1
                                    
                                    echo "AWS credentials configured from file"
                                    
                                    # Update kubeconfig
                                    aws eks update-kubeconfig \\
                                        --region ${AWS_REGION} \\
                                        --name ${eksClusterName}
                                """
                                
                                // Get RDS endpoint from Terraform
                                def rdsEndpoint = sh(
                                    script: '''
                                        # Parse and export AWS credentials
                                        export AWS_ACCESS_KEY_ID=$(grep aws_access_key_id "$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                        export AWS_SECRET_ACCESS_KEY=$(grep aws_secret_access_key "$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                        export AWS_SESSION_TOKEN=$(grep aws_session_token "$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                        export AWS_DEFAULT_REGION=us-east-1
                                        
                                        terraform output -raw rds_endpoint 2>/dev/null || echo ""
                                    ''',
                                    returnStdout: true
                                ).trim()
                                
                                if (rdsEndpoint) {
                                    echo "RDS Endpoint: ${rdsEndpoint}"
                                    echo "Updating ConfigMap with RDS endpoint..."
                                } else {
                                    echo "WARNING: Could not retrieve RDS endpoint!"
                                }
                                
                                // Deploy to EKS
                                sh """
                                    # Create namespace first
                                    echo "Creating namespace..."
                                    kubectl apply -f ../k8s/namespace.yaml
                                    
                                    # Apply secrets and configmaps
                                    echo "Applying secrets and configmaps..."
                                    kubectl apply -f ../k8s/secret.yaml
                                    
                                    # Update ConfigMap with actual RDS endpoint
                                    if [ -n "${rdsEndpoint}" ]; then
                                        echo "Updating ConfigMap with RDS endpoint: ${rdsEndpoint}"
                                        cat ../k8s/configmap.yaml | \\
                                            sed "s|RDS_ENDPOINT_PLACEHOLDER|${rdsEndpoint}|g" | \\
                                            kubectl apply -f -
                                    else
                                        echo "WARNING: Applying ConfigMap without RDS endpoint update!"
                                        kubectl apply -f ../k8s/configmap.yaml
                                    fi
                                    
                                    # Apply all other Kubernetes manifests (deployments, services, etc.)
                                    echo "Applying deployments and services..."
                                    kubectl apply -f ../k8s/deployment.yaml
                                    kubectl apply -f ../k8s/frontend-deployment.yaml
                                    kubectl apply -f ../k8s/service.yaml
                                    kubectl apply -f ../k8s/hpa.yaml
                                    
                                    # Now update deployment images to specific build versions
                                    echo "Updating deployment images to build ${BUILD_NUMBER}..."
                                    kubectl set image deployment/achat-app -n achat-app \\
                                        achat-app=${DOCKER_REGISTRY}/habibmanai/${DOCKER_IMAGE_NAME}:${BUILD_NUMBER} \\
                                        --record
                                    
                                    kubectl set image deployment/achat-frontend -n achat-app \\
                                        frontend=${DOCKER_REGISTRY}/habibmanai/achat-frontend:${BUILD_NUMBER} \\
                                        --record
                                    
                                    # Wait for backend rollout
                                    echo "Waiting for backend deployment..."
                                    kubectl rollout status deployment/achat-app -n achat-app --timeout=5m
                                    
                                    # Wait for frontend rollout
                                    echo "Waiting for frontend deployment..."
                                    kubectl rollout status deployment/achat-frontend -n achat-app --timeout=5m
                                    
                                    # Get service endpoints
                                    echo ""
                                    echo "=== EKS Services ==="
                                    kubectl get svc -n achat-app
                                    
                                    # Get Load Balancer URLs
                                    echo ""
                                    echo "=== Frontend LoadBalancer URL ==="
                                    FRONTEND_URL=\$(kubectl get svc achat-frontend -n achat-app -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || echo "Pending...")
                                    echo "Frontend will be accessible at: http://\${FRONTEND_URL}"
                                    
                                    echo ""
                                    echo "=== Backend LoadBalancer URL ==="
                                    BACKEND_URL=\$(kubectl get svc achat-app -n achat-app -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || echo "Pending...")
                                    echo "Backend will be accessible at: http://\${BACKEND_URL}/SpringMVC"
                                """
                                
                                echo '✓ Application deployed to EKS successfully!'
                            } else {
                                echo '⚠  EKS cluster not found. Skipping EKS deployment.'
                                echo 'Run terraform apply to create EKS cluster first.'
                            }
                        }
                    }
                }
            }
        }

        stage('Deploy to Local Kubernetes') {
            when {
                expression { return fileExists('k8s/deployment.yaml') }
            }
            steps {
                script {
                    echo '========================================='
                    echo 'Stage 14: Deploying to Local Kubernetes (Docker Desktop)'
                    echo '========================================='
                }
                
                // Deploy to Kubernetes
                withKubeConfig([credentialsId: 'kubeconfig-credentials']) {
                    sh """
                        # Update deployment image to use current build number
                        kubectl set image deployment/achat-app -n achat-app \\
                            achat-app=${DOCKER_REGISTRY}/habibmanai/${DOCKER_IMAGE_NAME}:${BUILD_NUMBER} \\
                            --record || echo "Deployment doesn't exist yet, applying manifests..."
                        
                        # Apply all manifests
                        kubectl apply -f k8s/
                        
                        # Wait for rollout to complete
                        kubectl rollout status deployment/achat-app -n achat-app
                    """
                }
                
                script {
                    echo 'ÃƒÆ’Ã‚Â¢Ãƒâ€¦Ã¢â‚¬Å“ÃƒÂ¢Ã¢â€šÂ¬Ã…â€œ Application deployed to Kubernetes'
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
                echo 'ÃƒÆ’Ã‚Â¢Ãƒâ€¦Ã¢â‚¬Å“ÃƒÂ¢Ã¢â€šÂ¬Ã…â€œÃƒÆ’Ã‚Â¢Ãƒâ€¦Ã¢â‚¬Å“ÃƒÂ¢Ã¢â€šÂ¬Ã…â€œÃƒÆ’Ã‚Â¢Ãƒâ€¦Ã¢â‚¬Å“ÃƒÂ¢Ã¢â€šÂ¬Ã…â€œ Pipeline completed successfully! ÃƒÆ’Ã‚Â¢Ãƒâ€¦Ã¢â‚¬Å“ÃƒÂ¢Ã¢â€šÂ¬Ã…â€œÃƒÆ’Ã‚Â¢Ãƒâ€¦Ã¢â‚¬Å“ÃƒÂ¢Ã¢â€šÂ¬Ã…â€œÃƒÆ’Ã‚Â¢Ãƒâ€¦Ã¢â‚¬Å“ÃƒÂ¢Ã¢â€šÂ¬Ã…â€œ'
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
                echo 'ÃƒÂ¢Ã…â€œÃ¢â‚¬â€ÃƒÂ¢Ã…â€œÃ¢â‚¬â€ÃƒÂ¢Ã…â€œÃ¢â‚¬â€ Pipeline failed! ÃƒÂ¢Ã…â€œÃ¢â‚¬â€ÃƒÂ¢Ã…â€œÃ¢â‚¬â€ÃƒÂ¢Ã…â€œÃ¢â‚¬â€'
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
                echo 'ÃƒÆ’Ã‚Â¢Ãƒâ€¦Ã‚Â¡Ãƒâ€šÃ‚Â  Pipeline is unstable ÃƒÆ’Ã‚Â¢Ãƒâ€¦Ã‚Â¡Ãƒâ€šÃ‚Â '
            }
        }
    }
}
