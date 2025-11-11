# Quick script to update AWS credentials when they expire
# This script helps you update .env.aws and reconfigure everything

function Show-Header {
    param([string]$Title, [string]$Color = "Cyan")
    cls
    Write-Host ""
    Write-Host "╔════════════════════════════════════════════════════════╗" -ForegroundColor $Color
    Write-Host "║  UPDATE AWS CREDENTIALS                                ║" -ForegroundColor $Color
    Write-Host "╚════════════════════════════════════════════════════════╝" -ForegroundColor $Color
    Write-Host ""
    Write-Host "  $Title" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Gray
    Write-Host ""
}

Show-Header "AWS Credentials Update Wizard"

Write-Host "This script will help you update your AWS credentials when they expire." -ForegroundColor Cyan
Write-Host ""
Write-Host "Steps:" -ForegroundColor Yellow
Write-Host "  1. Get new credentials from AWS Academy" -ForegroundColor White
Write-Host "  2. Update .env.aws file" -ForegroundColor White
Write-Host "  3. Reconfigure AWS CLI" -ForegroundColor White
Write-Host "  4. Update Jenkins credentials" -ForegroundColor White
Write-Host ""
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host ""

# Check if .env.aws exists
if (-not (Test-Path ".env.aws")) {
    Write-Host "❌ .env.aws file not found!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please run the initial setup first:" -ForegroundColor Yellow
    Write-Host "  .\scripts\configure-aws-from-env.ps1" -ForegroundColor Cyan
    Write-Host ""
    exit 1
}

Write-Host "Current .env.aws file found." -ForegroundColor Green
Write-Host ""

# Ask user what they want to do
Write-Host "What would you like to do?" -ForegroundColor Yellow
Write-Host ""
Write-Host "  1. Open .env.aws in editor (manual update)" -ForegroundColor White
Write-Host "  2. Paste new credentials interactively" -ForegroundColor White
Write-Host "  3. Just reconfigure from existing .env.aws" -ForegroundColor White
Write-Host ""
Write-Host "Choice (1/2/3): " -NoNewline -ForegroundColor Yellow
$choice = Read-Host

if ($choice -eq "1") {
    # Open in default editor
    Write-Host ""
    Write-Host "Opening .env.aws in editor..." -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Instructions:" -ForegroundColor Yellow
    Write-Host "  1. Go to AWS Academy → Learner Lab" -ForegroundColor White
    Write-Host "  2. Click 'AWS Details' → 'Show AWS CLI credentials'" -ForegroundColor White
    Write-Host "  3. Copy the credentials" -ForegroundColor White
    Write-Host "  4. Update these lines in .env.aws:" -ForegroundColor White
    Write-Host "     - AWS_ACCESS_KEY_ID" -ForegroundColor Cyan
    Write-Host "     - AWS_SECRET_ACCESS_KEY" -ForegroundColor Cyan
    Write-Host "     - AWS_SESSION_TOKEN" -ForegroundColor Cyan
    Write-Host "  5. Save and close the file" -ForegroundColor White
    Write-Host ""
    Write-Host "Press Enter to open the file..." -NoNewline -ForegroundColor Yellow
    Read-Host
    
    Start-Process notepad.exe -ArgumentList ".env.aws" -Wait
    
    Write-Host ""
    Write-Host "Did you update the credentials? (y/n): " -NoNewline -ForegroundColor Yellow
    $updated = Read-Host
    
    if ($updated -ne "y" -and $updated -ne "Y") {
        Write-Host ""
        Write-Host "⚠️  Credentials not updated. Exiting." -ForegroundColor Yellow
        exit 0
    }
    
} elseif ($choice -eq "2") {
    # Interactive update
    Write-Host ""
    Write-Host "Please paste your new AWS credentials:" -ForegroundColor Cyan
    Write-Host ""
    
    Write-Host "AWS Access Key ID: " -NoNewline -ForegroundColor Yellow
    $accessKey = Read-Host
    
    Write-Host "AWS Secret Access Key: " -NoNewline -ForegroundColor Yellow
    $secretKey = Read-Host
    
    Write-Host "AWS Session Token (press Enter to skip): " -NoNewline -ForegroundColor Yellow
    $sessionToken = Read-Host
    
    Write-Host "AWS Region [us-east-1]: " -NoNewline -ForegroundColor Yellow
    $region = Read-Host
    if (-not $region) {
        $region = "us-east-1"
    }
    
    # Update .env.aws file
    Write-Host ""
    Write-Host "Updating .env.aws..." -ForegroundColor Cyan
    
    $content = Get-Content ".env.aws" -Raw
    
    # Replace credentials
    $content = $content -replace 'AWS_ACCESS_KEY_ID=.*', "AWS_ACCESS_KEY_ID=$accessKey"
    $content = $content -replace 'AWS_SECRET_ACCESS_KEY=.*', "AWS_SECRET_ACCESS_KEY=$secretKey"
    
    if ($sessionToken) {
        $content = $content -replace 'AWS_SESSION_TOKEN=.*', "AWS_SESSION_TOKEN=$sessionToken"
    }
    
    $content = $content -replace 'AWS_REGION=.*', "AWS_REGION=$region"
    $content = $content -replace 'AWS_DEFAULT_REGION=.*', "AWS_DEFAULT_REGION=$region"
    
    [System.IO.File]::WriteAllText(".env.aws", $content, [System.Text.Encoding]::UTF8)
    
    Write-Host "✅ .env.aws updated" -ForegroundColor Green
    
} elseif ($choice -eq "3") {
    # Just reconfigure
    Write-Host ""
    Write-Host "Using existing .env.aws file..." -ForegroundColor Cyan
    
} else {
    Write-Host ""
    Write-Host "❌ Invalid choice. Exiting." -ForegroundColor Red
    exit 1
}

# ============================================================================
# Reconfigure AWS CLI and Jenkins
# ============================================================================

Write-Host ""
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host ""
Write-Host "Reconfiguring AWS CLI from .env.aws..." -ForegroundColor Cyan
Write-Host ""

# Run the configuration script
& ".\scripts\configure-aws-from-env.ps1"

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "❌ AWS CLI configuration failed!" -ForegroundColor Red
    exit 1
}

# ============================================================================
# Ask about Jenkins update
# ============================================================================

Write-Host ""
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host ""
Write-Host "Do you want to update Jenkins credentials now? (y/n): " -NoNewline -ForegroundColor Yellow
$updateJenkins = Read-Host

if ($updateJenkins -eq "y" -or $updateJenkins -eq "Y") {
    Write-Host ""
    & ".\scripts\configure-aws-from-env.ps1" -UpdateJenkins
}

# ============================================================================
# Summary
# ============================================================================

Write-Host ""
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host ""
Write-Host "✅ Credentials Updated Successfully!" -ForegroundColor Green
Write-Host ""
Write-Host "What's been updated:" -ForegroundColor Yellow
Write-Host "  ✅ .env.aws file" -ForegroundColor Green
Write-Host "  ✅ AWS CLI configuration (~/.aws/credentials)" -ForegroundColor Green
Write-Host "  ✅ AWS CLI tested and working" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "  • If you updated Jenkins credentials, you're all set!" -ForegroundColor White
Write-Host "  • If not, run: .\scripts\configure-aws-from-env.ps1 -UpdateJenkins" -ForegroundColor White
Write-Host "  • Test your pipeline: Go to Jenkins and click 'Build Now'" -ForegroundColor White
Write-Host ""
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host ""

