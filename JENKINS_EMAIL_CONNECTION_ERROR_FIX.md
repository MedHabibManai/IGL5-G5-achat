# 🔧 Fix: Email Connection Error in Jenkins

## ❌ Error You're Seeing

```
Connection error sending email, retrying once more in 10 seconds...
Failed after second try sending email
```

But then it says: "Email notification sent successfully" (which is misleading - the email actually FAILED)

---

## 🔍 Root Cause

The email is **NOT being sent** due to an SMTP connection error. The `emailext()` function doesn't throw exceptions, so the code continues and prints "success" even though the email failed.

---

## ✅ Solutions

### Solution 1: Verify SMTP Configuration

1. Go to **Manage Jenkins** → **Configure System**
2. Scroll to **Extended E-mail Notification**
3. Verify these settings:

**For Gmail (Port 587):**
```
SMTP server: smtp.gmail.com
SMTP Port: 587
☑️ Use SMTP Authentication
   User Name: your-email@gmail.com
   Password: [16-character App Password]
☑️ Use TLS
☐ Use SSL (unchecked)
```

4. Click **"Test configuration by sending test e-mail"**
5. Enter your email and click **Test**
6. Check if test email is received

---

### Solution 2: Check Network/Firewall

The connection error might be due to:

1. **Firewall blocking SMTP port 587**
   - Check if your network/firewall allows outbound connections on port 587
   - Try port 465 with SSL instead

2. **Jenkins container/network restrictions**
   - If Jenkins is in Docker, check network settings
   - Verify Jenkins can reach smtp.gmail.com

3. **Corporate network restrictions**
   - Some networks block SMTP
   - Try from a different network or use a VPN

---

### Solution 3: Try Alternative Port (465 with SSL)

If port 587 doesn't work:

1. Go to **Manage Jenkins** → **Configure System**
2. **Extended E-mail Notification** section:
   ```
   SMTP server: smtp.gmail.com
   SMTP Port: 465
   ☑️ Use SMTP Authentication
   ☐ Use TLS (uncheck)
   ☑️ Use SSL (check this for port 465)
   ```
3. Test again

---

### Solution 4: Check Jenkins Logs

1. Go to **Manage Jenkins** → **System Log**
2. Look for email-related errors
3. Check for SSL/TLS handshake errors
4. Look for authentication failures

---

## 🧪 Testing Steps

1. **Test SMTP Configuration First:**
   - Use the "Test configuration" button in Jenkins
   - This will show you the exact error

2. **Check Console Output:**
   - Look for "Connection error" messages
   - Note the exact error message

3. **Verify Credentials:**
   - Make sure you're using Gmail App Password (not regular password)
   - Verify 2-Step Verification is enabled
   - Regenerate App Password if needed

---

## 📋 Common Issues & Fixes

| Error | Cause | Fix |
|-------|-------|-----|
| Connection timeout | Port blocked | Try port 465 with SSL |
| Authentication failed | Wrong password | Use App Password, not regular password |
| SSL exception | Wrong TLS/SSL settings | Port 587 = TLS only, Port 465 = SSL only |
| Connection refused | Firewall/Network | Check network settings, try different network |

---

## ✅ After Fixing

Once SMTP test succeeds:

1. Run a build
2. Check console output - should NOT see "Connection error"
3. Check email inbox
4. You should receive the notification

---

## 🔍 How to Verify Email Was Actually Sent

**Good signs:**
- No "Connection error" in console
- No "Failed after second try" message
- Console shows email was sent

**Bad signs:**
- "Connection error sending email"
- "Failed after second try sending email"
- No email received (even after checking spam)

---

The key is to fix the SMTP connection error first, then emails will work!

