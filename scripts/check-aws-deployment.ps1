# Quick script to check AWS deployment status
# Run this to diagnose connectivity issues

Write-Host "===============================================" -ForegroundColor Cyan
Write-Host "AWS Deployment Diagnostic Tool" -ForegroundColor Cyan
Write-Host "===============================================" -ForegroundColor Cyan
Write-Host ""

# Get Terraform outputs from Jenkins container
Write-Host "Fetching Terraform outputs..." -ForegroundColor Yellow
docker exec jenkins-with-docker bash -c "cd /var/jenkins_home/workspace/IGL5-G5-achat/terraform && terraform output -json" > terraform-outputs.json

if (Test-Path terraform-outputs.json) {
    $outputs = Get-Content terraform-outputs.json | ConvertFrom-Json
    
    Write-Host "✓ EC2 Instance Information:" -ForegroundColor Green
    Write-Host "  Instance ID: $($outputs.instance_id.value)" -ForegroundColor White
    Write-Host "  Instance State: $($outputs.instance_state.value)" -ForegroundColor White
    Write-Host "  Private IP: $($outputs.private_ip.value)" -ForegroundColor White
    Write-Host ""
    
    Write-Host "✓ Public IP Information:" -ForegroundColor Green
    Write-Host "  Elastic IP: $($outputs.public_ip.value)" -ForegroundColor White
    Write-Host "  Public DNS: $($outputs.public_dns.value)" -ForegroundColor White
    Write-Host ""
    
    Write-Host "✓ Application URLs:" -ForegroundColor Green
    Write-Host "  Application: $($outputs.application_url.value)" -ForegroundColor White
    Write-Host "  Health Check: $($outputs.health_check_url.value)" -ForegroundColor White
    Write-Host ""
    
    Write-Host "✓ Database Information:" -ForegroundColor Green
    Write-Host "  RDS Endpoint: $($outputs.rds_endpoint.value)" -ForegroundColor White
    Write-Host ""
    
    # Try to ping the application
    $ip = $outputs.public_ip.value
    $healthUrl = "$($outputs.health_check_url.value)"
    
    Write-Host "===============================================" -ForegroundColor Cyan
    Write-Host "Testing Connectivity..." -ForegroundColor Yellow
    Write-Host "===============================================" -ForegroundColor Cyan
    Write-Host ""
    
    Write-Host "Testing port 8089 on $ip..." -ForegroundColor Yellow
    $tcpTest = Test-NetConnection -ComputerName $ip -Port 8089 -WarningAction SilentlyContinue
    
    if ($tcpTest.TcpTestSucceeded) {
        Write-Host "✓ Port 8089 is OPEN and reachable" -ForegroundColor Green
        
        Write-Host ""
        Write-Host "Attempting HTTP health check..." -ForegroundColor Yellow
        try {
            $response = Invoke-WebRequest -Uri $healthUrl -TimeoutSec 5 -ErrorAction Stop
            Write-Host "✓ Health check SUCCESS!" -ForegroundColor Green
            Write-Host "  Status: $($response.StatusCode)" -ForegroundColor White
            Write-Host "  Response: $($response.Content)" -ForegroundColor White
        } catch {
            Write-Host "✗ Health check FAILED" -ForegroundColor Red
            Write-Host "  Error: $($_.Exception.Message)" -ForegroundColor Red
            Write-Host ""
            Write-Host "Possible reasons:" -ForegroundColor Yellow
            Write-Host "  1. Application is still starting (wait 5-10 minutes)" -ForegroundColor White
            Write-Host "  2. Container failed to start (check EC2 logs)" -ForegroundColor White
            Write-Host "  3. Database connection issue" -ForegroundColor White
        }
    } else {
        Write-Host "✗ Port 8089 is CLOSED or unreachable" -ForegroundColor Red
        Write-Host ""
        Write-Host "Possible reasons:" -ForegroundColor Yellow
        Write-Host "  1. Application not started yet (wait 5-10 minutes)" -ForegroundColor White
        Write-Host "  2. Security group not allowing traffic" -ForegroundColor White
        Write-Host "  3. EC2 instance not running" -ForegroundColor White
        Write-Host "  4. Elastic IP not properly associated" -ForegroundColor White
    }
    
    Write-Host ""
    Write-Host "===============================================" -ForegroundColor Cyan
    Write-Host "AWS Console Quick Links:" -ForegroundColor Yellow
    Write-Host "===============================================" -ForegroundColor Cyan
    Write-Host "EC2 Instance: https://console.aws.amazon.com/ec2/v2/home?region=us-east-1#Instances:instanceId=$($outputs.instance_id.value)" -ForegroundColor Cyan
    Write-Host "RDS Database: https://console.aws.amazon.com/rds/home?region=us-east-1" -ForegroundColor Cyan
    Write-Host ""
    
} else {
    Write-Host "✗ Could not fetch Terraform outputs" -ForegroundColor Red
    Write-Host "  Make sure Terraform has been applied successfully" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "===============================================" -ForegroundColor Cyan
