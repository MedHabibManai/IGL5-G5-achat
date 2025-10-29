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
        SONAR_HOST_URL = 'http://sonarqube:9000'
        SONAR_PROJECT_KEY = 'tn.esprit.rh:achat'
        
        // Nexus (Phase 3)
        NEXUS_URL = 'http://nexus:8081'
        NEXUS_REPOSITORY = 'maven-releases'
        NEXUS_CREDENTIAL_ID = 'nexus-credentials'
        
        // Docker (Phase 5)
        DOCKER_IMAGE = "achat-app:${BUILD_NUMBER}"
        DOCKER_REGISTRY = 'your-docker-registry'  // Update with your registry
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
                    echo "✓ Successfully checked out branch: ${env.GIT_BRANCH}"
                    echo "✓ Commit: ${env.GIT_COMMIT}"
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
                    echo '✓ Build completed successfully'
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
                    echo '✓ All unit tests passed'
                }
            }
            
            post {
                always {
                    // Publish JUnit test results
                    junit '**/target/surefire-reports/*.xml'
                    
                    script {
                        echo '✓ Test results published'
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
                    echo "✓ Application packaged: ${ARTIFACT_NAME}"
                }
            }
            
            post {
                success {
                    // Archive the artifacts
                    archiveArtifacts artifacts: '**/target/*.jar', fingerprint: true
                    
                    script {
                        echo '✓ Artifacts archived successfully'
                    }
                }
            }
        }
        
        stage('Code Quality Analysis - SonarQube') {
            when {
                expression { return fileExists('sonar-project.properties') }
            }
            steps {
                script {
                    echo '========================================='
                    echo 'Stage 5: SonarQube Code Quality Analysis'
                    echo '========================================='
                    echo 'Note: This stage requires Phase 2 setup'
                }
                
                // Run SonarQube analysis
                withSonarQubeEnv('SonarQube-Server') {
                    sh '''
                        mvn sonar:sonar \
                          -Dsonar.projectKey=${SONAR_PROJECT_KEY} \
                          -Dsonar.host.url=${SONAR_HOST_URL}
                    '''
                }
                
                script {
                    echo '✓ SonarQube analysis completed'
                }
            }
        }
        
        stage('Quality Gate') {
            when {
                expression { return fileExists('sonar-project.properties') }
            }
            steps {
                script {
                    echo '========================================='
                    echo 'Stage 6: Waiting for Quality Gate'
                    echo '========================================='
                }
                
                // Wait for SonarQube Quality Gate result
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: false
                }
                
                script {
                    echo '✓ Quality Gate check completed'
                }
            }
        }
        
        stage('Upload to Nexus') {
            when {
                branch 'main'
                expression { return fileExists('pom.xml') }
            }
            steps {
                script {
                    echo '========================================='
                    echo 'Stage 7: Uploading artifacts to Nexus'
                    echo '========================================='
                    echo 'Note: This stage requires Phase 3 setup'
                }
                
                // Upload to Nexus Repository
                nexusArtifactUploader(
                    nexusVersion: 'nexus3',
                    protocol: 'http',
                    nexusUrl: "${NEXUS_URL}",
                    groupId: 'tn.esprit.rh',
                    version: "${PROJECT_VERSION}-${BUILD_NUMBER}",
                    repository: "${NEXUS_REPOSITORY}",
                    credentialsId: "${NEXUS_CREDENTIAL_ID}",
                    artifacts: [
                        [
                            artifactId: "${PROJECT_NAME}",
                            classifier: '',
                            file: "target/${PROJECT_NAME}-${PROJECT_VERSION}.jar",
                            type: 'jar'
                        ]
                    ]
                )
                
                script {
                    echo '✓ Artifacts uploaded to Nexus successfully'
                }
            }
        }
        
        stage('Build Docker Image') {
            when {
                branch 'main'
                expression { return fileExists('Dockerfile') }
            }
            steps {
                script {
                    echo '========================================='
                    echo 'Stage 8: Building Docker Image'
                    echo '========================================='
                    echo 'Note: This stage requires Phase 5 setup'
                }
                
                // Build Docker image
                sh "docker build -t ${DOCKER_IMAGE} ."
                
                script {
                    echo "✓ Docker image built: ${DOCKER_IMAGE}"
                }
            }
        }
        
        stage('Push Docker Image') {
            when {
                branch 'main'
                expression { return fileExists('Dockerfile') }
            }
            steps {
                script {
                    echo '========================================='
                    echo 'Stage 9: Pushing Docker Image to Registry'
                    echo '========================================='
                }
                
                // Push to Docker registry
                withCredentials([usernamePassword(credentialsId: 'docker-registry-credentials', 
                                                  usernameVariable: 'DOCKER_USER', 
                                                  passwordVariable: 'DOCKER_PASS')]) {
                    sh '''
                        echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin ${DOCKER_REGISTRY}
                        docker tag ${DOCKER_IMAGE} ${DOCKER_REGISTRY}/${DOCKER_IMAGE}
                        docker push ${DOCKER_REGISTRY}/${DOCKER_IMAGE}
                    '''
                }
                
                script {
                    echo '✓ Docker image pushed to registry'
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
                    echo '✓ Application deployed to Kubernetes'
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
                echo '✓✓✓ Pipeline completed successfully! ✓✓✓'
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
                echo '⚠ Pipeline is unstable ⚠'
            }
        }
    }
}

