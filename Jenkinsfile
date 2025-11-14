// Main Jenkinsfile - Refactored to load stages from external files
// BOM fix

// Define stage files in order
def stageFiles = [
    'jenkins/stages/checkout.groovy',
    'jenkins/stages/build.groovy',
    'jenkins/stages/unitTests.groovy',
    'jenkins/stages/package.groovy',
    'jenkins/stages/sonar.groovy',
    'jenkins/stages/qualityGate.groovy',
    'jenkins/stages/deployToNexus.groovy',
    'jenkins/stages/buildDockerImage.groovy',
    'jenkins/stages/pushDockerImage.groovy',
    'jenkins/stages/cleanupAWS.groovy',
    'jenkins/stages/refreshEC2.groovy',
    'jenkins/stages/terraformInit.groovy',
    'jenkins/stages/preTerraformValidation.groovy',
    'jenkins/stages/terraformPlan.groovy',
    'jenkins/stages/terraformApply.groovy',
    'jenkins/stages/getAWSInfo.groovy',
    'jenkins/stages/debugEC2.groovy',
    'jenkins/stages/healthCheck.groovy',
    'jenkins/stages/buildFrontend.groovy',
    'jenkins/stages/buildFrontendDocker.groovy',
    'jenkins/stages/pushFrontendDocker.groovy',
    'jenkins/stages/deployToEKS.groovy',
    'jenkins/stages/deployToLocalK8s.groovy'
]

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
            - NORMAL: Deploy fresh infrastructure (may fail if VPC limit reached)
            - CLEANUP_AND_DEPLOY: Destroy old resources first, then deploy new ones
            - REUSE_INFRASTRUCTURE: Keep VPC/RDS, only recreate EC2 instance (fastest for testing)'''
        )
    }
    
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
        stage('Load Stages') {
            steps {
                script {
                    // Load and execute each stage file
                    stageFiles.each { file ->
                        try {
                            if (fileExists(file)) {
                                def stage = load file
                                stage.call()
                            } else {
                                echo "Skipping stage: ${file} not found."
                            }
                        } catch (Exception e) {
                            echo "Error loading stage from ${file}: ${e.message}"
                            throw e
                        }
                    }
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
                echo 'Pipeline completed successfully! '
            }
        }
        
        failure {
            script {
                echo 'Pipeline failed!'
            }
        }
        
        unstable {
            script {
                echo 'unstable'
            }
        }
    }
}

