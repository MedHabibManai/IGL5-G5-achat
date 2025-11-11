# 🚀 Quick Reference Card

## 📌 Service URLs

| Service | URL | Credentials |
|---------|-----|-------------|
| Jenkins | http://localhost:8080 | admin / (see setup) |
| SonarQube | http://localhost:9000 | admin / ##Azeraoi123 |
| Nexus | http://localhost:8081 | admin / ##Azeraoi123 |
| Grafana | http://localhost:3000 | admin / admin |
| Prometheus | http://localhost:9090 | - |

## ⚡ Common Commands

### Docker Compose

```bash
# Start all services
docker-compose up -d

# Stop all services
docker-compose stop

# Stop and remove containers
docker-compose down

# View running containers
docker-compose ps

# View logs
docker-compose logs -f [service-name]

# Restart a service
docker-compose restart [service-name]

# Rebuild and restart
docker-compose up -d --build
```

### Docker

```bash
# List all containers
docker ps -a

# View logs
docker logs jenkins-cicd
docker logs sonarqube
docker logs nexus

# Execute command in container
docker exec -it jenkins-cicd bash

# Check disk usage
docker system df

# Clean up
docker system prune -a
```

### Jenkins Container

```bash
# Get initial password
docker exec jenkins-cicd cat /var/jenkins_home/secrets/initialAdminPassword

# Check AWS CLI
docker exec jenkins-cicd aws --version

# Check Terraform
docker exec jenkins-cicd terraform version

# Test AWS credentials
docker exec jenkins-cicd aws sts get-caller-identity

# Access Jenkins shell
docker exec -it jenkins-cicd bash
```

### Git Workflow

```bash
# Create feature branch
git checkout -b feature/my-feature

# Check status
git status

# Add changes
git add .

# Commit
git commit -m "Description"

# Push
git push origin feature/my-feature

# Pull latest
git pull origin main

# Merge main into your branch
git merge main
```

## 🔧 Troubleshooting Commands

### Check Service Health

```bash
# All services status
docker-compose ps

# Jenkins health
curl http://localhost:8080

# SonarQube health
curl http://localhost:9000

# Nexus health
curl http://localhost:8081
```

### View Logs

```bash
# All services
docker-compose logs

# Specific service (follow mode)
docker-compose logs -f jenkins-cicd
docker-compose logs -f sonarqube
docker-compose logs -f nexus

# Last 100 lines
docker-compose logs --tail=100 jenkins-cicd
```

### Restart Services

```bash
# Restart all
docker-compose restart

# Restart specific service
docker-compose restart jenkins-cicd
docker-compose restart sonarqube
docker-compose restart nexus
```

### Clean Up

```bash
# Remove stopped containers
docker container prune

# Remove unused images
docker image prune -a

# Remove unused volumes
docker volume prune

# Remove everything unused
docker system prune -a --volumes
```

## 🔐 AWS Commands

### Update Credentials

```bash
# Windows
.\scripts\update-jenkins-aws-credentials.ps1

# Linux/Mac
./scripts/update-jenkins-aws-credentials.sh
```

### Verify AWS Setup

```bash
# Check AWS CLI version
docker exec jenkins-cicd aws --version

# Test credentials
docker exec jenkins-cicd aws sts get-caller-identity

# List S3 buckets (test)
docker exec jenkins-cicd aws s3 ls

# Check Terraform
docker exec jenkins-cicd terraform version
```

### Terraform Commands

```bash
# Initialize
docker exec jenkins-cicd terraform -chdir=terraform init

# Plan
docker exec jenkins-cicd terraform -chdir=terraform plan

# Apply
docker exec jenkins-cicd terraform -chdir=terraform apply

# Destroy
docker exec jenkins-cicd terraform -chdir=terraform destroy

# Show state
docker exec jenkins-cicd terraform -chdir=terraform state list
```

## 📊 Pipeline Commands

### Trigger Build

```bash
# Via Jenkins UI
# http://localhost:8080 → Click job → Build Now

# Via Jenkins CLI (if configured)
java -jar jenkins-cli.jar -s http://localhost:8080/ build IGL5-G5-Achat-Pipeline
```

### Check Build Status

```bash
# View in Jenkins UI
# http://localhost:8080 → Click job → Click build number

# View console output
# http://localhost:8080 → Click job → Click build → Console Output
```

## 🔍 Monitoring Commands

### Prometheus

```bash
# Check targets
curl http://localhost:9090/api/v1/targets

# Query metrics
curl 'http://localhost:9090/api/v1/query?query=up'
```

### Grafana

```bash
# Access dashboards
# http://localhost:3000/dashboards
```

## 📁 File Locations

### In Jenkins Container

```bash
# Jenkins home
/var/jenkins_home/

# Workspace
/var/jenkins_home/workspace/IGL5-G5-Achat-Pipeline/

# AWS credentials
/var/jenkins_home/.aws/credentials

# Terraform state
/var/jenkins_home/workspace/IGL5-G5-Achat-Pipeline/terraform/terraform.tfstate
```

### On Host Machine

```bash
# Project root
./

# Docker compose
./docker-compose.yml

# Jenkinsfile
./Jenkinsfile

# AWS credentials template
./.env.aws.example

# AWS credentials (your copy)
./.env.aws

# Terraform files
./terraform/
```

## 🎯 Daily Workflow

### Morning

```bash
# 1. Start Docker Desktop
# 2. Verify services
docker-compose ps

# 3. Pull latest code
git pull origin main
```

### During Development

```bash
# 1. Create branch
git checkout -b feature/my-feature

# 2. Make changes
# ... edit files ...

# 3. Test locally (optional)
mvn clean test

# 4. Commit
git add .
git commit -m "Add feature"

# 5. Push
git push origin feature/my-feature

# 6. Trigger pipeline in Jenkins
```

### Evening

```bash
# 1. Commit all changes
git add .
git commit -m "End of day commit"
git push

# 2. (Optional) Stop services
docker-compose stop
```

## 🆘 Emergency Commands

### Everything is Broken

```bash
# Nuclear option - restart everything
docker-compose down
docker-compose up -d

# Wait 5 minutes, then check
docker-compose ps
```

### Out of Disk Space

```bash
# Check usage
docker system df

# Clean everything
docker system prune -a --volumes

# Remove old images
docker image prune -a
```

### Jenkins Won't Start

```bash
# Check logs
docker logs jenkins-cicd

# Restart Jenkins
docker-compose restart jenkins-cicd

# If still broken, recreate
docker-compose up -d --force-recreate jenkins-cicd
```

### Pipeline Keeps Failing

```bash
# 1. Check Jenkins console output
# 2. Verify all services are up
docker-compose ps

# 3. Check SonarQube
curl http://localhost:9000

# 4. Check Nexus
curl http://localhost:8081

# 5. Restart all services
docker-compose restart
```

## 📞 Quick Help

| Issue | Solution |
|-------|----------|
| Can't access Jenkins | Wait 2-3 min, check `docker logs jenkins-cicd` |
| Pipeline fails at SonarQube | Check http://localhost:9000 is accessible |
| Pipeline fails at Nexus | Check http://localhost:8081 is accessible |
| Docker build fails | Check Dockerfile, verify base image |
| AWS deployment fails | Update credentials, check `.env.aws` |
| Out of disk space | Run `docker system prune -a` |
| Port already in use | Stop conflicting service or change port |

## 🔗 Useful Links

- **Jenkins Docs**: https://www.jenkins.io/doc/
- **Docker Docs**: https://docs.docker.com/
- **Terraform Docs**: https://www.terraform.io/docs/
- **SonarQube Docs**: https://docs.sonarqube.org/
- **Spring Boot Docs**: https://spring.io/projects/spring-boot

---

**Print this and keep it handy! 📋**

