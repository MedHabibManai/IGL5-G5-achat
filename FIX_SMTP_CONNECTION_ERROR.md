# 🔧 Fix: SMTP Connection Error in Jenkins

## ❌ Current Error

```
Connection error sending email, retrying once more in 10 seconds...
Failed after second try sending email
```

## ✅ What's Working

- ✅ Email recipient is set correctly: `sinkingecstasies@gmail.com`
- ✅ Post section is running
- ✅ Email attempt is being made
- ✅ Code is working correctly

## ❌ What's NOT Working

- ❌ Jenkins cannot connect to SMTP server (smtp.gmail.com)
- ❌ This is a **network/configuration issue**, not a code issue

---

## 🔍 Step-by-Step Fix

### Step 1: Test SMTP Configuration in Jenkins

1. Go to **Manage Jenkins** → **Configure System**
2. Scroll to **Extended E-mail Notification**
3. Click **"Test configuration by sending test e-mail"**
4. Enter: `sinkingecstasies@gmail.com`
5. Click **Test**
6. **Check the result:**
   - ✅ If test email is received → SMTP works, but pipeline has network issue
   - ❌ If test fails → SMTP configuration is wrong

---

### Step 2: Verify SMTP Settings

**For Gmail (Port 587):**
```
SMTP server: smtp.gmail.com
SMTP Port: 587
☑️ Use SMTP Authentication
   User Name: sinkingecstasies@gmail.com
   Password: [Your 16-character App Password]
☑️ Use TLS
☐ Use SSL (unchecked)
```

**Double-check:**
- [ ] Port is exactly `587` (not 465, not 25)
- [ ] **Use TLS** is checked
- [ ] **Use SSL** is unchecked
- [ ] User Name is your full email address
- [ ] Password is the App Password (16 characters, no spaces)

---

### Step 3: Try Alternative Port (465)

If port 587 doesn't work, try port 465:

```
SMTP server: smtp.gmail.com
SMTP Port: 465
☑️ Use SMTP Authentication
   User Name: sinkingecstasies@gmail.com
   Password: [Your App Password]
☐ Use TLS (unchecked)
☑️ Use SSL (checked)
```

Then test again.

---

### Step 4: Check Network/Firewall

The connection error might be due to:

1. **Firewall blocking port 587**
   - Check if your network allows outbound SMTP
   - Try from a different network

2. **Jenkins container network issue**
   - If Jenkins is in Docker, check network settings
   - Verify Jenkins can reach `smtp.gmail.com`

3. **Corporate network restrictions**
   - Some networks block SMTP ports
   - Try using a VPN or different network

---

### Step 5: Verify Gmail App Password

1. Go to: https://myaccount.google.com/security
2. Check **App passwords** section
3. Verify you have an App Password for "Mail"
4. If not, generate a new one
5. Make sure you're using the App Password (not regular password)

---

### Step 6: Check Jenkins Logs

1. Go to **Manage Jenkins** → **System Log**
2. Look for email-related errors
3. Check for:
   - SSL/TLS handshake errors
   - Authentication failures
   - Connection timeouts

---

## 🧪 Testing Checklist

- [ ] Test SMTP configuration in Jenkins (Step 1)
- [ ] Verify SMTP settings match exactly (Step 2)
- [ ] Try port 465 if 587 doesn't work (Step 3)
- [ ] Check network/firewall (Step 4)
- [ ] Verify App Password is correct (Step 5)
- [ ] Check Jenkins logs for errors (Step 6)

---

## 📋 Common Issues & Solutions

| Error | Cause | Solution |
|-------|-------|----------|
| Connection timeout | Port blocked | Try port 465 with SSL |
| Authentication failed | Wrong password | Use App Password, not regular password |
| SSL exception | Wrong TLS/SSL | Port 587 = TLS only, Port 465 = SSL only |
| Connection refused | Firewall/Network | Check network, try different network |
| "Failed after second try" | Can't connect | Check SMTP server, port, and network |

---

## ✅ Success Indicators

You'll know it's fixed when:

1. **Test email in Jenkins succeeds** (Step 1)
2. **Console output shows NO "Connection error"**
3. **You receive the email notification**

---

## 🎯 Quick Test

1. Go to Jenkins → Configure System → Extended E-mail Notification
2. Click "Test configuration by sending test e-mail"
3. Enter your email
4. Click Test
5. **If this fails, fix the SMTP settings first**
6. **If this succeeds, the pipeline should work too**

---

The code is working correctly. The issue is the SMTP connection configuration in Jenkins. Fix the SMTP settings, and emails will work! 🎉

