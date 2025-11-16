# 🔧 Fix: Pipeline Email Fails But Test Works

## ✅ What's Working

- ✅ SMTP test email works (configuration is correct)
- ✅ Email recipient is set correctly
- ✅ Post section runs correctly

## ❌ What's NOT Working

- ❌ Pipeline emails fail with "Connection error"
- ❌ This happens even though test email works

---

## 🔍 Root Cause

If the **test email works** but **pipeline emails fail**, there are usually two possible causes:

### Cause 1: Two Email Configurations

Jenkins has **TWO** email configuration sections:
1. **"E-mail Notification"** (Default) - Used by some plugins
2. **"Extended E-mail Notification"** - Used by `emailext()` in pipelines

The test button might be using one, while `emailext()` uses the other!

### Cause 2: Network Context Difference

The pipeline might run in a different network context (Docker container) that has different network access than the Jenkins UI.

---

## ✅ Solution 1: Configure BOTH Email Sections

### Step 1: Configure "Extended E-mail Notification"

1. Go to **Manage Jenkins** → **Configure System**
2. Scroll to **"Extended E-mail Notification"** section
3. Make sure it's configured exactly like your test:
   ```
   SMTP server: smtp.gmail.com
   SMTP Port: 587
   ☑️ Use SMTP Authentication
      User Name: sinkingecstasies@gmail.com
      Password: [Your App Password]
   ☑️ Use TLS
   ☐ Use SSL (unchecked)
   ```
4. **Save** the configuration

### Step 2: Check "E-mail Notification" Section

1. In the same **Configure System** page
2. Scroll to **"E-mail Notification"** section (above Extended)
3. Configure it with the **same settings**:
   ```
   SMTP server: smtp.gmail.com
   ☑️ Use SMTP Authentication
      User Name: sinkingecstasies@gmail.com
      Password: [Your App Password]
   ☑️ Use TLS
   SMTP Port: 587
   ```
4. **Save** the configuration

---

## ✅ Solution 2: Check Email Extension Plugin Settings

1. Go to **Manage Jenkins** → **Configure System**
2. Scroll to **"Extended E-mail Notification"**
3. Look for **"Advanced"** settings
4. Check these settings:
   - **SMTP Port**: Should be `587`
   - **Use SSL**: Should be **unchecked** (for port 587)
   - **Use TLS**: Should be **checked** (for port 587)
   - **SMTP Username**: Your full email
   - **SMTP Password**: Your App Password

5. Scroll down and check:
   - **"Default Triggers"** - Make sure it's configured
   - **"Default Content"** - Can leave as default

---

## ✅ Solution 3: Add Explicit SMTP Settings to emailext()

Sometimes `emailext()` needs explicit SMTP settings. Let's update the code to specify SMTP settings directly:

```groovy
emailext(
    subject: emailSubject,
    body: emailBody,
    mimeType: 'text/html',
    to: config.to,
    attachLog: config.attachLog,
    compressLog: true,
    // Add explicit SMTP settings
    replyTo: env.EMAIL_RECIPIENTS ?: '',
    from: env.EMAIL_RECIPIENTS ?: ''
)
```

---

## 🧪 Testing Steps

1. **Configure both email sections** (Solution 1)
2. **Verify Extended E-mail Notification settings** (Solution 2)
3. **Run a build** and check console output
4. **Check if connection error is gone**

---

## 📋 Checklist

- [ ] "Extended E-mail Notification" is configured
- [ ] "E-mail Notification" (default) is also configured
- [ ] Both have the same SMTP settings
- [ ] Both use the same App Password
- [ ] Test email works (you confirmed this ✅)
- [ ] Run a build and check console

---

## 🎯 Most Likely Fix

Since the test works, **configure the "E-mail Notification" section** (the default one) with the same settings. The `emailext()` function might be falling back to the default email configuration if Extended E-mail Notification isn't fully configured.

---

## 💡 Quick Test

1. Go to **Manage Jenkins** → **Configure System**
2. Configure **both** email sections with identical settings
3. Save
4. Run a build
5. Check console - connection error should be gone!

The key is: **Both email configuration sections need to be set up identically!**

