# 🚀 ngrok Setup - Next Steps

## ✅ Step 1: Start ngrok

Open a **new terminal/PowerShell window** and run:

```powershell
ngrok http 8080
```

**Keep this terminal open!** ngrok needs to keep running.

---

## ✅ Step 2: Get Your Public URL

After starting ngrok, you'll see output like this:

```
ngrok

Session Status                online
Account                       Your Name (Plan: Free)
Version                       3.x.x
Region                        United States (us)
Latency                       45ms
Web Interface                 http://127.0.0.1:4040
Forwarding                    https://abc123def456.ngrok-free.app -> http://localhost:8080

Connections                   ttl     opn     rt1     rt5     p50     p90
                              0       0       0.00    0.00    0.00    0.00
```

**Copy the HTTPS URL:**
```
https://abc123def456.ngrok-free.app
```

This is your public URL to Jenkins!

---

## ✅ Step 3: Test Jenkins Access

Open a browser and go to:
```
https://YOUR_NGROK_URL.ngrok-free.app
```

You should see your Jenkins login page!

✅ **If this works, ngrok is set up correctly!**

---

## ✅ Step 4: Configure GitHub Webhook

### 4.1 Go to GitHub Webhook Settings

1. Go to: https://github.com/MedHabibManai/IGL5-G5-achat/settings/hooks
2. Click **"Add webhook"**

### 4.2 Configure the Webhook

**Payload URL:**
```
https://YOUR_NGROK_URL.ngrok-free.app/github-webhook/
```

⚠️ **Important:** 
- Replace `YOUR_NGROK_URL` with your actual ngrok URL
- Must end with `/github-webhook/`

**Example:**
```
https://abc123def456.ngrok-free.app/github-webhook/
```

**Content type:**
```
application/json
```

**Secret:**
```
(leave empty)
```

**Which events would you like to trigger this webhook?**
- ☑️ **Just the push event**

**Active:**
- ☑️ **Checked**

### 4.3 Save Webhook

Click **"Add webhook"**

GitHub will immediately test it - you should see a ✅ green checkmark!

---

## ✅ Step 5: Verify Jenkins is Ready

Make sure in Jenkins:
1. Job → Configure
2. **Build Triggers** section: ☑️ "GitHub hook trigger for GITScm polling"
3. **Source Code Management**: Branch set correctly
4. **Save**

---

## 🧪 Step 6: Test the Webhook

### Test 1: Check ngrok Logs

In your ngrok terminal, you should see:
```
POST /github-webhook/          200 OK
```

This means GitHub successfully sent the webhook!

### Test 2: Trigger a Build

```powershell
cd C:\Users\MSI\Documents\TESTING\IGL5-G5-achat
git commit --allow-empty -m "Test webhook trigger"
git push
```

**Expected:**
- Within seconds, Jenkins should start a build automatically!
- Check Jenkins dashboard - you should see a new build running

---

## ⚠️ Important Notes

1. **Keep ngrok running**: Don't close the ngrok terminal window
2. **URL changes**: Free ngrok URLs change each time you restart ngrok
3. **Update webhook**: If you restart ngrok, update the webhook URL in GitHub

---

## 🎯 Quick Checklist

- [ ] ngrok is running (`ngrok http 8080`)
- [ ] Copied the HTTPS URL from ngrok
- [ ] Tested Jenkins access via ngrok URL
- [ ] Configured GitHub webhook with: `https://YOUR_URL/github-webhook/`
- [ ] Webhook shows ✅ in GitHub
- [ ] Jenkins has "GitHub hook trigger" enabled
- [ ] Tested with a commit

---

## 🚨 Troubleshooting

**Problem: Can't access Jenkins via ngrok URL**
- Check if Jenkins is running on port 8080
- Check if ngrok is still running
- Try restarting ngrok

**Problem: Webhook shows ❌ in GitHub**
- Check ngrok URL is correct
- Make sure URL ends with `/github-webhook/`
- Check ngrok is still running
- Look at Recent Deliveries in GitHub for error details

**Problem: Webhook works but build doesn't start**
- Verify "GitHub hook trigger" is enabled in Jenkins
- Check branch name matches in Jenkins configuration
- Check Jenkins logs

---

You're almost done! Just start ngrok and configure the GitHub webhook! 🎉

