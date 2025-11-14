// Test script to run only the deployToEKS stage
// Usage: Run this as a Jenkins Pipeline script or via Jenkins Script Console

// Set required environment variables (adjust as needed)
env.BUILD_NUMBER = env.BUILD_NUMBER ?: "999"
env.AWS_REGION = env.AWS_REGION ?: "us-east-1"
env.AWS_CREDENTIAL_ID = env.AWS_CREDENTIAL_ID ?: "aws-sandbox-credentials"
env.TERRAFORM_DIR = env.TERRAFORM_DIR ?: "terraform"
env.DOCKER_REGISTRY = env.DOCKER_REGISTRY ?: "docker.io"
env.DOCKER_IMAGE_NAME = env.DOCKER_IMAGE_NAME ?: "achat-app"

// Load and execute the deployToEKS stage
def deployToEKS = load 'jenkins/stages/deployToEKS.groovy'
deployToEKS.call()

