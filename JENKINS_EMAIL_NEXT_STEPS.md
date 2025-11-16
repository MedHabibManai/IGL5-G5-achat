# 📧 Next Steps: Complete Email Notification Setup

## ✅ Step 1: Configure Email Recipients

You need to tell Jenkins where to send the emails. You have two options:

### Option A: Set in Jenkins Job Configuration (Recommended)

1. Go to your Jenkins job (the pipeline)
2. Click **Configure**
3. Scroll down to find **"Build Environment"** section
4. Look for **"Use secret text(s) or file(s)"** or **"Environment variables"**
5. Add a new environment variable:
   - **Name**: `EMAIL_RECIPIENTS`
   - **Value**: `your-email@gmail.com` (or multiple emails: `email1@gmail.com,email2@gmail.com`)
6. Click **Save**

### Option B: Set in Jenkinsfile (Global for all builds)

Edit the `Jenkinsfile` and update line 99:

```groovy
EMAIL_RECIPIENTS = 'your-email@gmail.com'  // Change this to your email
```

Or for multiple recipients:
```groovy
EMAIL_RECIPIENTS = 'your-email@gmail.com,team-member@example.com'
```

---

## 🧪 Step 2: Test Email Notifications

1. **Trigger a build** (success or failure)
2. **Check your email inbox** after the build completes
3. You should receive an email with:
   - ✅ Build status (Success/Failure/Unstable)
   - 📊 Build information (duration, build number, etc.)
   - 🌐 Deployment URLs (if deployment succeeded)
   - 🔗 Links to build console

---

## 📧 What You'll Receive

### On Success:
- ✅ Green header
- Application URL
- Health check URL
- Swagger UI link
- Instance details (IP, Instance ID)
- Build logs attached

### On Failure:
- ❌ Red header
- Error information
- Link to console output
- Build logs attached
- Failure details

### On Unstable:
- ⚠️ Yellow header
- Test results summary
- Link to test reports

---

## 🎯 Step 3: Commit Email Notification Code

The email notification code is ready. Let's commit it:

```bash
git add Jenkinsfile jenkins/stages/sendEmailNotification.groovy
git commit -m "Add email notification support for build status"
git push
```

---

## 🔧 Step 4: Customize (Optional)

You can customize the email template by editing:
```
jenkins/stages/sendEmailNotification.groovy
```

The template includes:
- HTML formatting
- Color-coded status
- Deployment information
- Build metadata

---

## ✅ Quick Checklist

- [x] SMTP server configured ✅
- [x] Test email sent successfully ✅
- [ ] Email recipients configured (EMAIL_RECIPIENTS)
- [ ] Build triggered to test notifications
- [ ] Email received and verified
- [ ] Code committed to repository

---

## 🎉 You're All Set!

Once you configure `EMAIL_RECIPIENTS`, your Jenkins pipeline will automatically send email notifications on every build completion!

**Next build will send you an email!** 📧

