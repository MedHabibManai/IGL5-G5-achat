# How to Test Only the EKS Deployment Stage (Stage 13)

## Quick Method: Use Jenkins Replay

### Step-by-Step Instructions:

1. **Open Jenkins**
   - Go to: http://localhost:8080
   - Navigate to: **IGL5-G5-achat** job

2. **Find the Last Build**
   - Click on the latest build number (e.g., #267)
   - Look for the **"Replay"** button in the left sidebar

3. **Click "Replay"**
   - This opens an editor with the current Jenkinsfile

4. **Replace the Entire Content**
   - **DELETE** all the content in the editor
   - **COPY** the entire content from: `Jenkinsfile.test-eks-only`
   - **PASTE** it into the Replay editor

5. **Click "Run"**
   - This will execute ONLY the EKS deployment stage
   - All other stages will be skipped

## What You'll See:

The test pipeline will:
- ✅ Checkout your code
- ✅ Run ONLY Stage 13: Deploy to EKS
- ⏭️ Skip all other stages (build, test, docker, etc.)
- ✅ Show results in ~5-10 minutes instead of 20+ minutes

## Alternative: Create a New Test Job

If you prefer a separate job:

1. Jenkins → **New Item** → **Pipeline**
2. Name: `test-eks-deployment`
3. **Pipeline** section:
   - Definition: **Pipeline script from SCM**
   - SCM: **Git**
   - Repository URL: Your repo URL
   - **Script Path**: `Jenkinsfile.test-eks-only`
4. **Save** and **Build Now**

## Files Reference:

- **Jenkinsfile** - Main pipeline (runs all stages) - DON'T use for testing
- **Jenkinsfile.test-eks-only** - Test pipeline (only EKS stage) - USE THIS for testing
- **Jenkinsfile.backup** - Backup file - DON'T use
- **Jenkinsfile.corrupted** - Old corrupted file - DON'T use
- **Jenkinsfile.diff** - Diff file - DON'T use

## Quick Copy-Paste Content:

If you need the content quickly, here's what to paste in Replay:

```groovy
// Test Jenkinsfile - Only runs deployToEKS stage
pipeline {
    agent any
    options {
        timeout(time: 30, unit: 'MINUTES')
        skipDefaultCheckout()
    }
    environment {
        GIT_SSL_NO_VERIFY = 'true'
        AWS_REGION = 'us-east-1'
        AWS_CREDENTIAL_ID = 'aws-sandbox-credentials'
        TERRAFORM_DIR = 'terraform'
        DOCKER_REGISTRY = 'docker.io'
        DOCKER_IMAGE_NAME = 'achat-app'
        BUILD_NUMBER = "${env.BUILD_NUMBER ?: '999'}"
    }
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        stage('Deploy to EKS (Test Only)') {
            steps {
                script {
                    def deployToEKS = load 'jenkins/stages/deployToEKS.groovy'
                    deployToEKS.call()
                }
            }
        }
    }
    post {
        always {
            script {
                echo '========================================='
                echo 'EKS Deployment Test Summary'
                echo '========================================='
                echo "Build Number: ${BUILD_NUMBER}"
                echo "Build Status: ${currentBuild.currentResult}"
            }
        }
    }
}
```

