# Kubernetes Deployment for Achat Application

This directory contains Kubernetes manifests for deploying the Achat Spring Boot application to a Kubernetes cluster.

## 📁 Files Overview

- **namespace.yaml** - Creates the `achat-app` namespace
- **configmap.yaml** - Application configuration (non-sensitive)
- **secret.yaml** - Sensitive data (database credentials)
- **mysql-deployment.yaml** - MySQL database deployment with persistent storage
- **deployment.yaml** - Achat application deployment
- **service.yaml** - LoadBalancer service for external access
- **ingress.yaml** - Ingress rules for HTTP routing (optional)
- **hpa.yaml** - Horizontal Pod Autoscaler for automatic scaling

## 🚀 Quick Start

### Prerequisites

1. **Kubernetes cluster** (one of the following):
   - Minikube (local development)
   - Docker Desktop with Kubernetes enabled
   - AKS (Azure Kubernetes Service)
   - EKS (Amazon Elastic Kubernetes Service)
   - GKE (Google Kubernetes Engine)

2. **kubectl** installed and configured

3. **Docker image** available on Docker Hub:
   - `habibmanai/achat-app:latest`

### Deploy to Kubernetes

```bash
# 1. Create namespace and resources
kubectl apply -f k8s/namespace.yaml

# 2. Create ConfigMap and Secret
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secret.yaml

# 3. Deploy MySQL database
kubectl apply -f k8s/mysql-deployment.yaml

# Wait for MySQL to be ready
kubectl wait --for=condition=ready pod -l app=mysql -n achat-app --timeout=300s

# 4. Deploy the application
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml

# 5. (Optional) Deploy Ingress and HPA
kubectl apply -f k8s/ingress.yaml
kubectl apply -f k8s/hpa.yaml
```

### Or deploy everything at once:

```bash
kubectl apply -f k8s/
```

## 🔍 Verify Deployment

### Check all resources:
```bash
kubectl get all -n achat-app
```

### Check pods:
```bash
kubectl get pods -n achat-app
```

### Check logs:
```bash
# Application logs
kubectl logs -f deployment/achat-app -n achat-app

# MySQL logs
kubectl logs -f deployment/mysql -n achat-app
```

### Check services:
```bash
kubectl get svc -n achat-app
```

## 🌐 Access the Application

### Using LoadBalancer (Cloud providers):
```bash
# Get external IP
kubectl get svc achat-app -n achat-app

# Access application at:
http://<EXTERNAL-IP>/SpringMVC
http://<EXTERNAL-IP>/SpringMVC/actuator/health
http://<EXTERNAL-IP>/SpringMVC/swagger-ui/index.html
```

### Using Minikube:
```bash
# Get service URL
minikube service achat-app -n achat-app --url

# Or use port forwarding
kubectl port-forward svc/achat-app 8089:80 -n achat-app
# Then access: http://localhost:8089/SpringMVC
```

### Using Port Forward (any cluster):
```bash
kubectl port-forward svc/achat-app 8089:80 -n achat-app

# Access application at:
http://localhost:8089/SpringMVC
http://localhost:8089/SpringMVC/actuator/health
http://localhost:8089/SpringMVC/swagger-ui/index.html
```

## 🔒 Security Notes

**⚠️ IMPORTANT:** Before deploying to production:

1. **Change database passwords** in `k8s/secret.yaml`
2. **Use proper secrets management** (e.g., Azure Key Vault, AWS Secrets Manager)
3. **Enable TLS** in the Ingress
4. **Set resource limits** appropriately for your workload
5. **Configure network policies** for pod-to-pod communication

### Update secrets:
```bash
# Base64 encode your password
echo -n 'YourNewPassword' | base64

# Update the secret
kubectl edit secret achat-app-secret -n achat-app
```

## 📊 Monitoring & Scaling

### View HPA status:
```bash
kubectl get hpa -n achat-app
```

### Scale manually:
```bash
kubectl scale deployment achat-app --replicas=5 -n achat-app
```

### View resource usage:
```bash
kubectl top pods -n achat-app
kubectl top nodes
```

## 🧹 Cleanup

### Delete all resources:
```bash
kubectl delete namespace achat-app
```

### Or delete specific resources:
```bash
kubectl delete -f k8s/
```

## 🔧 Troubleshooting

### Pod not starting:
```bash
kubectl describe pod <pod-name> -n achat-app
kubectl logs <pod-name> -n achat-app
```

### Database connection issues:
```bash
# Check if MySQL is ready
kubectl get pods -l app=mysql -n achat-app

# Test connection from app pod
kubectl exec -it deployment/achat-app -n achat-app -- sh
mysql -h mysql -u admin -p achatdb
```

### View events:
```bash
kubectl get events -n achat-app --sort-by='.lastTimestamp'
```

## 📝 Configuration

### Update application configuration:
```bash
# Edit ConfigMap
kubectl edit configmap achat-app-config -n achat-app

# Restart deployment to apply changes
kubectl rollout restart deployment/achat-app -n achat-app
```

### Update Docker image:
```bash
# Update to new version
kubectl set image deployment/achat-app achat-app=habibmanai/achat-app:49 -n achat-app

# Check rollout status
kubectl rollout status deployment/achat-app -n achat-app
```

## 🌟 Advanced Features

### Rolling Updates
Kubernetes automatically performs rolling updates with zero downtime.

### Auto-Scaling
HPA automatically scales pods based on CPU/Memory usage (70% CPU, 80% Memory).

### Health Checks
- Liveness probe: Restarts unhealthy pods
- Readiness probe: Removes pods from service when not ready
- Startup probe: Gives pods time to start before checking health

### Persistent Storage
MySQL data is persisted using PersistentVolumeClaim (10GB).

## 🔗 Useful Commands

```bash
# Watch pod status in real-time
kubectl get pods -n achat-app -w

# Execute commands in pod
kubectl exec -it deployment/achat-app -n achat-app -- bash

# Copy files from/to pod
kubectl cp achat-app/<pod-name>:/path/to/file ./local-file -n achat-app

# View full deployment YAML
kubectl get deployment achat-app -n achat-app -o yaml
```

## 📚 Additional Resources

- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [kubectl Cheat Sheet](https://kubernetes.io/docs/reference/kubectl/cheatsheet/)
- [Spring Boot on Kubernetes](https://spring.io/guides/gs/spring-boot-kubernetes/)
