# 🔧 Configure Extended E-mail Notification with OAuth2

## 🔍 What You're Seeing

In **Extended E-mail Notification**, you see:
- ✅ "Use OAuth2" option
- ❌ No "Use SMTP Authentication" option

This is because Extended E-mail Notification uses **OAuth2** for Gmail authentication, which is more secure than App Passwords.

---

## ✅ Solution: Configure OAuth2 for Gmail

### Option 1: Use OAuth2 (Recommended for Extended E-mail Notification)

OAuth2 is the modern way to authenticate with Gmail. Here's how to set it up:

#### Step 1: Create Google OAuth2 Credentials

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Enable **Gmail API**:
   - Go to **APIs & Services** → **Library**
   - Search for "Gmail API"
   - Click **Enable**

4. Create OAuth2 Credentials:
   - Go to **APIs & Services** → **Credentials**
   - Click **Create Credentials** → **OAuth client ID**
   - Application type: **Web application**
   - Name: **Jenkins Email**
   - Authorized redirect URIs: `http://localhost:8080/descriptorByName/hudson.plugins.emailext.ExtendedEmailPublisher/configure`
   - Click **Create**
   - **Copy the Client ID and Client Secret**

#### Step 2: Configure OAuth2 in Jenkins

1. Go to **Manage Jenkins** → **Configure System**
2. Scroll to **Extended E-mail Notification**
3. Configure:
   ```
   SMTP server: smtp.gmail.com
   SMTP Port: 587
   ☑️ Use OAuth2
      OAuth Client ID: [Paste Client ID from Google]
      OAuth Client Secret: [Paste Client Secret from Google]
      OAuth Token: [Will be generated after first use]
   ☑️ Use TLS
   ☐ Use SSL (unchecked)
   ```
4. Click **Test OAuth2 Connection** or **Save**
5. You'll be redirected to Google to authorize Jenkins
6. After authorization, the OAuth Token will be saved automatically

---

### Option 2: Use Default E-mail Notification (Fallback)

If OAuth2 setup is too complex, you can configure the **default E-mail Notification** section instead, but you'll need to modify the code to use the default emailer instead of `emailext()`.

However, **Option 1 (OAuth2) is recommended** because:
- ✅ More secure
- ✅ Works with Extended E-mail Notification
- ✅ No need for App Passwords
- ✅ Better for production use

---

## 🔄 Alternative: Use mail() Instead of emailext()

If OAuth2 setup is not possible, we can modify the code to use the default `mail()` function which uses the "E-mail Notification" settings:

```groovy
// Instead of emailext(), use mail()
mail(
    to: config.to,
    subject: emailSubject,
    body: emailBody,
    mimeType: 'text/html'
)
```

But this has limitations:
- ❌ No HTML formatting support
- ❌ No attachment support
- ❌ Less features than emailext()

---

## 📋 Quick Setup Checklist for OAuth2

- [ ] Create Google Cloud project
- [ ] Enable Gmail API
- [ ] Create OAuth2 credentials (Client ID & Secret)
- [ ] Configure Extended E-mail Notification with OAuth2
- [ ] Authorize Jenkins in Google
- [ ] Test email sending
- [ ] Run pipeline and verify emails work

---

## 🎯 Recommended Approach

**Use OAuth2** - It's the proper way to configure Extended E-mail Notification with Gmail. The setup is a bit more involved, but it's more secure and works better with modern Gmail authentication.

---

## 💡 Quick Alternative (If OAuth2 is Too Complex)

If you want a quick fix without OAuth2 setup, I can modify the code to use the default `mail()` function which will use your "E-mail Notification" settings (where SMTP Authentication works).

Let me know which approach you prefer!

