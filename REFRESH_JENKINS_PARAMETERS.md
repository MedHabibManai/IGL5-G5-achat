# How to Refresh Jenkins Parameters to See EKS_ONLY Option

## Problem
Jenkins caches parameter definitions. After updating the Jenkinsfile, you need to refresh the parameters.

## Solution: Run the Pipeline Once

Jenkins will automatically update the parameters when you run the pipeline after the Jenkinsfile change.

### Quick Steps:

1. **Open Jenkins**
   - Go to: http://localhost:8080
   - Navigate to: **IGL5-G5-achat** job

2. **Click "Build Now"** (or "Build with Parameters" with any existing option)
   - This will trigger a build that loads the new Jenkinsfile
   - The build can fail - that's OK, we just need it to refresh parameters

3. **After the build starts (or fails)**
   - Go back to the job page
   - Click **"Build with Parameters"** again
   - You should now see **EKS_ONLY** in the dropdown!

## Alternative: Manual Refresh via Script Console

If the above doesn't work, you can force a refresh:

1. Go to: **Manage Jenkins** → **Script Console**
2. Paste this script:
```groovy
def job = Jenkins.instance.getItemByFullName('IGL5-G5-achat')
if (job) {
    job.doReload()
    println "Job reloaded successfully!"
} else {
    println "Job not found!"
}
```
3. Click **Run**
4. Go back to the job and click **"Build with Parameters"**

## Verify It Worked

After refreshing, when you click "Build with Parameters", you should see:
- ✅ NORMAL
- ✅ CLEANUP_AND_DEPLOY
- ✅ REUSE_INFRASTRUCTURE
- ✅ **EKS_ONLY** ← This should now appear!

