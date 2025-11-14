// BOM Fix
// jenkins/stages/package.groovy
def call() {
    stage('Package') {
        echo '========================================='
        echo 'Stage 4: Packaging the application'
        echo '========================================='
        // Package the application
        sh 'mvn package -DskipTests'
        echo "Application packaged: ${ARTIFACT_NAME}"
        }
    post {
        success {
        // Archive the artifacts
        archiveArtifacts artifacts: '**/target/*.jar', fingerprint: true
        echo 'Artifacts archived successfully'
        }
    }
}
return this