// BOM Fix
// jenkins/stages/unitTests.groovy
def call() {
    stage('Unit Tests') {
        echo '========================================='
        echo 'Stage 3: Running Unit Tests'
        echo '========================================='

        try {
            sh 'mvn test'
            echo 'All unit tests passed'
        } finally {
            junit '**/target/surefire-reports/*.xml'
            echo 'Test results published'
        }
    }
}
return this
