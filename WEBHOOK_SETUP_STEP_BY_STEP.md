# 📋 Webhook Setup - Step by Step with Screenshots

## ✅ Step 1: Verify Source Code Management (What You're Looking At)

In the Jenkins job configuration page you're viewing:

### 1.1 Pipeline Definition
- ✅ **"Pipeline script from SCM"** should be selected (looks correct in your screenshot)

### 1.2 SCM Configuration
- ✅ **SCM**: Git (looks correct)
- ✅ **Repository URL**: `https://github.com/MedHabibManai/IGL5-G5-achat.git` (correct!)
- ⚠️ **Credentials**: Currently "- none -" (this is OK if the repo is public)

### 1.3 Branch Specification
- **Branches to build**: You need to specify your branch here!
  - Enter: `*/MohamedHabibManai-GL5-G5-Produit`
  - Or: `*/main` if you want to build main branch
  - Or: `**` to build all branches

**Click "Advanced" if you need to:**
- Specify a different branch
- Add additional branches
- Configure submodule options

---

## ✅ Step 2: Enable Build Trigger (IMPORTANT!)

Scroll down to find the **"Build Triggers"** section (below Source Code Management):

### What to Check:
- ☑️ **"GitHub hook trigger for GITScm polling"**

This is the key setting that allows Jenkins to receive webhooks from GitHub!

**If you don't see this option:**
1. Make sure GitHub Plugin is installed
2. Go to: Manage Jenkins → Manage Plugins → Installed
3. Search for "GitHub Plugin"
4. Install if missing

---

## ✅ Step 3: Save Configuration

1. Scroll to the bottom
2. Click **"Save"**
3. Jenkins is now ready to receive webhooks!

---

## 📋 Complete Checklist

In the configuration page you're viewing:

- [ ] **Pipeline Definition**: "Pipeline script from SCM" ✅ (you have this)
- [ ] **SCM**: Git ✅ (you have this)
- [ ] **Repository URL**: Correct ✅ (you have this)
- [ ] **Branches to build**: Set to your branch (e.g., `*/MohamedHabibManai-GL5-G5-Produit`)
- [ ] **Build Triggers**: "GitHub hook trigger for GITScm polling" ☑️ (check this!)
- [ ] **Save**: Click Save button

---

## 🎯 Next Steps After This

1. **Set the branch** in "Branches to build" field
2. **Enable** "GitHub hook trigger for GITScm polling" in Build Triggers section
3. **Save** the configuration
4. **Make Jenkins accessible** (use ngrok or public IP)
5. **Configure GitHub webhook** (see WEBHOOK_QUICK_SETUP.md)

---

## 💡 Quick Reference

**Branch to build:**
```
*/MohamedHabibManai-GL5-G5-Produit
```

**Build Trigger to enable:**
```
☑️ GitHub hook trigger for GITScm polling
```

That's it! Once you enable the trigger and save, Jenkins will be ready for webhooks! 🚀

