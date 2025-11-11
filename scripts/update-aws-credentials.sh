#!/bin/bash

##############################################################################
# AWS Credentials Update Script for Jenkins Container
##############################################################################
# This script helps update AWS Academy Learner Lab credentials in Jenkins
# 
# Usage:
#   1. Copy credentials from AWS Academy (AWS Details → Show → AWS CLI)
#   2. Run: ./scripts/update-aws-credentials.sh
#   3. Paste the credentials when prompted
#
# Or provide credentials as arguments:
#   ./scripts/update-aws-credentials.sh ACCESS_KEY SECRET_KEY SESSION_TOKEN
##############################################################################

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Container name
JENKINS_CONTAINER="jenkins-cicd"

echo ""
echo -e "${CYAN}════════════════════════════════════════════════════════${NC}"
echo -e "${CYAN}  AWS Credentials Update for Jenkins${NC}"
echo -e "${CYAN}════════════════════════════════════════════════════════${NC}"
echo ""

# Check if Jenkins container is running
if ! docker ps --format '{{.Names}}' | grep -q "^${JENKINS_CONTAINER}$"; then
    echo -e "${RED}✗ Jenkins container '${JENKINS_CONTAINER}' is not running!${NC}"
    echo ""
    echo "Start Jenkins with:"
    echo "  docker-compose up -d"
    echo ""
    exit 1
fi

echo -e "${GREEN}✓ Jenkins container is running${NC}"
echo ""

# Function to update credentials
update_credentials() {
    local access_key="$1"
    local secret_key="$2"
    local session_token="$3"
    
    echo -e "${YELLOW}Updating AWS credentials in Jenkins container...${NC}"
    echo ""
    
    # Create credentials file content
    cat > /tmp/aws_credentials_temp << EOF
[default]
aws_access_key_id=${access_key}
aws_secret_access_key=${secret_key}
aws_session_token=${session_token}
EOF
    
    # Copy credentials to Jenkins container
    docker cp /tmp/aws_credentials_temp ${JENKINS_CONTAINER}:/var/jenkins_home/.aws/credentials
    
    # Set proper permissions
    docker exec ${JENKINS_CONTAINER} chmod 600 /var/jenkins_home/.aws/credentials
    
    # Clean up temp file
    rm /tmp/aws_credentials_temp
    
    echo -e "${GREEN}✓ Credentials updated successfully!${NC}"
    echo ""
    
    # Verify credentials
    echo -e "${YELLOW}Verifying credentials...${NC}"
    echo ""
    
    if docker exec ${JENKINS_CONTAINER} bash -c 'export AWS_SHARED_CREDENTIALS_FILE=/var/jenkins_home/.aws/credentials && export AWS_CONFIG_FILE=/var/jenkins_home/.aws/config && aws sts get-caller-identity' > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Credentials are valid!${NC}"
        echo ""
        
        echo -e "${CYAN}Current AWS Identity:${NC}"
        docker exec ${JENKINS_CONTAINER} bash -c 'export AWS_SHARED_CREDENTIALS_FILE=/var/jenkins_home/.aws/credentials && export AWS_CONFIG_FILE=/var/jenkins_home/.aws/config && aws sts get-caller-identity'
        echo ""
    else
        echo -e "${RED}✗ Credentials verification failed!${NC}"
        echo "Please check the credentials and try again."
        echo ""
        exit 1
    fi
}

# Check if credentials provided as arguments
if [ $# -eq 3 ]; then
    echo -e "${BLUE}Using credentials from command line arguments${NC}"
    echo ""
    update_credentials "$1" "$2" "$3"
else
    echo -e "${YELLOW}Please provide your AWS Academy credentials:${NC}"
    echo ""
    echo "Go to AWS Academy Learner Lab:"
    echo "  1. Click 'AWS Details'"
    echo "  2. Click 'Show' under 'AWS CLI'"
    echo "  3. Copy the credentials"
    echo ""
    echo -e "${CYAN}────────────────────────────────────────────────────────${NC}"
    echo ""
    
    read -p "AWS Access Key ID: " access_key
    read -p "AWS Secret Access Key: " secret_key
    read -p "AWS Session Token: " session_token
    
    echo ""
    
    if [ -z "$access_key" ] || [ -z "$secret_key" ] || [ -z "$session_token" ]; then
        echo -e "${RED}✗ All three credentials are required!${NC}"
        echo ""
        exit 1
    fi
    
    update_credentials "$access_key" "$secret_key" "$session_token"
fi

echo -e "${CYAN}════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "${GREEN}✅ AWS credentials updated successfully!${NC}"
echo ""
echo "You can now run your Jenkins pipeline."
echo "The credentials will be verified in Stage 10."
echo ""
echo -e "${YELLOW}⚠ Remember:${NC}"
echo "  • AWS Academy credentials expire after a few hours"
echo "  • Update credentials when you start a new lab session"
echo "  • Run 'terraform destroy' before session expires"
echo ""

