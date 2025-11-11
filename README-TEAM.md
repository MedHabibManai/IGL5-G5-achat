# 🚀 Complete DevOps CI/CD Pipeline

A production-ready, fully automated CI/CD pipeline for Java Spring Boot applications using Docker, Jenkins, SonarQube, Nexus, and AWS deployment.

## 📋 What's Included

This pipeline provides:

- ✅ **Continuous Integration** with Jenkins
- ✅ **Automated Testing** (107 unit tests)
- ✅ **Code Quality Analysis** with SonarQube
- ✅ **Artifact Management** with Nexus Repository
- ✅ **Docker Containerization** and Docker Hub integration
- ✅ **Infrastructure as Code** with Terraform
- ✅ **AWS Deployment** (VPC, EC2, Security Groups)
- ✅ **Monitoring** with Prometheus and Grafana
- ✅ **Health Checks** and automated verification

## 🎯 Quick Start for Team Members

### Prerequisites

- Docker Desktop installed and running
- Git installed
- 8GB RAM minimum (16GB recommended)
- 20GB free disk space

### Setup (3 Commands)

```bash
# 1. Clone the repository
git clone https://github.com/MedHabibManai/IGL5-G5-achat.git
cd IGL5-G5-achat

# 2. Run setup script
# Windows:
.\scripts\setup-pipeline.ps1

# Linux/Mac:
chmod +x scripts/setup-pipeline.sh
./scripts/setup-pipeline.sh

# 3. Open Jenkins
# http://localhost:8080
```

**That's it!** The setup script handles everything automatically.

## 🌐 Service Access

After setup, access these services:

| Service | URL | Default Credentials |
|---------|-----|---------------------|
| Jenkins | http://localhost:8080 | admin / (see console) |
| SonarQube | http://localhost:9000 | admin / ##Azeraoi123 |
| Nexus | http://localhost:8081 | admin / ##Azeraoi123 |
| Grafana | http://localhost:3000 | admin / admin |
| Prometheus | http://localhost:9090 | No auth required |

## 📊 Pipeline Stages

The pipeline automatically runs 14 stages:

### Build & Test (Stages 1-4)
1. **Checkout** - Clone code from GitHub
2. **Build** - Compile with Maven
3. **Unit Tests** - Run 107 JUnit tests
4. **Package** - Create JAR artifact

### Quality & Artifacts (Stages 5-7)
5. **SonarQube Analysis** - Code quality scan
6. **Quality Gate** - Enforce quality standards
7. **Deploy to Nexus** - Store artifacts

### Containerization (Stages 8-9)
8. **Build Docker Image** - Create container image
9. **Push to Docker Hub** - Publish image

### AWS Deployment (Stages 10-14)
10. **Terraform Init** - Initialize infrastructure
11. **Terraform Plan** - Preview changes
12. **Terraform Apply** - Deploy to AWS
13. **Get Deployment Info** - Display URLs
14. **Health Check** - Verify deployment

## 🔧 Configuration for Your Team

### 1. Docker Hub Credentials

Each team member should configure their Docker Hub credentials:

1. Open Jenkins → Manage Jenkins → Credentials
2. Add new credentials:
   - ID: `dockerhub-credentials`
   - Username: Your Docker Hub username
   - Password: Your Docker Hub password

### 2. AWS Credentials (Optional)

To deploy to AWS:

```bash
# Copy template
cp .env.aws.example .env.aws

# Edit with your AWS Academy credentials
notepad .env.aws  # Windows
nano .env.aws     # Linux/Mac

# Update Jenkins container
.\scripts\update-jenkins-aws-credentials.ps1  # Windows
./scripts/update-jenkins-aws-credentials.sh   # Linux/Mac
```

### 3. GitHub Repository (Optional)

To use your own fork:

1. Fork the repository
2. Update the GitHub URL in Jenkins pipeline configuration
3. Update `docker-compose.yml` if needed

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Developer Workstation                    │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │ Jenkins  │  │SonarQube │  │  Nexus   │  │ Grafana  │   │
│  │  :8080   │  │  :9000   │  │  :8081   │  │  :3000   │   │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘   │
│                                                              │
│  All running in Docker containers                           │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
                    ┌───────────────┐
                    │   Docker Hub  │
                    │  (Public)     │
                    └───────────────┘
                            │
                            ▼
                    ┌───────────────┐
                    │   AWS Cloud   │
                    │  - VPC        │
                    │  - EC2        │
                    │  - Security   │
                    └───────────────┘
```

## 📁 Project Structure

```
IGL5-G5-achat/
├── src/                        # Java Spring Boot source code
├── docker-compose.yml          # All services configuration
├── Dockerfile                  # Application container
├── Dockerfile.jenkins          # Custom Jenkins image
├── Jenkinsfile                 # Pipeline definition
├── pom.xml                     # Maven configuration
├── .env.aws.example           # AWS credentials template
├── .gitignore                 # Git ignore rules
├── SETUP.md                   # Detailed setup guide
├── README-TEAM.md             # This file
├── scripts/
│   ├── setup-pipeline.ps1     # Windows setup
│   ├── setup-pipeline.sh      # Linux/Mac setup
│   └── update-jenkins-aws-credentials.ps1
├── terraform/                  # AWS infrastructure
│   ├── main.tf                # Main infrastructure
│   ├── variables.tf           # Input variables
│   ├── outputs.tf             # Output values
│   └── provider.tf            # AWS provider config
└── monitoring/                 # Monitoring configs
    ├── prometheus/
    │   └── prometheus.yml
    └── grafana/
        ├── datasources/
        └── dashboards/
```

## 🔄 Daily Workflow

### Starting Your Day

```bash
# Make sure Docker Desktop is running
# All services start automatically with Docker Desktop

# Verify services are running
docker-compose ps
```

### Running the Pipeline

1. Open Jenkins: http://localhost:8080
2. Click on "IGL5-G5-Achat-Pipeline"
3. Click "Build Now"
4. Watch the pipeline execute all 14 stages
5. Check the console output for results

### Making Changes

```bash
# 1. Create a feature branch
git checkout -b feature/my-feature

# 2. Make your changes
# Edit code, tests, etc.

# 3. Commit and push
git add .
git commit -m "Add my feature"
git push origin feature/my-feature

# 4. Trigger pipeline in Jenkins
# Or configure webhook for automatic builds
```

### Stopping Services

```bash
# Stop all services (keeps data)
docker-compose stop

# Stop and remove containers (keeps data)
docker-compose down

# Stop and remove everything including data
docker-compose down -v
```

## 🛠️ Troubleshooting

### Services Not Starting

```bash
# Check Docker is running
docker ps

# View logs for specific service
docker-compose logs jenkins-cicd
docker-compose logs sonarqube
docker-compose logs nexus

# Restart all services
docker-compose restart
```

### Jenkins Not Accessible

```bash
# Wait 2-3 minutes for Jenkins to fully start
# Check logs
docker logs jenkins-cicd

# Get initial password
docker exec jenkins-cicd cat /var/jenkins_home/secrets/initialAdminPassword
```

### Pipeline Failing

1. Check Jenkins console output for errors
2. Verify all services are running: `docker-compose ps`
3. Check SonarQube is accessible: http://localhost:9000
4. Check Nexus is accessible: http://localhost:8081
5. Verify Docker Hub credentials in Jenkins

### AWS Deployment Issues

```bash
# Verify AWS credentials
docker exec jenkins-cicd aws sts get-caller-identity

# Update credentials
.\scripts\update-jenkins-aws-credentials.ps1

# Check Terraform state
docker exec jenkins-cicd terraform -chdir=terraform state list
```

### Disk Space Issues

```bash
# Check Docker disk usage
docker system df

# Clean up unused resources
docker system prune -a

# Remove old images
docker image prune -a
```

## 🔐 Security Best Practices

### For Development

- ✅ Use `.env.aws` for AWS credentials (never commit!)
- ✅ Change default passwords in production
- ✅ Use separate Docker Hub accounts for team members
- ✅ Rotate AWS credentials regularly (every 3-4 hours for AWS Academy)

### For Production

- 🔒 Use Jenkins credentials manager for all secrets
- 🔒 Enable HTTPS for all services
- 🔒 Use private Docker registry
- 🔒 Implement proper IAM roles for AWS
- 🔒 Enable authentication on Prometheus/Grafana
- 🔒 Use secrets management (AWS Secrets Manager, HashiCorp Vault)

## 📚 Additional Resources

### Documentation

- [Jenkins Documentation](https://www.jenkins.io/doc/)
- [SonarQube Documentation](https://docs.sonarqube.org/)
- [Terraform AWS Provider](https://registry.terraform.io/providers/hashicorp/aws/latest/docs)
- [Docker Documentation](https://docs.docker.com/)

### Learning Resources

- [Jenkins Pipeline Tutorial](https://www.jenkins.io/doc/book/pipeline/)
- [Terraform Getting Started](https://learn.hashicorp.com/terraform)
- [Docker Compose Guide](https://docs.docker.com/compose/)

## 🤝 Contributing

### For Team Members

1. Create a feature branch
2. Make your changes
3. Test locally with the pipeline
4. Create a pull request
5. Wait for code review and pipeline success

### Code Standards

- Follow Java coding conventions
- Write unit tests for new features
- Maintain >80% code coverage
- Pass SonarQube quality gate
- Update documentation

## 📞 Support

### Getting Help

1. Check this README and SETUP.md
2. Check the troubleshooting section
3. View service logs: `docker-compose logs [service]`
4. Ask team members
5. Check Jenkins console output

### Common Commands

```bash
# View all containers
docker-compose ps

# View logs
docker-compose logs -f [service-name]

# Restart a service
docker-compose restart [service-name]

# Execute command in container
docker exec -it jenkins-cicd bash

# Check Jenkins version
docker exec jenkins-cicd java -jar /usr/share/jenkins/jenkins.war --version
```

## ✨ Why This Pipeline is Easy to Share

✅ **One-Command Setup** - Automated script handles everything  
✅ **Docker-Based** - Consistent across all machines  
✅ **No Manual Configuration** - Scripts configure services  
✅ **Template Files** - Easy to customize for your needs  
✅ **Clear Documentation** - Step-by-step instructions  
✅ **Version Controlled** - Everything tracked in Git  
✅ **Portable** - Works on Windows, Mac, and Linux  
✅ **Reproducible** - Same setup for everyone  

## 🎓 For New Team Members

Welcome! Here's your onboarding checklist:

- [ ] Install Docker Desktop
- [ ] Clone the repository
- [ ] Run setup script
- [ ] Access Jenkins and explore
- [ ] Run your first pipeline build
- [ ] Configure Docker Hub credentials
- [ ] (Optional) Configure AWS credentials
- [ ] Read the documentation
- [ ] Ask questions!

---

**Happy DevOps-ing! 🚀**

