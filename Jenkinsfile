// Main Jenkinsfile - Refactored to load stages from external files
// BOM fix
// Updated: Fixed deployment mode detection for webhook builds
// Retry build after TLS connection issue

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
    'jenkins/stages/finalSummary.groovy'
]

pipeline {
    agent any

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 2, unit: 'HOURS')
        // Disable SSL verification for Git to avoid TLS handshake errors
        skipDefaultCheckout()
    }

    // Webhook trigger: Build automatically on GitHub push
    // Note: Enable "GitHub hook trigger for GITScm polling" in Jenkins job configuration
    // The triggers block is not needed - webhook is configured via Jenkins UI checkbox

    // Parameters to control pipeline behavior
    parameters {
        choice(
            name: 'DEPLOYMENT_MODE',
            choices: ['REUSE_INFRASTRUCTURE', 'NORMAL', 'CLEANUP_AND_DEPLOY', 'EKS_ONLY'],
            description: '''Deployment mode:
            - REUSE_INFRASTRUCTURE: Keep VPC/RDS, only recreate EC2 instance (fastest for testing) [DEFAULT for webhooks]
            - NORMAL: Deploy fresh infrastructure (may fail if VPC limit reached)
            - CLEANUP_AND_DEPLOY: Destroy old resources first, then deploy new ones
            - EKS_ONLY: Skip all stages except EKS deployment (for testing EKS stage only)'''
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
        
        // Email notifications (configure in Jenkins System Configuration)
        // Set EMAIL_RECIPIENTS environment variable in Jenkins job configuration
        // Format: "email1@example.com,email2@example.com"
        EMAIL_RECIPIENTS = 'sinkingecstasies@gmail.com' // Will be set from Jenkins job configuration or global settings
    }
    
    stages {
        stage('Checkout with SSL workaround') {
            steps {
                script {
                    // Checkout with retry and SSL verification disabled
                    retry(3) {
                        checkout([
                            $class: 'GitSCM',
                            branches: [[name: '*/MohamedHabibManai-GL5-G5-Produit']],
                            extensions: [
                                [$class: 'CloneOption', timeout: 60, noTags: true, shallow: true, depth: 1],
                                [$class: 'CheckoutOption', timeout: 60]
                            ],
                            userRemoteConfigs: [[
                                url: 'https://github.com/MedHabibManai/IGL5-G5-achat.git'
                            ]]
                        ])
                    }
                    
                    // Configure git to skip SSL verification for this workspace
                    sh 'git config http.sslVerify false'
                    
                    // Check if build was triggered manually (by user) or by webhook
                    def buildCauses = currentBuild.getBuildCauses()
                    def isManualBuild = buildCauses.any { cause -> 
                        cause.getClass().getSimpleName() == 'UserIdCause' 
                    }
                    
                    // Detect deployment mode from commit message
                    // Supports: [NORMAL], [CLEANUP_AND_DEPLOY], [REUSE_INFRASTRUCTURE], [EKS_ONLY]
                    // Also handles: [reuse_infrastructure], [Reuse_Infrastructure], etc. (case-insensitive)
                    def commitMessage = sh(
                        script: 'git log -1 --pretty=%B',
                        returnStdout: true
                    ).trim()
                    
                    def detectedMode = null
                    def validModes = ['NORMAL', 'CLEANUP_AND_DEPLOY', 'REUSE_INFRASTRUCTURE', 'EKS_ONLY']
                    
                    // Check for mode in commit message (case-insensitive)
                    def commitMessageUpper = commitMessage.toUpperCase()
                    validModes.each { mode ->
                        // Check with brackets: [REUSE_INFRASTRUCTURE] or [reuse_infrastructure]
                        if (commitMessageUpper.contains("[${mode}]") || 
                            commitMessageUpper.contains("[${mode.replace('_', '-')}]")) {
                            detectedMode = mode
                            return
                        }
                    }
                    
                    // Priority logic:
                    // 1. Manual builds: Always use params.DEPLOYMENT_MODE (user selection)
                    // 2. Webhook builds: Use commit message if present, otherwise use default param
                    if (isManualBuild) {
                        echo "========================================="
                        echo "👤 Manual build detected - using selected parameter: ${params.DEPLOYMENT_MODE}"
                        echo "Commit message: ${commitMessage}"
                        if (detectedMode) {
                            echo "ℹ️  Note: Commit message contains [${detectedMode}], but manual selection takes priority"
                        }
                        echo "========================================="
                        env.DEPLOYMENT_MODE = params.DEPLOYMENT_MODE
                    } else if (detectedMode) {
                        echo "========================================="
                        echo "✅ Deployment mode detected from commit message: ${detectedMode}"
                        echo "Commit message: ${commitMessage}"
                        echo "========================================="
                        env.DEPLOYMENT_MODE = detectedMode
                    } else {
                        echo "========================================="
                        echo "⚠️  No deployment mode in commit message, using default: ${params.DEPLOYMENT_MODE}"
                        echo "Commit message: ${commitMessage}"
                        echo "To specify mode, use: [NORMAL], [REUSE_INFRASTRUCTURE], [CLEANUP_AND_DEPLOY], or [EKS_ONLY]"
                        echo "========================================="
                        env.DEPLOYMENT_MODE = params.DEPLOYMENT_MODE
                    }
                    
                    // Log the final deployment mode that will be used
                    echo "🔧 Final deployment mode: ${env.DEPLOYMENT_MODE}"
                }
            }
        }
        
        stage('Load Stages') {
            steps {
                script {
                    // Use detected mode from commit message, or fall back to parameter default
                    def deploymentMode = env.DEPLOYMENT_MODE ?: params.DEPLOYMENT_MODE
                    
                    // If EKS_ONLY mode, only run the EKS deployment stage
                    if (deploymentMode == 'EKS_ONLY') {
                        echo '========================================='
                        echo 'EKS_ONLY Mode: Running only EKS deployment stage'
                        echo '========================================='
                        def eksStageFile = 'jenkins/stages/deployToEKS.groovy'
                        if (fileExists(eksStageFile)) {
                            def stage = load eksStageFile
                            stage.call()
                        } else {
                            error "EKS stage file not found: ${eksStageFile}"
                        }
                    } else {
                        // Normal mode: Load and execute each stage file
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
    }
    
    post {
        always {
            script {
                echo ''
                echo '========================================='
                echo 'Pipeline Execution Summary'
                echo '========================================='
                echo "Build Number: ${BUILD_NUMBER}"
                echo "Build Status: ${currentBuild.currentResult}"
                echo "Duration: ${currentBuild.durationString}"
                def deploymentMode = env.DEPLOYMENT_MODE ?: params.DEPLOYMENT_MODE
                echo "Deployment Mode: ${deploymentMode}"
                echo ''
                echo 'For all deployment URLs, see the "Final Deployment Summary" stage above.'
                echo '========================================='
                
                // Send email notification for ALL build statuses (including aborted)
                // This ensures email is sent even if build is cancelled early
                def buildStatus = currentBuild.currentResult ?: 'UNKNOWN'
                echo "Current build status: ${buildStatus}"
                echo "EMAIL_RECIPIENTS: ${env.EMAIL_RECIPIENTS ?: 'NOT SET'}"
                
                try {
                    def emailStage = load 'jenkins/stages/sendEmailNotification.groovy'
                    def emailSubject = ""
                    def emailBody = ""
                    
                    switch(buildStatus) {
                        case 'SUCCESS':
                            emailSubject = "✅ SUCCESS: ${env.JOB_NAME} [${env.BUILD_NUMBER}]"
                            emailBody = "The build completed successfully! Your application has been deployed."
                            break
                        case 'FAILURE':
                            emailSubject = "❌ FAILURE: ${env.JOB_NAME} [${env.BUILD_NUMBER}]"
                            emailBody = "The build failed. Please check the console output for details."
                            break
                        case 'UNSTABLE':
                            emailSubject = "⚠️ UNSTABLE: ${env.JOB_NAME} [${env.BUILD_NUMBER}]"
                            emailBody = "The build is unstable. Please review the test results."
                            break
                        case 'ABORTED':
                            emailSubject = "⏹️ ABORTED: ${env.JOB_NAME} [${env.BUILD_NUMBER}]"
                            emailBody = "The build was cancelled or aborted before completion."
                            break
                        default:
                            emailSubject = "📧 Build ${buildStatus}: ${env.JOB_NAME} [${env.BUILD_NUMBER}]"
                            emailBody = "Build status: ${buildStatus}"
                    }
                    
                    emailStage.call([
                        subject: emailSubject,
                        body: emailBody,
                        to: env.EMAIL_RECIPIENTS ?: ''
                    ])
                } catch (Exception e) {
                    echo "Email notification failed: ${e.message}"
                    echo "Stack trace: ${e.getStackTrace().join('\n')}"
                }
            }
            
            // Clean workspace AFTER email is sent
            cleanWs()
        }
    }
}

