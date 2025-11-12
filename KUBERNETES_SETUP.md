# Quick Kubernetes Setup Guide

## ✅ What's Been Created

All Kubernetes manifests are now in the `k8s/` directory:
- ✅ Namespace, ConfigMap, Secret
- ✅ MySQL deployment with persistent storage
- ✅ Application deployment (2 replicas)
- ✅ LoadBalancer service
- ✅ Ingress and HPA

## 🚀 Quick Deploy (Choose One Option)

### Option 1: Local Testing with Minikube

```powershell
# Install Minikube (if not installed)
choco install minikube

# Start Minikube
minikube start --cpus=4 --memory=8192

# Enable addons
minikube addons enable ingress
minikube addons enable metrics-server

# Deploy application
kubectl apply -f k8s/

# Wait for pods to be ready
kubectl wait --for=condition=ready pod -l app=achat-app -n achat-app --timeout=300s

# Access application
minikube service achat-app -n achat-app --url
# Or use port-forward:
kubectl port-forward svc/achat-app 8089:80 -n achat-app
# Then open: http://localhost:8089/SpringMVC/swagger-ui/index.html
```

### Option 2: Docker Desktop Kubernetes

```powershell
# 1. Enable Kubernetes in Docker Desktop
#    Settings → Kubernetes → Enable Kubernetes

# 2. Deploy application
kubectl apply -f k8s/

# 3. Access via port-forward
kubectl port-forward svc/achat-app 8089:80 -n achat-app

# 4. Open browser:
# http://localhost:8089/SpringMVC
# http://localhost:8089/SpringMVC/actuator/health
# http://localhost:8089/SpringMVC/swagger-ui/index.html
```

### Option 3: Azure AKS (Cloud)

```powershell
# 1. Create AKS cluster
az aks create `
  --resource-group achat-rg `
  --name achat-aks `
  --node-count 2 `
  --node-vm-size Standard_B2s `
  --enable-managed-identity `
  --generate-ssh-keys

# 2. Get credentials
az aks get-credentials --resource-group achat-rg --name achat-aks

# 3. Deploy application
kubectl apply -f k8s/

# 4. Get LoadBalancer IP
kubectl get svc achat-app -n achat-app -w
# Wait for EXTERNAL-IP to be assigned

# 5. Access application at:
# http://<EXTERNAL-IP>/SpringMVC
```

## 📊 Verify Deployment

```powershell
# Check all resources
kubectl get all -n achat-app

# Check pods status
kubectl get pods -n achat-app -w

# Check logs
kubectl logs -f deployment/achat-app -n achat-app

# Check MySQL
kubectl logs -f deployment/mysql -n achat-app
```

## 🎯 Access URLs

Once deployed, access your application:

**With Port-Forward:**
- Application: http://localhost:8089/SpringMVC
- Swagger UI: http://localhost:8089/SpringMVC/swagger-ui/index.html
- Health Check: http://localhost:8089/SpringMVC/actuator/health
- API: http://localhost:8089/SpringMVC/produit/retrieve-all-produits

**With LoadBalancer (Cloud):**
- Application: http://<EXTERNAL-IP>/SpringMVC
- Swagger UI: http://<EXTERNAL-IP>/SpringMVC/swagger-ui/index.html

## 🔄 Jenkins Pipeline

Your Jenkins pipeline will now:
1. ✅ Build & Test
2. ✅ Build Docker Image
3. ✅ Push to Docker Hub
4. ✅ Deploy to AWS EC2
5. ✅ **Deploy to Kubernetes** (NEW!)

The Kubernetes stage will automatically run when `k8s/deployment.yaml` exists.

## 🧹 Cleanup

```powershell
# Delete all resources
kubectl delete namespace achat-app

# Stop Minikube
minikube stop

# Delete AKS cluster (if using Azure)
az aks delete --resource-group achat-rg --name achat-aks --yes
```

## 📚 Need Help?

Check the detailed README: `k8s/README.md`
