# EKS and Deployment Mode Compatibility

## ✅ Changes Made

Your pipeline now supports **3 deployment modes** that work correctly with or without EKS:

### 1. NORMAL Mode (Default)
**What it does:**
- Deploys everything fresh
- Creates VPC, Subnets, EC2, RDS
- Creates EKS cluster (if enabled)
- May fail if VPC limit reached

**When to use:** First deployment or complete rebuild

### 2. CLEANUP_AND_DEPLOY Mode
**What it does:**
- Destroys ALL existing resources
- Creates everything fresh
- **WARNING:** This WILL destroy EKS cluster if it exists (takes 15-20 min to recreate)

**When to use:** When you need a clean slate

### 3. REUSE_INFRASTRUCTURE Mode ⭐ (Recommended)
**What it does:**
- **Keeps VPC, RDS, and EKS cluster** (expensive resources)
- Only destroys and recreates EC2 instance
- **Fastest option** (~5 minutes vs 20+ minutes)
- **Saves money** (doesn't destroy/recreate EKS)

**When to use:** For code changes, testing, rapid iterations

## 🎛️ EKS Control

EKS is now **OPTIONAL** and **DISABLED BY DEFAULT** to save costs.

### To Enable EKS:

**Option A: Via Terraform Variable**
Create `terraform/terraform.tfvars`:
```hcl
create_eks = true
```

**Option B: Via Jenkins Pipeline Parameter**
Add to Jenkinsfile terraform commands:
```groovy
terraform apply -auto-approve \
  -var="create_eks=true" \
  -var="docker_image=${TF_VAR_docker_image}"
```

### To Disable EKS:
Simply don't set `create_eks=true` (default is `false`)

## 💰 Cost Impact

### With EKS Disabled (Current Default):
- **EC2**: ~$15/month (t2.micro)
- **RDS**: ~$15/month (db.t3.micro)
- **Total**: **~$30/month** ✅

### With EKS Enabled:
- **EC2**: ~$15/month
- **RDS**: ~$15/month
- **EKS Control Plane**: $73/month
- **2x t3.small nodes**: $30/month
- **NAT Gateway**: $32/month
- **Load Balancers**: $33/month
- **Total**: **~$198/month** 💰

**Your $50 Budget:**
- Without EKS: **60+ days**
- With EKS: **~7.5 days**

## 🔄 How REUSE_INFRASTRUCTURE Works Now

### Without EKS (Default):
```bash
# Jenkinsfile automatically:
1. Destroys: EC2 + Elastic IP
2. Keeps: VPC + RDS
3. Recreates: EC2 + Elastic IP
4. Time: ~5 minutes
```

### With EKS Enabled:
```bash
# Jenkinsfile automatically:
1. Destroys: EC2 + Elastic IP
2. Keeps: VPC + RDS + EKS Cluster + Worker Nodes + NAT Gateway
3. Recreates: EC2 + Elastic IP
4. Time: ~5 minutes
5. Cost saved: ~$168/month by not destroying/recreating EKS
```

## 🚀 Recommended Workflow

### For Development (Current Setup):
```
1. Use REUSE_INFRASTRUCTURE mode
2. Keep EKS disabled (default)
3. Deploy to:
   - AWS EC2 (backend production)
   - Local Docker Desktop K8s (frontend testing)
4. Cost: ~$30/month
```

### When You Need Public Frontend:
```
1. Enable EKS by creating terraform/terraform.tfvars:
   create_eks = true

2. Run ONE build in NORMAL mode (creates EKS)

3. Switch to REUSE_INFRASTRUCTURE mode for all future builds
   (keeps EKS alive, only updates EC2)

4. Cost: ~$198/month while EKS is running

5. When done, destroy EKS:
   Use CLEANUP_AND_DEPLOY mode once
   OR manually: terraform destroy -target=aws_eks_cluster.main
```

### To Save Maximum Money:
```
1. Development: Use LOCAL K8s only
   - Frontend: http://localhost:30080
   - Backend: http://localhost/SpringMVC
   - Cost: $0/month

2. Production: Enable EKS only when needed
   - Start EKS Friday morning
   - Demo your app
   - Destroy EKS Friday evening
   - Pay for ~10 hours (~$2.30)
```

## 📝 Configuration Files

### terraform/variables.tf
```hcl
variable "create_eks" {
  description = "Create EKS cluster (WARNING: ~$168/month cost!)"
  type        = bool
  default     = false  # Disabled by default
}
```

### terraform/terraform.tfvars (create this to enable EKS)
```hcl
# Enable EKS
create_eks = true

# EKS Node Configuration
eks_desired_nodes = 2
eks_min_nodes = 1
eks_max_nodes = 3
eks_node_instance_type = "t3.small"
```

## 🔍 Checking Current State

### Is EKS Enabled?
```bash
cd terraform
terraform output eks_cluster_name

# If output is empty: EKS is disabled
# If output shows name: EKS is enabled
```

### Current Deployment Mode?
Check Jenkins Console Output for:
```
Deployment Mode: REUSE_INFRASTRUCTURE
```

## ⚠️ Important Notes

1. **EKS is DISABLED by default** - You won't be charged for EKS unless you explicitly enable it

2. **REUSE_INFRASTRUCTURE is SAFE** - It won't destroy your EKS cluster

3. **Local K8s works always** - Even without EKS, you can deploy to Docker Desktop K8s

4. **First EKS creation takes 20 minutes** - Be patient!

5. **Delete EKS when not using** - It costs ~$5.60/day even when idle

## 🎯 Next Steps

Your current pipeline will:
- ✅ Build backend and frontend
- ✅ Deploy to AWS EC2
- ✅ Deploy to Local Docker Desktop K8s
- ❌ Skip EKS deployment (disabled by default)

To enable EKS:
1. Create `terraform/terraform.tfvars` with `create_eks = true`
2. Commit and push
3. Wait 20 minutes for EKS creation
4. Get public LoadBalancer URLs

**Current recommendation:** Keep EKS disabled until you really need public frontend access! 💡
