# 📧 Quick Email Setup for Your Jenkins Configuration

Based on your current Jenkins SMTP settings screen, here's exactly what to configure:

## ✅ Configuration Steps

### 1. SMTP Server
- **Value**: `smtp.gmail.com` ✅ (Already set correctly)

### 2. SMTP Port
- **Value**: `587` ✅ (Already set correctly)

### 3. Authentication Settings
- ☑️ **Check "Use SMTP Authentication ?"**
  - After checking, you'll see fields for:
    - **User Name**: `your-email@gmail.com` (your full Gmail address)
    - **Password**: Your Gmail App Password (see below)

### 4. Security Settings
- ☑️ **Check "Use TLS"** (for port 587)
- ☐ **Leave "Use SSL" unchecked** (only for port 465)

### 5. Reply-To Address (Optional but Recommended)
- **Value**: `your-email@gmail.com`

### 6. Charset
- **Value**: `UTF-8` ✅ (Already set correctly)

### 7. Test Configuration
- ☑️ **Check "Test configuration by sending test e-mail"**
- Enter your email address
- Click **Test** button
- Check your inbox for the test email

---

## 🔑 Getting Gmail App Password

Since you're using Gmail, you need an **App Password** (not your regular password):

### Steps to Get Gmail App Password:

1. Go to your Google Account: https://myaccount.google.com/
2. Click **Security** (left sidebar)
3. Under "Signing in to Google":
   - Make sure **2-Step Verification** is enabled
   - If not, enable it first
4. Scroll down to **App passwords**
5. Click **App passwords**
6. Select app: **Mail**
7. Select device: **Other (Custom name)**
8. Enter: **Jenkins**
9. Click **Generate**
10. Copy the 16-character password (it will look like: `abcd efgh ijkl mnop`)
11. Use this password in Jenkins (remove spaces: `abcdefghijklmnop`)

---

## 📝 Complete Configuration Summary

```
SMTP server: smtp.gmail.com
SMTP Port: 587
☑️ Use SMTP Authentication
   User Name: your-email@gmail.com
   Password: [16-character App Password]
☑️ Use TLS
☐ Use SSL (unchecked)
Reply-To Address: your-email@gmail.com
Charset: UTF-8
```

---

## ✅ After Configuration

1. Click **Save** at the bottom
2. Go to your Jenkins job → **Configure**
3. Add environment variable:
   - **Name**: `EMAIL_RECIPIENTS`
   - **Value**: `your-email@gmail.com`
4. Save the job
5. Run a build to test email notifications

---

## 🧪 Testing

1. After saving, use the "Test configuration" feature
2. Enter your email address
3. Click **Test**
4. Check your inbox (and spam folder)
5. You should receive a test email from Jenkins

---

## ❓ Troubleshooting

**"Authentication failed"**
- Make sure you're using App Password, not regular password
- Verify 2-Step Verification is enabled
- Check username is your full email address

**"Connection timeout"**
- Verify port 587 is correct
- Check firewall isn't blocking SMTP
- Try port 465 with SSL instead (check "Use SSL", uncheck "Use TLS")

**"Test email not received"**
- Check spam/junk folder
- Verify email address is correct
- Wait a few minutes (Gmail can delay)

---

Your email notifications will work once these settings are configured! 🎉

