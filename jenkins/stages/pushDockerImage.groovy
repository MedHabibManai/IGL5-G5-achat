// BOM Fix
// jenkins/stages/pushDockerImage.groovy
def call() {
    stage('Push Docker Image') {
        echo '========================================='
        echo 'Stage 9: Pushing Docker Image to Registry'
        echo '========================================='
        // Push to Docker registry
        withCredentials([usernamePassword(credentialsId: "${DOCKER_CREDENTIAL_ID}",
        usernameVariable: 'DOCKER_USER',
        passwordVariable: 'DOCKER_PASS')]) {
        echo "Logging in to Docker Hub as ${DOCKER_USER}..."
        // Infinite retry for Docker login (handles TLS handshake timeouts)
        def loginSuccess = false
        while (!loginSuccess) {
        try {
        sh '''
        attempt=0
        while true; do
        attempt=$((attempt + 1))
        echo "Attempting Docker login (attempt $attempt)..."
        if echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin; then
        echo "V Docker login successful!"
        break
        else
        echo "? Login failed (likely network/TLS error), retrying in 15s..."
        sleep 15
        fi
        done
        '''
        loginSuccess = true
        echo "V Docker login completed!"
        } catch (Exception e) {
        echo "Unexpected error in login block: ${e.message}"
        echo "Retrying entire login sequence in 15s..."
        sleep(15)
        echo "Tagging images for Docker Hub..."
        // Tag with username prefix for Docker Hub
        sh """
        docker tag ${DOCKER_IMAGE} ${DOCKER_USER}/${DOCKER_IMAGE}
        docker tag ${DOCKER_IMAGE_NAME}:latest ${DOCKER_USER}/${DOCKER_IMAGE_NAME}:latest
        """
        echo "Pushing images to Docker Hub..."
        // Infinite retry for Docker push (handles TLS handshake timeouts)
        def pushSuccess = false
        while (!pushSuccess) {
        try {
        // Push with BUILD_NUMBER tag using infinite retry in shell
        sh """
        attempt=0
        while true; do
        attempt=\$((attempt + 1))
        echo "Attempting to push ${DOCKER_USER}/${DOCKER_IMAGE} (attempt \$attempt)..."
        if docker push ${DOCKER_USER}/${DOCKER_IMAGE}; then
        echo "Successfully pushed ${DOCKER_USER}/${DOCKER_IMAGE}"
        break
        else
        echo "Push failed (likely network/TLS error), retrying in 15s..."
        sleep 15
        fi
        done
        """
        // Push with latest tag using infinite retry in shell
        sh """
        attempt=0
        while true; do
        attempt=\$((attempt + 1))
        echo "Attempting to push ${DOCKER_USER}/${DOCKER_IMAGE_NAME}:latest (attempt \$attempt)..."
        if docker push ${DOCKER_USER}/${DOCKER_IMAGE_NAME}:latest; then
        echo "Successfully pushed ${DOCKER_USER}/${DOCKER_IMAGE_NAME}:latest"
        break
        else
        echo "Push failed (likely network/TLS error), retrying in 15s..."
        sleep 15
        fi
        done
        """
        pushSuccess = true
        echo "Docker push successful!"
        } catch (Exception e) {
        echo "Unexpected error in push block: ${e.message}"
        echo "Retrying entire push sequence in 15s..."
        sleep(15)
        echo "Docker images pushed successfully!"
        echo "  - ${DOCKER_USER}/${DOCKER_IMAGE}"
        echo "  - ${DOCKER_USER}/${DOCKER_IMAGE_NAME}:latest"
        echo ""
        echo "View on Docker Hub: https://hub.docker.com/r/${DOCKER_USER}/${DOCKER_IMAGE_NAME}"
        }
    }
}
return this