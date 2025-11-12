# Frontend Pipeline Integration Guide

## 🎉 What We've Added

Your Jenkins pipeline now **automatically builds and deploys the frontend** alongside your backend application! The frontend will always know the correct backend URL without manual configuration.

## 📋 New Pipeline Stages

### Stage 10: Build Frontend
- Automatically retrieves the backend URL from Terraform outputs
- Creates `.env.production` file with the correct `REACT_APP_API_URL`
- Installs npm dependencies
- Builds the React production bundle

### Stage 11: Build Frontend Docker Image
- Builds a Docker image with Nginx serving the React app
- Tags with build number (e.g., `achat-frontend:55`)
- Also creates `latest` tag

### Stage 12: Push Frontend Docker Image
- Pushes frontend image to Docker Hub
- Uses the same credentials as backend

### Stage 13: Deploy to Kubernetes (Enhanced)
- Deploys both backend and frontend to Kubernetes
- Updates both deployments with build-specific image tags
- Waits for rollouts to complete
- Frontend accessible at: **http://localhost:30080**
- Backend accessible at: **http://localhost/SpringMVC**

## 🚀 How to Run the Full Stack

### Option 1: Run a Jenkins Build
```bash
# Just trigger a build in Jenkins
# The pipeline will automatically:
# 1. Build the backend
# 2. Build the frontend (with correct backend URL)
# 3. Create Docker images for both
# 4. Deploy to AWS (backend)
# 5. Deploy to Kubernetes (backend + frontend)
```

**After the build completes:**
- **AWS Backend**: http://[DYNAMIC-IP]:8089/SpringMVC
- **Kubernetes Backend**: http://localhost/SpringMVC  
- **Kubernetes Frontend**: http://localhost:30080

### Option 2: Run Locally with Docker Compose
```bash
cd c:\Users\MSI\Documents\TESTING\IGL5-G5-achat
docker-compose -f docker-compose-fullstack.yml up --build
```

**Access points:**
- **Frontend**: http://localhost:3000
- **Backend**: http://localhost:8089/SpringMVC
- **MySQL**: localhost:3306

### Option 3: Development Mode
```bash
# Terminal 1: Run backend
cd c:\Users\MSI\Documents\TESTING\IGL5-G5-achat
mvn spring-boot:run

# Terminal 2: Run frontend
cd c:\Users\MSI\Documents\TESTING\IGL5-G5-achat\frontend

# First, update .env to point to localhost backend
echo "REACT_APP_API_URL=http://localhost:8089/SpringMVC" > .env

npm start
```

**Access points:**
- **Frontend**: http://localhost:3000 (or 3001 if 3000 is busy)
- **Backend**: http://localhost:8089/SpringMVC

## 📁 New Files Created

1. **`frontend/`** - Complete React application
   - `src/services/` - API client and service layers
   - `src/components/Produit/` - Product management UI
   - `Dockerfile` - Production build configuration
   - `nginx.conf` - Nginx configuration for serving React SPA

2. **`k8s/frontend-deployment.yaml`** - Kubernetes manifest for frontend
   - 2 replicas for high availability
   - NodePort service on port 30080
   - Health checks configured

3. **`docker-compose-fullstack.yml`** - Full stack orchestration

4. **`FULLSTACK_README.md`** - Comprehensive documentation

## 🔧 How the Dynamic Backend URL Works

### In Jenkins Pipeline:
```groovy
// Stage 10: Build Frontend
def backendUrl = ''
dir("${TERRAFORM_DIR}") {
    // Gets the URL from Terraform output
    backendUrl = sh(
        script: 'terraform output -raw application_url',
        returnStdout: true
    ).trim()
}

// Creates .env.production with dynamic URL
sh """
    echo "REACT_APP_API_URL=${backendUrl}" > .env.production
"""

// Build uses this file
sh 'npm run build'
```

### What This Means:
- ✅ No manual IP updates needed
- ✅ Frontend always knows where backend is
- ✅ Works with AWS Elastic IP (mostly stays the same)
- ✅ Works if backend IP changes

## 🎯 Testing the Integration

### 1. Check Pipeline Execution
```bash
# Watch Jenkins build console output
# Look for these stages:
# ✓ Stage 10: Building Frontend Application
# ✓ Stage 11: Building Frontend Docker Image  
# ✓ Stage 12: Pushing Frontend Image to Docker Hub
# ✓ Stage 13: Deploying to Kubernetes
```

### 2. Verify Kubernetes Deployment
```bash
# Check frontend pods are running
kubectl get pods -n achat-app | findstr frontend

# Should see:
# achat-frontend-xxxxx-yyy   1/1     Running   0          2m

# Check frontend service
kubectl get svc -n achat-app | findstr frontend

# Should see:
# achat-frontend   NodePort   10.x.x.x   <none>   80:30080/TCP   2m
```

### 3. Test Frontend Access
```bash
# Open in browser
start http://localhost:30080

# Should see:
# - Achat Application header
# - Product list (may be empty initially)
# - "Ajouter Produit" button
```

### 4. Test Full CRUD Operations
```
1. Click "+ Ajouter Produit"
2. Fill in product details:
   - Code: PROD001
   - Libelle: Test Product
   - Prix: 99.99
   - Date d'expiration: (select date)
   - Catégorie: (select category)
3. Click "Enregistrer"
4. Product should appear in the list
5. Click "Modifier" to edit
6. Click "Supprimer" to delete
```

## 🔄 Workflow After Changes

### Scenario 1: Backend Code Change
```bash
git add .
git commit -m "Updated backend logic"
git push

# Jenkins automatically:
# 1. Builds new backend
# 2. Rebuilds frontend (gets new backend URL if changed)
# 3. Deploys both
```

### Scenario 2: Frontend Code Change
```bash
# Edit frontend/src/components/Produit/ProduitList.jsx
git add .
git commit -m "Updated product UI"
git push

# Jenkins automatically:
# 1. Builds backend (quick if no changes)
# 2. Builds new frontend
# 3. Deploys both
```

### Scenario 3: AWS Backend IP Changed
```bash
# Jenkins build automatically:
# 1. Gets new IP from Terraform
# 2. Injects into frontend build
# 3. Frontend knows new backend location
# 
# YOU DON'T NEED TO DO ANYTHING! 🎉
```

## 📊 Architecture After Integration

```
┌─────────────────────────────────────────────────────┐
│                    JENKINS CI/CD                     │
│  ┌────────────┐  ┌──────────────┐  ┌──────────────┐│
│  │   Build    │→ │    Build     │→ │    Deploy    ││
│  │  Backend   │  │   Frontend   │  │   Both to    ││
│  │            │  │ (dynamic URL)│  │ AWS + K8s    ││
│  └────────────┘  └──────────────┘  └──────────────┘│
└─────────────────────────────────────────────────────┘
                          │
            ┌─────────────┴─────────────┐
            │                           │
            ▼                           ▼
   ┌────────────────┐         ┌────────────────┐
   │  AWS DEPLOYMENT│         │  KUBERNETES    │
   │                │         │                │
   │  EC2 Backend   │         │  Backend Pods  │
   │  (Dynamic IP)  │         │  Frontend Pods │
   │  RDS MySQL     │         │  MySQL Pod     │
   └────────────────┘         └────────────────┘
            │                           │
            │                           │
   http://[IP]:8089        http://localhost:30080
```

## 🎓 Key Concepts

### 1. Why Terraform Output?
```groovy
terraform output -raw application_url
```
- Terraform knows the current AWS infrastructure state
- Outputs include the current public IP
- This is the single source of truth for backend location

### 2. Why .env.production?
```bash
echo "REACT_APP_API_URL=http://98.94.165.36:8089/SpringMVC" > .env.production
```
- React reads environment variables at **build time** (not runtime)
- `.env.production` is used during `npm run build`
- The URL gets **baked into** the JavaScript bundle

### 3. Why Both AWS and Kubernetes?
- **AWS**: Production deployment, accessible from anywhere
- **Kubernetes**: Local testing, fast deployment, learning K8s
- Jenkins deploys to both automatically

## 🐛 Troubleshooting

### Frontend Shows Empty Product List
```bash
# 1. Check if backend is running
curl http://localhost/SpringMVC/actuator/health

# 2. Check frontend is calling correct backend
# Open browser DevTools (F12) → Network tab
# Look for API calls to /produit/retrieve-all-produits
# Check the URL being called

# 3. Check backend has data
curl http://localhost/SpringMVC/produit/retrieve-all-produits
```

### Frontend Not Accessible at localhost:30080
```bash
# Check frontend pods
kubectl get pods -n achat-app

# Check frontend service
kubectl get svc -n achat-app

# Check pod logs
kubectl logs -n achat-app deployment/achat-frontend
```

### Build Fails at "Build Frontend" Stage
```bash
# Check Node.js is available in Jenkins
docker exec jenkins-cicd node --version

# If not installed:
docker exec -u root jenkins-cicd bash -c "curl -fsSL https://deb.nodesource.com/setup_18.x | bash - && apt-get install -y nodejs"
```

### AWS Backend IP Changed, Frontend Still Uses Old IP
```bash
# This shouldn't happen as pipeline rebuilds frontend
# But if it does, manually trigger Jenkins build
# Pipeline will get new IP from Terraform and rebuild frontend
```

## 📚 Next Steps

1. **Add More Components**
   ```bash
   # Create components for other entities
   frontend/src/components/Stock/StockList.jsx
   frontend/src/components/Fournisseur/FournisseurList.jsx
   frontend/src/components/Facture/FactureList.jsx
   ```

2. **Add Authentication**
   - Implement JWT in backend
   - Add login/register components
   - Store token in localStorage
   - Add to apiClient headers

3. **Deploy Frontend to AWS**
   - Option 1: S3 + CloudFront (static hosting)
   - Option 2: EC2 with Nginx (alongside backend)
   - Option 3: ECS with Docker container

4. **Add End-to-End Tests**
   ```bash
   # Install Cypress or Playwright
   cd frontend
   npm install --save-dev cypress
   
   # Write E2E tests
   cypress/integration/produit.spec.js
   ```

## ✨ Summary

**What Changed:**
- ✅ Frontend automatically gets correct backend URL from Terraform
- ✅ Jenkins pipeline builds and deploys frontend alongside backend
- ✅ No manual IP configuration needed
- ✅ Frontend deployed to Kubernetes with backend
- ✅ Full CRUD operations working end-to-end

**What You Gain:**
- 🚀 Faster development (no manual URL updates)
- 🔄 True CI/CD for full stack
- 📦 Containerized frontend
- ☸️  Kubernetes-native deployment
- 🎯 Production-ready architecture

**Next Build:**
Just push any changes and Jenkins will handle everything! 🎉
