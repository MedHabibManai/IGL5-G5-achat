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

        stage('Terraform Init') {
            when {
                expression { return fileExists('terraform/main.tf') }
            }
            steps {
                script {
                    echo '========================================='
                    echo 'Stage 10: Terraform Initialization'
                    echo '========================================='
                }

                dir(TERRAFORM_DIR) {
                    sh '''
                        echo "Initializing Terraform..."
                        terraform init -input=false

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
                    echo 'Stage 11: Terraform Plan'
                    echo '========================================='
                }

                dir(TERRAFORM_DIR) {
                    sh '''
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
                    echo 'Stage 12: Terraform Apply (Deploy to AWS)'
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

                        // Parse and display key information
                        def outputsJson = readJSON text: outputs

                        if (outputsJson.application_url) {
                            def appUrl = outputsJson.application_url.value
                            echo ""
                            echo "========================================="
                            echo "APPLICATION DEPLOYED SUCCESSFULLY!"
                            echo "========================================="
                            echo "Application URL: ${appUrl}"
                            echo ""

                            if (outputsJson.health_check_url) {
                                echo "Health Check: ${outputsJson.health_check_url.value}"
                            }

                            if (outputsJson.public_ip) {
                                echo "Public IP: ${outputsJson.public_ip.value}"
                            }

                            echo "========================================="
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

