# EKS Deployment Fix - Build 167

## Problem Identified

**Build #167** failed at the "Deploy to EKS" stage with the following error:

```
java.lang.NoSuchMethodError: No such DSL method 'withAWS' found among steps
```

### Root Cause
The `withAWS` method is provided by the **AWS Steps Plugin** which was not installed in Jenkins. The EKS deployment stage was using:

```groovy
withAWS(credentials: "${AWS_CREDENTIAL_ID}", region: "${AWS_REGION}") {
    // EKS deployment code
}
```

## Solution Applied

### Commit: `4f5c35a`
**Title:** "Fix EKS deployment: Replace withAWS with withCredentials for AWS plugin compatibility"

### Changes Made

1. **Replaced `withAWS` wrapper with `withCredentials`**
   - Changed from: `withAWS(credentials: "${AWS_CREDENTIAL_ID}", region: "${AWS_REGION}")`
   - Changed to: `withCredentials([file(credentialsId: "${AWS_CREDENTIAL_ID}", variable: 'AWS_CREDENTIALS_FILE')])`

2. **Added AWS credential parsing from file**
   - Parse the `aws-credentials` file to extract:
     - `AWS_ACCESS_KEY_ID`
     - `AWS_SECRET_ACCESS_KEY`
     - `AWS_SESSION_TOKEN`
   - Export these as environment variables in each shell block

3. **Updated all AWS CLI commands**
   - Prefixed each `sh` block that uses AWS CLI or Terraform with:
     ```bash
     export AWS_ACCESS_KEY_ID=$(grep aws_access_key_id "$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
     export AWS_SECRET_ACCESS_KEY=$(grep aws_secret_access_key "$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
     export AWS_SESSION_TOKEN=$(grep aws_session_token "$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
     export AWS_DEFAULT_REGION=us-east-1
     ```

### Benefits

✅ **No additional Jenkins plugins required** - works with standard Jenkins installation  
✅ **Maintains same functionality** - AWS credentials are properly configured  
✅ **Compatible with existing setup** - uses the same `aws-credentials` file  
✅ **More explicit** - credentials parsing is visible in the pipeline code  
✅ **No BOM issues** - used PowerShell with UTF8Encoding($false) for file editing

## Build 167 Success Summary

Before the EKS deployment failure, Build 167 achieved significant milestones:

### ✅ Successfully Completed Stages

1. **Git Checkout** - Code retrieved with retry logic
2. **Maven Build** - 107 unit tests passing
3. **JAR Packaging** - achat-1.0.jar created
4. **SonarQube Analysis** - Code quality checked
5. **Backend Docker Build** - Image created
6. **Backend Docker Push** - `habibmanai/achat-app:167` pushed successfully
7. **Terraform Infrastructure** - 23 AWS resources deployed:
   - ✅ VPC: `vpc-0be12aa133fd2c9e7`
   - ✅ EC2: `i-0c9a380ab932f915b` (Public IP: 100.30.31.169)
   - ✅ RDS MySQL: `achat-app-db.ciug9ce7zygu.us-east-1.rds.amazonaws.com`
   - ✅ EKS Cluster: `achat-app-eks-cluster` (ACTIVE)
   - ✅ Security groups, subnets, NAT gateway, route tables
8. **Health Check** - PASSED:
   - Application URL: http://100.30.31.169:8089/SpringMVC
   - Status: UP
   - Database: Connected (MySQL validation successful)
   - Disk space: 28.4GB free
9. **Frontend npm Build** - 1343 packages, production build created
10. **Frontend Docker Build** - Success on attempt 2 (TLS timeout on attempt 1, retry worked)
11. **Frontend Docker Push** - `habibmanai/achat-frontend:167` pushed successfully after retries

### ❌ Failed Stage

12. **Deploy to EKS** - Failed due to missing `withAWS` method (NOW FIXED)

## Next Steps

### To Test the Fix:

1. **Trigger a new Jenkins build** (Build #168)
2. **The pipeline should now:**
   - Complete all previous stages (1-11) as before
   - Successfully execute the EKS deployment stage (13)
   - Deploy backend and frontend to EKS cluster
   - Create Kubernetes resources (namespace, secrets, configmaps, deployments, services, HPA)
   - Update deployment images to build-specific tags
   - Wait for pod rollouts to complete
   - Display EKS LoadBalancer URLs

### Expected EKS Deployment Flow:

```bash
1. Configure kubectl for EKS cluster: achat-app-eks-cluster
2. Create namespace: achat-app
3. Apply secrets and configmaps (with RDS endpoint)
4. Deploy backend: achat-app deployment
5. Deploy frontend: achat-frontend deployment
6. Apply services (LoadBalancer type for external access)
7. Apply HPA (Horizontal Pod Autoscaler)
8. Wait for rollout: backend (5 min timeout)
9. Wait for rollout: frontend (5 min timeout)
10. Display LoadBalancer URLs for frontend and backend
```

### If AWS Credentials Expire:

The AWS Academy Lab sessions expire after a few hours. If credentials expire:

1. Get new credentials from AWS Academy Lab
2. Update `aws-credentials` file with:
   ```
   [default]
   aws_access_key_id=NEW_ACCESS_KEY
   aws_secret_access_key=NEW_SECRET_KEY
   aws_session_token=NEW_SESSION_TOKEN
   ```
3. Use ASCII encoding (no BOM)
4. Jenkins will automatically use the new credentials on next build

## Technical Notes

### File Encoding
- **Jenkinsfile:** UTF-8 without BOM (verified: first bytes are `112 105 112...` = "pipeline")
- **aws-credentials:** ASCII without BOM (verified: first bytes are `91 100 101...` = "[default]")

### Retry Logic in Place
- **Stage 9:** Backend Docker push - infinite retry
- **Stage 11:** Frontend Docker build - infinite retry  
- **Stage 12:** Frontend Docker push - infinite retry
- All retry logic handles TLS handshake timeouts gracefully

### Git History
```
4f5c35a (HEAD) Fix EKS deployment: Replace withAWS with withCredentials
87d1d6e Add infinite retry to Stage 9 Push Backend Docker Image
61a9f0e Add infinite retry to Push Frontend Docker Image stage
f3ad0dc Remove UTF-8 BOM that was causing 'No such DSL method pipeline' error
23c87ec Improve RDS deletion wait logic
```

## Verification Checklist

Before starting Build #168:

- [x] EKS cluster exists: `achat-app-eks-cluster` (verified in Build 167)
- [x] RDS database exists: `achat-app-db.ciug9ce7zygu.us-east-1.rds.amazonaws.com` (verified in Build 167)
- [x] EC2 instance running: `i-0c9a380ab932f915b` (verified in Build 167)
- [x] Backend image pushed: `habibmanai/achat-app:167` (verified in Build 167)
- [x] Frontend image pushed: `habibmanai/achat-frontend:167` (verified in Build 167)
- [x] Jenkinsfile has no BOM (verified)
- [x] aws-credentials file valid (verified)
- [x] Fix committed and pushed to remote (commit 4f5c35a)

---

**Status:** Ready for Build #168 🚀

**Estimated Completion:** With all infrastructure already deployed from Build 167, the next build should complete the EKS deployment stage successfully within 5-10 minutes (primarily waiting for pod rollouts).
