# 🔧 Fix: SSL Exception in Jenkins Email Configuration

## ❌ Error You're Seeing

```
javax.net.ssl.SSLException: Unsupported or unrecognized SSL message
Could not connect to SMTP host: smtp.gmail.com, port: 587
```

## ✅ Solution: Correct SSL/TLS Settings

The issue is with your SSL/TLS checkbox configuration. Here's the **exact** setup for Gmail:

### For Port 587 (Recommended):
```
SMTP server: smtp.gmail.com
SMTP Port: 587
☑️ Use SMTP Authentication
   User Name: your-email@gmail.com
   Password: [Your App Password]
☑️ Use TLS          ← CHECK THIS
☐ Use SSL          ← UNCHECK THIS (Important!)
```

### Alternative: Port 465 (If 587 doesn't work)
```
SMTP server: smtp.gmail.com
SMTP Port: 465
☑️ Use SMTP Authentication
   User Name: your-email@gmail.com
   Password: [Your App Password]
☐ Use TLS          ← UNCHECK THIS
☑️ Use SSL          ← CHECK THIS (for port 465)
```

---

## 🔍 Step-by-Step Fix

### Option 1: Use Port 587 with TLS (Recommended)

1. **SMTP Port**: Make sure it's `587`
2. **Use TLS**: ✅ **CHECK this box**
3. **Use SSL**: ❌ **UNCHECK this box** (This is critical!)
4. **Use SMTP Authentication**: ✅ Check this
5. Enter your credentials
6. Click **Test configuration**

### Option 2: Use Port 465 with SSL (Alternative)

If port 587 still doesn't work:

1. **SMTP Port**: Change to `465`
2. **Use SSL**: ✅ **CHECK this box**
3. **Use TLS**: ❌ **UNCHECK this box**
4. **Use SMTP Authentication**: ✅ Check this
5. Enter your credentials
6. Click **Test configuration**

---

## 🎯 Most Common Issue

**The problem is usually:**
- Both SSL and TLS are checked at the same time ❌
- Or SSL is checked when using port 587 ❌

**The fix:**
- Port 587 = TLS only (uncheck SSL)
- Port 465 = SSL only (uncheck TLS)

---

## 📋 Complete Configuration Checklist

For **Port 587** (Recommended):
- [ ] SMTP server: `smtp.gmail.com`
- [ ] SMTP Port: `587`
- [ ] Use SMTP Authentication: ✅ Checked
- [ ] User Name: `your-email@gmail.com`
- [ ] Password: `[16-character App Password]`
- [ ] Use TLS: ✅ Checked
- [ ] Use SSL: ❌ Unchecked
- [ ] Reply-To Address: `your-email@gmail.com` (optional)

---

## 🧪 Test Again

After making these changes:

1. Click **Save** at the bottom
2. Go back to the email configuration
3. Check "Test configuration by sending test e-mail"
4. Enter your email address
5. Click **Test**
6. Check your inbox!

---

## ❓ Still Not Working?

If you still get errors, try:

1. **Clear browser cache** and refresh Jenkins
2. **Restart Jenkins** (if you have access)
3. **Try port 465 with SSL** instead of 587 with TLS
4. **Check firewall** - make sure port 587 or 465 isn't blocked
5. **Verify App Password** - make sure you're using the 16-character App Password, not your regular password

---

## 🔐 Quick Reference

| Port | Use TLS | Use SSL | When to Use |
|------|---------|---------|-------------|
| 587  | ✅ Yes  | ❌ No   | Recommended (STARTTLS) |
| 465  | ❌ No   | ✅ Yes  | Alternative (SSL/TLS) |
| 25   | ❌ No   | ❌ No   | Not recommended (often blocked) |

---

The key is: **Port 587 needs TLS, NOT SSL!** Make sure SSL is unchecked when using port 587.

