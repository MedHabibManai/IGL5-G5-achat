// BOM Fix
// jenkins/stages/checkout.groovy
def call() {
    stage('Checkout') {
        echo '========================================='
        echo 'Stage 1: Checking out code from GitHub'
        echo '========================================='

        // Configure Git SSL settings BEFORE attempting checkout
        sh '''
            git config --global http.sslVerify false
            git config --global http.postBuffer 524288000
            git config --global http.lowSpeedLimit 0
            git config --global http.lowSpeedTime 0
            git config --global http.version HTTP/1.1
        '''
        
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
                            [$class: 'CloneOption', timeout: 120, noTags: true, shallow: true, depth: 1],
                            [$class: 'CheckoutOption', timeout: 120]
                        ],
                        userRemoteConfigs: [[url: 'https://github.com/MedHabibManai/IGL5-G5-achat.git']]
                    ])
                    
                    // Verify checkout succeeded
                    sh 'git config http.sslVerify false'
                    success = true
                } catch (Exception e) {
                    if (retryCount >= maxRetries) {
                        echo "Checkout failed after ${maxRetries} attempts. Last error: ${e.message}"
                        throw e
                    }
                    echo "Checkout failed: ${e.message}. Retrying..."
                }
            }
        }
    
        echo "âœ“ Successfully checked out branch: ${env.GIT_BRANCH}"
        echo "âœ“ Commit: ${env.GIT_COMMIT}"
    }
}
return this