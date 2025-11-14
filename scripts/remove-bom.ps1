# Script to remove BOM from all Groovy files
# Run this script whenever you need to ensure all Jenkins files are BOM-free

$files = @(
    "Jenkinsfile"
    "jenkins/stages/checkout.groovy"
    "jenkins/stages/build.groovy"
    "jenkins/stages/unitTests.groovy"
    "jenkins/stages/package.groovy"
    "jenkins/stages/sonar.groovy"
    "jenkins/stages/qualityGate.groovy"
    "jenkins/stages/deployToNexus.groovy"
    "jenkins/stages/buildDockerImage.groovy"
    "jenkins/stages/pushDockerImage.groovy"
    "jenkins/stages/cleanupAWS.groovy"
    "jenkins/stages/refreshEC2.groovy"
    "jenkins/stages/terraformInit.groovy"
    "jenkins/stages/preTerraformValidation.groovy"
    "jenkins/stages/terraformPlan.groovy"
    "jenkins/stages/terraformApply.groovy"
    "jenkins/stages/getAWSInfo.groovy"
    "jenkins/stages/debugEC2.groovy"
    "jenkins/stages/healthCheck.groovy"
    "jenkins/stages/buildFrontend.groovy"
    "jenkins/stages/buildFrontendDocker.groovy"
    "jenkins/stages/pushFrontendDocker.groovy"
    "jenkins/stages/deployToEKS.groovy"
    "jenkins/stages/deployToLocalK8s.groovy"
)

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$processedCount = 0
$errorCount = 0

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Removing BOM from Jenkins Pipeline Files" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

foreach ($file in $files) {
    $fullPath = Join-Path $PSScriptRoot "..\$file"
    
    if (Test-Path $fullPath) {
        try {
            # Read the file content
            $content = Get-Content -Path $fullPath -Raw
            
            # Check for BOM
            $bytes = [System.IO.File]::ReadAllBytes($fullPath)
            $hasBom = $bytes.Length -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF
            
            if ($hasBom) {
                Write-Host "[BOM FOUND] $file - Removing..." -ForegroundColor Yellow
                [System.IO.File]::WriteAllText($fullPath, $content, $utf8NoBom)
                $processedCount++
                Write-Host "[FIXED]     $file" -ForegroundColor Green
            } else {
                Write-Host "[OK]        $file - No BOM detected" -ForegroundColor Gray
            }
        }
        catch {
            Write-Host "[ERROR]     $file - $($_.Exception.Message)" -ForegroundColor Red
            $errorCount++
        }
    } else {
        Write-Host "[MISSING]   $file - File not found" -ForegroundColor Red
        $errorCount++
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Summary:" -ForegroundColor Cyan
Write-Host "  Files processed: $processedCount" -ForegroundColor Green
Write-Host "  Errors: $errorCount" -ForegroundColor $(if ($errorCount -eq 0) { "Green" } else { "Red" })
Write-Host "========================================" -ForegroundColor Cyan
