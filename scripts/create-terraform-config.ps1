# Create Terraform Configuration
# Interactive script to set up Terraform for AWS deployment

function Show-Header {
    param([string]$Title)
    cls
    Write-Host ""
    Write-Host "╔════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
    Write-Host "║  TERRAFORM CONFIGURATION SETUP                         ║" -ForegroundColor Cyan
    Write-Host "╚════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "  $Title" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Gray
    Write-Host ""
}

Show-Header "Terraform Setup for AWS Deployment"

# ============================================================================
# STEP 1: Check Terraform Installation
# ============================================================================

Write-Host "STEP 1: Checking Terraform installation..." -ForegroundColor Cyan
Write-Host ""

try {
    $tfVersion = terraform version 2>&1
    Write-Host "✅ Terraform is installed:" -ForegroundColor Green
    Write-Host "   $($tfVersion | Select-Object -First 1)" -ForegroundColor Gray
    Write-Host ""
} catch {
    Write-Host "❌ Terraform is not installed" -ForegroundColor Red
    Write-Host ""
    Write-Host "Installing Terraform via Chocolatey..." -ForegroundColor Yellow
    
    try {
        choco install terraform -y
        Write-Host "✅ Terraform installed successfully!" -ForegroundColor Green
        Write-Host ""
        Write-Host "⚠️  Please close and reopen PowerShell" -ForegroundColor Yellow
        exit 0
    } catch {
        Write-Host "❌ Failed to install Terraform" -ForegroundColor Red
        Write-Host ""
        Write-Host "Please install manually:" -ForegroundColor Yellow
        Write-Host "  1. Download from: https://www.terraform.io/downloads" -ForegroundColor White
        Write-Host "  2. Or use Chocolatey: choco install terraform" -ForegroundColor White
        exit 1
    }
}

# ============================================================================
# STEP 2: Verify Terraform Files
# ============================================================================

Write-Host "STEP 2: Verifying Terraform configuration files..." -ForegroundColor Cyan
Write-Host ""

$terraformDir = "terraform"
$requiredFiles = @("provider.tf", "variables.tf", "main.tf", "outputs.tf")

if (-not (Test-Path $terraformDir)) {
    Write-Host "❌ Terraform directory not found!" -ForegroundColor Red
    exit 1
}

foreach ($file in $requiredFiles) {
    $filePath = Join-Path $terraformDir $file
    if (Test-Path $filePath) {
        Write-Host "  ✓ $file" -ForegroundColor Green
    } else {
        Write-Host "  ✗ $file (missing)" -ForegroundColor Red
    }
}

Write-Host ""

# ============================================================================
# STEP 3: Create terraform.tfvars
# ============================================================================

Write-Host "STEP 3: Creating terraform.tfvars..." -ForegroundColor Cyan
Write-Host ""

$tfvarsPath = Join-Path $terraformDir "terraform.tfvars"
$tfvarsExamplePath = Join-Path $terraformDir "terraform.tfvars.example"

if (Test-Path $tfvarsPath) {
    Write-Host "⚠️  terraform.tfvars already exists" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Do you want to recreate it? (y/n): " -NoNewline -ForegroundColor Yellow
    $response = Read-Host
    
    if ($response -ne "y" -and $response -ne "Y") {
        Write-Host "Keeping existing terraform.tfvars" -ForegroundColor Gray
        $createTfvars = $false
    } else {
        $createTfvars = $true
    }
} else {
    $createTfvars = $true
}

if ($createTfvars) {
    Write-Host ""
    Write-Host "Let's configure your deployment:" -ForegroundColor Cyan
    Write-Host ""
    
    # Get AWS region
    Write-Host "AWS Region (default: us-east-1): " -NoNewline -ForegroundColor Yellow
    $region = Read-Host
    if ([string]::IsNullOrWhiteSpace($region)) {
        $region = "us-east-1"
    }
    
    # Get Docker image
    Write-Host "Docker image (default: rayenslouma/achat-app:latest): " -NoNewline -ForegroundColor Yellow
    $dockerImage = Read-Host
    if ([string]::IsNullOrWhiteSpace($dockerImage)) {
        $dockerImage = "rayenslouma/achat-app:latest"
    }
    
    # SSH access
    Write-Host ""
    Write-Host "Do you want to enable SSH access? (y/n): " -NoNewline -ForegroundColor Yellow
    $enableSSH = Read-Host
    
    $sshConfig = ""
    if ($enableSSH -eq "y" -or $enableSSH -eq "Y") {
        Write-Host "Enter your SSH key pair name (must exist in AWS): " -NoNewline -ForegroundColor Yellow
        $keyName = Read-Host
        
        Write-Host "Enter your public IP for SSH access (or 0.0.0.0/0 for anywhere): " -NoNewline -ForegroundColor Yellow
        $sshIP = Read-Host
        if ([string]::IsNullOrWhiteSpace($sshIP)) {
            $sshIP = "0.0.0.0/0"
        }
        
        $sshConfig = @"
key_name         = "$keyName"
allowed_ssh_cidr = ["$sshIP"]
"@
    } else {
        $sshConfig = @"
key_name         = ""
allowed_ssh_cidr = []
"@
    }
    
    # Create terraform.tfvars
    $tfvarsContent = @"
# Terraform Variables - Auto-generated
# Generated on: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")

# General Configuration
project_name = "achat-app"
environment  = "sandbox"
aws_region   = "$region"

# Network Configuration
vpc_cidr           = "10.0.0.0/16"
public_subnet_cidr = "10.0.1.0/24"
availability_zone  = "${region}a"

# EC2 Configuration
instance_type    = "t2.micro"
root_volume_size = 20
$sshConfig

# Application Configuration
app_name        = "achat"
app_port        = 8080
docker_image    = "$dockerImage"
docker_username = "rayenslouma"

# Security Configuration
allowed_http_cidr = ["0.0.0.0/0"]

# Feature Flags
enable_monitoring   = false
enable_auto_scaling = false
create_rds          = false

# Tags
common_tags = {
  Project     = "achat-app"
  Environment = "sandbox"
  ManagedBy   = "Terraform"
  Owner       = "DevOps-Team"
}
"@
    
    $tfvarsContent | Out-File -FilePath $tfvarsPath -Encoding UTF8
    Write-Host ""
    Write-Host "✅ terraform.tfvars created!" -ForegroundColor Green
}

Write-Host ""

# ============================================================================
# STEP 4: Initialize Terraform
# ============================================================================

Write-Host "STEP 4: Initializing Terraform..." -ForegroundColor Cyan
Write-Host ""

Push-Location $terraformDir

try {
    Write-Host "Running: terraform init" -ForegroundColor Gray
    Write-Host ""
    
    terraform init
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "✅ Terraform initialized successfully!" -ForegroundColor Green
    } else {
        throw "Terraform init failed"
    }
} catch {
    Write-Host ""
    Write-Host "❌ Terraform initialization failed: $_" -ForegroundColor Red
    Pop-Location
    exit 1
}

Write-Host ""

# ============================================================================
# STEP 5: Validate Configuration
# ============================================================================

Write-Host "STEP 5: Validating Terraform configuration..." -ForegroundColor Cyan
Write-Host ""

try {
    Write-Host "Running: terraform validate" -ForegroundColor Gray
    Write-Host ""
    
    terraform validate
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "✅ Configuration is valid!" -ForegroundColor Green
    } else {
        throw "Terraform validation failed"
    }
} catch {
    Write-Host ""
    Write-Host "❌ Terraform validation failed: $_" -ForegroundColor Red
    Pop-Location
    exit 1
}

Pop-Location

Write-Host ""

# ============================================================================
# STEP 6: Summary
# ============================================================================

Show-Header "Terraform Setup Complete!"

Write-Host "✅ Terraform installed and configured" -ForegroundColor Green
Write-Host "✅ Configuration files validated" -ForegroundColor Green
Write-Host "✅ terraform.tfvars created" -ForegroundColor Green
Write-Host ""
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host ""
Write-Host "📋 TERRAFORM COMMANDS:" -ForegroundColor Yellow
Write-Host ""
Write-Host "  cd terraform" -ForegroundColor White
Write-Host ""
Write-Host "  # Preview changes" -ForegroundColor Gray
Write-Host "  terraform plan" -ForegroundColor Cyan
Write-Host ""
Write-Host "  # Deploy infrastructure" -ForegroundColor Gray
Write-Host "  terraform apply" -ForegroundColor Cyan
Write-Host ""
Write-Host "  # Destroy infrastructure" -ForegroundColor Gray
Write-Host "  terraform destroy" -ForegroundColor Cyan
Write-Host ""
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host ""
Write-Host "🎯 NEXT STEPS:" -ForegroundColor Yellow
Write-Host ""
Write-Host "1. Test Terraform locally (optional)" -ForegroundColor White
Write-Host "   → cd terraform && terraform plan" -ForegroundColor Cyan
Write-Host ""
Write-Host "2. Update Jenkinsfile with Terraform stages" -ForegroundColor White
Write-Host "   → Run: .\scripts\update-jenkinsfile-aws.ps1" -ForegroundColor Cyan
Write-Host ""
Write-Host "3. Commit Terraform files to Git" -ForegroundColor White
Write-Host "   → git add terraform/" -ForegroundColor Cyan
Write-Host "   → git commit -m 'Add Terraform AWS infrastructure'" -ForegroundColor Cyan
Write-Host "   → git push origin main" -ForegroundColor Cyan
Write-Host ""
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host ""
Write-Host "📚 Documentation: AWS_DEPLOYMENT_GUIDE.md" -ForegroundColor Gray
Write-Host ""

Write-Host "Do you want to test Terraform plan now? (y/n): " -NoNewline -ForegroundColor Yellow
$testPlan = Read-Host

if ($testPlan -eq "y" -or $testPlan -eq "Y") {
    Write-Host ""
    Write-Host "Running terraform plan..." -ForegroundColor Cyan
    Write-Host ""
    
    Push-Location $terraformDir
    terraform plan
    Pop-Location
    
    Write-Host ""
    Write-Host "Review the plan above to see what will be created." -ForegroundColor Yellow
    Write-Host ""
}

Write-Host "Setup complete!" -ForegroundColor Green
Write-Host ""

