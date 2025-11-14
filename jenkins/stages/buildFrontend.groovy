// BOM Fix
// jenkins/stages/buildFrontend.groovy
def call() {
    stage('Build Frontend') {
        when {
        expression { return fileExists('frontend/package.json') }
        }
        echo '========================================='
        echo 'Stage 10: Building Frontend Application'
        echo '========================================='
        dir('frontend') {
        // Get backend URL from Terraform output
        def backendUrl = ''
        dir("../${TERRAFORM_DIR}") {
        backendUrl = sh(
        script: 'terraform output -raw application_url 2>/dev/null || echo ""',
        returnStdout: true
        ).trim()
        if (backendUrl) {
        echo "Backend URL: ${backendUrl}"
        // Create .env.production file with correct backend URL
        sh """
        echo "REACT_APP_API_URL=${backendUrl}" > .env.production
        cat .env.production
        """
        } else {
        echo "Warning: Could not get backend URL from Terraform"
        // Install dependencies and build
        sh '''
        npm install
        npm run build
        '''
        echo 'Frontend built successfully'
        }
    }
}
return this