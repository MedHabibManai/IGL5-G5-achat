# 🔑 How to Get a Gmail App Password for Jenkins

## What is a Gmail App Password?

A **Gmail App Password** is a special 16-character password that Google generates for applications (like Jenkins) to access your Gmail account. You need this when you have **2-Step Verification** enabled on your Google account.

**Why?** Google doesn't allow regular passwords for SMTP when 2FA is enabled - you must use an App Password instead.

---

## 📋 Step-by-Step Instructions

### Step 1: Enable 2-Step Verification (If Not Already Enabled)

1. Go to: https://myaccount.google.com/security
2. Scroll down to **"How you sign in to Google"**
3. Find **"2-Step Verification"**
4. If it says **"Off"**, click on it and follow the steps to enable it
   - You'll need your phone to verify
5. If it's already **"On"**, you're good to go!

### Step 2: Generate App Password

1. Still on the Security page: https://myaccount.google.com/security
2. Scroll down to **"2-Step Verification"** section
3. Click on **"App passwords"** (it's a link below the 2-Step Verification toggle)
   - If you don't see it, make sure 2-Step Verification is enabled first
4. You might need to sign in again for security
5. You'll see a page titled **"App passwords"**

### Step 3: Create App Password for Jenkins

1. On the App passwords page, you'll see a dropdown menu
2. Select:
   - **App**: Choose **"Mail"** from the dropdown
3. Select:
   - **Device**: Choose **"Other (Custom name)"**
4. Type: **Jenkins** (or any name you prefer)
5. Click **"Generate"** button

### Step 4: Copy the Password

1. Google will show you a **16-character password**
2. It will look like: `abcd efgh ijkl mnop` (with spaces)
3. **Copy this password** (you can click the copy icon)
4. **Important**: You won't be able to see it again, so copy it now!
5. When you paste it into Jenkins, **remove the spaces**:
   - Example: `abcdefghijklmnop`

---

## 🔧 Using the App Password in Jenkins

1. In Jenkins SMTP configuration:
   - **User Name**: Your full Gmail address (e.g., `yourname@gmail.com`)
   - **Password**: Paste the 16-character App Password (without spaces)

2. Example:
   ```
   User Name: john.doe@gmail.com
   Password: abcd efgh ijkl mnop  →  abcd efgh ijkl mnop (remove spaces)
   ```

---

## 📸 Visual Guide

**What you'll see:**

```
┌─────────────────────────────────────┐
│  App passwords                      │
├─────────────────────────────────────┤
│  Select app: [Mail ▼]              │
│  Select device: [Other ▼]          │
│  Name: [Jenkins        ]           │
│                                     │
│  [Generate]                         │
└─────────────────────────────────────┘

After clicking Generate:

┌─────────────────────────────────────┐
│  Your app password is:              │
│                                     │
│  abcd efgh ijkl mnop                │
│                                     │
│  [Copy]                             │
│                                     │
│  Note: You won't be able to see     │
│  this password again.              │
└─────────────────────────────────────┘
```

---

## ✅ Quick Checklist

- [ ] 2-Step Verification is enabled on your Google account
- [ ] You can see "App passwords" option in Security settings
- [ ] Generated an App Password for "Mail" app
- [ ] Named it "Jenkins" (or similar)
- [ ] Copied the 16-character password
- [ ] Removed spaces from the password
- [ ] Used it in Jenkins SMTP configuration

---

## ❓ Common Questions

**Q: Do I need 2-Step Verification enabled?**  
A: Yes! App passwords only work when 2-Step Verification is enabled.

**Q: Can I use my regular Gmail password?**  
A: No, if 2FA is enabled, you must use an App Password.

**Q: What if I don't have 2-Step Verification?**  
A: You can either:
   - Enable 2FA and use App Password (recommended for security)
   - Or disable 2FA temporarily (not recommended)

**Q: Can I reuse the same App Password?**  
A: Yes, you can use the same App Password for multiple Jenkins instances.

**Q: What if I lose the App Password?**  
A: Just generate a new one! The old one will stop working, so update Jenkins with the new password.

**Q: Is this secure?**  
A: Yes! App Passwords are more secure because:
   - They're specific to one app
   - You can revoke them anytime
   - They don't give full account access

---

## 🎯 Next Steps

After getting your App Password:

1. Go back to Jenkins SMTP configuration
2. Check "Use SMTP Authentication"
3. Enter your Gmail address as User Name
4. Paste the App Password (without spaces)
5. Check "Use TLS"
6. Click "Test configuration"
7. Check your email for the test message

That's it! Your Jenkins email notifications should work now! 🎉

