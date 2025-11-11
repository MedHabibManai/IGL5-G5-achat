# Script de Configuration des Webhooks GitHub → Jenkins
# Ce script aide à configurer et tester les webhooks

param(
    [Parameter(Mandatory=$false)]
    [string]$Mode = "local"  # "local" ou "public"
)

Write-Host "`n╔════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║   Configuration Webhook GitHub → Jenkins              ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════════════╝`n" -ForegroundColor Cyan

# Fonction pour afficher les informations
function Show-Info {
    param([string]$Message, [string]$Color = "White")
    Write-Host "ℹ️  $Message" -ForegroundColor $Color
}

# Fonction pour afficher les succès
function Show-Success {
    param([string]$Message)
    Write-Host "✅ $Message" -ForegroundColor Green
}

# Fonction pour afficher les erreurs
function Show-Error {
    param([string]$Message)
    Write-Host "❌ $Message" -ForegroundColor Red
}

# Fonction pour afficher les avertissements
function Show-Warning {
    param([string]$Message)
    Write-Host "⚠️  $Message" -ForegroundColor Yellow
}

# ============================================================================
# Étape 1 : Vérifier que Jenkins est accessible
# ============================================================================

Write-Host "📋 Étape 1 : Vérification de Jenkins`n" -ForegroundColor Yellow

try {
    $jenkinsResponse = Invoke-WebRequest -Uri "http://localhost:8080" -TimeoutSec 5 -UseBasicParsing -ErrorAction Stop
    Show-Success "Jenkins est accessible sur http://localhost:8080"
} catch {
    Show-Error "Jenkins n'est pas accessible sur http://localhost:8080"
    Show-Info "Vérifiez que Jenkins est démarré : docker ps | Select-String jenkins"
    exit 1
}

# ============================================================================
# Étape 2 : Déterminer l'URL du webhook
# ============================================================================

Write-Host "`n📋 Étape 2 : Détermination de l'URL du webhook`n" -ForegroundColor Yellow

$webhookUrl = ""

if ($Mode -eq "local") {
    Show-Info "Mode LOCAL détecté - Jenkins tourne en local"
    Show-Warning "GitHub ne peut pas atteindre localhost directement !"
    Write-Host ""
    Show-Info "Solutions possibles :" "Cyan"
    Write-Host "  1. Utiliser ngrok pour exposer Jenkins sur Internet" -ForegroundColor White
    Write-Host "  2. Déployer Jenkins sur un serveur avec IP publique" -ForegroundColor White
    Write-Host "  3. Utiliser GitHub Actions comme alternative" -ForegroundColor White
    Write-Host ""
    
    # Vérifier si ngrok est installé
    try {
        $ngrokVersion = ngrok version 2>&1
        Show-Success "ngrok est installé : $ngrokVersion"
        Write-Host ""
        Show-Info "Pour exposer Jenkins avec ngrok, exécutez :" "Cyan"
        Write-Host "  ngrok http 8080" -ForegroundColor Yellow
        Write-Host ""
        Show-Info "Puis utilisez l'URL fournie par ngrok (ex: https://abc123.ngrok.io)" "Cyan"
        Write-Host ""
        
        # Demander l'URL ngrok
        $ngrokUrl = Read-Host "Entrez l'URL ngrok (ou appuyez sur Entrée pour passer)"
        if ($ngrokUrl) {
            $webhookUrl = "$ngrokUrl/github-webhook/"
        }
    } catch {
        Show-Warning "ngrok n'est pas installé"
        Write-Host ""
        Show-Info "Pour installer ngrok :" "Cyan"
        Write-Host "  1. Téléchargez depuis https://ngrok.com/download" -ForegroundColor White
        Write-Host "  2. Ou installez avec Chocolatey : choco install ngrok" -ForegroundColor White
    }
    
} elseif ($Mode -eq "public") {
    Show-Info "Mode PUBLIC détecté - Récupération de l'IP publique..."
    
    try {
        $publicIp = (Invoke-RestMethod -Uri "https://api.ipify.org?format=json").ip
        Show-Success "IP publique détectée : $publicIp"
        $webhookUrl = "http://${publicIp}:8080/github-webhook/"
        
        Write-Host ""
        Show-Warning "Assurez-vous que :"
        Write-Host "  1. Le port 8080 est ouvert dans votre pare-feu" -ForegroundColor White
        Write-Host "  2. Votre routeur redirige le port 8080 vers cette machine" -ForegroundColor White
        Write-Host "  3. Jenkins est accessible depuis Internet" -ForegroundColor White
    } catch {
        Show-Error "Impossible de récupérer l'IP publique"
        exit 1
    }
}

# ============================================================================
# Étape 3 : Afficher les instructions de configuration
# ============================================================================

Write-Host "`n📋 Étape 3 : Configuration dans GitHub`n" -ForegroundColor Yellow

if ($webhookUrl) {
    Show-Success "URL du webhook à configurer :"
    Write-Host ""
    Write-Host "  $webhookUrl" -ForegroundColor Cyan
    Write-Host ""
    
    # Copier dans le presse-papier
    try {
        Set-Clipboard -Value $webhookUrl
        Show-Success "URL copiée dans le presse-papier !"
    } catch {
        Show-Warning "Impossible de copier dans le presse-papier"
    }
}

Write-Host "Instructions pour configurer le webhook dans GitHub :" -ForegroundColor White
Write-Host ""
Write-Host "1. Allez sur : https://github.com/MedHabibManai/IGL5-G5-achat" -ForegroundColor Gray
Write-Host "2. Cliquez sur Settings → Webhooks → Add webhook" -ForegroundColor Gray
Write-Host "3. Configurez :" -ForegroundColor Gray
Write-Host "   • Payload URL : $webhookUrl" -ForegroundColor Yellow
Write-Host "   • Content type : application/json" -ForegroundColor Gray
Write-Host "   • Secret : (laissez vide)" -ForegroundColor Gray
Write-Host "   • Events : Just the push event" -ForegroundColor Gray
Write-Host "   • Active : ✓ Coché" -ForegroundColor Gray
Write-Host "4. Cliquez sur Add webhook" -ForegroundColor Gray
Write-Host ""

# ============================================================================
# Étape 4 : Vérifier la configuration Jenkins
# ============================================================================

Write-Host "`n📋 Étape 4 : Vérification de la configuration Jenkins`n" -ForegroundColor Yellow

# Vérifier si le Jenkinsfile contient le trigger
$jenkinsfilePath = "Jenkinsfile"
if (Test-Path $jenkinsfilePath) {
    $jenkinsfileContent = Get-Content $jenkinsfilePath -Raw
    
    if ($jenkinsfileContent -match "triggers\s*\{") {
        Show-Success "Le Jenkinsfile contient déjà une section 'triggers'"
        
        if ($jenkinsfileContent -match "githubPush\(\)") {
            Show-Success "Le trigger 'githubPush()' est configuré"
        } else {
            Show-Warning "Le trigger 'githubPush()' n'est pas configuré"
            Write-Host ""
            Show-Info "Ajoutez cette ligne dans la section triggers :" "Cyan"
            Write-Host "  githubPush()" -ForegroundColor Yellow
        }
    } else {
        Show-Warning "Le Jenkinsfile ne contient pas de section 'triggers'"
        Write-Host ""
        Show-Info "Ajoutez cette section après 'agent any' :" "Cyan"
        Write-Host ""
        Write-Host "  triggers {" -ForegroundColor Yellow
        Write-Host "      githubPush()" -ForegroundColor Yellow
        Write-Host "  }" -ForegroundColor Yellow
        Write-Host ""
        
        # Proposer de modifier automatiquement
        $response = Read-Host "Voulez-vous que je modifie le Jenkinsfile automatiquement ? (o/n)"
        if ($response -eq "o" -or $response -eq "O") {
            # Créer une sauvegarde
            Copy-Item $jenkinsfilePath "${jenkinsfilePath}.backup"
            Show-Success "Sauvegarde créée : ${jenkinsfilePath}.backup"
            
            # Ajouter le trigger
            $newContent = $jenkinsfileContent -replace "(agent\s+any)", "`$1`n    `n    triggers {`n        githubPush()`n    }"
            Set-Content -Path $jenkinsfilePath -Value $newContent
            
            Show-Success "Jenkinsfile modifié avec succès !"
            Show-Info "N'oubliez pas de commit et push les changements"
        }
    }
} else {
    Show-Error "Jenkinsfile introuvable dans le répertoire actuel"
}

# ============================================================================
# Étape 5 : Instructions pour Jenkins Web UI
# ============================================================================

Write-Host "`n📋 Étape 5 : Configuration dans l'interface Jenkins`n" -ForegroundColor Yellow

Write-Host "Configurez le job Jenkins :" -ForegroundColor White
Write-Host ""
Write-Host "1. Allez sur : http://localhost:8080" -ForegroundColor Gray
Write-Host "2. Cliquez sur votre pipeline job" -ForegroundColor Gray
Write-Host "3. Cliquez sur Configure" -ForegroundColor Gray
Write-Host "4. Section Build Triggers :" -ForegroundColor Gray
Write-Host "   ☑ GitHub hook trigger for GITScm polling" -ForegroundColor Yellow
Write-Host "5. Cliquez sur Save" -ForegroundColor Gray
Write-Host ""

# ============================================================================
# Étape 6 : Test du webhook
# ============================================================================

Write-Host "`n📋 Étape 6 : Test du webhook`n" -ForegroundColor Yellow

if ($webhookUrl) {
    Show-Info "Test de l'endpoint webhook..."
    
    try {
        $testPayload = @{
            ref = "refs/heads/main"
            repository = @{
                name = "IGL5-G5-achat"
                clone_url = "https://github.com/MedHabibManai/IGL5-G5-achat.git"
            }
        } | ConvertTo-Json
        
        $response = Invoke-WebRequest -Uri $webhookUrl -Method POST -Body $testPayload -ContentType "application/json" -TimeoutSec 10 -UseBasicParsing -ErrorAction Stop
        
        if ($response.StatusCode -eq 200) {
            Show-Success "Webhook endpoint répond correctement (HTTP 200)"
        } else {
            Show-Warning "Webhook endpoint répond avec le code : $($response.StatusCode)"
        }
    } catch {
        Show-Error "Impossible de tester le webhook : $($_.Exception.Message)"
        Show-Info "Cela peut être normal si Jenkins n'est pas encore configuré"
    }
}

# ============================================================================
# Résumé
# ============================================================================

Write-Host "`n╔════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║   Résumé de la Configuration                          ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════════════╝`n" -ForegroundColor Cyan

Write-Host "📊 Webhooks configurés dans votre pipeline :" -ForegroundColor White
Write-Host ""
Write-Host "  1. ✅ SonarQube → Jenkins" -ForegroundColor Green
Write-Host "     URL: http://jenkins-cicd:8080/sonarqube-webhook/" -ForegroundColor Gray
Write-Host "     Fonction: Notification Quality Gate" -ForegroundColor Gray
Write-Host ""
Write-Host "  2. ⏳ GitHub → Jenkins" -ForegroundColor Yellow
Write-Host "     URL: $webhookUrl" -ForegroundColor Gray
Write-Host "     Fonction: Déclenchement automatique des builds" -ForegroundColor Gray
Write-Host ""

Write-Host "🧪 Pour tester le webhook GitHub :" -ForegroundColor White
Write-Host ""
Write-Host "  git commit --allow-empty -m 'Test webhook'" -ForegroundColor Yellow
Write-Host "  git push origin main" -ForegroundColor Yellow
Write-Host ""
Write-Host "  → Le build devrait démarrer automatiquement dans Jenkins !" -ForegroundColor Green
Write-Host ""

Write-Host "📚 Documentation complète : WEBHOOK_CONFIGURATION.md`n" -ForegroundColor Cyan

