// BOM Fix
// jenkins/stages/qualityGate.groovy
def call() {
    stage('Quality Gate') {
        echo '========================================='
        echo 'Stage 6: Waiting for Quality Gate'
        echo '========================================='
        // Wait for SonarQube Quality Gate result
        timeout(time: 5, unit: 'MINUTES') {
        def qg = waitForQualityGate()
        if (qg.status != 'OK') {
        echo "Quality Gate status: ${qg.status}"
        echo "Pipeline will continue but code quality needs attention"
        } else {
        echo 'Quality Gate passed!'
        }
    }
}
return this