// jenkins/stages/checkout.groovy
def call() {
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
}
return this
