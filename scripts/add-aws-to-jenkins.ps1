# Add AWS Credentials to Jenkins
# Interactive guide for adding AWS credentials to Jenkins Credentials Manager

function Show-Header {
    param([string]$Title)
    cls
    Write-Host ""
    Write-Host "╔════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
    Write-Host "║  ADD AWS CREDENTIALS TO JENKINS                        ║" -ForegroundColor Cyan
    Write-Host "╚════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "  $Title" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Gray
    Write-Host ""
}

Show-Header "Step-by-Step Guide"

# ============================================================================
# STEP 1: Check Jenkins is running
# ============================================================================

Write-Host "STEP 1: Checking Jenkins status..." -ForegroundColor Cyan
Write-Host ""

try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080" -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop
    Write-Host "✅ Jenkins is running at http://localhost:8080" -ForegroundColor Green
} catch {
    Write-Host "❌ Jenkins is not accessible at http://localhost:8080" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please start Jenkins first:" -ForegroundColor Yellow
    Write-Host "  docker-compose up -d jenkins-cicd" -ForegroundColor Cyan
    Write-Host ""
    exit 1
}

Write-Host ""

# ============================================================================
# STEP 2: Check AWS CLI credentials
# ============================================================================

Write-Host "STEP 2: Checking AWS credentials..." -ForegroundColor Cyan
Write-Host ""

# Read credentials from AWS CLI config
$credentialsPath = "$env:USERPROFILE\.aws\credentials"
$configPath = "$env:USERPROFILE\.aws\config"

if (Test-Path $credentialsPath) {
    Write-Host "✅ AWS credentials file found" -ForegroundColor Green
    
    # Parse credentials
    $credContent = Get-Content $credentialsPath -Raw
    
    if ($credContent -match "aws_access_key_id\s*=\s*(.+)") {
        $accessKeyId = $matches[1].Trim()
        Write-Host "   Access Key ID: $($accessKeyId.Substring(0, [Math]::Min(10, $accessKeyId.Length)))..." -ForegroundColor Gray
    }
    
    if ($credContent -match "aws_secret_access_key\s*=\s*(.+)") {
        $secretAccessKey = $matches[1].Trim()
        Write-Host "   Secret Access Key: ****" -ForegroundColor Gray
    }
    
    $hasSessionToken = $credContent -match "aws_session_token"
    if ($hasSessionToken) {
        Write-Host "   Session Token: Found (sandbox environment)" -ForegroundColor Gray
    }
} else {
    Write-Host "⚠️  AWS credentials file not found" -ForegroundColor Yellow
    Write-Host "   Run: .\scripts\setup-aws-deployment.ps1" -ForegroundColor Cyan
    Write-Host ""
}

if (Test-Path $configPath) {
    $configContent = Get-Content $configPath -Raw
    if ($configContent -match "region\s*=\s*(.+)") {
        $region = $matches[1].Trim()
        Write-Host "   Region: $region" -ForegroundColor Gray
    }
}

Write-Host ""

# ============================================================================
# STEP 3: Install Jenkins Plugins
# ============================================================================

Write-Host "STEP 3: Jenkins Plugins Required" -ForegroundColor Cyan
Write-Host ""
Write-Host "The following plugins must be installed in Jenkins:" -ForegroundColor Yellow
Write-Host ""
Write-Host "  ✓ CloudBees AWS Credentials Plugin" -ForegroundColor White
Write-Host "  ✓ Pipeline: AWS Steps" -ForegroundColor White
Write-Host ""
Write-Host "To install:" -ForegroundColor Gray
Write-Host "  1. Go to: http://localhost:8080/manage/pluginManager/" -ForegroundColor White
Write-Host "  2. Click 'Available' tab" -ForegroundColor White
Write-Host "  3. Search for 'AWS Credentials' and 'Pipeline AWS Steps'" -ForegroundColor White
Write-Host "  4. Check both plugins and click 'Install without restart'" -ForegroundColor White
Write-Host ""

Write-Host "Press Enter when plugins are installed..." -NoNewline -ForegroundColor Yellow
Read-Host

# ============================================================================
# STEP 4: Add Credentials to Jenkins
# ============================================================================

Show-Header "Add Credentials to Jenkins"

Write-Host "STEP 4: Adding AWS Credentials to Jenkins" -ForegroundColor Cyan
Write-Host ""
Write-Host "Opening Jenkins Credentials page..." -ForegroundColor Gray
Start-Process "http://localhost:8080/credentials/store/system/domain/_/"
Write-Host ""

Write-Host "Follow these steps in Jenkins:" -ForegroundColor Yellow
Write-Host ""
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Gray
Write-Host ""
Write-Host "METHOD A: AWS Credentials (Recommended)" -ForegroundColor Cyan
Write-Host ""
Write-Host "1. Click 'Add Credentials' button" -ForegroundColor White
Write-Host ""
Write-Host "2. Fill in the form:" -ForegroundColor White
Write-Host "   Kind: AWS Credentials" -ForegroundColor Gray
Write-Host "   ID: aws-sandbox-credentials" -ForegroundColor Yellow
Write-Host "   Description: AWS CloudFormation Sandbox Credentials" -ForegroundColor Gray
Write-Host "   Access Key ID: $accessKeyId" -ForegroundColor Gray
Write-Host "   Secret Access Key: [paste your secret key]" -ForegroundColor Gray
Write-Host ""
Write-Host "3. Click 'OK'" -ForegroundColor White
Write-Host ""

if ($hasSessionToken) {
    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Gray
    Write-Host ""
    Write-Host "METHOD B: Add Session Token (For Sandbox Environments)" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Since you have a session token, add it separately:" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "1. Click 'Add Credentials' again" -ForegroundColor White
    Write-Host ""
    Write-Host "2. Fill in the form:" -ForegroundColor White
    Write-Host "   Kind: Secret text" -ForegroundColor Gray
    Write-Host "   Secret: [paste your session token]" -ForegroundColor Gray
    Write-Host "   ID: aws-session-token" -ForegroundColor Yellow
    Write-Host "   Description: AWS Session Token (expires in 3-4 hours)" -ForegroundColor Gray
    Write-Host ""
    Write-Host "3. Click 'OK'" -ForegroundColor White
    Write-Host ""
    Write-Host "⚠️  NOTE: Session tokens expire! You'll need to update this regularly." -ForegroundColor Yellow
    Write-Host ""
}

Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Gray
Write-Host ""

Write-Host "Press Enter when credentials are added..." -NoNewline -ForegroundColor Yellow
Read-Host

# ============================================================================
# STEP 5: Create Test Pipeline
# ============================================================================

Show-Header "Test AWS Credentials in Jenkins"

Write-Host "STEP 5: Creating test pipeline..." -ForegroundColor Cyan
Write-Host ""

$testPipeline = @"
pipeline {
    agent any
    
    environment {
        AWS_DEFAULT_REGION = '$region'
    }
    
    stages {
        stage('Test AWS Credentials') {
            steps {
                script {
                    echo '========================================='
                    echo 'Testing AWS Credentials'
                    echo '========================================='
                }
                
                withAWS(credentials: 'aws-sandbox-credentials', region: '$region') {
                    sh '''
                        echo "Testing AWS CLI..."
                        aws --version
                        
                        echo ""
                        echo "Getting caller identity..."
                        aws sts get-caller-identity
                        
                        echo ""
                        echo "Listing S3 buckets..."
                        aws s3 ls || echo "No S3 buckets or no permission"
                        
                        echo ""
                        echo "Getting available regions..."
                        aws ec2 describe-regions --query 'Regions[*].RegionName' --output table
                    '''
                }
                
                script {
                    echo '========================================='
                    echo 'AWS Credentials Test Complete!'
                    echo '========================================='
                }
            }
        }
    }
}
"@

# Save test pipeline
$testPipelinePath = "Jenkinsfile.aws-test"
$testPipeline | Out-File -FilePath $testPipelinePath -Encoding UTF8

Write-Host "✅ Test pipeline created: $testPipelinePath" -ForegroundColor Green
Write-Host ""
Write-Host "To test the credentials in Jenkins:" -ForegroundColor Yellow
Write-Host ""
Write-Host "1. Go to Jenkins: http://localhost:8080" -ForegroundColor White
Write-Host "2. Click 'New Item'" -ForegroundColor White
Write-Host "3. Enter name: 'AWS-Credentials-Test'" -ForegroundColor White
Write-Host "4. Select 'Pipeline' and click OK" -ForegroundColor White
Write-Host "5. Scroll to 'Pipeline' section" -ForegroundColor White
Write-Host "6. Select 'Pipeline script from SCM'" -ForegroundColor White
Write-Host "7. SCM: Git" -ForegroundColor White
Write-Host "8. Repository URL: https://github.com/MedHabibManai/IGL5-G5-achat" -ForegroundColor White
Write-Host "9. Script Path: Jenkinsfile.aws-test" -ForegroundColor White
Write-Host "10. Click 'Save' and then 'Build Now'" -ForegroundColor White
Write-Host ""

Write-Host "Or create a simple pipeline job with this script:" -ForegroundColor Gray
Write-Host ""
Write-Host $testPipeline -ForegroundColor DarkGray
Write-Host ""

Write-Host "Press Enter to commit the test pipeline..." -NoNewline -ForegroundColor Yellow
Read-Host

# Commit test pipeline
git add $testPipelinePath
git commit -m "Add AWS credentials test pipeline"
git push origin main

Write-Host ""
Write-Host "✅ Test pipeline committed and pushed!" -ForegroundColor Green
Write-Host ""

# ============================================================================
# STEP 6: Summary
# ============================================================================

Show-Header "Setup Complete!"

Write-Host "✅ AWS credentials added to Jenkins" -ForegroundColor Green
Write-Host "✅ Test pipeline created and committed" -ForegroundColor Green
Write-Host ""
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host ""
Write-Host "🎯 NEXT STEPS:" -ForegroundColor Yellow
Write-Host ""
Write-Host "1. Test the credentials in Jenkins" -ForegroundColor White
Write-Host "   → Create and run the AWS-Credentials-Test pipeline" -ForegroundColor Cyan
Write-Host ""
Write-Host "2. Create Terraform configuration" -ForegroundColor White
Write-Host "   → Run: .\scripts\create-terraform-config.ps1" -ForegroundColor Cyan
Write-Host ""
Write-Host "3. Update main Jenkinsfile with AWS deployment" -ForegroundColor White
Write-Host "   → Run: .\scripts\update-jenkinsfile-aws.ps1" -ForegroundColor Cyan
Write-Host ""
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host ""
Write-Host "📚 Documentation: AWS_DEPLOYMENT_GUIDE.md" -ForegroundColor Gray
Write-Host ""

