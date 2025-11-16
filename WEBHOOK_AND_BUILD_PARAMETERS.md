# 🔧 Webhook Events vs Build Parameters

## ✅ Short Answer

**No, it doesn't matter!** The webhook events and build parameters work independently.

---

## 🔍 How They Work Together

### Webhook Events (GitHub Side)
- **Purpose**: Tells GitHub **when** to send the webhook
- **Options**: Push, Pull Request, etc.
- **Example**: "Just the push event" = send webhook when code is pushed

### Build Parameters (Jenkins Side)
- **Purpose**: Tells Jenkins **how** to run the build
- **Example**: `DEPLOYMENT_MODE` = NORMAL, CLEANUP_AND_DEPLOY, etc.
- **Default**: Jenkins uses default values when webhook triggers the build

---

## 📋 What Happens When Webhook Triggers Build

1. **GitHub sends webhook** (on push event)
2. **Jenkins receives webhook** and starts build
3. **Jenkins uses default parameter values** (or you can configure specific values)

---

## ⚙️ Configure Default Parameter Values

### Option 1: Set Defaults in Jenkinsfile

Your Jenkinsfile already has defaults:

```groovy
parameters {
    choice(
        name: 'DEPLOYMENT_MODE',
        choices: ['NORMAL', 'CLEANUP_AND_DEPLOY', 'REUSE_INFRASTRUCTURE', 'EKS_ONLY'],
        // First choice is the default
    )
}
```

The **first choice** (`NORMAL`) is the default.

### Option 2: Configure in Jenkins Job Settings

1. Go to Jenkins → Your Job → Configure
2. Scroll to **"Build with Parameters"** section
3. Set default values for each parameter
4. When webhook triggers, it uses these defaults

---

## 🎯 Recommended Webhook Event Settings

For your use case, I recommend:

**Which events would you like to trigger this webhook?**
- ☑️ **Just the push event** ← **Recommended**

**Why?**
- Triggers on every `git push`
- Simple and reliable
- Works with your build parameters

**Alternative (if you want PR builds too):**
- ☑️ **Let me select individual events**
  - ☑️ Pushes
  - ☑️ Pull requests (optional)

---

## 💡 How It Works in Practice

### Scenario 1: Webhook Triggers Build

```
You: git push
  ↓
GitHub: Sends webhook (push event)
  ↓
Jenkins: Receives webhook, starts build
  ↓
Jenkins: Uses default DEPLOYMENT_MODE = NORMAL
  ↓
Build runs with default parameters
```

### Scenario 2: Manual Build with Parameters

```
You: Click "Build with Parameters" in Jenkins
  ↓
Jenkins: Shows parameter selection
  ↓
You: Choose DEPLOYMENT_MODE = REUSE_INFRASTRUCTURE
  ↓
Build runs with your selected parameters
```

---

## ✅ Summary

- **Webhook events**: Choose "Just the push event" ✅
- **Build parameters**: Don't affect webhook configuration ✅
- **Default values**: Jenkins will use defaults when webhook triggers ✅
- **Manual builds**: You can still choose parameters manually ✅

---

## 🎯 What to Select in GitHub

**Which events would you like to trigger this webhook?**
- ☑️ **Just the push event** ← **Select this**

This is the simplest and most common setup. Your build parameters will work fine with this!

---

**Bottom line:** Select "Just the push event" and your build parameters will work perfectly! 🚀

