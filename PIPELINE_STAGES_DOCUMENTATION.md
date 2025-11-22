# Jenkins Pipeline Stages Documentation

This document explains each stage in the CI/CD pipeline, their purpose, execution order, and how they interact with different deployment modes.

## 📋 Table of Contents

1. [Pipeline Overview](#pipeline-overview)
2. [Stage Execution Order](#stage-execution-order)
3. [Detailed Stage Descriptions](#detailed-stage-descriptions)
4. [Deployment Modes](#deployment-modes)
5. [Stage Dependencies](#stage-dependencies)

---

## Pipeline Overview

The pipeline is divided into **5 main phases**:

1. **Source Control & Build** (Stages 1-4)
2. **Quality Assurance** (Stages 5-6)
3. **Artifact Management** (Stages 7-8)
4. **Containerization** (Stages 9-10)
5. **Infrastructure & Deployment** (Stages 11-15)
6. **Frontend & EKS** (Stages 16-19)
7. **Summary** (Stage 20)

---

## Stage Execution Order

The stages execute in this order (as defined in `Jenkinsfile`):

```
1.  Checkout
2.  Build
3.  Unit Tests
4.  Package
5.  SonarQube Analysis
6.  Quality Gate
7.  Deploy to Nexus
8.  Build Docker Image
9.  Push Docker Image
10. Cleanup AWS (conditional)
11. Refresh EC2 (conditional)
12. Terraform Init (conditional)
13. Pre-Terraform Validation (conditional)
14. Terraform Plan (conditional)
15. Terraform Apply (conditional)
16. Get AWS Info
17. Debug EC2
18. Health Check
19. Build Frontend
20. Build Frontend Docker
21. Push Frontend Docker
22. Deploy to EKS
23. Final Summary
```

---

## Detailed Stage Descriptions

### Stage 1: Checkout

**File:** `jenkins/stages/checkout.groovy`

**Purpose:** Retrieves the source code from GitHub repository.

**What it does:**
- Clones the repository from GitHub
- Checks out the branch `MohamedHabibManai-GL5-G5-Produit`
- Configures Git to skip SSL verification (to avoid TLS issues)
- Implements retry logic (up to 5 attempts) with exponential backoff
- Uses shallow clone (depth=1) for faster checkout

**Key Features:**
- Retry mechanism for network failures
- SSL verification disabled for compatibility
- Large file support (postBuffer increased)

**When it runs:** Always (first stage)

---

### Stage 2: Build

**File:** `jenkins/stages/build.groovy`

**Purpose:** Compiles the Java application using Maven.

**What it does:**
- Runs `mvn clean compile` to compile the source code
- Uses system Maven (not wrapper)
- Cleans previous build artifacts before compiling

**Key Features:**
- Simple compilation step
- No tests executed (tests run in next stage)

**When it runs:** Always

**Dependencies:** Requires successful checkout

---

### Stage 3: Unit Tests

**File:** `jenkins/stages/unitTests.groovy`

**Purpose:** Executes unit tests and publishes test results.

**What it does:**
- Runs `mvn test` to execute all unit tests
- Publishes test results using JUnit plugin
- Archives test reports from `target/surefire-reports/*.xml`

**Key Features:**
- Test results are published to Jenkins UI
- Uses try-finally to ensure results are published even if tests fail
- Test failures will fail the build

**When it runs:** Always

**Dependencies:** Requires successful build

---

### Stage 4: Package

**File:** `jenkins/stages/package.groovy`

**Purpose:** Packages the application into a JAR file.

**What it does:**
- Runs `mvn package -DskipTests` (tests already ran in previous stage)
- Creates JAR artifact: `achat-1.0-SNAPSHOT-${BUILD_NUMBER}.jar`
- Archives the JAR file for later use
- Fingerprints artifacts for tracking

**Key Features:**
- Skips tests (already executed)
- Archives JAR for Docker build stage
- Artifact naming includes build number

**When it runs:** Always

**Dependencies:** Requires successful unit tests

---

### Stage 5: SonarQube Analysis

**File:** `jenkins/stages/sonar.groovy`

**Purpose:** Performs code quality analysis using SonarQube.

**What it does:**
- Runs SonarQube scanner to analyze code quality
- Checks code coverage, code smells, bugs, vulnerabilities
- Sends results to SonarQube server at `http://sonarqube-server:9000`
- Project key: `achat`

**Key Features:**
- Code quality metrics
- Security vulnerability scanning
- Code coverage analysis

**When it runs:** Always

**Dependencies:** Requires successful package

---

### Stage 6: Quality Gate

**File:** `jenkins/stages/qualityGate.groovy`

**Purpose:** Validates that code quality meets the defined standards.

**What it does:**
- Waits for SonarQube analysis to complete
- Checks if code quality gate passes
- Fails build if quality standards are not met
- Prevents deployment of low-quality code

**Key Features:**
- Quality gate enforcement
- Blocks deployment if quality is insufficient

**When it runs:** Always

**Dependencies:** Requires successful SonarQube analysis

---

### Stage 7: Deploy to Nexus

**File:** `jenkins/stages/deployToNexus.groovy`

**Purpose:** Uploads the JAR artifact to Nexus repository for version control.

**What it does:**
- Deploys JAR to Nexus repository (`nexus-repository:8081`)
- Uses repository: `maven-snapshots`
- Stores artifact for future reference and dependency management

**Key Features:**
- Artifact versioning
- Centralized artifact storage
- Enables dependency management

**When it runs:** Always

**Dependencies:** Requires successful package and quality gate

---

### Stage 8: Build Docker Image

**File:** `jenkins/stages/buildDockerImage.groovy`

**Purpose:** Creates a Docker image containing the application.

**What it does:**
- Pre-pulls base image `eclipse-temurin:8-jre-alpine` (with retry logic)
- Builds Docker image using the packaged JAR file
- Tags image as: `achat-app:${BUILD_NUMBER}` and `achat-app:latest`
- Uses Dockerfile in project root

**Key Features:**
- Retry logic for network issues (up to 3 attempts)
- Multiple tags (build number + latest)
- Base image pre-pull with retry

**When it runs:** Always

**Dependencies:** Requires successful package

---

### Stage 9: Push Docker Image

**File:** `jenkins/stages/pushDockerImage.groovy`

**Purpose:** Pushes the Docker image to Docker Hub registry.

**What it does:**
- Pushes image to `docker.io/habibmanai/achat-app:${BUILD_NUMBER}`
- Also pushes `latest` tag
- Uses Docker Hub credentials from Jenkins

**Key Features:**
- Image available for deployment
- Versioned by build number
- Latest tag for convenience

**When it runs:** Always

**Dependencies:** Requires successful Docker image build

---

### Stage 10: Cleanup AWS

**File:** `jenkins/stages/cleanupAWS.groovy`

**Purpose:** Destroys existing AWS infrastructure before fresh deployment.

**What it does:**
- Only runs in `CLEANUP_AND_DEPLOY` mode
- Destroys ALL AWS resources (VPC, EC2, RDS, EKS, etc.)
- Uses `terraform destroy` to remove infrastructure
- Waits for resources to be fully deleted

**Key Features:**
- Complete infrastructure cleanup
- Prevents resource conflicts
- Only runs in CLEANUP_AND_DEPLOY mode

**When it runs:** Only when `DEPLOYMENT_MODE == 'CLEANUP_AND_DEPLOY'`

**Dependencies:** None (first AWS stage)

---

### Stage 11: Refresh EC2

**File:** `jenkins/stages/refreshEC2.groovy`

**Purpose:** Recreates only the EC2 instance while keeping VPC and RDS.

**What it does:**
- Only runs in `REUSE_INFRASTRUCTURE` mode
- Imports existing VPC, subnets, security groups, RDS
- Destroys only EC2 instance and Elastic IP
- Recreates EC2 instance with new user-data
- Uses `terraform apply -target` to only modify EC2 resources

**Key Features:**
- Fast deployment (~5 minutes vs 20+ minutes)
- Preserves expensive resources (RDS, VPC)
- Imports existing infrastructure into Terraform state
- Verifies RDS endpoint is available before creating EC2

**When it runs:** Only when `DEPLOYMENT_MODE == 'REUSE_INFRASTRUCTURE'`

**Dependencies:** Requires existing infrastructure from previous deployment

---

### Stage 12: Terraform Init

**File:** `jenkins/stages/terraformInit.groovy`

**Purpose:** Initializes Terraform and downloads required providers.

**What it does:**
- Runs `terraform init` to initialize Terraform
- Downloads AWS provider and other required plugins
- Implements retry logic (up to 7 attempts) with exponential backoff
- Verifies AWS credentials
- Skips in REUSE_INFRASTRUCTURE mode

**Key Features:**
- Retry mechanism for network issues
- Provider caching for faster retries
- AWS credential verification

**When it runs:** Only in `NORMAL` or `CLEANUP_AND_DEPLOY` mode

**Dependencies:** Requires terraform configuration files

---

### Stage 13: Pre-Terraform Validation

**File:** `jenkins/stages/preTerraformValidation.groovy`

**Purpose:** Validates that AWS resources don't already exist before creating them.

**What it does:**
- Checks if EKS cluster already exists
- Checks if DB subnet group already exists
- Checks if RDS instances already exist
- Fails build if conflicts are found
- Skips in REUSE_INFRASTRUCTURE mode

**Key Features:**
- Prevents resource conflicts
- Early failure detection
- Clear error messages

**When it runs:** Only in `NORMAL` or `CLEANUP_AND_DEPLOY` mode

**Dependencies:** Requires Terraform init

---

### Stage 14: Terraform Plan

**File:** `jenkins/stages/terraformPlan.groovy`

**Purpose:** Creates an execution plan showing what Terraform will create/modify.

**What it does:**
- Runs `terraform plan` to generate execution plan
- Shows what resources will be created, modified, or destroyed
- Saves plan to `tfplan` file for apply stage
- Implements retry logic (up to 3 attempts)
- Skips in REUSE_INFRASTRUCTURE mode

**Key Features:**
- Preview of infrastructure changes
- Plan file for safe apply
- Retry mechanism

**When it runs:** Only in `NORMAL` or `CLEANUP_AND_DEPLOY` mode

**Dependencies:** Requires Terraform init

---

### Stage 15: Terraform Apply

**File:** `jenkins/stages/terraformApply.groovy`

**Purpose:** Creates or modifies AWS infrastructure according to the plan.

**What it does:**
- Applies the Terraform plan created in previous stage
- Creates VPC, subnets, EC2, RDS, security groups, etc.
- Waits for EKS cluster deletion if needed
- Implements retry logic (up to 3 attempts)
- Skips in REUSE_INFRASTRUCTURE mode

**Key Features:**
- Infrastructure provisioning
- Automatic retry on failures
- EKS deletion wait logic

**When it runs:** Only in `NORMAL` or `CLEANUP_AND_DEPLOY` mode

**Dependencies:** Requires Terraform plan

---

### Stage 16: Get AWS Info

**File:** `jenkins/stages/getAWSInfo.groovy`

**Purpose:** Retrieves and displays AWS infrastructure information.

**What it does:**
- Gets EC2 instance details (ID, IP, status)
- Gets RDS endpoint information
- Gets VPC and subnet information
- Displays all information in formatted output

**Key Features:**
- Infrastructure visibility
- Useful for debugging
- Always runs (not conditional)

**When it runs:** Always (if terraform config exists)

**Dependencies:** Requires successful infrastructure deployment

---

### Stage 17: Debug EC2

**File:** `jenkins/stages/debugEC2.groovy`

**Purpose:** Provides debugging information about the EC2 instance.

**What it does:**
- Shows EC2 instance status
- Displays user-data script logs
- Shows Docker container status
- Provides troubleshooting information

**Key Features:**
- Debugging assistance
- Container status visibility
- Log access

**When it runs:** Always (if terraform config exists)

**Dependencies:** Requires EC2 instance

---

### Stage 18: Health Check

**File:** `jenkins/stages/healthCheck.groovy`

**Purpose:** Verifies that the application is running and healthy.

**What it does:**
- Checks application health endpoint: `/SpringMVC/actuator/health`
- Implements retry logic (up to 15-20 attempts)
- Waits for application to start (4 minutes in REUSE mode, 1 minute in NORMAL mode)
- Checks port 8089 status
- Uses SSM to check container status if port is open but app not responding
- Provides diagnostic information

**Key Features:**
- Health verification
- Automatic retry
- SSM-based diagnostics
- Deployment mode aware (different wait times)

**When it runs:** Always (if terraform config exists)

**Dependencies:** Requires EC2 instance and application deployment

---

### Stage 19: Build Frontend

**File:** `jenkins/stages/buildFrontend.groovy`

**Purpose:** Builds the React frontend application.

**What it does:**
- Runs `npm install` to install dependencies
- Runs `npm run build` to build the frontend
- Creates production-ready frontend bundle

**Key Features:**
- Frontend build process
- Dependency management

**When it runs:** Always

**Dependencies:** Requires frontend source code

---

### Stage 20: Build Frontend Docker

**File:** `jenkins/stages/buildFrontendDocker.groovy`

**Purpose:** Creates Docker image for the frontend application.

**What it does:**
- Builds Docker image for React frontend
- Uses Nginx to serve static files
- Tags image appropriately

**Key Features:**
- Frontend containerization
- Nginx-based serving

**When it runs:** Always

**Dependencies:** Requires successful frontend build

---

### Stage 21: Push Frontend Docker

**File:** `jenkins/stages/pushFrontendDocker.groovy`

**Purpose:** Pushes frontend Docker image to registry.

**What it does:**
- Pushes frontend image to Docker Hub
- Makes image available for EKS deployment

**Key Features:**
- Image registry storage
- Version control

**When it runs:** Always

**Dependencies:** Requires successful frontend Docker build

---

### Stage 22: Deploy to EKS

**File:** `jenkins/stages/deployToEKS.groovy`

**Purpose:** Deploys backend and frontend to AWS EKS (Kubernetes).

**What it does:**
- Configures kubectl with AWS credentials
- Updates kubeconfig for EKS cluster
- Deploys backend application to EKS
- Deploys frontend application to EKS
- Creates Kubernetes services and load balancers
- Waits for deployments to be ready

**Key Features:**
- Kubernetes deployment
- Load balancer provisioning
- Service creation
- Only runs if EKS is enabled

**When it runs:** Always (if EKS is configured)

**Dependencies:** Requires EKS cluster and Docker images

---

### Stage 23: Final Summary

**File:** `jenkins/stages/finalSummary.groovy`

**Purpose:** Displays comprehensive deployment summary with all URLs.

**What it does:**
- Collects all deployment URLs (AWS EC2, EKS backend, EKS frontend)
- Displays Swagger UI URLs
- Shows health check URLs
- Provides quick test commands
- Formats output for easy reading

**Key Features:**
- Centralized information
- Easy access to all endpoints
- Test commands included

**When it runs:** Always (last stage)

**Dependencies:** None (information gathering only)

---

## Deployment Modes

The pipeline supports **4 deployment modes** that affect which stages run:

### 1. REUSE_INFRASTRUCTURE (Default for Webhooks)

**What it does:**
- Skips: Terraform Init, Pre-Terraform Validation, Terraform Plan, Terraform Apply
- Runs: Refresh EC2 (recreates only EC2 instance)
- Keeps: VPC, RDS, EKS (if exists)
- Speed: ~5 minutes

**When to use:** For code changes, testing, rapid iterations

### 2. NORMAL

**What it does:**
- Runs: All Terraform stages (Init, Validation, Plan, Apply)
- Creates: Fresh infrastructure (VPC, EC2, RDS, EKS if enabled)
- Speed: ~20 minutes

**When to use:** First deployment or when infrastructure needs to be recreated

### 3. CLEANUP_AND_DEPLOY

**What it does:**
- Runs: Cleanup AWS (destroys everything), then all Terraform stages
- Destroys: All existing resources first
- Creates: Fresh infrastructure
- Speed: ~25 minutes (includes cleanup time)

**When to use:** When you need a completely clean slate

### 4. EKS_ONLY

**What it does:**
- Skips: All stages except Deploy to EKS
- Only runs: EKS deployment stage
- Speed: ~5 minutes

**When to use:** For testing EKS deployment only

---

## Stage Dependencies

### Build Phase Dependencies
```
Checkout → Build → Unit Tests → Package
```

### Quality Phase Dependencies
```
Package → SonarQube → Quality Gate
```

### Artifact Phase Dependencies
```
Package → Deploy to Nexus
Package → Build Docker Image → Push Docker Image
```

### Infrastructure Phase Dependencies

**NORMAL/CLEANUP_AND_DEPLOY:**
```
Cleanup AWS (if CLEANUP) → Terraform Init → Pre-Terraform Validation → Terraform Plan → Terraform Apply
```

**REUSE_INFRASTRUCTURE:**
```
Refresh EC2 (imports existing, recreates EC2 only)
```

### Deployment Phase Dependencies
```
Terraform Apply / Refresh EC2 → Get AWS Info → Debug EC2 → Health Check
```

### Frontend Phase Dependencies
```
Build Frontend → Build Frontend Docker → Push Frontend Docker
```

### EKS Phase Dependencies
```
Push Docker Image + Push Frontend Docker → Deploy to EKS
```

### Summary Phase
```
All previous stages → Final Summary
```

---

## Post-Build Actions

After all stages complete (or fail), the pipeline:

1. **Sends Email Notification** - Sends build status email to configured recipients
2. **Cleans Workspace** - Removes all files from workspace to save disk space

---

## Notes

- **Retry Logic:** Many stages implement retry mechanisms to handle transient network issues
- **Conditional Execution:** Infrastructure stages are conditional based on deployment mode
- **Error Handling:** Stages use try-catch blocks to provide meaningful error messages
- **State Management:** REUSE mode imports existing resources into Terraform state
- **Health Checks:** Health check stage waits longer in REUSE mode (4 minutes) vs NORMAL mode (1 minute)

---

## Troubleshooting

### If a stage fails:

1. **Check the console output** - Detailed logs are available
2. **Review stage-specific documentation** - Each stage file has comments
3. **Check deployment mode** - Ensure correct mode is being used
4. **Verify prerequisites** - Ensure previous stages completed successfully
5. **Check AWS resources** - Use AWS Console to verify resource status

### Common Issues:

- **Terraform Init fails:** Network issue, retry will be attempted automatically
- **Health Check fails:** Application may still be starting, check logs
- **EKS deployment fails:** Check if EKS cluster exists and is accessible
- **Docker build fails:** Check Docker daemon and network connectivity

---

**Last Updated:** Based on current pipeline configuration
**Pipeline File:** `Jenkinsfile`
**Stage Files Location:** `jenkins/stages/`

