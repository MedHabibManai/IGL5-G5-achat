# 🚀 DevOps CI/CD Pipeline - Quick Setup Guide

This guide will help you set up the complete CI/CD pipeline on your local machine in **less than 10 minutes**.

## 📋 Prerequisites

Before you start, make sure you have:

- ✅ **Docker Desktop** installed and running
- ✅ **Git** installed
- ✅ **PowerShell** (Windows) or **Bash** (Linux/Mac)
- ✅ **8GB RAM minimum** (16GB recommended)
- ✅ **20GB free disk space**

## 🎯 Quick Start (3 Steps)

### Step 1: Clone the Repository

```bash
git clone https://github.com/MedHabibManai/IGL5-G5-achat.git
cd IGL5-G5-achat
```

### Step 2: Run the Setup Script

**Windows (PowerShell):**
```powershell
.\scripts\setup-pipeline.ps1
```

**Linux/Mac (Bash):**
```bash
chmod +x scripts/setup-pipeline.sh
./scripts/setup-pipeline.sh
```

### Step 3: Configure AWS Credentials (Optional)

If you want to deploy to AWS:

1. Copy the AWS credentials template:
   ```powershell
   cp .env.aws.example .env.aws
   ```

2. Edit `.env.aws` with your AWS Academy credentials:
   ```powershell
   notepad .env.aws  # Windows
   nano .env.aws     # Linux/Mac
   ```

3. Update Jenkins with your credentials:
   ```powershell
   .\scripts\update-jenkins-aws-credentials.ps1
   ```

**That's it!** 🎉

---

## 🌐 Access the Services

After setup completes, access these URLs:

| Service | URL | Username | Password |
|---------|-----|----------|----------|
| **Jenkins** | http://localhost:8080 | admin | Check console output |
| **SonarQube** | http://localhost:9000 | admin | ##Azeraoi123 |
| **Nexus** | http://localhost:8081 | admin | ##Azeraoi123 |
| **Grafana** | http://localhost:3000 | admin | admin |
| **Prometheus** | http://localhost:9090 | - | - |

---

## 🔧 What Gets Installed

The setup script automatically:

1. ✅ Starts all Docker containers (Jenkins, SonarQube, Nexus, Prometheus, Grafana)
2. ✅ Configures Jenkins with Maven and JDK
3. ✅ Sets up SonarQube with authentication token
4. ✅ Configures Nexus repository
5. ✅ Installs AWS CLI and Terraform in Jenkins container
6. ✅ Sets up monitoring dashboards
7. ✅ Creates the Jenkins pipeline job

---

## 📊 Running the Pipeline

1. Open Jenkins: http://localhost:8080
2. Click on **"IGL5-G5-Achat-Pipeline"**
3. Click **"Build Now"**

The pipeline will:
- ✅ Build the application
- ✅ Run 107 unit tests
- ✅ Analyze code quality (SonarQube)
- ✅ Deploy artifacts (Nexus)
- ✅ Build Docker image
- ✅ Push to Docker Hub
- ✅ Deploy to AWS (if credentials configured)

---

## 🔄 Updating AWS Credentials

AWS Academy credentials expire every 3-4 hours. To update:

1. Get new credentials from AWS Academy
2. Edit `.env.aws`:
   ```powershell
   notepad .env.aws
   ```
3. Run the update script:
   ```powershell
   .\scripts\update-jenkins-aws-credentials.ps1
   ```

---

## 🛠️ Troubleshooting

### Docker containers not starting?

```bash
docker-compose down
docker-compose up -d
```

### Jenkins not accessible?

Wait 2-3 minutes for Jenkins to fully start, then check:
```bash
docker logs jenkins-cicd
```

### Need to reset everything?

```bash
docker-compose down -v
docker-compose up -d
```

### Check container status:

```bash
docker-compose ps
```

---

## 📁 Project Structure

```
IGL5-G5-achat/
├── docker-compose.yml          # All services configuration
├── Dockerfile                  # Application Docker image
├── Dockerfile.jenkins          # Custom Jenkins image
├── Jenkinsfile                 # CI/CD pipeline definition
├── .env.aws.example           # AWS credentials template
├── scripts/
│   ├── setup-pipeline.ps1     # Windows setup script
│   ├── setup-pipeline.sh      # Linux/Mac setup script
│   └── update-jenkins-aws-credentials.ps1
├── terraform/                  # AWS infrastructure code
│   ├── main.tf
│   ├── variables.tf
│   ├── outputs.tf
│   └── provider.tf
└── monitoring/                 # Prometheus & Grafana configs
    ├── prometheus/
    └── grafana/
```

---

## 🎓 For Team Members

### First Time Setup

1. Make sure Docker Desktop is running
2. Clone the repository
3. Run the setup script
4. Wait for all services to start (~5 minutes)
5. Access Jenkins and run the pipeline

### Daily Usage

1. Start Docker Desktop
2. All services start automatically
3. Open Jenkins: http://localhost:8080
4. Click "Build Now" to run the pipeline

### Stopping the Pipeline

```bash
docker-compose stop
```

### Starting Again

```bash
docker-compose start
```

---

## 🔐 Security Notes

- **Never commit `.env.aws`** - It contains sensitive credentials
- **Change default passwords** in production environments
- **AWS credentials expire** - Update them regularly
- **Docker Hub credentials** - Configure in Jenkins for pushing images

---

## 📞 Need Help?

1. Check the logs: `docker-compose logs [service-name]`
2. Verify Docker is running: `docker ps`
3. Check disk space: `docker system df`
4. Clean up unused resources: `docker system prune`

---

## ✨ What Makes This Easy to Share?

✅ **One-command setup** - No manual configuration  
✅ **All services in Docker** - Consistent across machines  
✅ **Automated configuration** - Scripts handle everything  
✅ **Template files** - Easy to customize  
✅ **Clear documentation** - Step-by-step instructions  
✅ **Version controlled** - Everything in Git  

---

## 🚀 Ready to Go!

Your team can now:
- Clone the repo
- Run one script
- Start building!

**Happy DevOps-ing!** 🎉

