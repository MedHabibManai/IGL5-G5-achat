# 🎯 Specify Deployment Mode in Commit Messages

<!-- Test commit for REUSE_INFRASTRUCTURE mode -->

## ✅ How It Works

You can now specify the deployment mode **in your commit message** when pushing! Jenkins will automatically detect it and use that mode instead of the default.

---

## 📝 Usage Examples

### Example 1: Use REUSE_INFRASTRUCTURE (Fast Testing)

```powershell
git commit -m "[REUSE_INFRASTRUCTURE] Fix bug in payment service"
git push
```

**Result:** Build uses `REUSE_INFRASTRUCTURE` mode (fast, keeps VPC/RDS)

### Example 2: Use NORMAL (Full Deployment)

```powershell
git commit -m "[NORMAL] Add new feature"
git push
```

**Result:** Build uses `NORMAL` mode (full infrastructure deployment)

### Example 3: Use CLEANUP_AND_DEPLOY (Clean Slate)

```powershell
git commit -m "[CLEANUP_AND_DEPLOY] Major refactoring"
git push
```

**Result:** Build uses `CLEANUP_AND_DEPLOY` mode (destroys old, deploys new)

### Example 4: Use EKS_ONLY (EKS Testing)

```powershell
git commit -m "[EKS_ONLY] Test EKS deployment"
git push
```

**Result:** Build uses `EKS_ONLY` mode (only runs EKS deployment stage)

### Example 5: No Mode Specified (Uses Default)

```powershell
git commit -m "Regular commit without mode"
git push
```

**Result:** Build uses default mode (first in parameter list: `NORMAL`)

---

## 🎯 Format

Put the deployment mode in **square brackets** anywhere in your commit message:

- ✅ `[REUSE_INFRASTRUCTURE]` - Works
- ✅ `[NORMAL]` - Works
- ✅ `[CLEANUP_AND_DEPLOY]` - Works
- ✅ `[EKS_ONLY]` - Works
- ✅ `[reuse_infrastructure]` - Works (case insensitive)
- ✅ `Fix bug [REUSE_INFRASTRUCTURE]` - Works (anywhere in message)
- ❌ `REUSE_INFRASTRUCTURE` - Doesn't work (needs brackets)
- ❌ `(REUSE_INFRASTRUCTURE)` - Doesn't work (needs square brackets)

---

## 📋 Available Modes

| Mode | Format | What It Does |
|------|--------|--------------|
| **NORMAL** | `[NORMAL]` | Full infrastructure deployment |
| **CLEANUP_AND_DEPLOY** | `[CLEANUP_AND_DEPLOY]` | Destroy old, then deploy new |
| **REUSE_INFRASTRUCTURE** | `[REUSE_INFRASTRUCTURE]` | Keep VPC/RDS, recreate EC2 only (fastest) |
| **EKS_ONLY** | `[EKS_ONLY]` | Skip all except EKS deployment |

---

## 💡 Common Use Cases

### Daily Development (Fast Testing)
```powershell
git commit -m "[REUSE_INFRASTRUCTURE] Update API endpoint"
git push
```
**Why:** Fast, reliable, perfect for testing code changes

### Production Deployment
```powershell
git commit -m "[NORMAL] Release v1.2.0"
git push
```
**Why:** Full deployment ensures everything is fresh

### Clean Start
```powershell
git commit -m "[CLEANUP_AND_DEPLOY] Reset infrastructure"
git push
```
**Why:** Destroys old resources first, then deploys fresh

### EKS Testing
```powershell
git commit -m "[EKS_ONLY] Test Kubernetes deployment"
git push
```
**Why:** Only tests EKS deployment, skips other stages

---

## 🔍 How Jenkins Detects It

1. **Jenkins checks the commit message** for patterns like `[REUSE_INFRASTRUCTURE]`
2. **If found:** Uses that mode
3. **If not found:** Uses default parameter value (first in list)

---

## ✅ Verification

After pushing, check Jenkins console output. You'll see:

**If mode detected:**
```
=========================================
Deployment mode detected from commit message: REUSE_INFRASTRUCTURE
Commit message: [REUSE_INFRASTRUCTURE] Fix bug
=========================================
```

**If using default:**
```
=========================================
No deployment mode in commit message, using default: NORMAL
Commit message: Regular commit
To specify mode, use: [NORMAL], [REUSE_INFRASTRUCTURE], [CLEANUP_AND_DEPLOY], or [EKS_ONLY]
=========================================
```

---

## 🎯 Quick Reference

**Fast testing:**
```powershell
git commit -m "[REUSE_INFRASTRUCTURE] Your message"
```

**Full deployment:**
```powershell
git commit -m "[NORMAL] Your message"
```

**Clean slate:**
```powershell
git commit -m "[CLEANUP_AND_DEPLOY] Your message"
```

**EKS only:**
```powershell
git commit -m "[EKS_ONLY] Your message"
```

---

## 🚀 You're All Set!

Now you can control the deployment mode directly from your commit messages! Just add `[MODE]` to your commit message and push! 🎉

