# ============================================================================
# DevOps CI/CD Pipeline - Automated Setup Script
# ============================================================================
# This script sets up the complete CI/CD pipeline on your local machine
# ============================================================================

param(
    [switch]$SkipAWS = $false
)

$ErrorActionPreference = "Continue"

# Colors
function Write-Header {
    param([string]$Message)
    Write-Host ""
    Write-Host "╔════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
    Write-Host "║  $Message" -ForegroundColor Cyan
    Write-Host "╚════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
    Write-Host ""
}

function Write-Step {
    param([string]$Message)
    Write-Host "➤ $Message" -ForegroundColor Yellow
}

function Write-Success {
    param([string]$Message)
    Write-Host "✓ $Message" -ForegroundColor Green
}

function Write-Info {
    param([string]$Message)
    Write-Host "  $Message" -ForegroundColor Gray
}

function Write-Warning {
    param([string]$Message)
    Write-Host "⚠ $Message" -ForegroundColor Yellow
}

function Write-Error-Custom {
    param([string]$Message)
    Write-Host "✗ $Message" -ForegroundColor Red
}

# ============================================================================
# Pre-flight Checks
# ============================================================================

Write-Header "DevOps CI/CD Pipeline Setup"

Write-Step "Running pre-flight checks..."

# Check Docker
try {
    $dockerVersion = docker --version
    Write-Success "Docker is installed: $dockerVersion"
} catch {
    Write-Error-Custom "Docker is not installed or not running!"
    Write-Info "Please install Docker Desktop and make sure it's running."
    exit 1
}

# Check Docker Compose
try {
    $composeVersion = docker-compose --version
    Write-Success "Docker Compose is installed: $composeVersion"
} catch {
    Write-Error-Custom "Docker Compose is not installed!"
    exit 1
}

# Check if Docker is running
try {
    docker ps | Out-Null
    Write-Success "Docker daemon is running"
} catch {
    Write-Error-Custom "Docker daemon is not running!"
    Write-Info "Please start Docker Desktop and try again."
    exit 1
}

Write-Success "All pre-flight checks passed!"

# ============================================================================
# Step 1: Stop and Clean Existing Containers
# ============================================================================

Write-Header "Step 1: Cleaning Up Existing Containers"

Write-Step "Stopping existing containers..."
docker-compose down 2>$null

Write-Success "Cleanup complete!"

# ============================================================================
# Step 2: Start All Services
# ============================================================================

Write-Header "Step 2: Starting All Services"

Write-Step "Starting Docker containers..."
Write-Info "This may take 3-5 minutes on first run (downloading images)..."

docker-compose up -d

if ($LASTEXITCODE -ne 0) {
    Write-Error-Custom "Failed to start containers!"
    exit 1
}

Write-Success "All containers started!"

# ============================================================================
# Step 3: Wait for Services to be Ready
# ============================================================================

Write-Header "Step 3: Waiting for Services to Initialize"

Write-Step "Waiting for Jenkins to start (this may take 2-3 minutes)..."
$maxAttempts = 60
$attempt = 0

while ($attempt -lt $maxAttempts) {
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8080" -TimeoutSec 2 -UseBasicParsing -ErrorAction SilentlyContinue
        if ($response.StatusCode -eq 200 -or $response.StatusCode -eq 403) {
            Write-Success "Jenkins is ready!"
            break
        }
    } catch {
        # Jenkins not ready yet
    }
    
    $attempt++
    Write-Host "." -NoNewline -ForegroundColor Gray
    Start-Sleep -Seconds 3
}

if ($attempt -ge $maxAttempts) {
    Write-Warning "Jenkins is taking longer than expected to start."
    Write-Info "You can check the logs with: docker logs jenkins-cicd"
}

Write-Host ""

Write-Step "Waiting for SonarQube to start..."
Start-Sleep -Seconds 10
Write-Success "SonarQube should be ready soon!"

Write-Step "Waiting for Nexus to start..."
Start-Sleep -Seconds 5
Write-Success "Nexus should be ready soon!"

# ============================================================================
# Step 4: Get Jenkins Initial Password
# ============================================================================

Write-Header "Step 4: Jenkins Configuration"

Write-Step "Retrieving Jenkins initial admin password..."

try {
    $jenkinsPassword = docker exec jenkins-cicd cat /var/jenkins_home/secrets/initialAdminPassword 2>$null
    
    if ($jenkinsPassword) {
        Write-Success "Jenkins initial admin password retrieved!"
        Write-Host ""
        Write-Host "════════════════════════════════════════════════════════" -ForegroundColor Yellow
        Write-Host "  JENKINS ADMIN PASSWORD: $jenkinsPassword" -ForegroundColor Green
        Write-Host "════════════════════════════════════════════════════════" -ForegroundColor Yellow
        Write-Host ""
        Write-Info "Save this password! You'll need it to unlock Jenkins."
    } else {
        Write-Warning "Could not retrieve Jenkins password. It may already be configured."
    }
} catch {
    Write-Warning "Jenkins may already be configured."
}

# ============================================================================
# Step 5: Install AWS CLI and Terraform in Jenkins (Optional)
# ============================================================================

if (-not $SkipAWS) {
    Write-Header "Step 5: Installing AWS Tools in Jenkins"

    Write-Step "Installing AWS CLI v2..."
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
    " 2>$null

    Write-Success "AWS CLI installed!"

    Write-Step "Installing Terraform..."
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
    " 2>$null

    Write-Success "Terraform installed!"
} else {
    Write-Info "Skipping AWS tools installation (use -SkipAWS:$false to install)"
}

# ============================================================================
# Step 6: Display Service URLs
# ============================================================================

Write-Header "Setup Complete!"

Write-Host ""
Write-Host "🎉 All services are running!" -ForegroundColor Green
Write-Host ""

Write-Host "════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "  SERVICE URLS" -ForegroundColor Yellow
Write-Host "════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""
Write-Host "  Jenkins:    http://localhost:8080" -ForegroundColor White
Write-Host "              Username: admin" -ForegroundColor Gray
if ($jenkinsPassword) {
    Write-Host "              Password: $jenkinsPassword" -ForegroundColor Gray
}
Write-Host ""
Write-Host "  SonarQube:  http://localhost:9000" -ForegroundColor White
Write-Host "              Username: admin" -ForegroundColor Gray
Write-Host "              Password: ##Azeraoi123" -ForegroundColor Gray
Write-Host ""
Write-Host "  Nexus:      http://localhost:8081" -ForegroundColor White
Write-Host "              Username: admin" -ForegroundColor Gray
Write-Host "              Password: ##Azeraoi123" -ForegroundColor Gray
Write-Host ""
Write-Host "  Grafana:    http://localhost:3000" -ForegroundColor White
Write-Host "              Username: admin" -ForegroundColor Gray
Write-Host "              Password: admin" -ForegroundColor Gray
Write-Host ""
Write-Host "  Prometheus: http://localhost:9090" -ForegroundColor White
Write-Host ""
Write-Host "════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

# ============================================================================
# Step 7: Next Steps
# ============================================================================

Write-Host "NEXT STEPS:" -ForegroundColor Yellow
Write-Host ""
Write-Host "1. Open Jenkins: http://localhost:8080" -ForegroundColor White
Write-Host "2. Unlock Jenkins with the password above" -ForegroundColor White
Write-Host "3. Install suggested plugins" -ForegroundColor White
Write-Host "4. Create your first admin user" -ForegroundColor White
Write-Host "5. Create a pipeline job pointing to your Jenkinsfile" -ForegroundColor White
Write-Host ""

if (-not $SkipAWS) {
    Write-Host "TO DEPLOY TO AWS:" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "1. Copy .env.aws.example to .env.aws" -ForegroundColor White
    Write-Host "2. Edit .env.aws with your AWS credentials" -ForegroundColor White
    Write-Host "3. Run: .\scripts\update-jenkins-aws-credentials.ps1" -ForegroundColor White
    Write-Host ""
}

Write-Host "════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""
Write-Host "✨ Setup complete! Happy DevOps-ing! ✨" -ForegroundColor Green
Write-Host ""

