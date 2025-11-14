# Accessing Your Frontend Application on EKS

## Quick Access Methods

### Method 1: From Jenkins Pipeline Output
After the pipeline completes, check the console output for:
```
=== Frontend LoadBalancer URL ===
✓ Frontend is accessible at: http://<LOAD_BALANCER_HOSTNAME>
```

### Method 2: Using kubectl (if you have access to the cluster)
```bash
# Get the frontend LoadBalancer URL
kubectl get svc achat-frontend -n achat-app -o jsonpath='{.status.loadBalancer.ingress[0].hostname}'

# Or get full service details
kubectl get svc -n achat-app
```

### Method 3: From AWS Console
1. Go to AWS Console → EC2 → Load Balancers
2. Look for load balancers with name containing "achat-frontend" or "achat-app"
3. Copy the DNS name
4. Access at: `http://<DNS_NAME>`

### Method 4: Using AWS CLI
```bash
# List all load balancers
aws elbv2 describe-load-balancers --region us-east-1 --query 'LoadBalancers[?contains(LoadBalancerName, `achat`)].{Name:LoadBalancerName,DNS:DNSName}' --output table

# Get frontend URL
aws elbv2 describe-load-balancers --region us-east-1 --query 'LoadBalancers[?contains(LoadBalancerName, `frontend`)].DNSName' --output text
```

## Troubleshooting

### If LoadBalancer shows "Pending"
- Wait 2-5 minutes for AWS to provision the LoadBalancer
- Check with: `kubectl get svc achat-frontend -n achat-app`
- Verify EKS cluster has proper IAM permissions for ELB

### If DNS doesn't resolve
- LoadBalancer DNS can take 1-2 minutes to propagate
- Try accessing via IP if available
- Check AWS Route53 if using custom domain

### If you get connection refused
- Check if pods are running: `kubectl get pods -n achat-app`
- Check pod logs: `kubectl logs -n achat-app -l app=achat-frontend`
- Verify service selector matches pod labels

## Backend Access
Backend is accessible at:
- `http://<BACKEND_LB_DNS>/SpringMVC`
- Health check: `http://<BACKEND_LB_DNS>/actuator/health`

## Port Information
- Frontend: Port 80 (HTTP)
- Backend: Port 80 → 8089 (container port)

