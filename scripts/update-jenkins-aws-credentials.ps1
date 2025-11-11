# Update AWS Credentials in Jenkins Container
# This script updates AWS credentials in the Jenkins container from .env.aws

Write-Host ""
Write-Host "Updating AWS Credentials in Jenkins Container..." -ForegroundColor Cyan
Write-Host ""

# Check if .env.aws exists
if (-not (Test-Path ".env.aws")) {
    Write-Host "ERROR: .env.aws file not found!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please create .env.aws with your AWS credentials first." -ForegroundColor Yellow
    exit 1
}

# Load credentials from .env.aws
Write-Host "Loading credentials from .env.aws..." -ForegroundColor Gray
$envVars = @{}
Get-Content ".env.aws" | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith('#')) {
        if ($line -match '^([^=]+)=(.*)$') {
            $envVars[$matches[1].Trim()] = $matches[2].Trim()
        }
    }
}

# Validate required variables
$required = @('AWS_ACCESS_KEY_ID', 'AWS_SECRET_ACCESS_KEY', 'AWS_REGION')
$missing = @()

foreach ($var in $required) {
    if (-not $envVars.ContainsKey($var) -or -not $envVars[$var]) {
        $missing += $var
    }
}

if ($missing.Count -gt 0) {
    Write-Host "ERROR: Missing required variables in .env.aws:" -ForegroundColor Red
    foreach ($var in $missing) {
        Write-Host "  - $var" -ForegroundColor Yellow
    }
    exit 1
}

Write-Host "Credentials loaded!" -ForegroundColor Green
Write-Host ""

# Check if Jenkins container is running
Write-Host "Checking Jenkins container..." -ForegroundColor Gray
$jenkinsRunning = docker ps --filter "name=jenkins-cicd" --format "{{.Names}}" 2>$null

if (-not $jenkinsRunning) {
    Write-Host "ERROR: Jenkins container is not running!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Start Jenkins with: docker-compose up -d jenkins-cicd" -ForegroundColor Yellow
    exit 1
}

Write-Host "Jenkins container is running!" -ForegroundColor Green
Write-Host ""

# Update credentials in Jenkins container
Write-Host "Updating credentials in Jenkins container..." -ForegroundColor Gray

$accessKey = $envVars['AWS_ACCESS_KEY_ID']
$secretKey = $envVars['AWS_SECRET_ACCESS_KEY']
$sessionToken = $envVars['AWS_SESSION_TOKEN']
$region = $envVars['AWS_REGION']

docker exec -u root jenkins-cicd bash -c "
mkdir -p /var/jenkins_home/.aws

cat > /var/jenkins_home/.aws/credentials <<'EOF'
[default]
aws_access_key_id=$accessKey
aws_secret_access_key=$secretKey
aws_session_token=$sessionToken
EOF

cat > /var/jenkins_home/.aws/config <<'EOF'
[default]
region=$region
output=json
EOF

chown -R jenkins:jenkins /var/jenkins_home/.aws
chmod 600 /var/jenkins_home/.aws/credentials
chmod 600 /var/jenkins_home/.aws/config
"

if ($LASTEXITCODE -eq 0) {
    Write-Host "Credentials updated successfully!" -ForegroundColor Green
    Write-Host ""
    
    # Test credentials
    Write-Host "Testing credentials..." -ForegroundColor Gray
    docker exec -u jenkins jenkins-cicd bash -c '/usr/local/bin/aws sts get-caller-identity' 2>$null
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "SUCCESS! AWS credentials are working in Jenkins!" -ForegroundColor Green
        Write-Host ""
    } else {
        Write-Host ""
        Write-Host "WARNING: Credentials updated but test failed!" -ForegroundColor Yellow
        Write-Host "This might be normal if credentials are expired." -ForegroundColor Gray
        Write-Host ""
    }
} else {
    Write-Host "ERROR: Failed to update credentials!" -ForegroundColor Red
    Write-Host ""
    exit 1
}

Write-Host "Done!" -ForegroundColor Green
Write-Host ""

