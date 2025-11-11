# AWS CloudFormation + Jenkins CI/CD Integration Guide

## 📋 Overview

This guide will help you integrate AWS deployment into your Jenkins CI/CD pipeline using Terraform for Infrastructure as Code (IaC).

---

## 🎯 Phase 1: Retrieve AWS Credentials from CloudFormation Sandbox

### What You Need

AWS provides three types of credentials for programmatic access:

1. **AWS Access Key ID** - Public identifier (e.g., `AKIAIOSFODNN7EXAMPLE`)
2. **AWS Secret Access Key** - Private key (e.g., `wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY`)
3. **AWS Session Token** - Temporary token for sandbox/lab environments (optional but common in training environments)

### Where to Find Credentials

#### Option A: AWS Academy / CloudFormation Course Sandbox

1. **Log in to AWS Academy**
   - Go to your AWS Academy course portal
   - Navigate to "Modules" or "Learner Lab"

2. **Start the Lab Environment**
   - Click "Start Lab" button
   - Wait for the AWS indicator to turn green

3. **Get AWS CLI Credentials**
   - Click "AWS Details" button
   - Click "Show" next to "AWS CLI"
   - Copy the credentials block that looks like:
   ```bash
   [default]
   aws_access_key_id=ASIAIOSFODNN7EXAMPLE
   aws_secret_access_key=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
   aws_session_token=FwoGZXIvYXdzEBYaD...VERY_LONG_TOKEN...
   ```

4. **Note the Region**
   - Usually displayed as `us-east-1` or similar
   - This is important for Terraform configuration

#### Option B: AWS Console (IAM User)

1. **Log in to AWS Console**
   - Go to https://console.aws.amazon.com

2. **Navigate to IAM**
   - Search for "IAM" in the services search bar
   - Click "Users" in the left sidebar

3. **Create or Select User**
   - Click "Add users" (if creating new)
   - Or select existing user

4. **Create Access Key**
   - Go to "Security credentials" tab
   - Click "Create access key"
   - Select "Command Line Interface (CLI)"
   - Download the CSV file or copy credentials

#### Option C: AWS CloudFormation Stack Outputs

If your course provides a CloudFormation stack:

1. Go to **CloudFormation** service
2. Select your stack
3. Click **Outputs** tab
4. Look for keys like `AccessKeyId`, `SecretAccessKey`, `SessionToken`

### ⚠️ Important Security Notes

- **NEVER commit credentials to Git**
- **Session tokens expire** (usually 3-4 hours in sandbox environments)
- **Rotate credentials regularly** in production
- **Use IAM roles** instead of access keys when possible (for EC2, Lambda, etc.)

---

## 🖥️ Phase 2: Install and Configure AWS CLI on Windows

### Step 1: Install AWS CLI

#### Method A: MSI Installer (Recommended)

1. **Download AWS CLI v2**
   ```powershell
   # Open PowerShell and download
   Invoke-WebRequest -Uri "https://awscli.amazonaws.com/AWSCLIV2.msi" -OutFile "$env:TEMP\AWSCLIV2.msi"
   ```

2. **Install**
   ```powershell
   Start-Process msiexec.exe -ArgumentList "/i $env:TEMP\AWSCLIV2.msi /quiet" -Wait
   ```

3. **Verify Installation**
   ```powershell
   # Close and reopen PowerShell, then:
   aws --version
   # Expected output: aws-cli/2.x.x Python/3.x.x Windows/10 exe/AMD64
   ```

#### Method B: Chocolatey

```powershell
choco install awscli -y
```

### Step 2: Configure AWS Credentials

#### Option A: Interactive Configuration (Simple)

```powershell
aws configure
```

You'll be prompted for:
```
AWS Access Key ID [None]: AKIAIOSFODNN7EXAMPLE
AWS Secret Access Key [None]: wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
Default region name [None]: us-east-1
Default output format [None]: json
```

#### Option B: Manual Configuration (For Session Tokens)

1. **Create/Edit credentials file**
   ```powershell
   # Create .aws directory if it doesn't exist
   New-Item -ItemType Directory -Force -Path "$env:USERPROFILE\.aws"
   
   # Edit credentials file
   notepad "$env:USERPROFILE\.aws\credentials"
   ```

2. **Add credentials** (paste this format):
   ```ini
   [default]
   aws_access_key_id = ASIAIOSFODNN7EXAMPLE
   aws_secret_access_key = wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
   aws_session_token = FwoGZXIvYXdzEBYaD...VERY_LONG_TOKEN...
   ```

3. **Create/Edit config file**
   ```powershell
   notepad "$env:USERPROFILE\.aws\config"
   ```

4. **Add configuration**:
   ```ini
   [default]
   region = us-east-1
   output = json
   ```

#### Option C: Environment Variables (Temporary)

```powershell
# Set for current PowerShell session only
$env:AWS_ACCESS_KEY_ID="ASIAIOSFODNN7EXAMPLE"
$env:AWS_SECRET_ACCESS_KEY="wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"
$env:AWS_SESSION_TOKEN="FwoGZXIvYXdzEBYaD...VERY_LONG_TOKEN..."
$env:AWS_DEFAULT_REGION="us-east-1"
```

### Step 3: Test AWS Credentials

Run these commands to verify your credentials work:

```powershell
# Test 1: Get caller identity (who am I?)
aws sts get-caller-identity

# Expected output:
# {
#     "UserId": "AIDAI...",
#     "Account": "123456789012",
#     "Arn": "arn:aws:iam::123456789012:user/student"
# }

# Test 2: List S3 buckets
aws s3 ls

# Test 3: List EC2 instances (if any)
aws ec2 describe-instances --query 'Reservations[*].Instances[*].[InstanceId,State.Name,InstanceType]' --output table

# Test 4: Get available regions
aws ec2 describe-regions --query 'Regions[*].RegionName' --output table
```

### Troubleshooting

| Error | Solution |
|-------|----------|
| `Unable to locate credentials` | Check credentials file or environment variables |
| `The security token included in the request is expired` | Session token expired - get new credentials from sandbox |
| `An error occurred (UnauthorizedOperation)` | IAM permissions issue - check user policies |
| `Could not connect to the endpoint URL` | Check region configuration or internet connection |

---

## 🔐 Phase 3: Add AWS Credentials to Jenkins

### Prerequisites

- Jenkins running (http://localhost:8080)
- AWS CLI credentials tested and working

### Step 1: Install AWS Credentials Plugin

1. **Navigate to Jenkins Plugin Manager**
   - Go to: http://localhost:8080/manage/pluginManager/
   - Or: Dashboard → Manage Jenkins → Manage Plugins

2. **Install Required Plugins**
   - Click "Available" tab
   - Search for and install:
     - ✅ **CloudBees AWS Credentials Plugin**
     - ✅ **Pipeline: AWS Steps** (for `withAWS` step)
   - Click "Install without restart"

### Step 2: Add AWS Credentials to Jenkins

#### Method A: AWS Credentials Type (Recommended)

1. **Go to Credentials Manager**
   - Navigate to: http://localhost:8080/credentials/
   - Or: Dashboard → Manage Jenkins → Manage Credentials

2. **Add New Credentials**
   - Click "System" → "Global credentials (unrestricted)"
   - Click "Add Credentials"

3. **Configure AWS Credentials**
   - **Kind**: `AWS Credentials`
   - **ID**: `aws-sandbox-credentials` (you'll use this in Jenkinsfile)
   - **Description**: `AWS CloudFormation Sandbox Credentials`
   - **Access Key ID**: `ASIAIOSFODNN7EXAMPLE`
   - **Secret Access Key**: `wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY`
   - Click "OK"

#### Method B: Secret Text (For Session Token)

If you have a session token, you need to add it separately:

1. **Add Session Token as Secret Text**
   - Click "Add Credentials" again
   - **Kind**: `Secret text`
   - **Secret**: `FwoGZXIvYXdzEBYaD...VERY_LONG_TOKEN...`
   - **ID**: `aws-session-token`
   - **Description**: `AWS Session Token (expires in 3-4 hours)`
   - Click "OK"

### Step 3: Verify Credentials in Jenkins

Create a test pipeline to verify credentials work:

```groovy
pipeline {
    agent any
    
    stages {
        stage('Test AWS Credentials') {
            steps {
                withAWS(credentials: 'aws-sandbox-credentials', region: 'us-east-1') {
                    sh '''
                        aws sts get-caller-identity
                        aws s3 ls
                    '''
                }
            }
        }
    }
}
```

---

## 🏗️ Phase 4: Terraform Configuration Files

We'll create Terraform files to deploy your Spring Boot application to AWS.

### Architecture Overview

```
AWS Deployment Architecture:
┌─────────────────────────────────────────────────────────┐
│  VPC (10.0.0.0/16)                                      │
│  ┌───────────────────────────────────────────────────┐  │
│  │  Public Subnet (10.0.1.0/24)                      │  │
│  │  ┌─────────────────────────────────────────────┐  │  │
│  │  │  EC2 Instance (t2.micro)                    │  │  │
│  │  │  - Docker installed                         │  │  │
│  │  │  - Spring Boot app in container             │  │  │
│  │  │  - Port 8080 exposed                        │  │  │
│  │  └─────────────────────────────────────────────┘  │  │
│  │                                                    │  │
│  │  Security Group:                                   │  │
│  │  - Allow SSH (22) from your IP                    │  │
│  │  - Allow HTTP (8080) from anywhere                │  │
│  └───────────────────────────────────────────────────┘  │
│                                                          │
│  Internet Gateway → Route Table → Public Subnet         │
└─────────────────────────────────────────────────────────┘
```

### Files We'll Create

1. **`terraform/provider.tf`** - AWS provider configuration
2. **`terraform/variables.tf`** - Input variables
3. **`terraform/main.tf`** - Main infrastructure resources
4. **`terraform/outputs.tf`** - Output values
5. **`terraform/terraform.tfvars`** - Variable values (gitignored)
6. **`.gitignore`** - Protect sensitive files

---

## 🔄 Phase 5: Jenkins Pipeline Stages

We'll add these stages to your existing Jenkinsfile:

1. **Terraform Init** - Initialize Terraform
2. **Terraform Plan** - Preview infrastructure changes
3. **Terraform Apply** - Deploy to AWS
4. **Deploy Application** - Deploy Docker container to EC2
5. **Health Check** - Verify deployment
6. **Terraform Destroy** (optional) - Clean up resources

---

## ✅ Quick Start Guide

Follow these steps in order:

### Step 1: Setup AWS CLI and Credentials
```powershell
.\scripts\setup-aws-deployment.ps1
```

### Step 2: Add Credentials to Jenkins
```powershell
.\scripts\add-aws-to-jenkins.ps1
```

### Step 3: Configure Terraform
```powershell
.\scripts\create-terraform-config.ps1
```

### Step 4: Commit and Push Changes
```powershell
git add terraform/ Jenkinsfile AWS_DEPLOYMENT_GUIDE.md
git commit -m "Add AWS deployment with Terraform"
git push origin main
```

### Step 5: Run Jenkins Pipeline
1. Go to http://localhost:8080
2. Click on your pipeline job
3. Click "Build Now"
4. Watch the deployment stages execute

---

## 📊 Pipeline Stages Overview

After integration, your Jenkins pipeline will have these stages:

| Stage | Description | Duration |
|-------|-------------|----------|
| 1. Checkout | Clone code from GitHub | ~10s |
| 2. Build | Compile with Maven | ~30s |
| 3. Unit Tests | Run 107 tests | ~45s |
| 4. Package | Create JAR file | ~20s |
| 5. SonarQube Analysis | Code quality scan | ~30s |
| 6. Quality Gate | Wait for SonarQube result | ~10s |
| 7. Deploy to Nexus | Upload artifacts | ~15s |
| 8. Build Docker Image | Create container image | ~30s |
| 9. Push Docker Image | Upload to Docker Hub | ~45s |
| **10. Terraform Init** | **Initialize Terraform** | **~20s** |
| **11. Terraform Plan** | **Preview infrastructure** | **~15s** |
| **12. Terraform Apply** | **Deploy to AWS** | **~3-5min** |
| **13. Get Deployment Info** | **Retrieve AWS outputs** | **~5s** |
| **14. Health Check** | **Verify application** | **~1-2min** |

**Total Pipeline Duration:** ~10-15 minutes (including AWS deployment)

---

## 🏗️ Infrastructure Created by Terraform

When you run the pipeline, Terraform will create:

### Network Resources
- ✅ **VPC** (10.0.0.0/16)
- ✅ **Public Subnet** (10.0.1.0/24)
- ✅ **Internet Gateway**
- ✅ **Route Table** with public route

### Compute Resources
- ✅ **EC2 Instance** (t2.micro - Free Tier)
  - Amazon Linux 2023
  - Docker pre-installed
  - Application auto-deployed
- ✅ **Elastic IP** (static public IP)

### Security Resources
- ✅ **Security Group**
  - Port 8080 (HTTP) - Application
  - Port 22 (SSH) - Optional
- ✅ **IAM Role** for EC2
  - SSM access (Session Manager)
  - CloudWatch logs

### Estimated AWS Costs
- **Free Tier:** $0/month (first 12 months)
- **After Free Tier:** ~$8-10/month
  - t2.micro: ~$8.50/month
  - EBS 20GB: ~$2/month
  - Data transfer: ~$1/month

---

## 🔧 Terraform Commands Reference

### Local Testing (Before Jenkins)

```powershell
# Navigate to terraform directory
cd terraform

# Initialize Terraform
terraform init

# Preview changes
terraform plan

# Deploy infrastructure
terraform apply

# View outputs
terraform output

# Get specific output
terraform output application_url

# Destroy infrastructure
terraform destroy
```

### Terraform State Management

```powershell
# List resources in state
terraform state list

# Show specific resource
terraform state show aws_instance.app

# Refresh state
terraform refresh
```

---

## 🐛 Troubleshooting Guide

### Issue 1: AWS Credentials Expired

**Symptom:**
```
Error: error configuring Terraform AWS Provider: error validating provider credentials
```

**Solution:**
1. Get new credentials from AWS Academy sandbox
2. Update Jenkins credentials:
   - Go to http://localhost:8080/credentials/
   - Edit `aws-sandbox-credentials`
   - Update Access Key, Secret Key, and Session Token
3. Re-run the pipeline

### Issue 2: Terraform State Lock

**Symptom:**
```
Error: Error acquiring the state lock
```

**Solution:**
```powershell
cd terraform
terraform force-unlock <LOCK_ID>
```

### Issue 3: EC2 Instance Not Accessible

**Symptom:** Cannot access application URL

**Solution:**
1. Check security group allows port 8080
2. Verify instance is running:
   ```powershell
   aws ec2 describe-instances --region us-east-1
   ```
3. Check application logs via SSM:
   ```powershell
   aws ssm start-session --target <instance-id>
   docker logs achat
   ```

### Issue 4: Health Check Fails

**Symptom:** Pipeline fails at Health Check stage

**Solution:**
1. Application may need more time to start
2. Increase sleep time in Jenkinsfile (currently 60s)
3. Check Docker container status:
   ```bash
   docker ps
   docker logs achat
   ```

### Issue 5: Insufficient Permissions

**Symptom:**
```
Error: UnauthorizedOperation: You are not authorized to perform this operation
```

**Solution:**
1. Check IAM user/role permissions
2. Required AWS permissions:
   - EC2: Full access
   - VPC: Full access
   - IAM: Create roles and policies
   - S3: Read/Write (for Terraform state)

---

## 🔐 Security Best Practices

### 1. Credentials Management
- ✅ Never commit AWS credentials to Git
- ✅ Use Jenkins Credentials Manager
- ✅ Rotate credentials regularly
- ✅ Use IAM roles instead of access keys when possible

### 2. Network Security
- ✅ Restrict SSH access to specific IPs
- ✅ Use Security Groups to limit traffic
- ✅ Enable VPC Flow Logs (optional)

### 3. Instance Security
- ✅ Use IMDSv2 (enabled in Terraform)
- ✅ Encrypt EBS volumes (enabled in Terraform)
- ✅ Use AWS Systems Manager instead of SSH
- ✅ Keep AMI updated

### 4. Application Security
- ✅ Use HTTPS in production (add ALB + ACM)
- ✅ Enable Spring Security
- ✅ Use environment variables for secrets
- ✅ Implement rate limiting

---

## 🚀 Advanced Features (Future Enhancements)

### 1. Auto Scaling
```hcl
# In terraform/main.tf
variable "enable_auto_scaling" {
  default = true
}

resource "aws_autoscaling_group" "app" {
  # ... configuration
}
```

### 2. RDS Database
```hcl
variable "create_rds" {
  default = true
}

resource "aws_db_instance" "mysql" {
  # ... configuration
}
```

### 3. Application Load Balancer
```hcl
resource "aws_lb" "app" {
  # ... configuration
}
```

### 4. CloudWatch Monitoring
```hcl
resource "aws_cloudwatch_metric_alarm" "cpu" {
  # ... configuration
}
```

### 5. S3 Backend for Terraform State
```hcl
terraform {
  backend "s3" {
    bucket = "your-terraform-state-bucket"
    key    = "achat-app/terraform.tfstate"
    region = "us-east-1"
  }
}
```

---

## 📚 Additional Resources

### AWS Documentation
- [EC2 User Guide](https://docs.aws.amazon.com/ec2/)
- [VPC User Guide](https://docs.aws.amazon.com/vpc/)
- [IAM Best Practices](https://docs.aws.amazon.com/IAM/latest/UserGuide/best-practices.html)

### Terraform Documentation
- [AWS Provider](https://registry.terraform.io/providers/hashicorp/aws/latest/docs)
- [Terraform CLI](https://www.terraform.io/docs/cli/index.html)
- [Best Practices](https://www.terraform.io/docs/cloud/guides/recommended-practices/index.html)

### Jenkins Documentation
- [Pipeline Syntax](https://www.jenkins.io/doc/book/pipeline/syntax/)
- [AWS Steps Plugin](https://plugins.jenkins.io/pipeline-aws/)
- [Credentials Plugin](https://plugins.jenkins.io/credentials/)

---

## 🎓 Learning Path

1. ✅ **Phase 1:** Setup AWS CLI and credentials
2. ✅ **Phase 2:** Add credentials to Jenkins
3. ✅ **Phase 3:** Create Terraform configuration
4. ✅ **Phase 4:** Deploy infrastructure manually
5. ✅ **Phase 5:** Integrate with Jenkins pipeline
6. 🔄 **Phase 6:** Monitor and optimize
7. 🔄 **Phase 7:** Add advanced features (ALB, RDS, Auto Scaling)

---

## ✅ Checklist

Before running the pipeline, ensure:

- [ ] AWS CLI installed and configured
- [ ] AWS credentials tested locally
- [ ] Jenkins plugins installed (AWS Credentials, Pipeline AWS Steps)
- [ ] AWS credentials added to Jenkins
- [ ] Terraform installed
- [ ] Terraform configuration validated
- [ ] Docker image pushed to Docker Hub
- [ ] Jenkinsfile updated with AWS stages
- [ ] All changes committed and pushed to GitHub

---

## 🆘 Getting Help

If you encounter issues:

1. Check the troubleshooting section above
2. Review Jenkins console output
3. Check Terraform logs
4. Verify AWS credentials are valid
5. Ensure all prerequisites are met

---

**Ready to deploy? Run the setup scripts and watch your application deploy to AWS! 🚀**

