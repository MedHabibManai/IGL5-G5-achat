# PowerShell script to test EKS deployment stage via Jenkins CLI
# This script triggers only the deployToEKS stage

param(
    [string]$JenkinsUrl = "http://localhost:8080",
    [string]$JobName = "IGL5-G5-achat",
    [string]$JenkinsUser = "",
    [string]$JenkinsToken = ""
)

Write-Host "Testing EKS Deployment Stage (Stage 13)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Check if Jenkins CLI is available
if (-not (Get-Command jenkins-cli -ErrorAction SilentlyContinue)) {
    Write-Host "Jenkins CLI not found. Installing..." -ForegroundColor Yellow
    Write-Host "Please install Jenkins CLI or use the web interface:" -ForegroundColor Yellow
    Write-Host "1. Go to: $JenkinsUrl/job/$JobName" -ForegroundColor Yellow
    Write-Host "2. Click 'Pipeline Syntax' or 'Build with Parameters'" -ForegroundColor Yellow
    Write-Host "3. Or use the test script: test-eks-deployment.groovy" -ForegroundColor Yellow
    exit 1
}

# Build the job (this will run all stages, but you can check the console for stage 13)
Write-Host "`nTriggering build to test EKS deployment stage..." -ForegroundColor Green
Write-Host "Note: This will run the full pipeline. Check the console output for Stage 13." -ForegroundColor Yellow

if ($JenkinsUser -and $JenkinsToken) {
    java -jar jenkins-cli.jar -s $JenkinsUrl -auth $JenkinsUser`:$JenkinsToken build $JobName -p DEPLOYMENT_MODE=REUSE_INFRASTRUCTURE
} else {
    java -jar jenkins-cli.jar -s $JenkinsUrl build $JobName -p DEPLOYMENT_MODE=REUSE_INFRASTRUCTURE
}

Write-Host "`nBuild triggered! Check the Jenkins console for Stage 13 output." -ForegroundColor Green

