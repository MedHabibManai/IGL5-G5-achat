# Jenkins CI/CD Pipeline Setup Guide

## Phase 1: Jenkins Setup with Docker

This guide will help you set up Jenkins in Docker and configure a complete CI/CD pipeline for the IGL5-G5-achat Spring Boot project.

---

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Starting Jenkins](#starting-jenkins)
3. [Initial Jenkins Configuration](#initial-jenkins-configuration)
4. [Installing Required Plugins](#installing-required-plugins)
5. [Configuring Jenkins Tools](#configuring-jenkins-tools)
6. [Setting Up GitHub Credentials](#setting-up-github-credentials)
7. [Creating the Pipeline Job](#creating-the-pipeline-job)
8. [Testing the Pipeline](#testing-the-pipeline)
9. [Troubleshooting](#troubleshooting)

---

## Prerequisites

Before starting, ensure you have:
- **Docker** installed and running
- **Docker Compose** installed
- **Git** installed
- **GitHub account** with access to the repository
- **At least 4GB RAM** available for Docker containers

---

## Starting Jenkins

### Step 1: Start Jenkins Container

Navigate to your project directory and run:

```bash
# Start only Jenkins (Phase 1)
docker-compose up -d jenkins

# Verify Jenkins is running
docker-compose ps
```

### Step 2: Access Jenkins

1. Open your browser and navigate to: **http://localhost:8080**
2. Wait for Jenkins to start (this may take 1-2 minutes)

### Step 3: Get Initial Admin Password

```bash
# Get the initial admin password
docker exec jenkins-cicd cat /var/jenkins_home/secrets/initialAdminPassword
```

Copy the password and paste it into the Jenkins setup wizard.

---

## Initial Jenkins Configuration

### Step 1: Install Suggested Plugins

1. After entering the admin password, select **"Install suggested plugins"**
2. Wait for the plugins to install (5-10 minutes)

### Step 2: Create Admin User

1. Create your admin user account:
   - **Username**: admin (or your preferred username)
   - **Password**: Choose a secure password
   - **Full Name**: Your name
   - **Email**: Your email address

2. Click **"Save and Continue"**

### Step 3: Configure Jenkins URL

1. Keep the default URL: **http://localhost:8080/**
2. Click **"Save and Finish"**
3. Click **"Start using Jenkins"**

---

## Installing Required Plugins

### Step 1: Navigate to Plugin Manager

1. Go to **Dashboard** → **Manage Jenkins** → **Manage Plugins**
2. Click on the **"Available"** tab

### Step 2: Install Required Plugins

Search for and install the following plugins (check the box and click "Install without restart"):

#### Essential Plugins for Phase 1:
- ✅ **Git Plugin** (usually pre-installed)
- ✅ **GitHub Plugin**
- ✅ **Pipeline Plugin** (usually pre-installed)
- ✅ **Maven Integration Plugin**
- ✅ **JUnit Plugin** (usually pre-installed)
- ✅ **Workspace Cleanup Plugin**

#### Plugins for Phase 2 (SonarQube):
- ✅ **SonarQube Scanner Plugin**

#### Plugins for Phase 3 (Nexus):
- ✅ **Nexus Artifact Uploader Plugin**

#### Plugins for Phase 5 (Docker & Kubernetes):
- ✅ **Docker Plugin**
- ✅ **Docker Pipeline Plugin**
- ✅ **Kubernetes Plugin**
- ✅ **Kubernetes CLI Plugin**

#### Optional but Recommended:
- ✅ **Blue Ocean** (modern UI for pipelines)
- ✅ **Email Extension Plugin** (for notifications)
- ✅ **Slack Notification Plugin** (if using Slack)

### Step 3: Restart Jenkins

After installing plugins, restart Jenkins:

```bash
docker-compose restart jenkins
```

Wait for Jenkins to come back online at http://localhost:8080

---

## Configuring Jenkins Tools

### Step 1: Configure JDK

1. Go to **Dashboard** → **Manage Jenkins** → **Global Tool Configuration**
2. Scroll to **JDK** section
3. Click **"Add JDK"**
4. Configure:
   - **Name**: `JDK-8`
   - **Uncheck** "Install automatically"
   - **JAVA_HOME**: `/usr/local/openjdk-8` (or find it with `docker exec jenkins-cicd which java`)

Alternatively, use automatic installation:
   - **Check** "Install automatically"
   - **Select**: Install from adoptium.net
   - **Version**: jdk-8u352-b08

### Step 2: Configure Maven

1. In the same **Global Tool Configuration** page
2. Scroll to **Maven** section
3. Click **"Add Maven"**
4. Configure:
   - **Name**: `Maven-3.8.6`
   - **Check** "Install automatically"
   - **Version**: Select `3.8.6` from dropdown

### Step 3: Configure Git (if needed)

1. Scroll to **Git** section
2. Usually auto-detected, but you can specify:
   - **Name**: `Default`
   - **Path to Git executable**: `git`

### Step 4: Save Configuration

Click **"Save"** at the bottom of the page.

---

## Setting Up GitHub Credentials

### Step 1: Generate GitHub Personal Access Token

1. Go to GitHub: **Settings** → **Developer settings** → **Personal access tokens** → **Tokens (classic)**
2. Click **"Generate new token (classic)"**
3. Configure:
   - **Note**: `Jenkins CI/CD for IGL5-G5-achat`
   - **Expiration**: 90 days (or as needed)
   - **Scopes**: Check `repo` (all sub-options)
4. Click **"Generate token"**
5. **Copy the token** (you won't see it again!)

### Step 2: Add Credentials to Jenkins

1. Go to **Dashboard** → **Manage Jenkins** → **Manage Credentials**
2. Click on **(global)** domain
3. Click **"Add Credentials"**
4. Configure:
   - **Kind**: `Username with password`
   - **Scope**: `Global`
   - **Username**: Your GitHub username
   - **Password**: Paste the Personal Access Token
   - **ID**: `github-credentials`
   - **Description**: `GitHub Access Token for CI/CD`
5. Click **"Create"**

---

## Creating the Pipeline Job

### Step 1: Create New Pipeline

1. Go to **Dashboard** → **New Item**
2. Enter item name: `IGL5-G5-Achat-Pipeline`
3. Select **"Pipeline"**
4. Click **"OK"**

### Step 2: Configure General Settings

1. **Description**: `CI/CD Pipeline for Spring Boot Achat Application`
2. **Check**: "GitHub project"
   - **Project URL**: `https://github.com/YOUR_USERNAME/IGL5-G5-achat/`

### Step 3: Configure Build Triggers

**Check**: "GitHub hook trigger for GITScm polling"

This will trigger builds automatically when you push to GitHub.

### Step 4: Configure Pipeline

1. **Definition**: Select `Pipeline script from SCM`
2. **SCM**: Select `Git`
3. **Repository URL**: `https://github.com/YOUR_USERNAME/IGL5-G5-achat.git`
4. **Credentials**: Select `github-credentials` (created earlier)
5. **Branches to build**: `*/main`
6. **Script Path**: `Jenkinsfile`

### Step 5: Save the Job

Click **"Save"** at the bottom.

---

## Testing the Pipeline

### Step 1: Manual Build

1. Go to your pipeline job: **IGL5-G5-Achat-Pipeline**
2. Click **"Build Now"**
3. Watch the build progress in the **Build History**
4. Click on the build number (e.g., #1) to see details
5. Click **"Console Output"** to see logs

### Step 2: Verify Build Stages

The pipeline should execute these stages:
1. ✅ **Checkout** - Clone code from GitHub
2. ✅ **Build** - Compile the application
3. ✅ **Unit Tests** - Run JUnit tests
4. ✅ **Package** - Create JAR file

### Step 3: Check Test Results

1. Go to the build page
2. Click **"Test Result"** to see JUnit test results
3. Verify all tests passed

### Step 4: Check Artifacts

1. On the build page, you should see **"Build Artifacts"**
2. The JAR file should be listed: `achat-1.0.jar`

---

## Setting Up GitHub Webhook (Optional but Recommended)

To automatically trigger builds on every push to GitHub:

### Step 1: Configure Webhook in GitHub

1. Go to your GitHub repository
2. Click **Settings** → **Webhooks** → **Add webhook**
3. Configure:
   - **Payload URL**: `http://YOUR_JENKINS_URL:8080/github-webhook/`
   - **Content type**: `application/json`
   - **Which events**: Select "Just the push event"
   - **Active**: Check this box
4. Click **"Add webhook"**

**Note**: If Jenkins is running locally, you'll need to use a service like **ngrok** to expose it to the internet, or configure it on a public server.

---

## Troubleshooting

### Issue 1: Jenkins Container Won't Start

```bash
# Check logs
docker-compose logs jenkins

# Restart the container
docker-compose restart jenkins
```

### Issue 2: Cannot Access Jenkins UI

```bash
# Verify port is not in use
netstat -an | findstr "8080"  # Windows
netstat -an | grep "8080"     # Linux/Mac

# Try a different port in docker-compose.yml
ports:
  - "8081:8080"
```

### Issue 3: Maven Build Fails

```bash
# Enter Jenkins container
docker exec -it jenkins-cicd bash

# Verify Maven is installed
mvn --version

# Check Java version
java -version
```

### Issue 4: Tests Fail

```bash
# Run tests locally first
mvn clean test

# Check test reports in target/surefire-reports/
```

### Issue 5: Git Checkout Fails

- Verify GitHub credentials are correct
- Check repository URL is correct
- Ensure Personal Access Token has `repo` scope

---

## Next Steps

Once Phase 1 is working successfully:

1. ✅ Verify builds trigger automatically on push to `main` branch
2. ✅ Confirm all tests pass
3. ✅ Check artifacts are archived

You're now ready for **Phase 2: SonarQube Integration**!

---

## Useful Commands

```bash
# Start Jenkins
docker-compose up -d jenkins

# Stop Jenkins
docker-compose stop jenkins

# View Jenkins logs
docker-compose logs -f jenkins

# Restart Jenkins
docker-compose restart jenkins

# Access Jenkins container
docker exec -it jenkins-cicd bash

# Remove Jenkins (WARNING: deletes data)
docker-compose down -v
```

---

## Additional Resources

- [Jenkins Documentation](https://www.jenkins.io/doc/)
- [Jenkins Pipeline Syntax](https://www.jenkins.io/doc/book/pipeline/syntax/)
- [Maven Integration](https://www.jenkins.io/doc/book/pipeline/maven/)
- [GitHub Integration](https://plugins.jenkins.io/github/)

---

**Created for**: IGL5-G5-Achat DevOps Project  
**Phase**: 1 - Jenkins Setup with Docker  
**Last Updated**: 2025-10-29

