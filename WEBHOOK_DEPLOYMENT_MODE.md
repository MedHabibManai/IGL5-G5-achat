# 🎯 Webhook Deployment Mode - What Gets Used?

## 📋 Current Default

When a webhook automatically triggers a build, Jenkins uses the **first choice** in the parameter list as the default.

**Current Jenkinsfile:**
```groovy
choices: ['NORMAL', 'CLEANUP_AND_DEPLOY', 'REUSE_INFRASTRUCTURE', 'EKS_ONLY']
```

**Default for webhook builds:** `NORMAL` ✅

---

## 🔍 What Each Mode Does

| Mode | What It Does | Speed | Use Case |
|------|--------------|-------|----------|
| **NORMAL** | Deploy fresh infrastructure | Slow | Full deployment |
| **CLEANUP_AND_DEPLOY** | Destroy old, then deploy new | Slowest | Clean slate |
| **REUSE_INFRASTRUCTURE** | Keep VPC/RDS, recreate EC2 only | **Fastest** | **Testing/Development** ⭐ |
| **EKS_ONLY** | Skip all except EKS | Fast | EKS testing only |

---

## 💡 Recommendation for Webhook Builds

For **automatic webhook builds**, I recommend using **`REUSE_INFRASTRUCTURE`** because:

- ✅ **Faster** - Only recreates EC2 instance, keeps VPC/RDS
- ✅ **More reliable** - Won't hit VPC limits
- ✅ **Better for testing** - Quick feedback on code changes
- ✅ **Cost-effective** - Doesn't recreate expensive resources

---

## ✅ How to Change the Default

### Option 1: Change Order in Jenkinsfile (Recommended)

Reorder the choices so `REUSE_INFRASTRUCTURE` is first:

```groovy
choices: ['REUSE_INFRASTRUCTURE', 'NORMAL', 'CLEANUP_AND_DEPLOY', 'EKS_ONLY']
```

Now webhook builds will use `REUSE_INFRASTRUCTURE` by default!

### Option 2: Keep NORMAL but Understand It

If you want to keep `NORMAL` as default:
- Webhook builds will do full deployment
- Slower but more thorough
- May fail if VPC limit is reached

---

## 🎯 My Recommendation

**Change the default to `REUSE_INFRASTRUCTURE`** for webhook builds:

1. It's faster (better for frequent pushes)
2. More reliable (won't hit AWS limits)
3. Still tests your code changes
4. You can still manually trigger with `NORMAL` when needed

---

## 📋 Summary

**Current:** Webhook uses `NORMAL` (first in list)
**Recommended:** Change to `REUSE_INFRASTRUCTURE` (faster for testing)

Would you like me to update the Jenkinsfile to use `REUSE_INFRASTRUCTURE` as the default for webhook builds? 🚀

