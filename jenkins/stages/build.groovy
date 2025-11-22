// BOM Fix
// jenkins/stages/build.groovy
//testing
def call() {
    stage('Build') {
                echo '========================================='
                echo 'Stage 2: Building the application'
                echo '========================================='
            
            // Use system Maven (wrapper files missing)
            sh 'mvn clean compile'
            
                echo 'Build completed successfully'
    }
}
return this
