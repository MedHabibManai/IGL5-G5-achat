// jenkins/stages/deployToNexus.groovy
def call() {
    stage('Deploy to Nexus') {
        steps {
            script {
                echo '========================================='
                echo 'Stage 7: Deploying artifacts to Nexus'
                echo '========================================='
            }

            // Create Maven settings.xml with Nexus credentials
            withCredentials([usernamePassword(credentialsId: "${NEXUS_CREDENTIAL_ID}",
                                              usernameVariable: 'NEXUS_USER',
                                              passwordVariable: 'NEXUS_PASS')]) {
                sh '''

                    # Create settings.xml with Nexus credentials
                    cat > settings.xml << EOF
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
  <servers>
    <server>
      <id>nexus-releases</id>
      <username>${NEXUS_USER}</username>
      <password>${NEXUS_PASS}</password>
    </server>
    <server>
      <id>nexus-snapshots</id>
      <username>${NEXUS_USER}</username>
      <password>${NEXUS_PASS}</password>
    </server>
  </servers>
</settings>
EOF

                    # Deploy to Nexus
                    mvn deploy -DskipTests -s settings.xml
                '''
            }

            script {
                echo 'Artifacts deployed to Nexus successfully'
                echo "View artifacts at: http://localhost:8081/#browse/browse:maven-releases"
            }
        }
    }
}
return this
