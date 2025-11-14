// BOM Fix
// jenkins/stages/unitTests.groovy
def call() {
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
                echo 'All unit tests passed'
            }
        }
        
        post {
            always {
                // Publish JUnit test results
                junit '**/target/surefire-reports/*.xml'
                
                script {
                    echo 'Test results published'
                }
            }
        }
    }
}
return this
