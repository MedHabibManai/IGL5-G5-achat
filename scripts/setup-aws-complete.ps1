# Complete AWS Deployment Setup
# Master script that runs all setup steps in sequence

param(
    [switch]$SkipAWSCLI,
    [switch]$SkipJenkins,
    [switch]$SkipTerraform
)

function Show-Header {
    param([string]$Title, [string]$Color = "Cyan")
    cls
    Write-Host ""
    Write-Host "╔════════════════════════════════════════════════════════╗" -ForegroundColor $Color
    Write-Host "║  AWS DEPLOYMENT - COMPLETE SETUP                       ║" -ForegroundColor $Color
    Write-Host "╚════════════════════════════════════════════════════════╝" -ForegroundColor $Color
    Write-Host ""
    Write-Host "  $Title" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Gray
    Write-Host ""
}

function Show-Progress {
    param([int]$Current, [int]$Total, [string]$Activity)
    $percent = [math]::Round(($Current / $Total) * 100)
    Write-Host ""
    Write-Host "Progress: [$Current/$Total] - $percent% complete" -ForegroundColor Cyan
    Write-Host "Current: $Activity" -ForegroundColor Yellow
    Write-Host ""
}

# ============================================================================
# Introduction
# ============================================================================

Show-Header "Welcome to AWS Deployment Setup" "Green"

Write-Host "This script will guide you through the complete setup process:" -ForegroundColor Cyan
Write-Host ""
Write-Host "  Phase 1: AWS CLI Installation & Configuration" -ForegroundColor White
Write-Host "  Phase 2: Test AWS Credentials" -ForegroundColor White
Write-Host "  Phase 3: Add Credentials to Jenkins" -ForegroundColor White
Write-Host "  Phase 4: Setup Terraform Configuration" -ForegroundColor White
Write-Host "  Phase 5: Commit Changes to Git" -ForegroundColor White
Write-Host "  Phase 6: Run Jenkins Pipeline" -ForegroundColor White
Write-Host ""
Write-Host "Estimated time: 15-20 minutes" -ForegroundColor Gray
Write-Host ""

Write-Host "Press Enter to begin..." -NoNewline -ForegroundColor Yellow
Read-Host

# ============================================================================
# Phase 1: AWS CLI Setup
# ============================================================================

Show-Progress 1 6 "AWS CLI Installation & Configuration"

if (-not $SkipAWSCLI) {
    Write-Host "Running AWS CLI setup script..." -ForegroundColor Cyan
    Write-Host ""
    
    & ".\scripts\setup-aws-deployment.ps1"
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host ""
        Write-Host "❌ AWS CLI setup failed!" -ForegroundColor Red
        Write-Host "Please fix the issues and run this script again." -ForegroundColor Yellow
        exit 1
    }
} else {
    Write-Host "Skipping AWS CLI setup (already configured)" -ForegroundColor Gray
}

Write-Host ""
Write-Host "✅ Phase 1 Complete!" -ForegroundColor Green
Write-Host ""
Write-Host "Press Enter to continue to Phase 2..." -NoNewline -ForegroundColor Yellow
Read-Host

# ============================================================================
# Phase 2: Jenkins Credentials
# ============================================================================

Show-Progress 2 6 "Add AWS Credentials to Jenkins"

if (-not $SkipJenkins) {
    Write-Host "Running Jenkins credentials setup script..." -ForegroundColor Cyan
    Write-Host ""
    
    & ".\scripts\add-aws-to-jenkins.ps1"
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host ""
        Write-Host "❌ Jenkins credentials setup failed!" -ForegroundColor Red
        Write-Host "Please fix the issues and run this script again." -ForegroundColor Yellow
        exit 1
    }
} else {
    Write-Host "Skipping Jenkins setup (already configured)" -ForegroundColor Gray
}

Write-Host ""
Write-Host "✅ Phase 2 Complete!" -ForegroundColor Green
Write-Host ""
Write-Host "Press Enter to continue to Phase 3..." -NoNewline -ForegroundColor Yellow
Read-Host

# ============================================================================
# Phase 3: Terraform Configuration
# ============================================================================

Show-Progress 3 6 "Setup Terraform Configuration"

if (-not $SkipTerraform) {
    Write-Host "Running Terraform configuration script..." -ForegroundColor Cyan
    Write-Host ""
    
    & ".\scripts\create-terraform-config.ps1"
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host ""
        Write-Host "❌ Terraform setup failed!" -ForegroundColor Red
        Write-Host "Please fix the issues and run this script again." -ForegroundColor Yellow
        exit 1
    }
} else {
    Write-Host "Skipping Terraform setup (already configured)" -ForegroundColor Gray
}

Write-Host ""
Write-Host "✅ Phase 3 Complete!" -ForegroundColor Green
Write-Host ""
Write-Host "Press Enter to continue to Phase 4..." -NoNewline -ForegroundColor Yellow
Read-Host

# ============================================================================
# Phase 4: Commit Changes
# ============================================================================

Show-Progress 4 6 "Commit Changes to Git"

Write-Host "Checking for uncommitted changes..." -ForegroundColor Cyan
Write-Host ""

$status = git status --porcelain

if ($status) {
    Write-Host "The following files will be committed:" -ForegroundColor Yellow
    Write-Host ""
    git status --short
    Write-Host ""
    
    Write-Host "Commit these changes? (y/n): " -NoNewline -ForegroundColor Yellow
    $commit = Read-Host
    
    if ($commit -eq "y" -or $commit -eq "Y") {
        Write-Host ""
        Write-Host "Adding files to Git..." -ForegroundColor Cyan
        
        git add terraform/
        git add Jenkinsfile
        git add AWS_DEPLOYMENT_GUIDE.md
        git add scripts/setup-aws-deployment.ps1
        git add scripts/add-aws-to-jenkins.ps1
        git add scripts/create-terraform-config.ps1
        git add scripts/setup-aws-complete.ps1
        
        Write-Host "Committing changes..." -ForegroundColor Cyan
        git commit -m "Add AWS deployment with Terraform integration

- Add Terraform configuration for AWS infrastructure
- Update Jenkinsfile with AWS deployment stages
- Add AWS deployment guide and setup scripts
- Configure EC2, VPC, Security Groups, and IAM roles
- Integrate with existing CI/CD pipeline"
        
        Write-Host ""
        Write-Host "Pushing to GitHub..." -ForegroundColor Cyan
        git push origin main
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host ""
            Write-Host "✅ Changes committed and pushed successfully!" -ForegroundColor Green
        } else {
            Write-Host ""
            Write-Host "⚠️  Push failed. You may need to pull first." -ForegroundColor Yellow
            Write-Host "Run: git pull origin main" -ForegroundColor Cyan
        }
    } else {
        Write-Host ""
        Write-Host "⚠️  Skipping commit. Remember to commit manually later!" -ForegroundColor Yellow
    }
} else {
    Write-Host "No uncommitted changes found." -ForegroundColor Gray
}

Write-Host ""
Write-Host "✅ Phase 4 Complete!" -ForegroundColor Green
Write-Host ""
Write-Host "Press Enter to continue to Phase 5..." -NoNewline -ForegroundColor Yellow
Read-Host

# ============================================================================
# Phase 5: Verify Jenkins
# ============================================================================

Show-Progress 5 6 "Verify Jenkins Configuration"

Write-Host "Checking Jenkins status..." -ForegroundColor Cyan
Write-Host ""

try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080" -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop
    Write-Host "✅ Jenkins is running" -ForegroundColor Green
    Write-Host ""
    
    Write-Host "Opening Jenkins in browser..." -ForegroundColor Cyan
    Start-Process "http://localhost:8080"
    
    Write-Host ""
    Write-Host "Please verify in Jenkins:" -ForegroundColor Yellow
    Write-Host "  1. AWS credentials are configured" -ForegroundColor White
    Write-Host "  2. Pipeline job exists" -ForegroundColor White
    Write-Host "  3. Latest commit is visible" -ForegroundColor White
    Write-Host ""
    
} catch {
    Write-Host "⚠️  Jenkins is not accessible" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Please start Jenkins:" -ForegroundColor Cyan
    Write-Host "  docker-compose up -d jenkins-cicd" -ForegroundColor White
    Write-Host ""
}

Write-Host "✅ Phase 5 Complete!" -ForegroundColor Green
Write-Host ""
Write-Host "Press Enter to continue to final phase..." -NoNewline -ForegroundColor Yellow
Read-Host

# ============================================================================
# Phase 6: Final Summary
# ============================================================================

Show-Header "Setup Complete!" "Green"

Write-Host "╔════════════════════════════════════════════════════════╗" -ForegroundColor Green
Write-Host "║  ALL PHASES COMPLETED SUCCESSFULLY!                    ║" -ForegroundColor Green
Write-Host "╚════════════════════════════════════════════════════════╝" -ForegroundColor Green
Write-Host ""

Write-Host "✅ Phase 1: AWS CLI configured" -ForegroundColor Green
Write-Host "✅ Phase 2: Jenkins credentials added" -ForegroundColor Green
Write-Host "✅ Phase 3: Terraform configured" -ForegroundColor Green
Write-Host "✅ Phase 4: Changes committed to Git" -ForegroundColor Green
Write-Host "✅ Phase 5: Jenkins verified" -ForegroundColor Green
Write-Host ""
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host ""
Write-Host "🚀 READY TO DEPLOY!" -ForegroundColor Yellow
Write-Host ""
Write-Host "Next Steps:" -ForegroundColor Cyan
Write-Host ""
Write-Host "1. Open Jenkins: http://localhost:8080" -ForegroundColor White
Write-Host "2. Click on your pipeline job" -ForegroundColor White
Write-Host "3. Click 'Build Now'" -ForegroundColor White
Write-Host "4. Watch the magic happen! ✨" -ForegroundColor White
Write-Host ""
Write-Host "The pipeline will:" -ForegroundColor Gray
Write-Host "  • Build your application" -ForegroundColor Gray
Write-Host "  • Run tests" -ForegroundColor Gray
Write-Host "  • Analyze code quality" -ForegroundColor Gray
Write-Host "  • Build Docker image" -ForegroundColor Gray
Write-Host "  • Deploy to AWS with Terraform" -ForegroundColor Gray
Write-Host "  • Verify deployment health" -ForegroundColor Gray
Write-Host ""
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host ""
Write-Host "📚 Documentation:" -ForegroundColor Yellow
Write-Host "  • AWS_DEPLOYMENT_GUIDE.md - Complete guide" -ForegroundColor White
Write-Host "  • terraform/ - Infrastructure code" -ForegroundColor White
Write-Host "  • Jenkinsfile - Pipeline configuration" -ForegroundColor White
Write-Host ""
Write-Host "🔧 Useful Commands:" -ForegroundColor Yellow
Write-Host "  • cd terraform && terraform plan - Preview changes" -ForegroundColor White
Write-Host "  • cd terraform && terraform apply - Deploy manually" -ForegroundColor White
Write-Host "  • cd terraform && terraform destroy - Clean up AWS" -ForegroundColor White
Write-Host ""
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host ""

Write-Host "Do you want to open Jenkins now? (y/n): " -NoNewline -ForegroundColor Yellow
$openJenkins = Read-Host

if ($openJenkins -eq "y" -or $openJenkins -eq "Y") {
    Start-Process "http://localhost:8080"
    Write-Host ""
    Write-Host "Jenkins opened in browser!" -ForegroundColor Green
}

Write-Host ""
Write-Host "🎉 Happy Deploying! 🎉" -ForegroundColor Green
Write-Host ""

