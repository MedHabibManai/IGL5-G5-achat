#!/bin/bash

# ============================================================================
# DevOps CI/CD Pipeline - Automated Setup Script (Linux/Mac)
# ============================================================================
# This script sets up the complete CI/CD pipeline on your local machine
# ============================================================================

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
GRAY='\033[0;37m'
NC='\033[0m' # No Color

# Helper functions
print_header() {
    echo ""
    echo -e "${CYAN}╔════════════════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║  $1${NC}"
    echo -e "${CYAN}╚════════════════════════════════════════════════════════╝${NC}"
    echo ""
}

print_step() {
    echo -e "${YELLOW}➤ $1${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_info() {
    echo -e "${GRAY}  $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

# Parse arguments
SKIP_AWS=false
while [[ $# -gt 0 ]]; do
    case $1 in
        --skip-aws)
            SKIP_AWS=true
            shift
            ;;
        *)
            shift
            ;;
    esac
done

# ============================================================================
# Pre-flight Checks
# ============================================================================

print_header "DevOps CI/CD Pipeline Setup"

print_step "Running pre-flight checks..."

# Check Docker
if command -v docker &> /dev/null; then
    DOCKER_VERSION=$(docker --version)
    print_success "Docker is installed: $DOCKER_VERSION"
else
    print_error "Docker is not installed or not running!"
    print_info "Please install Docker Desktop and make sure it's running."
    exit 1
fi

# Check Docker Compose
if command -v docker-compose &> /dev/null; then
    COMPOSE_VERSION=$(docker-compose --version)
    print_success "Docker Compose is installed: $COMPOSE_VERSION"
else
    print_error "Docker Compose is not installed!"
    exit 1
fi

# Check if Docker is running
if docker ps &> /dev/null; then
    print_success "Docker daemon is running"
else
    print_error "Docker daemon is not running!"
    print_info "Please start Docker Desktop and try again."
    exit 1
fi

print_success "All pre-flight checks passed!"

# ============================================================================
# Step 1: Stop and Clean Existing Containers
# ============================================================================

print_header "Step 1: Cleaning Up Existing Containers"

print_step "Stopping existing containers..."
docker-compose down 2>/dev/null || true

print_success "Cleanup complete!"

# ============================================================================
# Step 2: Start All Services
# ============================================================================

print_header "Step 2: Starting All Services"

print_step "Starting Docker containers..."
print_info "This may take 3-5 minutes on first run (downloading images)..."

docker-compose up -d

print_success "All containers started!"

# ============================================================================
# Step 3: Wait for Services to be Ready
# ============================================================================

print_header "Step 3: Waiting for Services to Initialize"

print_step "Waiting for Jenkins to start (this may take 2-3 minutes)..."
MAX_ATTEMPTS=60
ATTEMPT=0

while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
    if curl -s -o /dev/null -w "%{http_code}" http://localhost:8080 | grep -q "200\|403"; then
        print_success "Jenkins is ready!"
        break
    fi
    
    ATTEMPT=$((ATTEMPT + 1))
    echo -n "."
    sleep 3
done

if [ $ATTEMPT -ge $MAX_ATTEMPTS ]; then
    print_warning "Jenkins is taking longer than expected to start."
    print_info "You can check the logs with: docker logs jenkins-cicd"
fi

echo ""

print_step "Waiting for SonarQube to start..."
sleep 10
print_success "SonarQube should be ready soon!"

print_step "Waiting for Nexus to start..."
sleep 5
print_success "Nexus should be ready soon!"

# ============================================================================
# Step 4: Get Jenkins Initial Password
# ============================================================================

print_header "Step 4: Jenkins Configuration"

print_step "Retrieving Jenkins initial admin password..."

JENKINS_PASSWORD=$(docker exec jenkins-cicd cat /var/jenkins_home/secrets/initialAdminPassword 2>/dev/null || echo "")

if [ -n "$JENKINS_PASSWORD" ]; then
    print_success "Jenkins initial admin password retrieved!"
    echo ""
    echo -e "${YELLOW}════════════════════════════════════════════════════════${NC}"
    echo -e "${GREEN}  JENKINS ADMIN PASSWORD: $JENKINS_PASSWORD${NC}"
    echo -e "${YELLOW}════════════════════════════════════════════════════════${NC}"
    echo ""
    print_info "Save this password! You'll need it to unlock Jenkins."
else
    print_warning "Could not retrieve Jenkins password. It may already be configured."
fi

# ============================================================================
# Step 5: Install AWS CLI and Terraform in Jenkins (Optional)
# ============================================================================

if [ "$SKIP_AWS" = false ]; then
    print_header "Step 5: Installing AWS Tools in Jenkins"

    print_step "Installing AWS CLI v2..."
    docker exec -u root jenkins-cicd bash -c "
        if ! command -v aws &> /dev/null; then
            cd /tmp
            curl -s 'https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip' -o 'awscliv2.zip'
            unzip -q awscliv2.zip
            ./aws/install
            rm -rf aws awscliv2.zip
            echo 'AWS CLI installed'
        else
            echo 'AWS CLI already installed'
        fi
    " 2>/dev/null

    print_success "AWS CLI installed!"

    print_step "Installing Terraform..."
    docker exec -u root jenkins-cicd bash -c "
        if ! command -v terraform &> /dev/null; then
            cd /tmp
            wget -q https://releases.hashicorp.com/terraform/1.6.6/terraform_1.6.6_linux_amd64.zip
            unzip -q terraform_1.6.6_linux_amd64.zip
            mv terraform /usr/local/bin/
            chmod +x /usr/local/bin/terraform
            rm terraform_1.6.6_linux_amd64.zip
            echo 'Terraform installed'
        else
            echo 'Terraform already installed'
        fi
    " 2>/dev/null

    print_success "Terraform installed!"
else
    print_info "Skipping AWS tools installation (remove --skip-aws to install)"
fi

# ============================================================================
# Step 6: Display Service URLs
# ============================================================================

print_header "Setup Complete!"

echo ""
echo -e "${GREEN}🎉 All services are running!${NC}"
echo ""

echo -e "${CYAN}════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}  SERVICE URLS${NC}"
echo -e "${CYAN}════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "  Jenkins:    http://localhost:8080"
echo -e "${GRAY}              Username: admin${NC}"
if [ -n "$JENKINS_PASSWORD" ]; then
    echo -e "${GRAY}              Password: $JENKINS_PASSWORD${NC}"
fi
echo ""
echo -e "  SonarQube:  http://localhost:9000"
echo -e "${GRAY}              Username: admin${NC}"
echo -e "${GRAY}              Password: ##Azeraoi123${NC}"
echo ""
echo -e "  Nexus:      http://localhost:8081"
echo -e "${GRAY}              Username: admin${NC}"
echo -e "${GRAY}              Password: ##Azeraoi123${NC}"
echo ""
echo -e "  Grafana:    http://localhost:3000"
echo -e "${GRAY}              Username: admin${NC}"
echo -e "${GRAY}              Password: admin${NC}"
echo ""
echo -e "  Prometheus: http://localhost:9090"
echo ""
echo -e "${CYAN}════════════════════════════════════════════════════════${NC}"
echo ""

# ============================================================================
# Step 7: Next Steps
# ============================================================================

echo -e "${YELLOW}NEXT STEPS:${NC}"
echo ""
echo "1. Open Jenkins: http://localhost:8080"
echo "2. Unlock Jenkins with the password above"
echo "3. Install suggested plugins"
echo "4. Create your first admin user"
echo "5. Create a pipeline job pointing to your Jenkinsfile"
echo ""

if [ "$SKIP_AWS" = false ]; then
    echo -e "${YELLOW}TO DEPLOY TO AWS:${NC}"
    echo ""
    echo "1. Copy .env.aws.example to .env.aws"
    echo "2. Edit .env.aws with your AWS credentials"
    echo "3. Run: ./scripts/update-jenkins-aws-credentials.sh"
    echo ""
fi

echo -e "${CYAN}════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "${GREEN}✨ Setup complete! Happy DevOps-ing! ✨${NC}"
echo ""

