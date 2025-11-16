# 🚀 Quick Webhook Setup Guide

## 📋 What You're Setting Up

A **GitHub webhook** that automatically triggers Jenkins builds when you push code to GitHub.

**Flow:** `git push` → GitHub → Webhook → Jenkins → Build starts automatically

---

## ✅ Step 1: Configure Jenkins Job

### 1.1 Enable Webhook Trigger in Jenkins

1. Go to Jenkins: `http://localhost:8080` (or your Jenkins URL)
2. Click on your pipeline job (e.g., "IGL5-G5-achat")
3. Click **Configure**
4. Scroll to **"Build Triggers"** section
5. ☑️ **Check**: `GitHub hook trigger for GITScm polling`
6. Scroll to **"Source Code Management"** section
7. Verify:
   - **Repository URL**: `https://github.com/MedHabibManai/IGL5-G5-achat.git`
   - **Branch**: `*/main` or `*/MohamedHabibManai-GL5-G5-Produit` (your branch)
8. Click **Save**

✅ **Jenkins is now ready to receive webhooks!**

---

## ✅ Step 2: Make Jenkins Accessible

You need to make Jenkins accessible from the internet so GitHub can send webhooks.

### Option A: Using ngrok (For Local Jenkins - Recommended for Testing)

1. **Install ngrok:**
   ```powershell
   # Download from https://ngrok.com/download
   # Or with Chocolatey:
   choco install ngrok
   ```

2. **Start ngrok:**
   ```powershell
   ngrok http 8080
   ```

3. **Copy the HTTPS URL** (looks like: `https://abc123.ngrok-free.app`)
   - This is your public URL to Jenkins

4. **Keep ngrok running** (don't close the terminal)

⚠️ **Note:** Free ngrok URLs change each time you restart. For production, use Option B.

### Option B: Public IP/Server (For Production)

If Jenkins is on a server with a public IP:

1. **Get your public IP:**
   ```powershell
   Invoke-RestMethod -Uri "https://api.ipify.org?format=json" | Select-Object -ExpandProperty ip
   ```

2. **Open firewall port 8080:**
   ```powershell
   New-NetFirewallRule -DisplayName "Jenkins" -Direction Inbound -LocalPort 8080 -Protocol TCP -Action Allow
   ```

3. **Your Jenkins URL:** `http://YOUR_PUBLIC_IP:8080`

---

## ✅ Step 3: Configure GitHub Webhook

### 3.1 Access GitHub Settings

1. Go to: https://github.com/MedHabibManai/IGL5-G5-achat
2. Click **Settings** (⚙️ icon)
3. Click **Webhooks** (left sidebar)
4. Click **Add webhook**

### 3.2 Configure Webhook

**Payload URL:**
```
https://YOUR_NGROK_URL.ngrok-free.app/github-webhook/
```
or
```
http://YOUR_PUBLIC_IP:8080/github-webhook/
```

⚠️ **Important:** Must end with `/github-webhook/`

**Content type:**
```
application/json
```

**Secret:**
```
(leave empty for now)
```

**Which events would you like to trigger this webhook?**
- ☑️ **Just the push event** (recommended)

**Active:**
- ☑️ **Checked**

### 3.3 Save Webhook

Click **Add webhook**

GitHub will immediately test the webhook with a ping!

**Expected result:**
- ✅ Green checkmark next to webhook
- Recent Deliveries shows: **200 OK**

---

## 🧪 Step 4: Test the Webhook

### Test 1: Empty Commit (Quick Test)

```powershell
cd C:\Users\MSI\Documents\TESTING\IGL5-G5-achat
git commit --allow-empty -m "Test webhook trigger"
git push
```

**Expected:**
- Jenkins build starts automatically within seconds
- Check Jenkins dashboard - you should see a new build running

### Test 2: Real Change

```powershell
echo "# Webhook test" >> README.md
git add README.md
git commit -m "Test webhook with real change"
git push
```

**Expected:**
- Build triggers automatically
- Build completes successfully

---

## 🔍 Troubleshooting

### Problem: Webhook shows ❌ (red X) in GitHub

**Check:**
1. Is ngrok running? (if using ngrok)
2. Is Jenkins accessible? Try: `http://localhost:8080` in browser
3. Is the URL correct? Must end with `/github-webhook/`
4. Check Recent Deliveries in GitHub - what's the error code?

**Common errors:**
- **404**: URL is wrong (check `/github-webhook/` at the end)
- **502**: Jenkins not accessible (ngrok not running or firewall blocking)
- **403**: Authentication issue

### Problem: Webhook works but build doesn't start

**Check:**
1. Is "GitHub hook trigger for GITScm polling" enabled in Jenkins job?
2. Is the branch name correct in Jenkins job configuration?
3. Check Jenkins logs: `docker logs jenkins-cicd --tail 50`

### Problem: ngrok URL changes

**Solution:**
- Update the webhook URL in GitHub Settings → Webhooks
- Or use ngrok with a static domain (paid plan)

---

## ✅ Verification Checklist

- [ ] Jenkins job has "GitHub hook trigger" enabled
- [ ] Jenkins is accessible (via ngrok or public IP)
- [ ] GitHub webhook is configured with correct URL
- [ ] Webhook shows ✅ (green checkmark) in GitHub
- [ ] Test commit triggers build automatically

---

## 📚 Additional Resources

- Full documentation: `WEBHOOK_CONFIGURATION.md`
- ngrok guide: `GUIDE_WEBHOOK_NGROK.md`
- Setup script: `scripts/setup-webhook-ngrok.ps1`

---

## 🎯 Quick Summary

1. **Jenkins:** Enable "GitHub hook trigger for GITScm polling"
2. **Make Jenkins accessible:** Use ngrok or public IP
3. **GitHub:** Add webhook with URL: `https://YOUR_URL/github-webhook/`
4. **Test:** Push a commit and watch Jenkins build automatically!

That's it! Your webhook is now configured! 🎉

