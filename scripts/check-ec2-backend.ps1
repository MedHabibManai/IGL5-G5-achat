# Check EC2 Backend Status
# This script checks if the EC2 backend is accessible

$EC2_IP = "44.209.192.135"
$PORT = "8089"
$BASE_URL = "http://${EC2_IP}:${PORT}/SpringMVC"

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "EC2 Backend Status Check" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

# Test 1: Health Check
Write-Host "1. Testing Health Check..." -ForegroundColor Yellow
$healthUrl = "${BASE_URL}/actuator/health"
try {
    $response = Invoke-WebRequest -Uri $healthUrl -TimeoutSec 10 -ErrorAction Stop
    Write-Host "   ✓ Health check: OK (HTTP $($response.StatusCode))" -ForegroundColor Green
    Write-Host "   Response: $($response.Content)" -ForegroundColor Gray
} catch {
    Write-Host "   ✗ Health check: FAILED" -ForegroundColor Red
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        Write-Host "   HTTP Status: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Yellow
    }
}

Write-Host ""

# Test 2: Swagger UI
Write-Host "2. Testing Swagger UI..." -ForegroundColor Yellow
$swaggerUrl = "${BASE_URL}/swagger-ui/index.html"
try {
    $response = Invoke-WebRequest -Uri $swaggerUrl -TimeoutSec 10 -ErrorAction Stop
    Write-Host "   ✓ Swagger UI: OK (HTTP $($response.StatusCode))" -ForegroundColor Green
} catch {
    Write-Host "   ✗ Swagger UI: FAILED" -ForegroundColor Red
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        Write-Host "   HTTP Status: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Yellow
    }
}

Write-Host ""

# Test 3: Main Application
Write-Host "3. Testing Main Application..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri $BASE_URL -TimeoutSec 10 -ErrorAction Stop
    Write-Host "   ✓ Main application: OK (HTTP $($response.StatusCode))" -ForegroundColor Green
} catch {
    Write-Host "   ✗ Main application: FAILED" -ForegroundColor Red
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Summary" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "EC2 IP: $EC2_IP" -ForegroundColor White
Write-Host "Port: $PORT" -ForegroundColor White
Write-Host "Base URL: $BASE_URL" -ForegroundColor White
Write-Host ""
Write-Host "If all tests failed, the EC2 instance might:" -ForegroundColor Yellow
Write-Host "  1. Still be initializing (user-data script running)" -ForegroundColor Yellow
Write-Host "  2. Have a crashed Docker container" -ForegroundColor Yellow
Write-Host "  3. Have security group blocking port $PORT" -ForegroundColor Yellow
Write-Host "  4. Need to be restarted" -ForegroundColor Yellow
Write-Host ""
Write-Host "To fix:" -ForegroundColor Cyan
Write-Host "  - Run a Jenkins build with REUSE_INFRASTRUCTURE mode" -ForegroundColor White
Write-Host "    This will recreate the EC2 instance and restart the container" -ForegroundColor White
Write-Host "  - Or check AWS Console for EC2 instance status" -ForegroundColor White

