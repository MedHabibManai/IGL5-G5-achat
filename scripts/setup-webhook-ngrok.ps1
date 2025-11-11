# Script interactif pour configurer le webhook GitHub -> Jenkins avec ngrok
# Execute ce script et suivez les instructions

$ErrorActionPreference = "Stop"

function Write-Step {
    param([string]$Message)
    Write-Host "`n╔════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
    Write-Host "║ $Message" -ForegroundColor Cyan
    Write-Host "╚════════════════════════════════════════════════════════╝`n" -ForegroundColor Cyan
}

function Write-Success {
    param([string]$Message)
    Write-Host "✅ $Message" -ForegroundColor Green
}

function Write-Info {
    param([string]$Message)
    Write-Host "ℹ️  $Message" -ForegroundColor White
}

function Write-Warning {
    param([string]$Message)
    Write-Host "⚠️  $Message" -ForegroundColor Yellow
}

function Write-Error-Custom {
    param([string]$Message)
    Write-Host "❌ $Message" -ForegroundColor Red
}

function Wait-UserConfirmation {
    param([string]$Message = "Appuyez sur Entree pour continuer...")
    Write-Host "`n$Message" -ForegroundColor Yellow
    Read-Host
}

# ============================================================================
# ETAPE 1 : Verifier Jenkins
# ============================================================================

Write-Step "ETAPE 1/7 : Verification de Jenkins"

Write-Info "Verification que Jenkins est accessible..."
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080" -TimeoutSec 5 -UseBasicParsing -ErrorAction Stop
    Write-Success "Jenkins est accessible sur http://localhost:8080"
} catch {
    Write-Error-Custom "Jenkins n'est pas accessible !"
    Write-Info "Verifiez que Jenkins tourne : docker ps | Select-String jenkins"
    exit 1
}

Wait-UserConfirmation

# ============================================================================
# ETAPE 2 : Verifier ngrok
# ============================================================================

Write-Step "ETAPE 2/7 : Verification de ngrok"

try {
    $ngrokPath = Get-Command ngrok -ErrorAction Stop
    Write-Success "ngrok est installe : $($ngrokPath.Source)"
    
    $version = ngrok version 2>&1 | Select-Object -First 1
    Write-Info "Version : $version"
} catch {
    Write-Error-Custom "ngrok n'est pas installe !"
    Write-Host ""
    Write-Info "Installation :"
    Write-Host "  1. Telechargez : https://ngrok.com/download" -ForegroundColor White
    Write-Host "  2. Extrayez ngrok.exe dans C:\Windows\System32\" -ForegroundColor White
    Write-Host "  3. Ou installez avec : choco install ngrok" -ForegroundColor White
    Write-Host ""
    Write-Info "Apres installation, relancez ce script."
    exit 1
}

Wait-UserConfirmation

# ============================================================================
# ETAPE 3 : Demarrer ngrok
# ============================================================================

Write-Step "ETAPE 3/7 : Demarrage de ngrok"

Write-Info "Nous allons demarrer ngrok dans un nouveau terminal."
Write-Warning "IMPORTANT : Gardez ce terminal ngrok ouvert pendant tous les tests !"
Write-Host ""
Write-Info "Commande a executer dans le nouveau terminal :"
Write-Host "  ngrok http 8080" -ForegroundColor Yellow
Write-Host ""

Wait-UserConfirmation "Appuyez sur Entree pour ouvrir un nouveau terminal..."

# Ouvrir un nouveau terminal avec ngrok
Start-Process powershell -ArgumentList "-NoExit", "-Command", "Write-Host 'Demarrage de ngrok...' -ForegroundColor Cyan; ngrok http 8080"

Write-Host ""
Write-Success "Terminal ngrok ouvert !"
Write-Host ""
Write-Info "Dans le terminal ngrok, cherchez la ligne :"
Write-Host "  Forwarding  https://XXXXXXXX.ngrok-free.app -> http://localhost:8080" -ForegroundColor Yellow
Write-Host ""
Write-Warning "Copiez l'URL https://XXXXXXXX.ngrok-free.app"
Write-Host ""

$ngrokUrl = Read-Host "Collez l'URL ngrok ici"

if (-not $ngrokUrl) {
    Write-Error-Custom "URL ngrok requise !"
    exit 1
}

# Nettoyer l'URL
$ngrokUrl = $ngrokUrl.Trim()
if ($ngrokUrl -notmatch "^https://") {
    Write-Error-Custom "L'URL doit commencer par https://"
    exit 1
}

Write-Success "URL ngrok enregistree : $ngrokUrl"

# Tester l'acces
Write-Host ""
Write-Info "Test de l'acces a Jenkins via ngrok..."
Start-Sleep -Seconds 3

try {
    $response = Invoke-WebRequest -Uri $ngrokUrl -TimeoutSec 10 -UseBasicParsing -ErrorAction Stop
    Write-Success "Jenkins est accessible via ngrok !"
} catch {
    Write-Error-Custom "Impossible d'acceder a Jenkins via ngrok"
    Write-Info "Verifiez que ngrok est bien demarre"
    exit 1
}

Wait-UserConfirmation

# ============================================================================
# ETAPE 4 : Modifier le Jenkinsfile
# ============================================================================

Write-Step "ETAPE 4/7 : Configuration du Jenkinsfile"

$jenkinsfilePath = "Jenkinsfile"
if (-not (Test-Path $jenkinsfilePath)) {
    Write-Error-Custom "Jenkinsfile introuvable !"
    exit 1
}

$content = Get-Content $jenkinsfilePath -Raw

if ($content -match "triggers\s*\{") {
    Write-Success "Section 'triggers' deja presente dans le Jenkinsfile"
    
    if ($content -match "githubPush\(\)") {
        Write-Success "Trigger 'githubPush()' deja configure"
    } else {
        Write-Warning "Trigger 'githubPush()' manquant"
        Write-Info "Ajoutez 'githubPush()' dans la section triggers"
    }
} else {
    Write-Warning "Section 'triggers' manquante dans le Jenkinsfile"
    Write-Host ""
    $response = Read-Host "Voulez-vous que je l'ajoute automatiquement ? (o/n)"
    
    if ($response -eq "o" -or $response -eq "O") {
        # Creer une sauvegarde
        Copy-Item $jenkinsfilePath "${jenkinsfilePath}.backup"
        Write-Success "Sauvegarde creee : ${jenkinsfilePath}.backup"
        
        # Ajouter le trigger apres "agent any"
        $newContent = $content -replace "(agent\s+any)", "`$1`n    `n    triggers {`n        githubPush()`n    }"
        Set-Content -Path $jenkinsfilePath -Value $newContent
        
        Write-Success "Jenkinsfile modifie !"
        Write-Host ""
        Write-Info "Commitez les changements :"
        Write-Host "  git add Jenkinsfile" -ForegroundColor Yellow
        Write-Host "  git commit -m 'Add GitHub webhook trigger'" -ForegroundColor Yellow
        Write-Host "  git push origin main" -ForegroundColor Yellow
        Write-Host ""
        
        $commitNow = Read-Host "Voulez-vous commiter maintenant ? (o/n)"
        if ($commitNow -eq "o" -or $commitNow -eq "O") {
            git add Jenkinsfile
            git commit -m "Add GitHub webhook trigger"
            git push origin main
            Write-Success "Changements commites et pushes !"
        }
    }
}

Wait-UserConfirmation

# ============================================================================
# ETAPE 5 : Configuration Jenkins Web UI
# ============================================================================

Write-Step "ETAPE 5/7 : Configuration de Jenkins (Interface Web)"

Write-Info "Ouvrez Jenkins dans votre navigateur..."
Start-Process "http://localhost:8080"

Write-Host ""
Write-Info "Instructions :"
Write-Host "  1. Cliquez sur votre pipeline job" -ForegroundColor White
Write-Host "  2. Cliquez sur 'Configure'" -ForegroundColor White
Write-Host "  3. Section 'Build Triggers' :" -ForegroundColor White
Write-Host "     ☑ GitHub hook trigger for GITScm polling" -ForegroundColor Yellow
Write-Host "  4. Cliquez sur 'Save'" -ForegroundColor White
Write-Host ""

Wait-UserConfirmation "Appuyez sur Entree une fois la configuration terminee..."

Write-Success "Configuration Jenkins terminee !"

# ============================================================================
# ETAPE 6 : Configuration GitHub Webhook
# ============================================================================

Write-Step "ETAPE 6/7 : Configuration du Webhook GitHub"

$webhookUrl = "$ngrokUrl/github-webhook/"

Write-Info "URL du webhook a configurer :"
Write-Host "  $webhookUrl" -ForegroundColor Cyan
Write-Host ""

# Copier dans le presse-papier
try {
    Set-Clipboard -Value $webhookUrl
    Write-Success "URL copiee dans le presse-papier !"
} catch {
    Write-Warning "Impossible de copier dans le presse-papier"
}

Write-Host ""
Write-Info "Ouvrez GitHub dans votre navigateur..."
Start-Process "https://github.com/MedHabibManai/IGL5-G5-achat/settings/hooks/new"

Write-Host ""
Write-Info "Configuration du webhook :"
Write-Host "  Payload URL : $webhookUrl" -ForegroundColor Yellow
Write-Host "  Content type : application/json" -ForegroundColor White
Write-Host "  Secret : (laissez vide)" -ForegroundColor White
Write-Host "  Events : Just the push event" -ForegroundColor White
Write-Host "  Active : ✓ Coche" -ForegroundColor White
Write-Host ""
Write-Info "Cliquez sur 'Add webhook'"
Write-Host ""

Wait-UserConfirmation "Appuyez sur Entree une fois le webhook cree..."

Write-Success "Webhook GitHub configure !"

# ============================================================================
# ETAPE 7 : Test du Webhook
# ============================================================================

Write-Step "ETAPE 7/7 : Test du Webhook"

Write-Info "Nous allons tester le webhook avec un commit vide"
Write-Host ""

$response = Read-Host "Voulez-vous lancer le test maintenant ? (o/n)"

if ($response -eq "o" -or $response -eq "O") {
    Write-Info "Creation d'un commit vide..."
    git commit --allow-empty -m "Test webhook trigger - $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
    
    Write-Info "Push vers GitHub..."
    git push origin main
    
    Write-Host ""
    Write-Success "Commit pousse vers GitHub !"
    Write-Host ""
    Write-Info "Verifications :"
    Write-Host "  1. Terminal ngrok : Vous devriez voir 'POST /github-webhook/ 200 OK'" -ForegroundColor White
    Write-Host "  2. Jenkins : Un nouveau build devrait demarrer automatiquement" -ForegroundColor White
    Write-Host "  3. Interface ngrok : http://127.0.0.1:4040" -ForegroundColor White
    Write-Host ""
    
    Write-Info "Ouverture de Jenkins..."
    Start-Process "http://localhost:8080"
    
    Write-Info "Ouverture de l'interface ngrok..."
    Start-Process "http://127.0.0.1:4040"
    
    Write-Host ""
    Write-Success "Verifiez que le build a demarre dans Jenkins !"
}

# ============================================================================
# RESUME
# ============================================================================

Write-Host ""
Write-Host "╔════════════════════════════════════════════════════════╗" -ForegroundColor Green
Write-Host "║ CONFIGURATION TERMINEE !                              ║" -ForegroundColor Green
Write-Host "╚════════════════════════════════════════════════════════╝" -ForegroundColor Green
Write-Host ""

Write-Info "Resume de la configuration :"
Write-Host ""
Write-Host "  ✅ Jenkins : http://localhost:8080" -ForegroundColor Green
Write-Host "  ✅ ngrok : $ngrokUrl" -ForegroundColor Green
Write-Host "  ✅ Webhook : $webhookUrl" -ForegroundColor Green
Write-Host "  ✅ Interface ngrok : http://127.0.0.1:4040" -ForegroundColor Green
Write-Host ""

Write-Info "Pour tester a nouveau :"
Write-Host "  git commit --allow-empty -m 'Test webhook'" -ForegroundColor Yellow
Write-Host "  git push origin main" -ForegroundColor Yellow
Write-Host ""

Write-Warning "IMPORTANT : Gardez le terminal ngrok ouvert !"
Write-Host ""

Write-Info "Documentation complete : GUIDE_WEBHOOK_NGROK.md"
Write-Host ""

