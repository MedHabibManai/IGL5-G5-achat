# Direct test of EKS deployment stage
# This script runs the EKS deployment logic directly in the Jenkins container

Write-Host "Testing EKS Deployment Stage Directly" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

# Check if Jenkins is running
$jenkinsContainer = docker ps --filter "name=jenkins" --format "{{.Names}}" 2>$null
if (-not $jenkinsContainer) {
    Write-Host "ERROR: Jenkins container not running!" -ForegroundColor Red
    exit 1
}

Write-Host "Jenkins container found: $jenkinsContainer" -ForegroundColor Green
Write-Host ""

# Get the latest build number
Write-Host "Getting latest build info..." -ForegroundColor Yellow
$latestBuild = docker exec jenkins-cicd ls -t /var/jenkins_home/jobs/IGL5-G5-achat/builds 2>$null | Select-Object -First 1
if ($latestBuild) {
    Write-Host "Latest build: $latestBuild" -ForegroundColor Green
} else {
    Write-Host "Could not determine latest build. Using 999 as test build number." -ForegroundColor Yellow
    $latestBuild = "999"
}

Write-Host ""
Write-Host "To test the EKS deployment stage, you have these options:" -ForegroundColor Cyan
Write-Host ""
Write-Host "OPTION 1: Jenkins Replay (Easiest)" -ForegroundColor Green
Write-Host "  1. Open Jenkins: http://localhost:8080" -ForegroundColor White
Write-Host "  2. Go to: IGL5-G5-achat job > Last build" -ForegroundColor White
Write-Host "  3. Click 'Replay' button" -ForegroundColor White
Write-Host "  4. Replace the Jenkinsfile content with: Jenkinsfile.test-eks-only" -ForegroundColor White
Write-Host "  5. Click 'Run' - This will only execute the EKS stage" -ForegroundColor White
Write-Host ""
Write-Host "OPTION 2: Create New Test Job" -ForegroundColor Green
Write-Host "  1. Jenkins > New Item > Pipeline" -ForegroundColor White
Write-Host "  2. Name: test-eks-deployment" -ForegroundColor White
Write-Host "  3. Pipeline > Definition: Pipeline script from SCM" -ForegroundColor White
Write-Host "  4. SCM: Git, Repository: your repo" -ForegroundColor White
Write-Host "  5. Script Path: Jenkinsfile.test-eks-only" -ForegroundColor White
Write-Host "  6. Save and Build" -ForegroundColor White
Write-Host ""
Write-Host "OPTION 3: Manual kubectl test (quick check)" -ForegroundColor Green
Write-Host "  Testing current EKS connection..." -ForegroundColor Yellow
docker exec jenkins-cicd kubectl get nodes 2>&1 | Select-Object -First 5
Write-Host ""

