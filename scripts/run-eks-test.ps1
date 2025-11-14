# Quick script to test EKS deployment stage
# This creates a temporary Jenkinsfile that only runs the EKS stage

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "EKS Deployment Stage Test Script" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if we're in the right directory
if (-not (Test-Path "jenkins/stages/deployToEKS.groovy")) {
    Write-Host "ERROR: jenkins/stages/deployToEKS.groovy not found!" -ForegroundColor Red
    Write-Host "Please run this script from the project root directory." -ForegroundColor Red
    exit 1
}

Write-Host "Options to test EKS deployment stage:" -ForegroundColor Yellow
Write-Host ""
Write-Host "Option 1: Use Jenkins 'Replay' feature (Recommended)" -ForegroundColor Green
Write-Host "  1. Go to your last Jenkins build" -ForegroundColor White
Write-Host "  2. Click 'Replay' button" -ForegroundColor White
Write-Host "  3. Modify the Jenkinsfile to only include deployToEKS stage" -ForegroundColor White
Write-Host "  4. Or use the test file: Jenkinsfile.test-eks-only" -ForegroundColor White
Write-Host ""
Write-Host "Option 2: Create a new Jenkins job" -ForegroundColor Green
Write-Host "  1. Create new Pipeline job in Jenkins" -ForegroundColor White
Write-Host "  2. Point it to: Jenkinsfile.test-eks-only" -ForegroundColor White
Write-Host "  3. Run the job" -ForegroundColor White
Write-Host ""
Write-Host "Option 3: Test via Jenkins Script Console" -ForegroundColor Green
Write-Host "  1. Go to: Manage Jenkins > Script Console" -ForegroundColor White
Write-Host "  2. Paste the contents of: test-eks-deployment.groovy" -ForegroundColor White
Write-Host "  3. Adjust environment variables as needed" -ForegroundColor White
Write-Host "  4. Run the script" -ForegroundColor White
Write-Host ""
Write-Host "Option 4: Manual test via kubectl (if already connected)" -ForegroundColor Green
Write-Host "  Run: docker exec jenkins-cicd kubectl get pods -n achat-app" -ForegroundColor White
Write-Host ""

# Check if Jenkins is running
$jenkinsRunning = docker ps --filter "name=jenkins" --format "{{.Names}}" 2>$null
if ($jenkinsRunning) {
    Write-Host "Jenkins container is running: $jenkinsRunning" -ForegroundColor Green
    Write-Host ""
    Write-Host "Quick test - Check current EKS pods:" -ForegroundColor Cyan
    docker exec jenkins-cicd kubectl get pods -n achat-app 2>&1 | Select-Object -First 10
} else {
    Write-Host "Jenkins container not found. Make sure Jenkins is running." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Test files created:" -ForegroundColor Cyan
Write-Host "  - Jenkinsfile.test-eks-only (standalone test pipeline)" -ForegroundColor White
Write-Host "  - test-eks-deployment.groovy (script for Jenkins console)" -ForegroundColor White
Write-Host ""

