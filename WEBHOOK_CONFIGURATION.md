&²# 📡 Guide de Configuration des Webhooks

## 📚 Table des Matières
1. [Qu'est-ce qu'un Webhook ?](#quest-ce-quun-webhook-)
2. [Webhooks Existants](#webhooks-existants)
3. [Configuration GitHub Webhook](#configuration-github-webhook)
4. [Test et Vérification](#test-et-vérification)
5. [Dépannage](#dépannage)

---

## 🔍 Qu'est-ce qu'un Webhook ?

### Définition
Un **webhook** est un mécanisme de **notification HTTP automatique** qui permet à une application d'envoyer des données en temps réel vers une autre application lorsqu'un événement spécifique se produit.

### Analogie Simple
Imaginez que vous attendez un colis :
- **Sans webhook (polling)** : Vous vérifiez votre boîte aux lettres toutes les heures
- **Avec webhook** : Le facteur sonne à votre porte quand le colis arrive

### Fonctionnement Technique

```
┌─────────────────┐                           ┌─────────────────┐
│                 │   1. Événement déclenché  │                 │
│   Application   │   (ex: git push)          │   Application   │
│   Source        │                           │   Destination   │
│   (GitHub)      │                           │   (Jenkins)     │
│                 │                           │                 │
│                 │   2. HTTP POST Request    │                 │
│                 │──────────────────────────▶│                 │
│                 │   Payload JSON            │                 │
│                 │                           │                 │
│                 │   3. Réponse HTTP 200     │                 │
│                 │◀──────────────────────────│                 │
│                 │                           │                 │
│                 │                           │   4. Action     │
│                 │                           │   (Build)       │
└─────────────────┘                           └─────────────────┘
```

### Avantages vs Polling

| Critère | Polling (Sans Webhook) | Webhook |
|---------|------------------------|---------|
| **Délai** | 1-5 minutes | Instantané (< 1 seconde) |
| **Ressources** | Vérifications constantes | Notification uniquement si événement |
| **Charge serveur** | Élevée | Faible |
| **Efficacité** | ❌ Faible | ✅ Élevée |
| **Temps réel** | ❌ Non | ✅ Oui |

### Exemple de Payload Webhook

Quand vous faites un `git push`, GitHub envoie ce type de données à Jenkins :

```json
{
  "ref": "refs/heads/main",
  "repository": {
    "name": "IGL5-G5-achat",
    "full_name": "MedHabibManai/IGL5-G5-achat",
    "clone_url": "https://github.com/MedHabibManai/IGL5-G5-achat.git"
  },
  "pusher": {
    "name": "MedHabibManai",
    "email": "med@example.com"
  },
  "commits": [
    {
      "id": "abc123...",
      "message": "Fix bug in payment service",
      "timestamp": "2025-10-30T19:00:00Z",
      "author": {
        "name": "Med Habib Manai"
      }
    }
  ]
}
```

Jenkins reçoit ces données et déclenche automatiquement un build !

---

## ✅ Webhooks Existants

### 1. SonarQube → Jenkins (Déjà Configuré ✅)

**URL du Webhook :**
```
http://jenkins-cicd:8080/sonarqube-webhook/
```

**Fonction :**
- Notifie Jenkins quand l'analyse SonarQube est terminée
- Permet au pipeline d'attendre le Quality Gate
- Évite le timeout de 5 minutes

**Configuration :**
- ✅ Configuré dans SonarQube : Administration → Configuration → Webhooks
- ✅ Nom : `Jenkins`
- ✅ Secret : (optionnel, non configuré)

**Flux de travail :**
```
Jenkins Pipeline
    │
    ├─ Stage: SonarQube Analysis
    │     └─ Envoie le code à SonarQube
    │
    ├─ Stage: Quality Gate
    │     └─ Attend la notification webhook
    │
    ▼
SonarQube analyse le code
    │
    └─ Envoie webhook à Jenkins ──▶ Pipeline continue
```

---

## 🔧 Configuration GitHub Webhook

### Prérequis

1. **Jenkins accessible depuis Internet** (ou utiliser ngrok/localtunnel pour tests locaux)
2. **Droits administrateur** sur le repository GitHub
3. **Plugin GitHub** installé dans Jenkins (déjà installé)

### Option A : Configuration Locale avec ngrok (Pour Tests)

Si Jenkins tourne en local (localhost:8080), GitHub ne peut pas l'atteindre. Utilisez **ngrok** :

#### 1. Installer ngrok

```powershell
# Télécharger depuis https://ngrok.com/download
# Ou avec Chocolatey :
choco install ngrok
```

#### 2. Exposer Jenkins sur Internet

```powershell
ngrok http 8080
```

Vous obtiendrez une URL publique :
```
Forwarding  https://abc123.ngrok.io -> http://localhost:8080
```

#### 3. URL du Webhook

```
https://abc123.ngrok.io/github-webhook/
```

⚠️ **Note :** L'URL ngrok change à chaque redémarrage (version gratuite)

---

### Option B : Configuration avec IP Publique (Production)

Si Jenkins est sur un serveur avec IP publique :

#### 1. Vérifier l'IP Publique

```powershell
# Obtenir votre IP publique
Invoke-RestMethod -Uri "https://api.ipify.org?format=json" | Select-Object -ExpandProperty ip
```

#### 2. Configurer le Pare-feu

Ouvrir le port 8080 :
```powershell
# Windows Firewall
New-NetFirewallRule -DisplayName "Jenkins" -Direction Inbound -LocalPort 8080 -Protocol TCP -Action Allow
```

#### 3. URL du Webhook

```
http://<VOTRE_IP_PUBLIQUE>:8080/github-webhook/
```

---

### 📝 Configuration dans GitHub

#### Étape 1 : Accéder aux Paramètres du Repository

1. Allez sur : https://github.com/MedHabibManai/IGL5-G5-achat
2. Cliquez sur **Settings** (⚙️)
3. Dans le menu de gauche, cliquez sur **Webhooks**
4. Cliquez sur **Add webhook**

#### Étape 2 : Configurer le Webhook

**Payload URL :**
```
http://<VOTRE_IP_OU_NGROK>:8080/github-webhook/
```

**Content type :**
```
application/json
```

**Secret :** (optionnel, laissez vide pour l'instant)

**Which events would you like to trigger this webhook?**
- ☑️ **Just the push event** (recommandé pour commencer)
- Ou **Let me select individual events** :
  - ☑️ Pushes
  - ☑️ Pull requests
  - ☑️ Pull request reviews

**Active :**
- ☑️ Coché

#### Étape 3 : Sauvegarder

Cliquez sur **Add webhook**

GitHub va immédiatement tester le webhook en envoyant un ping !

---

### 🔧 Configuration dans Jenkins

#### Étape 1 : Modifier le Jenkinsfile

Ajoutez le trigger dans votre Jenkinsfile :

```groovy
pipeline {
    agent any
    
    // ✅ AJOUTER CETTE SECTION
    triggers {
        githubPush()  // Déclenche le build lors d'un push GitHub
    }
    
    tools {
        maven 'Maven-3.8.6'
        jdk 'JDK-8'
    }
    
    // ... reste du pipeline
}
```

#### Étape 2 : Configurer le Job Jenkins (Interface Web)

1. Allez sur Jenkins : http://localhost:8080
2. Cliquez sur votre pipeline job
3. Cliquez sur **Configure**
4. Section **Build Triggers** :
   - ☑️ **GitHub hook trigger for GITScm polling**
5. Section **Source Code Management** :
   - Vérifiez que l'URL du repository est correcte
   - Branch : `*/main`
6. Cliquez sur **Save**

---

## 🧪 Test et Vérification

### Test 1 : Vérifier le Webhook dans GitHub

1. Allez sur GitHub → Settings → Webhooks
2. Cliquez sur votre webhook
3. Onglet **Recent Deliveries**
4. Vous devriez voir un ping avec :
   - ✅ **Response code : 200** (succès)
   - ❌ **Response code : 404/500** (erreur)

### Test 2 : Déclencher un Build

```powershell
# Faire un changement et pusher
echo "# Test webhook" >> README.md
git add README.md
git commit -m "Test webhook trigger"
git push origin main
```

**Résultat attendu :**
- ⏱️ **< 5 secondes** : Jenkins reçoit la notification
- 🚀 Build démarre automatiquement
- 📊 Vous voyez le build dans Jenkins

### Test 3 : Vérifier les Logs Jenkins

```powershell
# Voir les logs Jenkins
docker logs jenkins-cicd --tail 50 | Select-String "webhook\|GitHub"
```

Vous devriez voir :
```
GitHub webhook received
Triggering build for branch main
```

---

## 🔍 Dépannage

### Problème 1 : Webhook GitHub retourne 404

**Cause :** URL incorrecte ou Jenkins non accessible

**Solution :**
```powershell
# Tester l'URL manuellement
Invoke-WebRequest -Uri "http://localhost:8080/github-webhook/" -Method POST
```

Si erreur 404 :
- Vérifiez que le plugin GitHub est installé
- Vérifiez l'URL (doit finir par `/github-webhook/`)

### Problème 2 : Build ne se déclenche pas

**Vérifications :**

1. **Trigger configuré dans Jenkinsfile ?**
   ```groovy
   triggers {
       githubPush()
   }
   ```

2. **Build Trigger activé dans Jenkins ?**
   - Job → Configure → Build Triggers
   - ☑️ GitHub hook trigger for GITScm polling

3. **Branch correcte ?**
   - Vérifiez que vous pushez sur la branche configurée (`main`)

### Problème 3 : Jenkins en local non accessible

**Solution : Utiliser ngrok**

```powershell
# Terminal 1 : Démarrer ngrok
ngrok http 8080

# Terminal 2 : Vérifier l'accès
$ngrokUrl = "https://abc123.ngrok.io"  # Remplacer par votre URL
Invoke-WebRequest -Uri "$ngrokUrl/github-webhook/" -Method POST
```

---

## 📊 Comparaison des Webhooks

| Webhook | Source | Destination | Événement | Status |
|---------|--------|-------------|-----------|--------|
| **SonarQube** | SonarQube | Jenkins | Quality Gate terminé | ✅ Configuré |
| **GitHub** | GitHub | Jenkins | git push | ⏳ À configurer |
| **Docker Hub** | Docker Hub | Jenkins | Image mise à jour | ❌ Non configuré |
| **Slack** | Jenkins | Slack | Build terminé | ❌ Non configuré |

---

## 🎯 Prochaines Étapes

1. ✅ Comprendre le concept de webhook
2. ⏳ Configurer GitHub webhook
3. ⏳ Tester avec un git push
4. ⏳ (Optionnel) Configurer Slack notifications
5. ⏳ (Optionnel) Configurer Docker Hub webhook

---

## 📚 Ressources

- [GitHub Webhooks Documentation](https://docs.github.com/en/webhooks)
- [Jenkins GitHub Plugin](https://plugins.jenkins.io/github/)
- [ngrok Documentation](https://ngrok.com/docs)
- [Webhook Testing Tool](https://webhook.site/)


