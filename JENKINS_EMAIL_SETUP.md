# 📧 Jenkins Email Notification Setup Guide

This guide will help you configure email notifications for your Jenkins pipeline.

## 📋 Prerequisites

1. **Email Extension Plugin** must be installed in Jenkins
2. Access to Jenkins **Manage Jenkins** → **Configure System**
3. SMTP server credentials (Gmail, Outlook, or your organization's SMTP server)

---

## 🔧 Step 1: Install Email Extension Plugin

1. Go to **Manage Jenkins** → **Manage Plugins**
2. Click on **Available** tab
3. Search for **"Email Extension"** or **"Email Extension Plugin"**
4. Check the box and click **Install without restart** (or **Download now and install after restart**)
5. Wait for installation to complete
6. Restart Jenkins if prompted

---

## ⚙️ Step 2: Configure SMTP Server in Jenkins

1. Go to **Manage Jenkins** → **Configure System**
2. Scroll down to **Extended E-mail Notification** section
3. Configure the following settings:

### SMTP Server Configuration

**For Gmail:**
```
SMTP server: smtp.gmail.com
SMTP Port: 587
Use SSL: ☑️ (checked)
Use TLS: ☑️ (checked)
```

**For Outlook/Office 365:**
```
SMTP server: smtp.office365.com
SMTP Port: 587
Use SSL: ☐ (unchecked)
Use TLS: ☑️ (checked)
```

**For Custom SMTP:**
```
SMTP server: [your-smtp-server]
SMTP Port: [your-port] (usually 25, 465, or 587)
Use SSL: [check if required]
Use TLS: [check if required]
```

### Authentication

- **User Name**: Your email address (e.g., `your-email@gmail.com`)
- **Password**: Your email password or App Password (for Gmail, use App Password)

**For Gmail App Password:**
1. Go to Google Account → Security
2. Enable 2-Step Verification
3. Go to App Passwords
4. Generate a new app password for "Mail"
5. Use this 16-character password in Jenkins

### Default Settings

- **Default Subject**: `Build ${BUILD_STATUS}: ${PROJECT_NAME} - Build #${BUILD_NUMBER}`
- **Default Content**: Leave as default or customize
- **Default Recipients**: Leave empty (will be set per job)
- **Reply To**: Your email address

4. Click **Test configuration by sending test e-mail**
5. Enter your email address and click **Test**
6. Check your inbox for the test email
7. Click **Save** at the bottom of the page

---

## 📝 Step 3: Configure Email Recipients for Your Job

You have two options:

### Option A: Set in Jenkins Job Configuration (Recommended)

1. Go to your Jenkins job
2. Click **Configure**
3. Scroll to **Build Environment** section
4. Add a new environment variable:
   - **Name**: `EMAIL_RECIPIENTS`
   - **Value**: `your-email@example.com,another-email@example.com`
5. Click **Save**

### Option B: Set in Jenkinsfile (Global)

Edit the `Jenkinsfile` and update the `EMAIL_RECIPIENTS` environment variable:

```groovy
environment {
    // ... other variables ...
    EMAIL_RECIPIENTS = 'your-email@example.com,team@example.com'
}
```

---

## 🎨 Step 4: Customize Email Templates (Optional)

The email notification stage (`jenkins/stages/sendEmailNotification.groovy`) includes:

- ✅ HTML formatted emails
- ✅ Build status with color coding
- ✅ Deployment information (URLs, IPs, Instance IDs)
- ✅ Quick links to build console
- ✅ Build duration and metadata

You can customize the email template by editing:
```
jenkins/stages/sendEmailNotification.groovy
```

---

## 🧪 Step 5: Test Email Notifications

1. Trigger a build (success or failure)
2. Check your email inbox
3. You should receive an email with:
   - Build status
   - Build information
   - Deployment URLs (if deployment succeeded)
   - Links to build console

---

## 📧 Email Notification Features

### What You'll Receive:

**On Success:**
- ✅ Green header with success message
- Application URL
- Health check URL
- Swagger UI link
- Instance details

**On Failure:**
- ❌ Red header with failure message
- Error information
- Link to console output
- Build logs attached

**On Unstable:**
- ⚠️ Yellow header with warning
- Test results summary
- Link to test reports

---

## 🔍 Troubleshooting

### Email Not Sending?

1. **Check Plugin Installation:**
   - Go to **Manage Jenkins** → **Manage Plugins** → **Installed**
   - Verify "Email Extension Plugin" is installed

2. **Check SMTP Configuration:**
   - Verify SMTP server and port are correct
   - Check SSL/TLS settings match your email provider
   - Test configuration using "Test configuration" button

3. **Check Credentials:**
   - For Gmail: Use App Password, not regular password
   - Verify username is your full email address
   - Check if 2FA is enabled (requires App Password)

4. **Check Email Recipients:**
   - Verify `EMAIL_RECIPIENTS` is set in job configuration
   - Check email addresses are valid and comma-separated

5. **Check Jenkins Logs:**
   - Go to **Manage Jenkins** → **System Log**
   - Look for email-related errors

### Common Issues:

**"Authentication failed"**
- Use App Password for Gmail
- Check username/password are correct
- Verify SMTP server allows authentication

**"Connection timeout"**
- Check firewall settings
- Verify SMTP port is correct
- Check if your network blocks SMTP

**"No recipients configured"**
- Set `EMAIL_RECIPIENTS` environment variable
- Check job configuration

---

## 📚 Additional Resources

- [Email Extension Plugin Documentation](https://plugins.jenkins.io/email-ext/)
- [Gmail App Passwords Guide](https://support.google.com/accounts/answer/185833)
- [Jenkins Email Configuration](https://www.jenkins.io/doc/book/system-administration/email/)

---

## ✅ Quick Checklist

- [ ] Email Extension Plugin installed
- [ ] SMTP server configured in Jenkins
- [ ] Test email sent successfully
- [ ] `EMAIL_RECIPIENTS` set in job configuration
- [ ] Build triggered and email received

---

## 🎯 Next Steps

After configuring email notifications:

1. Test with a successful build
2. Test with a failed build
3. Customize email template if needed
4. Add more recipients if needed
5. Set up email filters in your inbox for Jenkins notifications

Your Jenkins pipeline will now send email notifications automatically! 🎉

