// BOM Fix
// jenkins/stages/sonar.groovy
def call() {
    stage('Code Quality Analysis - SonarQube') {
        echo '========================================='
        echo 'Stage 5: SonarQube Code Quality Analysis'
        echo '========================================='
        // Run SonarQube analysis
        withSonarQubeEnv('SonarQube') {
        sh '''
        export MAVEN_OPTS="--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.io=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED"
        mvn sonar:sonar \
        -Dsonar.projectKey=${SONAR_PROJECT_KEY} \
        -Dsonar.projectName="${SONAR_PROJECT_NAME}" \
        -Dsonar.host.url=${SONAR_HOST_URL} \
        -Dsonar.java.binaries=target/classes
        '''
        echo 'SonarQube analysis completed'
        echo "View results at: ${SONAR_HOST_URL}/dashboard?id=${SONAR_PROJECT_KEY}"
        }
    }
}
return this