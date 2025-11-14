// BOM Fix
// jenkins/stages/package.groovy
def call() {
    stage('Package') {
        echo '========================================='
        echo 'Stage 4: Packaging the application'
        echo '========================================='

        def packageSucceeded = false
        try {
            sh 'mvn package -DskipTests'
            packageSucceeded = true
            echo "Application packaged: ${ARTIFACT_NAME}"
        } finally {
            if (packageSucceeded) {
                archiveArtifacts artifacts: '**/target/*.jar', fingerprint: true
                echo 'Artifacts archived successfully'
            }
        }
    }
}
return this
