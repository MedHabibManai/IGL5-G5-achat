# AWS Deployment - Quick Reference Card

## 🚀 Quick Start (5 Minutes)

```powershell
# Run the complete setup
.\scripts\setup-aws-complete.ps1

# Or run individual phases:
.\scripts\setup-aws-deployment.ps1      # Phase 1: AWS CLI
.\scripts\add-aws-to-jenkins.ps1        # Phase 2: Jenkins
.\scripts\create-terraform-config.ps1   # Phase 3: Terraform
```

---

## 📋 Prerequisites Checklist

- [ ] Docker Desktop running
- [ ] Jenkins running (http://localhost:8080)
- [ ] AWS Academy account or AWS account
- [ ] Git configured
- [ ] PowerShell 5.1+

---

## 🔑 AWS Credentials Locations

### AWS Academy / Learner Lab
1. Log in → Modules → Learner Lab
2. Start Lab (wait for green)
3. AWS Details → Show AWS CLI credentials
4. Copy the credentials block

### AWS Console (IAM)
1. IAM → Users → Security credentials
2. Create access key → CLI
3. Download CSV or copy credentials

---

## 🛠️ Essential Commands

### AWS CLI
```powershell
# Test credentials
aws sts get-caller-identity

# List S3 buckets
aws s3 ls

# List EC2 instances
aws ec2 describe-instances --region us-east-1
```

### Terraform
```powershell
cd terraform

# Initialize
terraform init

# Preview changes
terraform plan

# Deploy
terraform apply

# Get outputs
terraform output

# Destroy
terraform destroy
```

### Jenkins
```powershell
# Start Jenkins
docker-compose up -d jenkins-cicd

# View logs
docker logs jenkins-cicd

# Stop Jenkins
docker-compose stop jenkins-cicd
```

---

## 📊 Pipeline Stages

| # | Stage | Duration | Description |
|---|-------|----------|-------------|
| 1 | Checkout | 10s | Clone from GitHub |
| 2 | Build | 30s | Maven compile |
| 3 | Unit Tests | 45s | Run 107 tests |
| 4 | Package | 20s | Create JAR |
| 5 | SonarQube | 30s | Code analysis |
| 6 | Quality Gate | 10s | Wait for result |
| 7 | Nexus | 15s | Upload artifacts |
| 8 | Docker Build | 30s | Create image |
| 9 | Docker Push | 45s | Upload to Hub |
| **10** | **Terraform Init** | **20s** | **Initialize** |
| **11** | **Terraform Plan** | **15s** | **Preview** |
| **12** | **Terraform Apply** | **3-5min** | **Deploy AWS** |
| **13** | **Get Info** | **5s** | **Outputs** |
| **14** | **Health Check** | **1-2min** | **Verify** |

**Total:** ~10-15 minutes

---

## 🏗️ AWS Resources Created

```
VPC (10.0.0.0/16)
├── Internet Gateway
├── Public Subnet (10.0.1.0/24)
│   └── Route Table
├── Security Group
│   ├── Port 8080 (HTTP)
│   └── Port 22 (SSH - optional)
├── EC2 Instance (t2.micro)
│   ├── Amazon Linux 2023
│   ├── Docker installed
│   └── Application running
├── Elastic IP
└── IAM Role
    ├── SSM access
    └── CloudWatch logs
```

---

## 🔧 Troubleshooting

### Credentials Expired
```powershell
# Get new credentials from AWS Academy
# Update in Jenkins: http://localhost:8080/credentials/
```

### Terraform State Lock
```powershell
cd terraform
terraform force-unlock <LOCK_ID>
```

### Application Not Accessible
```powershell
# Check instance
aws ec2 describe-instances --region us-east-1

# Connect via SSM
aws ssm start-session --target <instance-id>

# Check Docker
docker ps
docker logs achat
```

### Health Check Fails
```powershell
# Increase wait time in Jenkinsfile (line ~475)
sleep(120)  # Instead of 60
```

---

## 📁 File Structure

```
IGL5-G5-achat/
├── terraform/
│   ├── provider.tf          # AWS provider config
│   ├── variables.tf         # Input variables
│   ├── main.tf              # Infrastructure resources
│   ├── outputs.tf           # Output values
│   ├── terraform.tfvars     # Variable values (gitignored)
│   └── terraform.tfvars.example
├── scripts/
│   ├── setup-aws-complete.ps1       # Master setup
│   ├── setup-aws-deployment.ps1     # AWS CLI setup
│   ├── add-aws-to-jenkins.ps1       # Jenkins credentials
│   └── create-terraform-config.ps1  # Terraform setup
├── Jenkinsfile              # Pipeline with AWS stages
├── AWS_DEPLOYMENT_GUIDE.md  # Complete guide
└── AWS_QUICK_REFERENCE.md   # This file
```

---

## 🌐 Important URLs

| Service | URL | Credentials |
|---------|-----|-------------|
| Jenkins | http://localhost:8080 | admin / (check console) |
| SonarQube | http://localhost:9000 | admin / ##Azeraoi123 |
| Nexus | http://localhost:8081 | admin / ##Azeraoi123 |
| Grafana | http://localhost:3000 | admin / admin |
| Prometheus | http://localhost:9090 | - |
| Application (AWS) | http://<PUBLIC_IP>:8080 | - |

---

## 💰 AWS Costs

| Resource | Free Tier | After Free Tier |
|----------|-----------|-----------------|
| t2.micro | 750 hrs/month | $8.50/month |
| EBS 20GB | 30 GB/month | $2/month |
| Data Transfer | 15 GB/month | $0.09/GB |
| **Total** | **$0** | **~$10/month** |

---

## 🔐 Security Checklist

- [ ] AWS credentials in Jenkins (not in code)
- [ ] terraform.tfvars in .gitignore
- [ ] SSH restricted to specific IPs
- [ ] Security group limits ports
- [ ] EBS volumes encrypted
- [ ] IMDSv2 enabled
- [ ] Session tokens rotated regularly

---

## 📞 Getting Help

1. Check AWS_DEPLOYMENT_GUIDE.md
2. Review Jenkins console output
3. Check Terraform logs
4. Verify AWS credentials
5. Ensure prerequisites met

---

## ✅ Success Indicators

After deployment, you should see:

```
✅ Jenkins pipeline: SUCCESS
✅ Terraform apply: Complete
✅ EC2 instance: Running
✅ Application URL: http://<IP>:8080
✅ Health check: {"status":"UP"}
✅ Docker container: Running
```

---

## 🎯 Next Steps After Deployment

1. **Access Application**
   ```
   http://<PUBLIC_IP>:8080
   ```

2. **View Swagger UI**
   ```
   http://<PUBLIC_IP>:8080/swagger-ui.html
   ```

3. **Check Health**
   ```
   http://<PUBLIC_IP>:8080/actuator/health
   ```

4. **Monitor in Grafana**
   ```
   http://localhost:3000
   ```

5. **Clean Up (When Done)**
   ```powershell
   cd terraform
   terraform destroy
   ```

---

## 🚨 Emergency Commands

### Stop Everything
```powershell
# Stop Jenkins pipeline
# Go to Jenkins → Click build → Click "X"

# Destroy AWS resources
cd terraform
terraform destroy -auto-approve

# Stop local services
docker-compose down
```

### Reset Everything
```powershell
# Clean Terraform state
cd terraform
rm -rf .terraform terraform.tfstate*

# Re-initialize
terraform init
```

---

**Last Updated:** 2025-11-11
**Version:** 1.0
**Author:** DevOps Team

