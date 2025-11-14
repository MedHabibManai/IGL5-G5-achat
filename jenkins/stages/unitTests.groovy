// BOM Fix
// jenkins/stages/unitTests.groovy
def call() {
    stage('Unit Tests') {
        echo '========================================='
        echo 'Stage 3: Running Unit Tests'
        echo '========================================='
        // Run tests
        sh 'mvn test'
        echo 'All unit tests passed'
    }
    post {
        always {
            // Publish JUnit test results
            junit '**/target/surefire-reports/*.xml'
            echo 'Test results published'
        }
    }
}
return this