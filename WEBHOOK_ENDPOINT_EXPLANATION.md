# 🔍 Understanding the Webhook Endpoint

## ✅ This is Normal!

When you visit `/github-webhook/` in a browser, you see "cannot process" - **this is expected!**

### Why?

- **Browser (GET request)**: Shows "cannot process" ❌
- **GitHub (POST request)**: Works correctly ✅

The webhook endpoint only accepts **POST requests** from GitHub, not browser GET requests.

---

## 🧪 How to Verify It's Working

### Method 1: Check GitHub Webhook Status

1. Go to: https://github.com/MedHabibManai/IGL5-G5-achat/settings/hooks
2. Click on your webhook
3. Look at **"Recent Deliveries"**
4. Check the latest delivery:
   - ✅ **200 OK** = Webhook is working!
   - ❌ **302** or other error = Still needs fixing

### Method 2: Test with a Commit

```powershell
cd C:\Users\MSI\Documents\TESTING\IGL5-G5-achat
git commit --allow-empty -m "Test webhook"
git push
```

**Expected:**
- Jenkins should start a build automatically within seconds
- Check Jenkins dashboard - you should see a new build

---

## 🔧 If GitHub Still Shows 302 Error

### Check 1: Verify the Exact URL Format

Your webhook URL should be:
```
https://opinionated-kortney-ineptly.ngrok-free.dev/github-webhook/
```

**Make sure:**
- ✅ Uses `https://`
- ✅ Ends with `/github-webhook/` (with trailing slash)
- ✅ No extra characters or spaces

### Check 2: Verify Jenkins Configuration

1. Go to Jenkins → Your Job → Configure
2. **Build Triggers** section:
   - ☑️ "GitHub hook trigger for GITScm polling" must be **checked**
3. **Source Code Management**:
   - Repository URL: `https://github.com/MedHabibManai/IGL5-G5-achat.git`
   - Branch: `*/MohamedHabibManai-GL5-G5-Produit` (or your branch)
4. Click **Save**

### Check 3: Check ngrok is Still Running

Make sure ngrok is still running:
```powershell
Get-Process -Name ngrok -ErrorAction SilentlyContinue
```

If nothing shows, restart ngrok:
```powershell
ngrok http 8080
```

**Important:** The ngrok URL changes each time you restart it, so update the webhook URL in GitHub!

---

## 🎯 What to Do Now

1. **Verify the webhook URL in GitHub:**
   - Should be: `https://opinionated-kortney-ineptly.ngrok-free.dev/github-webhook/`
   - Make sure it's exactly this (no typos)

2. **Check GitHub Recent Deliveries:**
   - Look at the latest delivery
   - What status code does it show?

3. **Test with a commit:**
   - Make a test commit and push
   - Does Jenkins start a build?

---

## 💡 The "Cannot Process" Page is OK!

**Don't worry about the "cannot process" page** when visiting `/github-webhook/` in a browser. That's normal!

The real test is:
- ✅ Does GitHub show 200 OK in Recent Deliveries?
- ✅ Does a git push trigger a Jenkins build?

If both are yes, your webhook is working! 🎉

