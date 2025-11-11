# AWS Deployment Setup Script
# Interactive guide for integrating AWS with Jenkins CI/CD pipeline

param(
    [switch]$SkipAWSCLI,
    [switch]$SkipCredentials
)

function Show-Header {
    param([string]$Title)
    cls
    Write-Host ""
    Write-Host "╔════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
    Write-Host "║  AWS + JENKINS CI/CD DEPLOYMENT SETUP                  ║" -ForegroundColor Cyan
    Write-Host "╚════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "  $Title" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Gray
    Write-Host ""
}

function Show-Phase {
    param([string]$Phase, [string]$Description)
    Write-Host ""
    Write-Host "═══════════════════════════════════════════════════════" -ForegroundColor Cyan
    Write-Host "  $Phase" -ForegroundColor Yellow
    Write-Host "  $Description" -ForegroundColor Gray
    Write-Host "═══════════════════════════════════════════════════════" -ForegroundColor Cyan
    Write-Host ""
}

function Confirm-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host "$Message (y/n): " -NoNewline -ForegroundColor Yellow
    $response = Read-Host
    return ($response -eq "y" -or $response -eq "Y")
}

# ============================================================================
# PHASE 1: AWS CLI Installation
# ============================================================================

Show-Header "Phase 1: AWS CLI Installation & Configuration"

if (-not $SkipAWSCLI) {
    Show-Phase "STEP 1.1" "Check AWS CLI Installation"
    
    try {
        $awsVersion = aws --version 2>&1
        Write-Host "✅ AWS CLI is already installed:" -ForegroundColor Green
        Write-Host "   $awsVersion" -ForegroundColor Gray
        Write-Host ""
        
        if (-not (Confirm-Step "Do you want to reinstall/update AWS CLI?")) {
            Write-Host "   Skipping AWS CLI installation..." -ForegroundColor Gray
        } else {
            $installAWS = $true
        }
    } catch {
        Write-Host "❌ AWS CLI is not installed" -ForegroundColor Red
        Write-Host ""
        $installAWS = $true
    }
    
    if ($installAWS) {
        Show-Phase "STEP 1.2" "Installing AWS CLI v2"
        
        Write-Host "Downloading AWS CLI installer..." -ForegroundColor Cyan
        $installerPath = "$env:TEMP\AWSCLIV2.msi"
        
        try {
            Invoke-WebRequest -Uri "https://awscli.amazonaws.com/AWSCLIV2.msi" -OutFile $installerPath
            Write-Host "✅ Download complete" -ForegroundColor Green
            
            Write-Host ""
            Write-Host "Installing AWS CLI (this may take a minute)..." -ForegroundColor Cyan
            Start-Process msiexec.exe -ArgumentList "/i $installerPath /quiet /norestart" -Wait
            
            Write-Host "✅ AWS CLI installed successfully!" -ForegroundColor Green
            Write-Host ""
            Write-Host "⚠️  Please close and reopen PowerShell for changes to take effect" -ForegroundColor Yellow
            Write-Host ""
            
            if (Confirm-Step "Continue with credential configuration?") {
                # Continue
            } else {
                Write-Host ""
                Write-Host "Please reopen PowerShell and run this script again." -ForegroundColor Yellow
                exit 0
            }
        } catch {
            Write-Host "❌ Failed to install AWS CLI: $_" -ForegroundColor Red
            Write-Host ""
            Write-Host "Please install manually from: https://awscli.amazonaws.com/AWSCLIV2.msi" -ForegroundColor Yellow
            exit 1
        }
    }
}

# ============================================================================
# PHASE 2: AWS Credentials Configuration
# ============================================================================

Show-Header "Phase 2: AWS Credentials Configuration"

if (-not $SkipCredentials) {
    Show-Phase "STEP 2.1" "Retrieve AWS Credentials from Sandbox"
    
    Write-Host "📋 Where to find your AWS credentials:" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Option A: AWS Academy / Learner Lab" -ForegroundColor Yellow
    Write-Host "  1. Log in to AWS Academy" -ForegroundColor White
    Write-Host "  2. Go to your course → Modules → Learner Lab" -ForegroundColor White
    Write-Host "  3. Click 'Start Lab' and wait for green indicator" -ForegroundColor White
    Write-Host "  4. Click 'AWS Details' → Show AWS CLI credentials" -ForegroundColor White
    Write-Host "  5. Copy the credentials block" -ForegroundColor White
    Write-Host ""
    Write-Host "Option B: AWS Console (IAM)" -ForegroundColor Yellow
    Write-Host "  1. Log in to AWS Console" -ForegroundColor White
    Write-Host "  2. Go to IAM → Users → Security credentials" -ForegroundColor White
    Write-Host "  3. Create access key → CLI" -ForegroundColor White
    Write-Host "  4. Download or copy credentials" -ForegroundColor White
    Write-Host ""
    
    if (-not (Confirm-Step "Have you retrieved your AWS credentials?")) {
        Write-Host ""
        Write-Host "Please retrieve your credentials first, then run this script again." -ForegroundColor Yellow
        Write-Host "See AWS_DEPLOYMENT_GUIDE.md for detailed instructions." -ForegroundColor Gray
        exit 0
    }
    
    Show-Phase "STEP 2.2" "Configure AWS Credentials"
    
    Write-Host "Choose configuration method:" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "  1. Interactive (aws configure) - Simple, no session token" -ForegroundColor White
    Write-Host "  2. Manual file edit - For session tokens (sandbox environments)" -ForegroundColor White
    Write-Host "  3. Skip - Already configured" -ForegroundColor Gray
    Write-Host ""
    Write-Host "Enter choice (1-3): " -NoNewline -ForegroundColor Yellow
    $choice = Read-Host
    
    switch ($choice) {
        "1" {
            Write-Host ""
            Write-Host "Running 'aws configure'..." -ForegroundColor Cyan
            Write-Host "You'll be prompted for:" -ForegroundColor Gray
            Write-Host "  - AWS Access Key ID" -ForegroundColor Gray
            Write-Host "  - AWS Secret Access Key" -ForegroundColor Gray
            Write-Host "  - Default region (e.g., us-east-1)" -ForegroundColor Gray
            Write-Host "  - Output format (json recommended)" -ForegroundColor Gray
            Write-Host ""
            
            aws configure
        }
        "2" {
            Write-Host ""
            Write-Host "Creating .aws directory..." -ForegroundColor Cyan
            New-Item -ItemType Directory -Force -Path "$env:USERPROFILE\.aws" | Out-Null
            
            Write-Host "Opening credentials file in Notepad..." -ForegroundColor Cyan
            Write-Host ""
            Write-Host "Paste your credentials in this format:" -ForegroundColor Yellow
            Write-Host ""
            Write-Host "[default]" -ForegroundColor Gray
            Write-Host "aws_access_key_id = YOUR_ACCESS_KEY_ID" -ForegroundColor Gray
            Write-Host "aws_secret_access_key = YOUR_SECRET_ACCESS_KEY" -ForegroundColor Gray
            Write-Host "aws_session_token = YOUR_SESSION_TOKEN" -ForegroundColor Gray
            Write-Host ""
            Write-Host "Press Enter to open the file..." -NoNewline -ForegroundColor Yellow
            Read-Host
            
            notepad "$env:USERPROFILE\.aws\credentials"
            
            Write-Host ""
            Write-Host "Opening config file..." -ForegroundColor Cyan
            Write-Host "Add this content:" -ForegroundColor Yellow
            Write-Host ""
            Write-Host "[default]" -ForegroundColor Gray
            Write-Host "region = us-east-1" -ForegroundColor Gray
            Write-Host "output = json" -ForegroundColor Gray
            Write-Host ""
            Write-Host "Press Enter to open the file..." -NoNewline -ForegroundColor Yellow
            Read-Host
            
            notepad "$env:USERPROFILE\.aws\config"
        }
        "3" {
            Write-Host "Skipping credential configuration..." -ForegroundColor Gray
        }
        default {
            Write-Host "Invalid choice. Skipping..." -ForegroundColor Red
        }
    }
}

# ============================================================================
# PHASE 3: Test AWS Credentials
# ============================================================================

Show-Header "Phase 3: Test AWS Credentials"

Show-Phase "STEP 3.1" "Verify AWS Credentials"

Write-Host "Testing AWS credentials..." -ForegroundColor Cyan
Write-Host ""

try {
    Write-Host "Test 1: Get caller identity (who am I?)" -ForegroundColor Yellow
    $identity = aws sts get-caller-identity 2>&1
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Credentials are valid!" -ForegroundColor Green
        Write-Host $identity -ForegroundColor Gray
        Write-Host ""
        
        # Parse account ID
        $identityJson = $identity | ConvertFrom-Json
        $accountId = $identityJson.Account
        $userId = $identityJson.UserId
        $arn = $identityJson.Arn
        
        Write-Host "Account ID: $accountId" -ForegroundColor Cyan
        Write-Host "User ID: $userId" -ForegroundColor Cyan
        Write-Host "ARN: $arn" -ForegroundColor Cyan
        Write-Host ""
    } else {
        throw "Failed to authenticate"
    }
    
    Write-Host "Test 2: List S3 buckets" -ForegroundColor Yellow
    $buckets = aws s3 ls 2>&1
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ S3 access confirmed!" -ForegroundColor Green
        if ($buckets) {
            Write-Host $buckets -ForegroundColor Gray
        } else {
            Write-Host "  (No buckets found - this is normal)" -ForegroundColor Gray
        }
    } else {
        Write-Host "⚠️  S3 access issue (may be permission-related)" -ForegroundColor Yellow
        Write-Host $buckets -ForegroundColor Gray
    }
    Write-Host ""
    
    Write-Host "Test 3: Get default region" -ForegroundColor Yellow
    $region = aws configure get region
    if ($region) {
        Write-Host "✅ Default region: $region" -ForegroundColor Green
    } else {
        Write-Host "⚠️  No default region set (will use us-east-1)" -ForegroundColor Yellow
        $region = "us-east-1"
    }
    Write-Host ""
    
    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Green
    Write-Host "✅ AWS CLI is configured and working!" -ForegroundColor Green
    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Green
    Write-Host ""
    
} catch {
    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Red
    Write-Host "❌ AWS credentials test failed!" -ForegroundColor Red
    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Red
    Write-Host ""
    Write-Host "Error: $_" -ForegroundColor Red
    Write-Host ""
    Write-Host "Common issues:" -ForegroundColor Yellow
    Write-Host "  1. Credentials not configured - run 'aws configure'" -ForegroundColor White
    Write-Host "  2. Session token expired - get new credentials from sandbox" -ForegroundColor White
    Write-Host "  3. Invalid credentials - check for typos" -ForegroundColor White
    Write-Host "  4. Network issues - check internet connection" -ForegroundColor White
    Write-Host ""
    Write-Host "See AWS_DEPLOYMENT_GUIDE.md for troubleshooting." -ForegroundColor Gray
    exit 1
}

# ============================================================================
# PHASE 4: Summary and Next Steps
# ============================================================================

Show-Header "Setup Complete - Next Steps"

Write-Host "✅ Phase 1: AWS CLI installed and configured" -ForegroundColor Green
Write-Host "✅ Phase 2: AWS credentials configured" -ForegroundColor Green
Write-Host "✅ Phase 3: Credentials tested successfully" -ForegroundColor Green
Write-Host ""
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host ""
Write-Host "🎯 NEXT STEPS:" -ForegroundColor Yellow
Write-Host ""
Write-Host "1. Add AWS credentials to Jenkins" -ForegroundColor White
Write-Host "   → Run: .\scripts\add-aws-to-jenkins.ps1" -ForegroundColor Cyan
Write-Host ""
Write-Host "2. Create Terraform configuration files" -ForegroundColor White
Write-Host "   → Run: .\scripts\create-terraform-config.ps1" -ForegroundColor Cyan
Write-Host ""
Write-Host "3. Update Jenkinsfile with AWS deployment stages" -ForegroundColor White
Write-Host "   → Run: .\scripts\update-jenkinsfile-aws.ps1" -ForegroundColor Cyan
Write-Host ""
Write-Host "4. Test the complete pipeline" -ForegroundColor White
Write-Host "   → Open Jenkins: http://localhost:8080" -ForegroundColor Cyan
Write-Host ""
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host ""
Write-Host "📚 Documentation: AWS_DEPLOYMENT_GUIDE.md" -ForegroundColor Gray
Write-Host ""

if (Confirm-Step "Do you want to continue with Jenkins credential setup now?") {
    Write-Host ""
    Write-Host "Opening Jenkins credentials page..." -ForegroundColor Cyan
    Start-Process "http://localhost:8080/credentials/"
    Write-Host ""
    Write-Host "Follow the instructions in AWS_DEPLOYMENT_GUIDE.md Phase 3" -ForegroundColor Yellow
    Write-Host ""
}

Write-Host "Setup script completed!" -ForegroundColor Green
Write-Host ""

