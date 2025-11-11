# CI/CD Pipeline for IGL5-G5-Achat

Complete CI/CD pipeline implementation with Jenkins, SonarQube, Nexus, Prometheus, Grafana, and Kubernetes deployment on AWS.

---

## 🚀 Quick Start

### Prerequisites
- Docker Desktop installed and running
- Git installed
- GitHub account with repository access
- At least 4GB RAM available

### Start Jenkins (Phase 1)

**Windows:**
```bash
start-jenkins.bat
```

**Linux/Mac:**
```bash
chmod +x start-jenkins.sh
./start-jenkins.sh
```

**Manual Start:**
```bash
docker-compose up -d jenkins
```

Access Jenkins at: **http://localhost:8080**

---

## 📋 Implementation Phases

### ✅ Phase 1: Jenkins Setup with Docker (CURRENT)
- [x] Docker Compose configuration
- [x] Jenkins container setup
- [x] Basic CI/CD pipeline (Jenkinsfile)
- [x] Maven build and test automation
- [x] JUnit test reporting
- [x] Artifact archiving

**Status**: ✅ **READY TO USE**

**Documentation**: See [JENKINS_SETUP.md](JENKINS_SETUP.md)

---

### 🔄 Phase 2: SonarQube Integration (NEXT)
- [ ] SonarQube container setup
- [ ] Code quality analysis
- [ ] Quality gates
- [ ] Code coverage reporting
- [ ] Security vulnerability scanning

**Start Phase 2:**
```bash
docker-compose --profile phase2 up -d
```

**Access SonarQube**: http://localhost:9000  
**Default credentials**: admin/admin

---

### 🔄 Phase 3: Nexus Repository
- [ ] Nexus container setup
- [ ] Maven repository configuration
- [ ] Artifact versioning and storage
- [ ] Docker registry (optional)

**Start Phase 3:**
```bash
docker-compose --profile phase3 up -d
```

**Access Nexus**: http://localhost:8081  
**Default credentials**: admin/admin123

---

### 🔄 Phase 4: Monitoring (Prometheus & Grafana)
- [ ] Prometheus setup for metrics collection
- [ ] Grafana dashboards
- [ ] Jenkins metrics monitoring
- [ ] Application performance monitoring
- [ ] Alert configuration

**Start Phase 4:**
```bash
docker-compose --profile phase4 up -d
```

**Access Prometheus**: http://localhost:9090  
**Access Grafana**: http://localhost:3000 (admin/admin)

---

### 🔄 Phase 5: Kubernetes Deployment on AWS
- [ ] Terraform infrastructure provisioning
- [ ] EKS cluster setup
- [ ] Kubernetes manifests
- [ ] Docker image building
- [ ] Automated deployment pipeline
- [ ] Rolling updates

---

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                         GitHub                              │
│                    (Source Repository)                      │
└────────────────────┬────────────────────────────────────────┘
                     │ Push/Merge to main
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                        Jenkins                              │
│                    (CI/CD Orchestrator)                     │
│  ┌──────────┬──────────┬──────────┬──────────┬──────────┐  │
│  │ Checkout │  Build   │   Test   │ Package  │  Deploy  │  │
│  └──────────┴──────────┴──────────┴──────────┴──────────┘  │
└───┬─────────────┬─────────────┬─────────────┬──────────────┘
    │             │             │             │
    ▼             ▼             ▼             ▼
┌────────┐  ┌──────────┐  ┌─────────┐  ┌──────────────┐
│SonarQube│  │  Nexus   │  │Prometheus│  │  Kubernetes  │
│(Quality)│  │(Artifacts)│  │& Grafana │  │   (AWS EKS)  │
└────────┘  └──────────┘  └─────────┘  └──────────────┘
```

---

## 📦 Project Structure

```
IGL5-G5-achat/
├── src/                          # Source code
│   ├── main/
│   └── test/
├── docker-compose.yml            # All services configuration
├── Jenkinsfile                   # CI/CD pipeline definition
├── JENKINS_SETUP.md              # Phase 1 detailed guide
├── CICD_README.md                # This file
├── start-jenkins.sh              # Quick start script (Linux/Mac)
├── start-jenkins.bat             # Quick start script (Windows)
├── .gitignore                    # Git ignore rules
├── pom.xml                       # Maven configuration
│
├── sonar-project.properties      # SonarQube config (Phase 2)
├── Dockerfile                    # Application Docker image (Phase 5)
│
├── k8s/                          # Kubernetes manifests (Phase 5)
│   ├── deployment.yaml
│   ├── service.yaml
│   └── ingress.yaml
│
├── terraform/                    # AWS infrastructure (Phase 5)
│   ├── main.tf
│   ├── variables.tf
│   └── outputs.tf
│
└── monitoring/                   # Monitoring configs (Phase 4)
    ├── prometheus.yml
    └── grafana/
        ├── dashboards/
        └── datasources/
```

---

## 🔧 Jenkins Pipeline Stages

The Jenkinsfile defines the following stages:

### Phase 1 Stages (Active):
1. **Checkout** - Clone code from GitHub
2. **Build** - Compile with Maven (`mvn clean compile`)
3. **Unit Tests** - Run JUnit tests (`mvn test`)
4. **Package** - Create JAR file (`mvn package`)

### Phase 2 Stages (Conditional):
5. **Code Quality Analysis** - SonarQube scan
6. **Quality Gate** - Check quality thresholds

### Phase 3 Stages (Conditional):
7. **Upload to Nexus** - Store artifacts

### Phase 5 Stages (Conditional):
8. **Build Docker Image** - Create container image
9. **Push Docker Image** - Upload to registry
10. **Deploy to Kubernetes** - Deploy to AWS EKS

---

## 🎯 Current Pipeline Features

### ✅ Implemented (Phase 1):
- Automatic build on push to `main` branch
- Maven compilation and packaging
- JUnit test execution (109 tests across 4 modules)
- Test result reporting
- Artifact archiving (JAR files)
- Build status notifications
- Workspace cleanup

### 🔄 Coming Soon:
- Code quality analysis (Phase 2)
- Artifact versioning and storage (Phase 3)
- Performance monitoring (Phase 4)
- Automated deployment (Phase 5)

---

## 📊 Test Coverage

Current test suite includes:

| Module      | Service Tests | Repository Tests | Controller Tests | Total |
|-------------|---------------|------------------|------------------|-------|
| Facture     | 12            | 14               | 12               | 38    |
| Operateur   | 8             | 10               | 8                | 26    |
| Reglement   | 9             | -                | 9                | 18    |
| Stock       | 10            | 10               | 7                | 27    |
| **TOTAL**   | **39**        | **34**           | **36**           | **109** |

---

## 🐳 Docker Services

### Phase 1 (Active):
- **Jenkins**: Port 8080 (UI), 50000 (Agents)

### Phase 2:
- **SonarQube**: Port 9000
- **PostgreSQL**: Internal (for SonarQube)

### Phase 3:
- **Nexus**: Port 8081 (UI), 8082 (Docker Registry)

### Phase 4:
- **Prometheus**: Port 9090
- **Grafana**: Port 3000

---

## 🔐 Security & Credentials

### Required Credentials in Jenkins:

1. **GitHub Credentials** (`github-credentials`)
   - Type: Username with password
   - Username: GitHub username
   - Password: Personal Access Token

2. **Nexus Credentials** (`nexus-credentials`) - Phase 3
   - Type: Username with password
   - Username: admin
   - Password: (set during Nexus setup)

3. **Docker Registry** (`docker-registry-credentials`) - Phase 5
   - Type: Username with password
   - Username: Docker Hub username
   - Password: Docker Hub token

4. **Kubernetes Config** (`kubeconfig-credentials`) - Phase 5
   - Type: Secret file
   - File: kubeconfig file from AWS

---

## 📝 Useful Commands

### Docker Compose Commands:

```bash
# Start all Phase 1 services
docker-compose up -d jenkins

# Start Phase 2 services (SonarQube)
docker-compose --profile phase2 up -d

# Start Phase 3 services (Nexus)
docker-compose --profile phase3 up -d

# Start Phase 4 services (Monitoring)
docker-compose --profile phase4 up -d

# Start ALL services
docker-compose --profile phase2 --profile phase3 --profile phase4 up -d

# View logs
docker-compose logs -f jenkins
docker-compose logs -f sonarqube

# Stop all services
docker-compose down

# Stop and remove volumes (WARNING: deletes all data)
docker-compose down -v

# Restart a service
docker-compose restart jenkins
```

### Jenkins Commands:

```bash
# Get initial admin password
docker exec jenkins-cicd cat /var/jenkins_home/secrets/initialAdminPassword

# Access Jenkins container
docker exec -it jenkins-cicd bash

# View Jenkins logs
docker logs -f jenkins-cicd
```

### Maven Commands (Local Testing):

```bash
# Clean and compile
mvn clean compile

# Run tests
mvn test

# Package application
mvn package

# Run application locally
mvn spring-boot:run
```

---

## 🐛 Troubleshooting

### Jenkins won't start
```bash
# Check logs
docker-compose logs jenkins

# Verify Docker has enough resources (4GB+ RAM)
docker info

# Restart Docker Desktop
```

### Build fails in Jenkins
```bash
# Test locally first
mvn clean install

# Check Jenkins console output
# Verify Maven and JDK are configured correctly
```

### Cannot access Jenkins UI
```bash
# Verify container is running
docker ps | grep jenkins

# Check port is not in use
netstat -an | findstr "8080"  # Windows
netstat -an | grep "8080"     # Linux/Mac
```

---

## 📚 Documentation

- **[JENKINS_SETUP.md](JENKINS_SETUP.md)** - Detailed Phase 1 setup guide
- **[Jenkinsfile](Jenkinsfile)** - Pipeline definition
- **[docker-compose.yml](docker-compose.yml)** - Service configuration

---

## 🎓 Learning Resources

- [Jenkins Documentation](https://www.jenkins.io/doc/)
- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Maven Documentation](https://maven.apache.org/guides/)
- [Spring Boot Testing](https://spring.io/guides/gs/testing-web/)

---

## 👥 Contributors

- **Rayen Slouma** - DevOps Implementation

---

## 📅 Project Timeline

- ✅ **Phase 1**: Jenkins Setup - COMPLETED
- 🔄 **Phase 2**: SonarQube Integration - PENDING
- 🔄 **Phase 3**: Nexus Repository - PENDING
- 🔄 **Phase 4**: Monitoring Setup - PENDING
- 🔄 **Phase 5**: Kubernetes Deployment - PENDING

---

**Last Updated**: 2025-10-29  
**Project**: IGL5-G5-Achat DevOps CI/CD Pipeline

