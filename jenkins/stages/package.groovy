// BOM Fix
// jenkins/stages/package.groovy
def call() {
    stage('Package') {
        steps {
            script {
                echo '========================================='
                echo 'Stage 4: Packaging the application'
                echo '========================================='
            }
            
            // Package the application
            sh 'mvn package -DskipTests'
            
            script {
                echo "Application packaged: ${ARTIFACT_NAME}"
            }
        }
        
        post {
            success {
                // Archive the artifacts
                archiveArtifacts artifacts: '**/target/*.jar', fingerprint: true
                
                script {
                    echo 'Artifacts archived successfully'
                }
            }
        }
    }
}
return this
