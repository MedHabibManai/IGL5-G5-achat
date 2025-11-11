# Configure AWS CLI from .env.aws file
# This script reads credentials from .env.aws and configures AWS CLI

param(
    [switch]$UpdateJenkins
)

function Show-Header {
    param([string]$Title, [string]$Color = "Cyan")
    cls
    Write-Host ""
    Write-Host "╔════════════════════════════════════════════════════════╗" -ForegroundColor $Color
    Write-Host "║  AWS CONFIGURATION FROM .env.aws                       ║" -ForegroundColor $Color
    Write-Host "╚════════════════════════════════════════════════════════╝" -ForegroundColor $Color
    Write-Host ""
    Write-Host "  $Title" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Gray
    Write-Host ""
}

function Load-EnvFile {
    param([string]$FilePath)
    
    if (-not (Test-Path $FilePath)) {
        Write-Host "ERROR: .env.aws file not found!" -ForegroundColor Red
        Write-Host ""
        Write-Host "Expected location: $FilePath" -ForegroundColor Yellow
        exit 1
    }
    
    $envVars = @{}
    Get-Content $FilePath | ForEach-Object {
        $line = $_.Trim()
        # Skip comments and empty lines
        if ($line -and -not $line.StartsWith('#')) {
            if ($line -match '^([^=]+)=(.*)$') {
                $key = $matches[1].Trim()
                $value = $matches[2].Trim()
                $envVars[$key] = $value
            }
        }
    }
    
    return $envVars
}

# Main Script
Show-Header "Loading AWS Credentials from .env.aws"

$envFile = ".env.aws"
if (-not (Test-Path $envFile)) {
    $envFile = "..\\.env.aws"
}

Write-Host "Loading credentials from: $envFile" -ForegroundColor Cyan
Write-Host ""

$env_vars = Load-EnvFile $envFile

# Validate required variables
$required = @('AWS_ACCESS_KEY_ID', 'AWS_SECRET_ACCESS_KEY', 'AWS_REGION')
$missing = @()

foreach ($var in $required) {
    if (-not $env_vars.ContainsKey($var) -or -not $env_vars[$var]) {
        $missing += $var
    }
}

if ($missing.Count -gt 0) {
    Write-Host "ERROR: Missing required variables in .env.aws:" -ForegroundColor Red
    foreach ($var in $missing) {
        Write-Host "  - $var" -ForegroundColor Yellow
    }
    Write-Host ""
    Write-Host "Please edit .env.aws and add the missing values." -ForegroundColor Yellow
    exit 1
}

Write-Host "All required variables found" -ForegroundColor Green
Write-Host ""
Write-Host "  AWS_ACCESS_KEY_ID: $($env_vars['AWS_ACCESS_KEY_ID'].Substring(0, 10))..." -ForegroundColor Gray
Write-Host "  AWS_SECRET_ACCESS_KEY: ****" -ForegroundColor Gray
if ($env_vars.ContainsKey('AWS_SESSION_TOKEN') -and $env_vars['AWS_SESSION_TOKEN']) {
    Write-Host "  AWS_SESSION_TOKEN: ****" -ForegroundColor Gray
}
Write-Host "  AWS_REGION: $($env_vars['AWS_REGION'])" -ForegroundColor Gray
Write-Host ""

# Configure AWS CLI
Write-Host "Configuring AWS CLI..." -ForegroundColor Cyan
Write-Host ""

# Create .aws directory if it doesn't exist
$awsDir = "$env:USERPROFILE\.aws"
if (-not (Test-Path $awsDir)) {
    New-Item -ItemType Directory -Path $awsDir -Force | Out-Null
    Write-Host "Created directory: $awsDir" -ForegroundColor Green
}

# Create credentials file
$credentialsFile = "$awsDir\credentials"
$credentialsContent = "[default]`n"
$credentialsContent += "aws_access_key_id=$($env_vars['AWS_ACCESS_KEY_ID'])`n"
$credentialsContent += "aws_secret_access_key=$($env_vars['AWS_SECRET_ACCESS_KEY'])`n"

if ($env_vars.ContainsKey('AWS_SESSION_TOKEN') -and $env_vars['AWS_SESSION_TOKEN']) {
    $credentialsContent += "aws_session_token=$($env_vars['AWS_SESSION_TOKEN'])`n"
}

[System.IO.File]::WriteAllText($credentialsFile, $credentialsContent, [System.Text.Encoding]::UTF8)
Write-Host "Created: $credentialsFile" -ForegroundColor Green

# Create config file
$configFile = "$awsDir\config"
$configContent = "[default]`n"
$configContent += "region=$($env_vars['AWS_REGION'])`n"
$configContent += "output=$($env_vars['AWS_OUTPUT'])`n"

[System.IO.File]::WriteAllText($configFile, $configContent, [System.Text.Encoding]::UTF8)
Write-Host "Created: $configFile" -ForegroundColor Green

Write-Host ""
Write-Host "AWS CLI configured successfully!" -ForegroundColor Green
Write-Host ""