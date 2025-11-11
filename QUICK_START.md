# 🚀 Quick Start Guide - Jenkins CI/CD Pipeline

## ⚡ 5-Minute Setup

### Step 1: Start Jenkins (Choose your OS)

**Windows:**
```bash
start-jenkins.bat
```

**Linux/Mac:**
```bash
chmod +x start-jenkins.sh
./start-jenkins.sh
```

### Step 2: Access Jenkins

Open browser: **http://localhost:8080**

### Step 3: Initial Setup

1. Copy the **Initial Admin Password** from the terminal
2. Paste it into Jenkins
3. Click **"Install suggested plugins"**
4. Create admin user
5. Click **"Start using Jenkins"**

### Step 4: Install Additional Plugins

1. Go to **Manage Jenkins** → **Manage Plugins** → **Available**
2. Search and install:
   - ✅ Maven Integration Plugin
   - ✅ GitHub Plugin
   - ✅ Pipeline Plugin
3. Restart Jenkins

### Step 5: Configure Tools

1. Go to **Manage Jenkins** → **Global Tool Configuration**
2. Add **JDK**:
   - Name: `JDK-8`
   - Install automatically: ✅
   - Version: jdk-8u352-b08
3. Add **Maven**:
   - Name: `Maven-3.8.6`
   - Install automatically: ✅
   - Version: 3.8.6
4. Click **Save**

### Step 6: Add GitHub Credentials

1. Generate GitHub Personal Access Token:
   - GitHub → Settings → Developer settings → Personal access tokens
   - Generate new token with `repo` scope
2. In Jenkins: **Manage Jenkins** → **Manage Credentials** → **Add Credentials**
   - Kind: Username with password
   - Username: Your GitHub username
   - Password: Your Personal Access Token
   - ID: `github-credentials`
3. Click **Create**

### Step 7: Create Pipeline Job

1. **Dashboard** → **New Item**
2. Name: `IGL5-G5-Achat-Pipeline`
3. Type: **Pipeline**
4. Configure:
   - Definition: Pipeline script from SCM
   - SCM: Git
   - Repository URL: `https://github.com/YOUR_USERNAME/IGL5-G5-achat.git`
   - Credentials: `github-credentials`
   - Branch: `*/main`
   - Script Path: `Jenkinsfile`
5. Click **Save**

### Step 8: Run Your First Build

1. Click **"Build Now"**
2. Watch the build progress
3. Check **Console Output** for logs
4. Verify all stages pass ✅

---

## 📊 Expected Results

After successful build, you should see:

✅ **Stage 1: Checkout** - Code cloned from GitHub  
✅ **Stage 2: Build** - Maven compilation successful  
✅ **Stage 3: Unit Tests** - 109 tests passed  
✅ **Stage 4: Package** - JAR file created  

**Build Artifacts**: `achat-1.0.jar`  
**Test Results**: 109 tests, 0 failures

---

## 🔧 Common Commands

```bash
# Start Jenkins
docker-compose up -d jenkins

# Stop Jenkins
docker-compose stop jenkins

# View logs
docker-compose logs -f jenkins

# Get admin password
docker exec jenkins-cicd cat /var/jenkins_home/secrets/initialAdminPassword

# Restart Jenkins
docker-compose restart jenkins
```

---

## 🆘 Quick Troubleshooting

**Problem**: Jenkins won't start  
**Solution**: Check Docker is running, restart Docker Desktop

**Problem**: Build fails  
**Solution**: Run `mvn clean install` locally first to verify code compiles

**Problem**: Can't access Jenkins UI  
**Solution**: Verify port 8080 is not in use, check firewall settings

**Problem**: Tests fail  
**Solution**: Check test reports in Jenkins, verify H2 database is configured

---

## 📚 Full Documentation

- **Detailed Setup**: [JENKINS_SETUP.md](JENKINS_SETUP.md)
- **Complete Guide**: [CICD_README.md](CICD_README.md)
- **Pipeline Definition**: [Jenkinsfile](Jenkinsfile)

---

## ✅ Checklist

- [ ] Docker Desktop running
- [ ] Jenkins container started
- [ ] Initial admin password retrieved
- [ ] Plugins installed
- [ ] JDK and Maven configured
- [ ] GitHub credentials added
- [ ] Pipeline job created
- [ ] First build successful
- [ ] All tests passing

---

## 🎯 Next Steps

Once Phase 1 is working:

1. **Phase 2**: Add SonarQube for code quality
2. **Phase 3**: Add Nexus for artifact storage
3. **Phase 4**: Add Prometheus & Grafana for monitoring
4. **Phase 5**: Deploy to Kubernetes on AWS

---

**Need Help?** Check [JENKINS_SETUP.md](JENKINS_SETUP.md) for detailed instructions.

