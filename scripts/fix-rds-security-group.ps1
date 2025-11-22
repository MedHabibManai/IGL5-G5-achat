# Fix RDS Security Group to Allow EKS Nodes
# This script manually updates the RDS security group to allow MySQL traffic from EKS nodes

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Fixing RDS Security Group for EKS" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

# Set AWS region
$env:AWS_DEFAULT_REGION = "us-east-1"

# Get VPC ID from Terraform output
Write-Host "Getting VPC ID from Terraform..." -ForegroundColor Yellow
$vpcId = terraform -chdir=terraform output -raw vpc_id 2>$null

if (-not $vpcId) {
    Write-Host "ERROR: Could not get VPC ID from Terraform" -ForegroundColor Red
    Write-Host "Make sure you're in the project root and Terraform has been applied" -ForegroundColor Yellow
    exit 1
}

Write-Host "VPC ID: $vpcId" -ForegroundColor Green
Write-Host ""

# Get RDS Security Group ID
Write-Host "Getting RDS Security Group ID..." -ForegroundColor Yellow
$rdsSgId = aws ec2 describe-security-groups `
    --region us-east-1 `
    --filters "Name=vpc-id,Values=$vpcId" "Name=group-name,Values=achat-app-rds-sg" `
    --query "SecurityGroups[0].GroupId" `
    --output text 2>$null

if (-not $rdsSgId -or $rdsSgId -eq "None") {
    Write-Host "ERROR: Could not find RDS security group" -ForegroundColor Red
    exit 1
}

Write-Host "RDS Security Group ID: $rdsSgId" -ForegroundColor Green
Write-Host ""

# Get EKS Nodes Security Group ID
Write-Host "Getting EKS Nodes Security Group ID..." -ForegroundColor Yellow
$eksNodesSgId = aws ec2 describe-security-groups `
    --region us-east-1 `
    --filters "Name=vpc-id,Values=$vpcId" "Name=group-name,Values=achat-app-eks-nodes-sg" `
    --query "SecurityGroups[0].GroupId" `
    --output text 2>$null

if (-not $eksNodesSgId -or $eksNodesSgId -eq "None") {
    Write-Host "ERROR: Could not find EKS nodes security group" -ForegroundColor Red
    exit 1
}

Write-Host "EKS Nodes Security Group ID: $eksNodesSgId" -ForegroundColor Green
Write-Host ""

# Create JSON for security group rule
$ipPermissionsJson = @"
[{
    "IpProtocol": "tcp",
    "FromPort": 3306,
    "ToPort": 3306,
    "UserIdGroupPairs": [{
        "GroupId": "$eksNodesSgId"
    }]
}]
"@

# Save JSON to temp file
$tempFile = [System.IO.Path]::GetTempFileName()
$ipPermissionsJson | Out-File -FilePath $tempFile -Encoding utf8

Write-Host "Adding security group rule..." -ForegroundColor Yellow
Write-Host "Allowing MySQL (port 3306) from EKS nodes ($eksNodesSgId) to RDS ($rdsSgId)" -ForegroundColor Cyan
Write-Host ""

# Add the security group rule
$output = aws ec2 authorize-security-group-ingress `
    --region us-east-1 `
    --group-id $rdsSgId `
    --ip-permissions file://$tempFile 2>&1

$exitCode = $LASTEXITCODE

# Clean up temp file
Remove-Item $tempFile -Force

if ($output -match "(already exists|Duplicate)") {
    Write-Host "✓ Security group rule already exists (this is OK)" -ForegroundColor Green
} elseif ($exitCode -eq 0) {
    Write-Host "✓ Security group rule added successfully!" -ForegroundColor Green
} else {
    Write-Host "Result: $output" -ForegroundColor Yellow
    Write-Host "Note: If rule already exists, this is expected and safe to ignore" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Next Steps:" -ForegroundColor Cyan
Write-Host "1. Wait 30 seconds for the security group rule to propagate" -ForegroundColor Yellow
Write-Host "2. Restart the backend pods to reconnect to RDS:" -ForegroundColor Yellow
Write-Host "   kubectl rollout restart deployment/achat-app -n achat-app" -ForegroundColor White
Write-Host "3. Check pod status:" -ForegroundColor Yellow
Write-Host "   kubectl get pods -n achat-app -l app=achat-app" -ForegroundColor White
Write-Host "=========================================" -ForegroundColor Cyan




