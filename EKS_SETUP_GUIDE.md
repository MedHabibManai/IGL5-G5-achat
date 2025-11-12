# AWS EKS Setup Guide for Achat Application

## 💰 Cost Estimate

**Monthly Costs (if running 24/7):**
- EKS Control Plane: $0.10/hour = **$73/month**
- 2x t3.small nodes: $0.0416/hour = **$30/month**
- NAT Gateway: $0.045/hour = **$32/month**
- Load Balancers (2): ~**$33/month**
- **Total: ~$168/month**

**With Your $50 Budget:**
- Run for **~9 days** at full capacity
- OR run **part-time** (stop cluster when not using): **~30 days**

## 🚀 Setup Steps

### Step 1: Deploy EKS Infrastructure with Terraform

```bash
cd c:\Users\MSI\Documents\TESTING\IGL5-G5-achat\terraform

# Initialize Terraform (if not already done)
terraform init

# Plan the EKS deployment
terraform plan

# Apply (this will create EKS cluster - takes ~15-20 minutes)
terraform apply -auto-approve
```

**Wait Time:** 15-20 minutes for EKS cluster creation

### Step 2: Configure kubectl for EKS

```bash
# Get the kubeconfig command from Terraform
terraform output eks_kubeconfig_command

# Run the output command (should look like this):
aws eks update-kubeconfig --region us-east-1 --name achat-app-eks-cluster

# Verify connection
kubectl get nodes

# Should see 2 nodes:
# NAME                           STATUS   ROLES    AGE   VERSION
# ip-10-0-1-xxx.ec2.internal     Ready    <none>   5m    v1.28.x
# ip-10-0-2-xxx.ec2.internal     Ready    <none>   5m    v1.28.x
```

### Step 3: Deploy Applications to EKS

**Option A: Via Jenkins Pipeline (Recommended)**
```bash
# Trigger a Jenkins build
# The pipeline will automatically:
# 1. Build backend & frontend
# 2. Deploy to EKS
# 3. Expose services via AWS LoadBalancers
```

**Option B: Manual Deployment**
```bash
cd c:\Users\MSI\Documents\TESTING\IGL5-G5-achat

# Deploy all manifests
kubectl apply -f k8s/

# Wait for deployments
kubectl rollout status deployment/achat-app -n achat-app
kubectl rollout status deployment/achat-frontend -n achat-app

# Check pods
kubectl get pods -n achat-app

# Get LoadBalancer URLs (takes 3-5 minutes to provision)
kubectl get svc -n achat-app
```

### Step 4: Access Your Application

```bash
# Get Frontend URL
kubectl get svc achat-frontend -n achat-app -o jsonpath='{.status.loadBalancer.ingress[0].hostname}'

# Get Backend URL
kubectl get svc achat-app -n achat-app -o jsonpath='{.status.loadBalancer.ingress[0].hostname}'

# Open in browser:
# Frontend: http://[LOAD-BALANCER-URL]
# Backend:  http://[LOAD-BALANCER-URL]/SpringMVC
```

## 📊 EKS Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      AWS EKS CLUSTER                         │
│                                                              │
│  ┌─────────────────────┐      ┌─────────────────────┐     │
│  │   Worker Node 1     │      │   Worker Node 2     │     │
│  │   (t3.small)        │      │   (t3.small)        │     │
│  │                     │      │                     │     │
│  │  ┌───────────────┐  │      │  ┌───────────────┐  │     │
│  │  │ Backend Pod   │  │      │  │ Backend Pod   │  │     │
│  │  │ (Spring Boot) │  │      │  │ (Spring Boot) │  │     │
│  │  └───────────────┘  │      │  └───────────────┘  │     │
│  │  ┌───────────────┐  │      │  ┌───────────────┐  │     │
│  │  │ Frontend Pod  │  │      │  │ Frontend Pod  │  │     │
│  │  │ (React/Nginx) │  │      │  │ (React/Nginx) │  │     │
│  │  └───────────────┘  │      │  └───────────────┘  │     │
│  │  ┌───────────────┐  │      │                     │     │
│  │  │  MySQL Pod    │  │      │                     │     │
│  │  └───────────────┘  │      │                     │     │
│  └─────────────────────┘      └─────────────────────┘     │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐   │
│  │         Kubernetes Services (LoadBalancer)          │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                            ↓
                ┌───────────────────────┐
                │  AWS Load Balancers   │
                │  (Classic ELB)        │
                └───────────────────────┘
                            ↓
                      Internet Users
          http://[LB-URL]/ (Frontend)
          http://[LB-URL]/SpringMVC (Backend)
```

## 🛠️ Jenkins Pipeline Integration

The Jenkins pipeline has been updated with a new stage:

### Stage 13: Deploy to EKS
- Automatically configures kubectl for EKS
- Deploys backend and frontend
- Updates deployment images with build numbers
- Waits for rollout completion
- Displays LoadBalancer URLs

### How to Use:
1. Push code to GitHub
2. Jenkins automatically triggers
3. Pipeline deploys to both AWS EC2 and EKS
4. Access via LoadBalancer URLs

## 💡 Cost Optimization Tips

### 1. Stop Cluster When Not Using
```bash
# Scale down node group to 0
aws eks update-nodegroup-config \
  --cluster-name achat-app-eks-cluster \
  --nodegroup-name achat-app-node-group \
  --scaling-config minSize=0,maxSize=3,desiredSize=0 \
  --region us-east-1

# This saves: $30/month (nodes) + $32/month (NAT) = $62/month
# You still pay: $73/month (control plane) + $33/month (LBs) = $106/month
```

### 2. Use Spot Instances (50-70% cheaper)
Edit `terraform/eks.tf`:
```hcl
resource "aws_eks_node_group" "main" {
  capacity_type  = "SPOT"  # Change from ON_DEMAND
  instance_types = ["t3.small", "t3a.small", "t2.small"]  # Multiple types
}
```

### 3. Delete Cluster When Not Needed
```bash
cd c:\Users\MSI\Documents\TESTING\IGL5-G5-achat\terraform

# Destroy EKS (saves all costs)
terraform destroy -auto-approve

# Recreate when needed
terraform apply -auto-approve
```

### 4. Use Single Node (Not Recommended for Prod)
Edit `terraform/variables.tf`:
```hcl
variable "eks_desired_nodes" {
  default     = 1  # Change from 2
}
```

## 📋 Useful kubectl Commands

```bash
# Check cluster info
kubectl cluster-info

# Get all resources in achat-app namespace
kubectl get all -n achat-app

# Check pod logs
kubectl logs -f deployment/achat-app -n achat-app
kubectl logs -f deployment/achat-frontend -n achat-app

# Describe a pod (troubleshooting)
kubectl describe pod [POD_NAME] -n achat-app

# Execute command in pod
kubectl exec -it [POD_NAME] -n achat-app -- /bin/bash

# Port forward (access without LoadBalancer)
kubectl port-forward svc/achat-frontend 8080:80 -n achat-app
# Access at http://localhost:8080

# Get events
kubectl get events -n achat-app --sort-by='.lastTimestamp'

# Scale deployment
kubectl scale deployment/achat-app --replicas=3 -n achat-app

# Delete all resources
kubectl delete namespace achat-app
```

## 🔧 Troubleshooting

### Pods in Pending State
```bash
# Check node resources
kubectl describe nodes

# Check events
kubectl get events -n achat-app

# Common cause: Insufficient node capacity
# Solution: Add more nodes or use smaller pod requests
```

### LoadBalancer URL Not Assigned
```bash
# Wait 3-5 minutes after service creation

# Check service status
kubectl describe svc achat-frontend -n achat-app

# Check AWS Load Balancer
aws elbv2 describe-load-balancers --region us-east-1

# If stuck, delete and recreate service
kubectl delete svc achat-frontend -n achat-app
kubectl apply -f k8s/frontend-deployment.yaml
```

### ImagePullBackOff Error
```bash
# Check if image exists on Docker Hub
docker pull habibmanai/achat-app:[BUILD_NUMBER]

# Check pod events
kubectl describe pod [POD_NAME] -n achat-app

# Solution: Verify Docker image was pushed successfully
```

### Cannot Connect to EKS Cluster
```bash
# Update kubeconfig again
aws eks update-kubeconfig --region us-east-1 --name achat-app-eks-cluster

# Check AWS credentials
aws sts get-caller-identity

# Verify cluster exists
aws eks describe-cluster --name achat-app-eks-cluster --region us-east-1
```

## 🎯 Next Steps After EKS Setup

1. **Configure Custom Domain**
   - Buy domain on Route 53
   - Create CNAME to LoadBalancer
   - Add SSL certificate

2. **Set Up Monitoring**
   - Install Prometheus & Grafana on EKS
   - Monitor pod metrics
   - Set up alerts

3. **Configure Autoscaling**
   - Horizontal Pod Autoscaler (HPA)
   - Cluster Autoscaler
   - Scale based on CPU/Memory

4. **Implement CI/CD Improvements**
   - Add automated tests before EKS deployment
   - Blue/Green deployments
   - Canary releases

5. **Secure Your Cluster**
   - Network policies
   - Pod security policies
   - RBAC (Role-Based Access Control)

## 🆘 Getting Help

- **AWS EKS Documentation**: https://docs.aws.amazon.com/eks/
- **Kubernetes Documentation**: https://kubernetes.io/docs/
- **Check your costs**: https://console.aws.amazon.com/billing/

## ⚠️ Important Reminders

1. **Monitor Your Costs Daily**: EKS can get expensive quickly!
2. **Delete When Not Using**: With $50, you have limited budget
3. **Set Billing Alerts**: AWS Console → Billing → Budgets
4. **Stop Nodes Overnight**: Save 50% of node costs
5. **Destroy After Testing**: If just learning, destroy after each session

## 📞 Support

If you encounter issues:
1. Check pod logs: `kubectl logs -f deployment/[NAME] -n achat-app`
2. Check events: `kubectl get events -n achat-app`
3. Check AWS Console: EKS → Clusters → achat-app-eks-cluster
4. Review Jenkins console output for deployment errors
