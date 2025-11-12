# Kubernetes Manifests for Achat Application

This directory contains Kubernetes manifests for deploying the Achat e-commerce application.

## Files

- `namespace.yaml` - Creates the `achat-app` namespace
- `deployment.yaml` - Defines the application deployment with 1 replica
- `service.yaml` - Exposes the application via NodePort on port 30080

## Deployment Methods

### Method 1: Automatic Deployment (via Terraform)

The Terraform configuration in `terraform/k8s.tf` automatically:
1. Creates an EC2 instance
2. Installs k3s (lightweight Kubernetes)
3. Deploys the application
4. Sets up nginx as a reverse proxy

Simply run:
```bash
cd terraform
terraform apply -var="deploy_mode=k8s"
```

### Method 2: Manual Deployment (to existing cluster)

If you have an existing Kubernetes cluster:

```bash
# Apply all manifests
kubectl apply -f k8s/

# Or apply individually
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
```

## Accessing the Application

### On k3s (deployed via Terraform)

The application is accessible via nginx reverse proxy on port 80:
- Main Application: `http://<PUBLIC_IP>/SpringMVC/`
- Health Check: `http://<PUBLIC_IP>/SpringMVC/actuator/health`
- Swagger UI: `http://<PUBLIC_IP>/SpringMVC/swagger-ui/`

### On other Kubernetes clusters

Access via NodePort:
- `http://<NODE_IP>:30080/SpringMVC/`

Or create an Ingress resource for production use.

## Monitoring

```bash
# Check deployment status
kubectl get deployments -n achat-app

# Check pods
kubectl get pods -n achat-app

# Check service
kubectl get svc -n achat-app

# View logs
kubectl logs -n achat-app -l app=achat-app

# Describe pod (for troubleshooting)
kubectl describe pod -n achat-app -l app=achat-app
```

## Scaling

To scale the application:
```bash
kubectl scale deployment achat-app -n achat-app --replicas=3
```

## Updating the Application

To update to a new Docker image:
```bash
# Update the image in deployment.yaml, then:
kubectl apply -f k8s/deployment.yaml

# Or use kubectl set image:
kubectl set image deployment/achat-app achat-app=rayenslouma/achat-app:NEW_TAG -n achat-app
```

## Cleanup

```bash
# Delete all resources
kubectl delete -f k8s/

# Or delete namespace (removes everything)
kubectl delete namespace achat-app
```

## Configuration

The application uses H2 in-memory database with the following environment variables:
- `SPRING_DATASOURCE_URL`: H2 in-memory database URL
- `SPRING_DATASOURCE_DRIVER_CLASS_NAME`: H2 driver
- `SPRING_JPA_HIBERNATE_DDL_AUTO`: create-drop (recreates schema on startup)

## Health Checks

The deployment includes:
- **Liveness Probe**: Checks if the application is running (restarts if failing)
- **Readiness Probe**: Checks if the application is ready to serve traffic

Both probes use the `/SpringMVC/actuator/health` endpoint.

## Resource Limits

Each pod is configured with:
- **Requests**: 512Mi memory, 250m CPU
- **Limits**: 1Gi memory, 500m CPU

Adjust these values in `deployment.yaml` based on your cluster capacity.

