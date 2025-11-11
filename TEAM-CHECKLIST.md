# ✅ Team Member Setup Checklist

Use this checklist to set up the CI/CD pipeline on your machine.

## 📋 Pre-Setup Checklist

- [ ] **Docker Desktop installed**
  - Download from: https://www.docker.com/products/docker-desktop
  - Minimum version: 20.10+
  
- [ ] **Docker Desktop is running**
  - Check system tray (Windows) or menu bar (Mac)
  - Verify with: `docker --version`
  
- [ ] **Git installed**
  - Verify with: `git --version`
  - Download from: https://git-scm.com/downloads
  
- [ ] **System Requirements Met**
  - [ ] 8GB RAM minimum (16GB recommended)
  - [ ] 20GB free disk space
  - [ ] Windows 10/11, macOS 10.15+, or Linux

## 🚀 Setup Steps

### Step 1: Clone Repository

- [ ] Open terminal/PowerShell
- [ ] Navigate to your workspace directory
- [ ] Run: `git clone https://github.com/MedHabibManai/IGL5-G5-achat.git`
- [ ] Run: `cd IGL5-G5-achat`

### Step 2: Run Setup Script

**Windows:**
- [ ] Open PowerShell as Administrator
- [ ] Run: `.\scripts\setup-pipeline.ps1`
- [ ] Wait for completion (5-10 minutes)

**Linux/Mac:**
- [ ] Open Terminal
- [ ] Run: `chmod +x scripts/setup-pipeline.sh`
- [ ] Run: `./scripts/setup-pipeline.sh`
- [ ] Wait for completion (5-10 minutes)

### Step 3: Save Jenkins Password

- [ ] Copy the Jenkins admin password from console output
- [ ] Save it somewhere safe (you'll need it to unlock Jenkins)

### Step 4: Access Jenkins

- [ ] Open browser: http://localhost:8080
- [ ] Paste the admin password
- [ ] Click "Continue"
- [ ] Select "Install suggested plugins"
- [ ] Wait for plugins to install (~3 minutes)

### Step 5: Create Jenkins Admin User

- [ ] Fill in your details:
  - [ ] Username: (your choice)
  - [ ] Password: (your choice)
  - [ ] Full name: (your name)
  - [ ] Email: (your email)
- [ ] Click "Save and Continue"
- [ ] Click "Save and Finish"
- [ ] Click "Start using Jenkins"

### Step 6: Create Pipeline Job

- [ ] Click "New Item"
- [ ] Enter name: `IGL5-G5-Achat-Pipeline`
- [ ] Select "Pipeline"
- [ ] Click "OK"
- [ ] Scroll to "Pipeline" section
- [ ] Select "Pipeline script from SCM"
- [ ] SCM: Git
- [ ] Repository URL: `https://github.com/MedHabibManai/IGL5-G5-achat.git`
- [ ] Branch: `*/main`
- [ ] Script Path: `Jenkinsfile`
- [ ] Click "Save"

### Step 7: Configure Docker Hub Credentials

- [ ] Go to Jenkins → Manage Jenkins → Credentials
- [ ] Click "(global)" domain
- [ ] Click "Add Credentials"
- [ ] Fill in:
  - [ ] Kind: Username with password
  - [ ] Username: (your Docker Hub username)
  - [ ] Password: (your Docker Hub password)
  - [ ] ID: `dockerhub-credentials`
  - [ ] Description: Docker Hub Credentials
- [ ] Click "Create"

### Step 8: Verify Other Services

- [ ] **SonarQube**: http://localhost:9000
  - [ ] Login: admin / ##Azeraoi123
  - [ ] Change password if prompted
  
- [ ] **Nexus**: http://localhost:8081
  - [ ] Login: admin / ##Azeraoi123
  - [ ] Complete setup wizard if shown
  
- [ ] **Grafana**: http://localhost:3000
  - [ ] Login: admin / admin
  - [ ] Change password if prompted
  
- [ ] **Prometheus**: http://localhost:9090
  - [ ] No login required
  - [ ] Check targets are up

### Step 9: Run Your First Build

- [ ] Go to Jenkins: http://localhost:8080
- [ ] Click on "IGL5-G5-Achat-Pipeline"
- [ ] Click "Build Now"
- [ ] Watch the build progress
- [ ] Verify all stages pass (may take 10-15 minutes)

## 🔧 Optional: AWS Configuration

Only complete this if you need to deploy to AWS:

- [ ] Get AWS Academy credentials
- [ ] Copy template: `cp .env.aws.example .env.aws`
- [ ] Edit `.env.aws` with your credentials
- [ ] Run update script:
  - Windows: `.\scripts\update-jenkins-aws-credentials.ps1`
  - Linux/Mac: `./scripts/update-jenkins-aws-credentials.sh`
- [ ] Verify: `docker exec jenkins-cicd aws sts get-caller-identity`

## ✅ Verification Checklist

After setup, verify everything works:

- [ ] **Docker containers running**
  - Run: `docker-compose ps`
  - All services should show "Up"

- [ ] **Jenkins accessible**
  - URL: http://localhost:8080
  - Can login successfully

- [ ] **SonarQube accessible**
  - URL: http://localhost:9000
  - Can login successfully

- [ ] **Nexus accessible**
  - URL: http://localhost:8081
  - Can login successfully

- [ ] **Pipeline job created**
  - Visible in Jenkins dashboard

- [ ] **First build successful**
  - All 9 stages pass (or 14 if AWS configured)
  - No red/failed stages

- [ ] **Docker image pushed**
  - Check Docker Hub for your image

## 🎯 Daily Usage Checklist

Use this for your daily workflow:

### Starting Your Day

- [ ] Start Docker Desktop
- [ ] Verify services: `docker-compose ps`
- [ ] Open Jenkins: http://localhost:8080

### Making Changes

- [ ] Create feature branch: `git checkout -b feature/my-feature`
- [ ] Make your changes
- [ ] Commit: `git commit -m "Description"`
- [ ] Push: `git push origin feature/my-feature`
- [ ] Trigger build in Jenkins

### Ending Your Day

- [ ] Commit and push all changes
- [ ] (Optional) Stop services: `docker-compose stop`
- [ ] (Optional) Keep Docker Desktop running for tomorrow

## 🆘 Troubleshooting Checklist

If something doesn't work:

- [ ] **Docker Desktop is running**
  - Check system tray/menu bar
  - Restart if needed

- [ ] **All containers are up**
  - Run: `docker-compose ps`
  - Restart if needed: `docker-compose restart`

- [ ] **Check service logs**
  - Jenkins: `docker-compose logs jenkins-cicd`
  - SonarQube: `docker-compose logs sonarqube`
  - Nexus: `docker-compose logs nexus`

- [ ] **Enough disk space**
  - Check: `docker system df`
  - Clean: `docker system prune`

- [ ] **Ports not in use**
  - 8080 (Jenkins)
  - 9000 (SonarQube)
  - 8081 (Nexus)
  - 3000 (Grafana)
  - 9090 (Prometheus)

- [ ] **Restart everything**
  - `docker-compose down`
  - `docker-compose up -d`
  - Wait 3-5 minutes

## 📞 Getting Help

If you're stuck:

1. [ ] Check SETUP.md for detailed instructions
2. [ ] Check README-TEAM.md for troubleshooting
3. [ ] Check service logs: `docker-compose logs [service]`
4. [ ] Ask a team member who has it working
5. [ ] Check Jenkins console output for errors

## 🎓 Learning Resources

After setup, explore these:

- [ ] Read README-TEAM.md completely
- [ ] Explore Jenkins pipeline stages
- [ ] Check SonarQube code quality reports
- [ ] Browse Nexus artifacts
- [ ] View Grafana dashboards
- [ ] Understand the Jenkinsfile
- [ ] Review Terraform configuration

## ✨ Success Criteria

You're ready when:

- [ ] All services are running
- [ ] You can access all web UIs
- [ ] Pipeline builds successfully
- [ ] You understand the workflow
- [ ] You can make changes and trigger builds
- [ ] You know how to troubleshoot basic issues

---

**Congratulations! You're all set up! 🎉**

Now you can:
- Make code changes
- Run the pipeline
- Deploy to AWS (if configured)
- Monitor with Grafana
- Collaborate with your team

**Happy coding! 🚀**

