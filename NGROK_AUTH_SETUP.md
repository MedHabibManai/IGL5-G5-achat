# 🔐 ngrok Authentication Setup

## ⚠️ ngrok Requires Account

ngrok now requires a free account to use. Here's how to set it up:

---

## ✅ Step 1: Sign Up for Free ngrok Account

1. Go to: https://dashboard.ngrok.com/signup
2. Sign up with:
   - Email address
   - Password
   - Or use Google/GitHub to sign in
3. Verify your email if needed

---

## ✅ Step 2: Get Your Authtoken

After signing up:

1. Go to: https://dashboard.ngrok.com/get-started/your-authtoken
2. You'll see your **authtoken** (looks like: `2abc123def456ghi789jkl012mno345pq_6r7s8t9u0v1w2x3y4z5`)
3. **Copy this token** - you'll need it in the next step

---

## ✅ Step 3: Configure ngrok with Your Token

Open PowerShell and run:

```powershell
ngrok config add-authtoken YOUR_AUTHTOKEN_HERE
```

Replace `YOUR_AUTHTOKEN_HERE` with the token you copied.

**Example:**
```powershell
ngrok config add-authtoken 2abc123def456ghi789jkl012mno345pq_6r7s8t9u0v1w2x3y4z5
```

**Expected output:**
```
Authtoken saved to configuration file: C:\Users\MSI\AppData\Local\ngrok\ngrok.yml
```

✅ **ngrok is now configured!**

---

## ✅ Step 4: Start ngrok

Now you can start ngrok:

```powershell
ngrok http 8080
```

**Expected output:**
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

---

## 🎯 Quick Summary

1. **Sign up**: https://dashboard.ngrok.com/signup
2. **Get authtoken**: https://dashboard.ngrok.com/get-started/your-authtoken
3. **Configure**: `ngrok config add-authtoken YOUR_TOKEN`
4. **Start**: `ngrok http 8080`
5. **Copy URL**: Use the HTTPS URL for GitHub webhook

---

## 💡 Alternative: Use ngrok Web Interface

After starting ngrok, you can also:
- Open: http://127.0.0.1:4040
- See all requests, inspect webhooks, and view logs

---

## 🚨 Troubleshooting

**Problem: "authtoken is required"**
- You need to sign up and add your authtoken first
- Follow Step 2 and Step 3 above

**Problem: "authtoken is invalid"**
- Make sure you copied the entire token
- Try getting a new token from the dashboard

**Problem: Can't access dashboard**
- Make sure you verified your email
- Try signing in again

---

Once you've added your authtoken, ngrok will work! 🚀

