# Jenkins Plugins Installation Guide

## 🎯 Quick Reference

### Phase 1 - Essential Plugins (Install Now)
These plugins are required for the basic CI/CD pipeline:

```
✅ Git Plugin
✅ GitHub Plugin
✅ Pipeline Plugin
✅ Pipeline: Stage View Plugin
✅ Maven Integration Plugin
✅ JUnit Plugin
✅ Workspace Cleanup Plugin
✅ Timestamper Plugin
✅ AnsiColor Plugin
✅ Blue Ocean (Recommended)
✅ Dashboard View Plugin
```

### Phase 2 - SonarQube (Install When Ready)
```
✅ SonarQube Scanner Plugin
```

### Phase 3 - Nexus (Install When Ready)
```
✅ Nexus Artifact Uploader Plugin
```

### Phase 5 - Docker & Kubernetes (Install When Ready)
```
✅ Docker Plugin
✅ Docker Pipeline Plugin
✅ Kubernetes Plugin
✅ Kubernetes CLI Plugin
```

### Optional - Notifications & Utilities
```
✅ Email Extension Plugin
✅ Slack Notification Plugin
✅ Build Timeout Plugin
```

---

## 📖 Detailed Installation Steps

### Step 1: Access Plugin Manager

1. Open Jenkins in your browser: **http://localhost:8080**
2. Complete the initial setup wizard (use the password: `dc47ac66c0464b68b5082fccd0e71b67`)
3. When asked, select **"Install suggested plugins"** - this will install most core plugins
4. Wait for the installation to complete (5-10 minutes)
5. Create your admin user account
6. Click **"Start using Jenkins"**

### Step 2: Install Additional Plugins

After the initial setup:

1. Go to **Dashboard** → **Manage Jenkins** → **Manage Plugins**
2. Click on the **"Available"** tab
3. Use the search box to find plugins

---

## 🔍 Plugin-by-Plugin Installation

### **Essential Plugins for Phase 1**

#### 1. GitHub Plugin
- **Search for**: `GitHub Plugin`
- **Purpose**: Integrates Jenkins with GitHub repositories
- **Required for**: Triggering builds from GitHub webhooks
- **Status**: ✅ Install Now

#### 2. Pipeline: Stage View Plugin
- **Search for**: `Pipeline: Stage View`
- **Purpose**: Visualize pipeline stages in a nice UI
- **Required for**: Better pipeline visualization
- **Status**: ✅ Install Now

#### 3. Maven Integration Plugin
- **Search for**: `Maven Integration`
- **Purpose**: Better Maven support in Jenkins
- **Required for**: Building Maven projects
- **Status**: ✅ Install Now

#### 4. Workspace Cleanup Plugin
- **Search for**: `Workspace Cleanup`
- **Purpose**: Clean workspace before/after builds
- **Required for**: Preventing build artifacts from accumulating
- **Status**: ✅ Install Now

#### 5. Timestamper Plugin
- **Search for**: `Timestamper`
- **Purpose**: Add timestamps to console output
- **Required for**: Debugging and performance analysis
- **Status**: ✅ Install Now

#### 6. AnsiColor Plugin
- **Search for**: `AnsiColor`
- **Purpose**: Colorize console output
- **Required for**: Better readability of build logs
- **Status**: ✅ Install Now

#### 7. Blue Ocean (Highly Recommended!)
- **Search for**: `Blue Ocean`
- **Purpose**: Modern, beautiful UI for Jenkins pipelines
- **Required for**: Much better user experience
- **Status**: ✅ Install Now (Recommended)
- **Note**: This will install several related plugins

#### 8. Dashboard View Plugin
- **Search for**: `Dashboard View`
- **Purpose**: Create custom dashboards
- **Required for**: Better organization of jobs
- **Status**: ✅ Install Now

---

### **Phase 2 Plugins (Install Later)**

#### 9. SonarQube Scanner Plugin
- **Search for**: `SonarQube Scanner`
- **Purpose**: Integrate with SonarQube for code quality analysis
- **Required for**: Phase 2 - Code Quality Analysis
- **Status**: 🔄 Install when starting Phase 2

---

### **Phase 3 Plugins (Install Later)**

#### 10. Nexus Artifact Uploader Plugin
- **Search for**: `Nexus Artifact Uploader`
- **Purpose**: Upload build artifacts to Nexus Repository
- **Required for**: Phase 3 - Artifact Management
- **Status**: 🔄 Install when starting Phase 3

---

### **Phase 5 Plugins (Install Later)**

#### 11. Docker Plugin
- **Search for**: `Docker Plugin`
- **Purpose**: Build and manage Docker containers
- **Required for**: Phase 5 - Containerization
- **Status**: 🔄 Install when starting Phase 5

#### 12. Docker Pipeline Plugin
- **Search for**: `Docker Pipeline`
- **Purpose**: Use Docker commands in Jenkins Pipeline
- **Required for**: Phase 5 - Docker in Pipeline
- **Status**: 🔄 Install when starting Phase 5

#### 13. Kubernetes Plugin
- **Search for**: `Kubernetes`
- **Purpose**: Deploy to Kubernetes clusters
- **Required for**: Phase 5 - Kubernetes Deployment
- **Status**: 🔄 Install when starting Phase 5

#### 14. Kubernetes CLI Plugin
- **Search for**: `Kubernetes CLI`
- **Purpose**: Run kubectl commands in pipeline
- **Required for**: Phase 5 - Kubernetes Management
- **Status**: 🔄 Install when starting Phase 5

---

### **Optional Plugins**

#### 15. Email Extension Plugin
- **Search for**: `Email Extension`
- **Purpose**: Send detailed email notifications
- **Required for**: Build notifications via email
- **Status**: ⭐ Optional

#### 16. Slack Notification Plugin
- **Search for**: `Slack Notification`
- **Purpose**: Send notifications to Slack
- **Required for**: Team notifications via Slack
- **Status**: ⭐ Optional (if you use Slack)

#### 17. Build Timeout Plugin
- **Search for**: `Build Timeout`
- **Purpose**: Automatically abort builds that run too long
- **Required for**: Preventing stuck builds
- **Status**: ⭐ Optional but recommended

---

## 🚀 Quick Installation Method

### Install All Phase 1 Plugins at Once:

1. Go to **Manage Jenkins** → **Manage Plugins** → **Available**
2. Check the boxes for these plugins:
   - [ ] GitHub Plugin
   - [ ] Pipeline: Stage View Plugin
   - [ ] Maven Integration Plugin
   - [ ] Workspace Cleanup Plugin
   - [ ] Timestamper Plugin
   - [ ] AnsiColor Plugin
   - [ ] Blue Ocean
   - [ ] Dashboard View Plugin

3. Click **"Install without restart"** at the bottom
4. Wait for installation to complete
5. Check **"Restart Jenkins when installation is complete and no jobs are running"**

---

## ✅ Verification Checklist

After installation, verify plugins are installed:

1. Go to **Manage Jenkins** → **Manage Plugins** → **Installed**
2. Search for each plugin name
3. Verify it appears in the list

### Essential Plugins Checklist:
- [ ] Git Plugin
- [ ] GitHub Plugin
- [ ] Pipeline Plugin
- [ ] Pipeline: Stage View Plugin
- [ ] Maven Integration Plugin
- [ ] JUnit Plugin
- [ ] Workspace Cleanup Plugin
- [ ] Timestamper Plugin
- [ ] AnsiColor Plugin
- [ ] Blue Ocean
- [ ] Dashboard View Plugin

---

## 🔧 Post-Installation Configuration

### After Installing Plugins:

1. **Restart Jenkins** (if not done automatically):
   - Go to **Manage Jenkins** → **Reload Configuration from Disk**
   - Or restart the container: `docker-compose restart jenkins`

2. **Verify Plugin Functionality**:
   - Check that no plugins have errors
   - Go to **Manage Jenkins** → **Manage Plugins** → **Installed**
   - Look for any red warnings

3. **Configure Tools** (Next Step):
   - Go to **Manage Jenkins** → **Global Tool Configuration**
   - Configure JDK and Maven (see JENKINS_SETUP.md)

---

## 📊 Plugin Summary by Phase

| Phase | Plugins | Purpose |
|-------|---------|---------|
| **Phase 1** | 11 plugins | Basic CI/CD with build, test, and artifact archiving |
| **Phase 2** | 1 plugin | Code quality analysis with SonarQube |
| **Phase 3** | 1 plugin | Artifact storage in Nexus |
| **Phase 5** | 4 plugins | Docker containerization and Kubernetes deployment |
| **Optional** | 3 plugins | Notifications and utilities |
| **TOTAL** | 20 plugins | Complete CI/CD pipeline |

---

## 🐛 Troubleshooting

### Plugin Installation Fails

**Problem**: Plugin installation fails or shows errors

**Solution**:
1. Check internet connection
2. Go to **Manage Jenkins** → **Manage Plugins** → **Advanced**
3. Click **"Check now"** to update plugin metadata
4. Try installing again

### Plugin Conflicts

**Problem**: Plugins have dependency conflicts

**Solution**:
1. Install plugins one at a time
2. Restart Jenkins after each installation
3. Check **Manage Jenkins** for warnings

### Jenkins Won't Restart

**Problem**: Jenkins hangs during restart

**Solution**:
```bash
# Force restart the container
docker-compose restart jenkins

# Or stop and start
docker-compose stop jenkins
docker-compose up -d jenkins
```

---

## 📚 Next Steps

After installing plugins:

1. ✅ **Configure Tools** - Set up JDK and Maven (see JENKINS_SETUP.md)
2. ✅ **Add Credentials** - Add GitHub credentials (see JENKINS_SETUP.md)
3. ✅ **Create Pipeline Job** - Create your first pipeline (see JENKINS_SETUP.md)
4. ✅ **Run First Build** - Test the pipeline

---

## 🔗 Useful Links

- [Jenkins Plugin Index](https://plugins.jenkins.io/)
- [Blue Ocean Documentation](https://www.jenkins.io/doc/book/blueocean/)
- [Pipeline Syntax Reference](https://www.jenkins.io/doc/book/pipeline/syntax/)

---

**Created for**: IGL5-G5-Achat DevOps Project  
**Phase**: 1 - Jenkins Plugin Installation  
**Last Updated**: 2025-10-29

