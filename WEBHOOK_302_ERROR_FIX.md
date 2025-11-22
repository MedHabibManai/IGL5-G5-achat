# 🔧 Fix: Webhook 302 Error (Invalid HTTP Response: 302)

## ❌ Error You're Seeing

```
Last delivery was not successful. Invalid HTTP Response: 302
```

## 🔍 What This Means

A **302** is a redirect response. This usually means:
1. The webhook URL is wrong or incomplete
2. Jenkins is redirecting (possibly to login page)
3. Missing trailing slash in the URL

---

## ✅ Solution 1: Check the Webhook URL Format

The webhook URL **must** be exactly:

```
https://YOUR_NGROK_URL.ngrok-free.app/github-webhook/
```

**Important:**
- ✅ Must start with `https://`
- ✅ Must include your ngrok URL
- ✅ Must end with `/github-webhook/` (with trailing slash)
- ❌ NOT `/github-webhook` (without trailing slash)
- ❌ NOT `/webhook/`
- ❌ NOT just the ngrok URL

**Example:**
```
✅ Correct: https://abc123def456.ngrok-free.app/github-webhook/
❌ Wrong:   https://abc123def456.ngrok-free.app/github-webhook
❌ Wrong:   https://abc123def456.ngrok-free.app/webhook/
❌ Wrong:   https://abc123def456.ngrok-free.app/
```

---

## ✅ Solution 2: Verify Jenkins is Accessible

1. **Test the ngrok URL in browser:**
   ```
   https://YOUR_NGROK_URL.ngrok-free.app
   ```
   - You should see Jenkins login page
   - If you see an error, ngrok isn't working correctly

2. **Test the webhook endpoint:**
   ```
   https://YOUR_NGROK_URL.ngrok-free.app/github-webhook/
   ```
   - Should show Jenkins webhook page (or redirect to login)
   - If 404, the path is wrong

---

## ✅ Solution 3: Check Jenkins Configuration

Make sure in Jenkins:

1. **Job → Configure**
2. **Build Triggers** section:
   - ☑️ "GitHub hook trigger for GITScm polling" is **checked**
3. **Save**

---

## ✅ Solution 4: Test Webhook Endpoint Manually

Open PowerShell and test:

```powershell
$webhookUrl = "https://YOUR_NGROK_URL.ngrok-free.app/github-webhook/"
Invoke-WebRequest -Uri $webhookUrl -Method POST -ContentType "application/json" -Body '{"test":"data"}'
```

**Expected:**
- Status code: 200 or 403 (not 302)
- If 302, the URL is wrong

---

## 🔍 Common Issues

### Issue 1: Missing Trailing Slash

**Wrong:**
```
https://abc123.ngrok-free.app/github-webhook
```

**Correct:**
```
https://abc123.ngrok-free.app/github-webhook/
```

### Issue 2: Wrong Path

**Wrong:**
```
https://abc123.ngrok-free.app/webhook/
https://abc123.ngrok-free.app/jenkins-webhook/
```

**Correct:**
```
https://abc123.ngrok-free.app/github-webhook/
```

### Issue 3: HTTP Instead of HTTPS

**Wrong:**
```
http://abc123.ngrok-free.app/github-webhook/
```

**Correct:**
```
https://abc123.ngrok-free.app/github-webhook/
```

---

## 🧪 Step-by-Step Fix

1. **Get your ngrok URL:**
   - Look at ngrok terminal
   - Copy the HTTPS URL (e.g., `https://abc123.ngrok-free.app`)

2. **Build the webhook URL:**
   ```
   https://abc123.ngrok-free.app/github-webhook/
   ```
   - Add `/github-webhook/` at the end
   - Make sure there's a trailing slash

3. **Update GitHub webhook:**
   - Go to: https://github.com/MedHabibManai/IGL5-G5-achat/settings/hooks
   - Click on your webhook
   - Click "Edit"
   - Update Payload URL to the correct format
   - Click "Update webhook"

4. **Test again:**
   - GitHub will automatically test it
   - Should show ✅ green checkmark

---

## ✅ Verification Checklist

- [ ] ngrok is running (`ngrok http 8080`)
- [ ] Jenkins is accessible via ngrok URL
- [ ] Webhook URL ends with `/github-webhook/` (with trailing slash)
- [ ] Webhook URL uses `https://` (not `http://`)
- [ ] "GitHub hook trigger" is enabled in Jenkins
- [ ] Webhook shows ✅ in GitHub (not ❌)

---

## 🎯 Most Likely Fix

**The URL is probably missing the trailing slash or has the wrong path.**

Make sure it's exactly:
```
https://YOUR_NGROK_URL.ngrok-free.app/github-webhook/
```

Update it in GitHub and test again! 🚀

