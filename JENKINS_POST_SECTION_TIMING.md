# ⏱️ Jenkins Post Section Timing - When Does Email Get Sent?

## 📋 How Post Section Works

The `post` → `always` block runs **AFTER** the build completes or is aborted, but there are some nuances:

---

## ✅ When Post Section WILL Run

1. **After build completes** (SUCCESS, FAILURE, UNSTABLE)
   - ✅ Post section runs
   - ✅ Email will be sent (if SMTP works)

2. **After build is aborted/cancelled** (ABORTED)
   - ✅ Post section runs
   - ✅ Email will be sent (if SMTP works)
   - ✅ Even if you cancel during a stage

3. **After any stage completes**
   - ✅ Post section runs
   - ✅ Email will be sent

---

## ❌ When Post Section MIGHT NOT Run

1. **If you cancel BEFORE pipeline starts executing**
   - ❌ If you cancel while Jenkins is still loading/parsing the Jenkinsfile
   - ❌ If you cancel during the very first checkout step (before any stages)
   - ⚠️ This is rare, but can happen

2. **If Jenkins crashes or is forcefully killed**
   - ❌ Post section won't run
   - ❌ Email won't be sent

---

## 🔍 How to Verify Post Section Ran

Check the console output. You should see:

```
[Pipeline] }
[Pipeline] // stage
[Pipeline] }
[Pipeline] post
[Pipeline] {
[Pipeline] always
[Pipeline] {
[Pipeline] script
[Pipeline] {
=========================================
Pipeline Execution Summary
=========================================
Current build status: ABORTED
EMAIL_RECIPIENTS: sinkingecstasies@gmail.com
```

If you see this, the post section **DID run**.

---

## ⚠️ Important: Connection Error vs Timing

**The connection error is NOT related to timing!**

- ❌ **Connection Error**: SMTP configuration problem (wrong port, TLS settings, firewall, etc.)
- ✅ **Timing Issue**: Post section not running (rare)

Even if the post section runs, you'll still get the connection error if SMTP isn't configured correctly.

---

## 🧪 Testing: Did Post Section Run?

### Test 1: Cancel After Build Starts

1. Start a build
2. Wait for it to reach at least the first stage (e.g., "Checkout")
3. Cancel it
4. **Result**: Post section WILL run ✅

### Test 2: Cancel Immediately

1. Start a build
2. Cancel it within 1-2 seconds (before first stage)
3. **Result**: Post section MIGHT not run ⚠️

### Test 3: Let Build Complete

1. Let build run to completion (success or failure)
2. **Result**: Post section WILL run ✅

---

## 🔧 Ensuring Post Section Always Runs

The current setup uses `post { always { ... } }` which is the best practice:

```groovy
post {
    always {
        // This runs for ALL build statuses
        // Including: SUCCESS, FAILURE, UNSTABLE, ABORTED
    }
}
```

This ensures the post section runs in 99% of cases.

---

## 📧 Email Sending Timeline

```
Build Starts
    ↓
Stages Execute (or get cancelled)
    ↓
Build Status Set (SUCCESS/FAILURE/ABORTED)
    ↓
Post Section Runs ← EMAIL IS SENT HERE
    ↓
Workspace Cleanup
    ↓
Build Ends
```

**Email is sent in the post section**, which runs after the build status is determined.

---

## ✅ Best Practice

1. **Wait a few seconds** after starting the build before canceling
2. This ensures the pipeline has started executing
3. The post section will definitely run
4. Email will be attempted (whether it succeeds depends on SMTP config)

---

## 🎯 Summary

| Scenario | Post Section Runs? | Email Sent? |
|----------|-------------------|-------------|
| Build completes | ✅ Yes | ✅ Yes (if SMTP works) |
| Cancel after 5+ seconds | ✅ Yes | ✅ Yes (if SMTP works) |
| Cancel immediately (< 2 sec) | ⚠️ Maybe | ⚠️ Maybe |
| SMTP connection error | ✅ Yes (post runs) | ❌ No (SMTP fails) |

**Bottom line**: The connection error is a **SMTP configuration issue**, not a timing issue. Fix the SMTP settings first!

