# Script to convert declarative pipeline stage files to scripted pipeline syntax
# Removes 'steps { }' and 'script { }' wrappers

$stagesPath = "jenkins/stages"
$stageFiles = Get-ChildItem -Path $stagesPath -Filter "*.groovy"

foreach ($file in $stageFiles) {
    Write-Host "Processing: $($file.Name)" -ForegroundColor Cyan
    
    $content = Get-Content -Path $file.FullName -Raw
    $originalContent = $content
    
    # Remove declarative 'steps {' blocks and their content, keeping just the inner script
    # Pattern: stage('name') { steps { script { CODE } } } => stage('name') { CODE }
    
    # First, remove the steps { wrapper if it exists
    $content = $content -replace '(?s)(stage\([^)]+\)\s*\{\s*)steps\s*\{\s*', '$1'
    
    # Remove script { wrappers inside stages
    $content = $content -replace '(?s)(stage\([^)]+\)\s*\{\s*)script\s*\{\s*', '$1'
    
    # Clean up extra closing braces that were left behind
    # This is tricky, so let's do it line by line
    $lines = $content -split "`r?`n"
    $newLines = @()
    $braceBalance = 0
    $inStage = $false
    $skipNextCloseBrace = $false
    
    for ($i = 0; $i -lt $lines.Length; $i++) {
        $line = $lines[$i]
        
        # Track if we're inside a stage definition
        if ($line -match '^\s*stage\(') {
            $inStage = $true
            $newLines += $line
            continue
        }
        
        # Skip empty 'script {' lines
        if ($line -match '^\s*script\s*\{\s*$') {
            $skipNextCloseBrace = $true
            continue
        }
        
        # Skip orphaned closing braces after we removed script/steps blocks
        if ($line -match '^\s*\}\s*$' -and $skipNextCloseBrace) {
            $skipNextCloseBrace = $false
            continue
        }
        
        # Check for the end of a stage (closing brace at start of line after stage content)
        if ($line -match '^\s*\}' -and $inStage) {
            # Count how many braces we have
            $openCount = ($line | Select-String '\{' -AllMatches).Matches.Count
            $closeCount = ($line | Select-String '\}' -AllMatches).Matches.Count
            
            # If we have extra closing braces, we might need to skip one
            if ($i -gt 0 -and $lines[$i-1] -match '^\s*\}\s*$') {
                # Skip this brace as it's likely redundant
                continue
            }
        }
        
        $newLines += $line
        
        if ($line -match '^}$' -and $inStage) {
            $inStage = $false
        }
    }
    
    $newContent = $newLines -join "`r`n"
    
    # Save with UTF-8 no BOM
    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($file.FullName, $newContent, $utf8NoBom)
    
    if ($newContent -ne $originalContent) {
        Write-Host "  ✓ Fixed syntax" -ForegroundColor Green
    } else {
        Write-Host "  - No changes needed" -ForegroundColor Gray
    }
}

Write-Host "`nAll stage files processed!" -ForegroundColor Green
