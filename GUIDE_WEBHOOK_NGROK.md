# 🚀 Guide Pratique : Webhook GitHub → Jenkins avec ngrok

## 📋 Ce que nous allons faire

Configurer un webhook pour que **chaque fois que vous faites un `git push` sur GitHub**, Jenkins **démarre automatiquement un build** sans attendre.

**Flux de travail :**
```
Vous → git push → GitHub → Webhook → ngrok → Jenkins → Build automatique
```

---

## ✅ Prérequis

- [x] Jenkins tourne sur http://localhost:8080
- [x] Repository GitHub : https://github.com/MedHabibManai/IGL5-G5-achat
- [ ] ngrok installé
- [ ] Compte ngrok (gratuit)

---

## 📝 ÉTAPE 1 : Installer ngrok

### Option A : Téléchargement Manuel

1. **Allez sur** : https://ngrok.com/download
2. **Téléchargez** : Windows (64-bit)
3. **Extrayez** : `ngrok.exe` dans `C:\Windows\System32\`
4. **Testez** :
   ```powershell
   ngrok version
   ```

### Option B : Avec Chocolatey

```powershell
choco install ngrok -y
```

### Créer un compte ngrok (Gratuit)

1. Allez sur : https://dashboard.ngrok.com/signup
2. Créez un compte (gratuit)
3. Copiez votre **Authtoken** depuis : https://dashboard.ngrok.com/get-started/your-authtoken

### Configurer l'Authtoken

```powershell
ngrok config add-authtoken VOTRE_TOKEN_ICI
```

✅ **Vérification :**
```powershell
ngrok version
# Devrait afficher : ngrok version 3.x.x
```

---

## 🌐 ÉTAPE 2 : Démarrer ngrok

### Ouvrir un nouveau terminal PowerShell

**Important :** Gardez ce terminal ouvert pendant toute la durée du test !

```powershell
ngrok http 8080
```

### Résultat attendu :

```
ngrok

Session Status                online
Account                       votre-email@example.com
Version                       3.x.x
Region                        United States (us)
Latency                       -
Web Interface                 http://127.0.0.1:4040
Forwarding                    https://abc123def456.ngrok-free.app -> http://localhost:8080

Connections                   ttl     opn     rt1     rt5     p50     p90
                              0       0       0.00    0.00    0.00    0.00
```

### 🎯 Informations importantes :

**URL publique ngrok :**
```
https://abc123def456.ngrok-free.app
```
☝️ **Copiez cette URL !** Elle change à chaque redémarrage de ngrok (version gratuite)

**Interface Web ngrok :**
```
http://127.0.0.1:4040
```
☝️ Permet de voir les requêtes en temps réel

---

## 🔧 ÉTAPE 3 : Tester l'accès à Jenkins via ngrok

### Dans un NOUVEAU terminal PowerShell :

```powershell
# Remplacez par VOTRE URL ngrok
$ngrokUrl = "https://abc123def456.ngrok-free.app"

# Tester l'accès
Invoke-WebRequest -Uri $ngrokUrl -UseBasicParsing
```

### Résultat attendu :

```
StatusCode        : 200
StatusDescription : OK
```

✅ **Jenkins est maintenant accessible depuis Internet via ngrok !**

### Ouvrir Jenkins dans le navigateur :

```powershell
# Ouvrir Jenkins via ngrok
start https://VOTRE_URL_NGROK.ngrok-free.app
```

⚠️ **Note :** ngrok affichera un avertissement. Cliquez sur "Visit Site" pour continuer.

---

## 📋 ÉTAPE 4 : Configurer le Jenkinsfile

### Vérifier si le trigger existe déjà :

```powershell
Get-Content Jenkinsfile | Select-String "triggers"
```

### Si rien n'apparaît, ajoutez le trigger :

Ouvrez `Jenkinsfile` et ajoutez cette section **après `agent any`** :

```groovy
pipeline {
    agent any
    
    // ✅ AJOUTER CETTE SECTION
    triggers {
        githubPush()
    }
    
    tools {
        maven 'Maven-3.8.6'
        jdk 'JDK-8'
    }
    
    // ... reste du pipeline
}
```

### Sauvegarder et commiter :

```powershell
git add Jenkinsfile
git commit -m "Add GitHub webhook trigger"
git push origin main
```

---

## ⚙️ ÉTAPE 5 : Configurer Jenkins (Interface Web)

### 1. Accéder à Jenkins

```
http://localhost:8080
```

### 2. Configurer le Job

1. **Cliquez** sur votre pipeline job (ex: "IGL5-G5-achat")
2. **Cliquez** sur **"Configure"** (à gauche)
3. **Section "Build Triggers"** :
   - ☑️ **Cochez** : `GitHub hook trigger for GITScm polling`
4. **Section "Source Code Management"** :
   - Vérifiez que l'URL du repository est : `https://github.com/MedHabibManai/IGL5-G5-achat.git`
   - Branch : `*/main`
5. **Cliquez** sur **"Save"**

✅ **Jenkins est maintenant prêt à recevoir les webhooks !**

---

## 🔗 ÉTAPE 6 : Configurer le Webhook dans GitHub

### 1. Accéder aux paramètres du repository

```
https://github.com/MedHabibManai/IGL5-G5-achat/settings/hooks
```

Ou manuellement :
1. Allez sur : https://github.com/MedHabibManai/IGL5-G5-achat
2. Cliquez sur **Settings** (⚙️)
3. Dans le menu de gauche : **Webhooks**
4. Cliquez sur **Add webhook**

### 2. Configurer le Webhook

**Payload URL :**
```
https://VOTRE_URL_NGROK.ngrok-free.app/github-webhook/
```
⚠️ **Important :** N'oubliez pas le `/github-webhook/` à la fin !

**Exemple :**
```
https://abc123def456.ngrok-free.app/github-webhook/
```

**Content type :**
```
application/json
```

**Secret :**
```
(laissez vide)
```

**Which events would you like to trigger this webhook?**
- ⚪ Just the push event ← **Sélectionnez cette option**

**Active :**
- ☑️ **Coché**

### 3. Ajouter le Webhook

Cliquez sur **"Add webhook"**

### 4. Vérifier le Ping

GitHub envoie automatiquement un "ping" pour tester le webhook.

**Résultat attendu :**
- ✅ **Coche verte** à côté du webhook
- **Recent Deliveries** : 1 delivery avec code **200**

**Si erreur :**
- ❌ **X rouge** : Vérifiez l'URL ngrok
- Code **404** : Vérifiez `/github-webhook/` à la fin
- Code **502** : ngrok n'est pas démarré

---

## 🧪 ÉTAPE 7 : Tester le Webhook

### Test 1 : Commit vide (rapide)

```powershell
# Créer un commit vide pour tester
git commit --allow-empty -m "Test webhook trigger"

# Pusher sur GitHub
git push origin main
```

### Résultat attendu :

**Dans le terminal ngrok :**
```
POST /github-webhook/          200 OK
```

**Dans Jenkins (http://localhost:8080) :**
- 🚀 Un nouveau build démarre **automatiquement** dans les 2-3 secondes !
- Vous voyez : `Started by GitHub push by MedHabibManai`

**Dans l'interface ngrok (http://127.0.0.1:4040) :**
- Vous voyez la requête POST de GitHub avec le payload JSON

### Test 2 : Modification réelle

```powershell
# Modifier un fichier
echo "# Test webhook" >> README.md

# Commiter et pusher
git add README.md
git commit -m "Test webhook with real change"
git push origin main
```

**Résultat :**
- ✅ Build démarre automatiquement dans Jenkins
- ✅ Toutes les étapes du pipeline s'exécutent
- ✅ Grafana montre l'activité (executors in use)

---

## 📊 ÉTAPE 8 : Vérifier les Webhooks

### Dans GitHub :

1. Allez sur : https://github.com/MedHabibManai/IGL5-G5-achat/settings/hooks
2. Cliquez sur votre webhook
3. Onglet **"Recent Deliveries"**
4. Vous devriez voir :
   - **Ping** (lors de la création) : ✅ 200
   - **Push** (votre test) : ✅ 200

### Cliquez sur une delivery pour voir :

**Request :**
```json
{
  "ref": "refs/heads/main",
  "commits": [
    {
      "message": "Test webhook trigger",
      "author": {
        "name": "MedHabibManai"
      }
    }
  ]
}
```

**Response :**
```
Status: 200 OK
```

### Dans Jenkins :

1. Allez sur votre build
2. **Console Output**
3. Vous devriez voir :
```
Started by GitHub push by MedHabibManai
```

---

## 🎯 Résumé de la Configuration

### ✅ Ce qui est configuré :

| Composant | Configuration | Status |
|-----------|---------------|--------|
| **ngrok** | Expose Jenkins sur Internet | ✅ |
| **Jenkinsfile** | `triggers { githubPush() }` | ✅ |
| **Jenkins Job** | GitHub hook trigger activé | ✅ |
| **GitHub Webhook** | Pointe vers ngrok URL | ✅ |

### 🔄 Flux de travail complet :

```
1. Vous : git push origin main
   ↓
2. GitHub : Détecte le push
   ↓
3. GitHub : Envoie webhook POST à ngrok
   ↓
4. ngrok : Redirige vers Jenkins local (localhost:8080)
   ↓
5. Jenkins : Reçoit la notification
   ↓
6. Jenkins : Démarre le build automatiquement
   ↓
7. Pipeline : Exécute toutes les étapes
   ↓
8. Vous : Voyez le résultat dans Jenkins
```

---

## ⚠️ Limitations de ngrok (Version Gratuite)

1. **URL change à chaque redémarrage**
   - Solution : Mettre à jour le webhook GitHub avec la nouvelle URL
   - Ou : Payer pour ngrok Pro (URL fixe)

2. **Limite de connexions**
   - 40 connexions/minute (largement suffisant pour les tests)

3. **Page d'avertissement**
   - ngrok affiche un avertissement avant d'accéder à Jenkins
   - Cliquez sur "Visit Site" pour continuer

---

## 🔧 Dépannage

### Problème : Webhook retourne 502

**Cause :** ngrok n'est pas démarré ou Jenkins est arrêté

**Solution :**
```powershell
# Vérifier Jenkins
docker ps | Select-String jenkins

# Redémarrer ngrok
ngrok http 8080
```

### Problème : Build ne démarre pas

**Vérifications :**

1. **Trigger dans Jenkinsfile ?**
   ```powershell
   Get-Content Jenkinsfile | Select-String "githubPush"
   ```

2. **Build Trigger activé dans Jenkins ?**
   - Job → Configure → Build Triggers
   - ☑️ GitHub hook trigger for GITScm polling

3. **Bonne branche ?**
   - Vérifiez que vous pushez sur `main`

### Problème : URL ngrok invalide

**Solution :**
```powershell
# Obtenir l'URL actuelle de ngrok
Invoke-RestMethod http://127.0.0.1:4040/api/tunnels | 
    Select-Object -ExpandProperty tunnels | 
    Select-Object -ExpandProperty public_url
```

---

## 🎉 Test Final

### Scénario complet :

```powershell
# 1. Vérifier que ngrok tourne
# (Dans le terminal ngrok, vous devez voir "Session Status: online")

# 2. Faire un changement
echo "# Webhook test $(Get-Date)" >> README.md

# 3. Commiter
git add README.md
git commit -m "Final webhook test"

# 4. Pusher
git push origin main

# 5. Regarder Jenkins
start http://localhost:8080

# 6. Regarder ngrok interface
start http://127.0.0.1:4040
```

**Résultat attendu :**
- ⏱️ **< 3 secondes** : Build démarre dans Jenkins
- 📊 **ngrok interface** : Montre la requête POST de GitHub
- ✅ **Jenkins** : Build s'exécute avec succès
- 📈 **Grafana** : Montre l'activité des executors

---

## 📚 Prochaines Étapes

1. ✅ Webhook GitHub → Jenkins configuré
2. ⏳ (Optionnel) Configurer Slack notifications
3. ⏳ (Optionnel) Ajouter des webhooks pour les Pull Requests
4. ⏳ (Production) Déployer Jenkins sur un serveur avec IP publique

---

## 💡 Conseils

- **Gardez le terminal ngrok ouvert** pendant vos tests
- **Notez l'URL ngrok** car elle change à chaque redémarrage
- **Utilisez l'interface ngrok** (http://127.0.0.1:4040) pour déboguer
- **Pour la production**, utilisez un serveur avec IP publique au lieu de ngrok


