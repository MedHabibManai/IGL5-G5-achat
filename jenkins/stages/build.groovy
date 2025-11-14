// BOM Fix
// jenkins/stages/build.groovy
def call() {
    stage('Build') {
        steps {
            script {
                echo '========================================='
                echo 'Stage 2: Building the application'
                echo '========================================='
            }
            
            // Use system Maven (wrapper files missing)
            sh 'mvn clean compile'
            
            script {
                echo 'Build completed successfully'
            }
        }
    }
}
return this
