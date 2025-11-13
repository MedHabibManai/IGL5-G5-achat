# Terraform Outputs
# Define output values to display after deployment

# ============================================================================
# Network Outputs
# ============================================================================

output "vpc_id" {
  description = "ID of the VPC"
  value       = aws_vpc.main.id
}

output "vpc_cidr" {
  description = "CIDR block of the VPC"
  value       = aws_vpc.main.cidr_block
}

output "subnet_id" {
  description = "ID of the public subnet"
  value       = aws_subnet.public.id
}

output "internet_gateway_id" {
  description = "ID of the Internet Gateway"
  value       = aws_internet_gateway.main.id
}

# ============================================================================
# Security Outputs
# ============================================================================

output "security_group_id" {
  description = "ID of the application security group"
  value       = aws_security_group.app.id
}

output "iam_role_arn" {
  description = "ARN of the EC2 IAM role (AWS Academy LabRole)"
  value       = local.lab_role_arn
}

output "iam_instance_profile" {
  description = "Name of the IAM instance profile (AWS Academy LabInstanceProfile)"
  value       = local.lab_instance_profile_name
}

# ============================================================================
# Deployment Mode Output
# ============================================================================

output "deploy_mode" {
  description = "Current deployment mode (ec2, k8s, or eks)"
  value       = var.deploy_mode
}

# ============================================================================
# EKS Cluster Outputs (when deploy_mode = "eks")
# ============================================================================

output "eks_cluster_name" {
  description = "Name of the EKS cluster"
  value       = var.deploy_mode == "eks" ? aws_eks_cluster.main[0].name : "N/A"
}

output "eks_cluster_id" {
  description = "ID of the EKS cluster"
  value       = var.deploy_mode == "eks" ? aws_eks_cluster.main[0].id : "N/A"
}

output "eks_cluster_endpoint" {
  description = "Endpoint for EKS cluster API server"
  value       = var.deploy_mode == "eks" ? aws_eks_cluster.main[0].endpoint : "N/A"
}

output "eks_cluster_version" {
  description = "Kubernetes version of the EKS cluster"
  value       = var.deploy_mode == "eks" ? aws_eks_cluster.main[0].version : "N/A"
}

output "eks_cluster_arn" {
  description = "ARN of the EKS cluster"
  value       = var.deploy_mode == "eks" ? aws_eks_cluster.main[0].arn : "N/A"
}

output "eks_cluster_security_group_id" {
  description = "Security group ID attached to the EKS cluster"
  value       = var.deploy_mode == "eks" ? aws_security_group.eks_cluster[0].id : "N/A"
}

output "eks_nodes_security_group_id" {
  description = "Security group ID for EKS worker nodes"
  value       = var.deploy_mode == "eks" ? aws_security_group.eks_nodes[0].id : "N/A"
}

output "private_subnet_id" {
  description = "ID of the private subnet (EKS mode only)"
  value       = var.deploy_mode == "eks" ? aws_subnet.private[0].id : "N/A"
}

output "nat_gateway_id" {
  description = "ID of the NAT Gateway (EKS mode only)"
  value       = var.deploy_mode == "eks" ? aws_nat_gateway.main[0].id : "N/A"
}

output "eks_node_group_id" {
  description = "ID of the EKS node group"
  value       = var.deploy_mode == "eks" ? aws_eks_node_group.main[0].id : "N/A"
}

output "eks_node_group_status" {
  description = "Status of the EKS node group"
  value       = var.deploy_mode == "eks" ? aws_eks_node_group.main[0].status : "N/A"
}

# ============================================================================
# EC2 Instance Outputs (when deploy_mode = "ec2")
# ============================================================================

output "instance_id" {
  description = "ID of the EC2 instance (N/A for EKS)"
  value       = var.deploy_mode == "ec2" ? aws_instance.app[0].id : (var.deploy_mode == "k8s" ? aws_instance.k8s[0].id : "N/A - Using EKS")
}

output "instance_type" {
  description = "Type of the EC2 instance (N/A for EKS)"
  value       = var.deploy_mode == "ec2" ? aws_instance.app[0].instance_type : (var.deploy_mode == "k8s" ? aws_instance.k8s[0].instance_type : "N/A - Using EKS")
}

output "instance_state" {
  description = "State of the EC2 instance (N/A for EKS)"
  value       = var.deploy_mode == "ec2" ? aws_instance.app[0].instance_state : (var.deploy_mode == "k8s" ? aws_instance.k8s[0].instance_state : "N/A - Using EKS")
}

output "private_ip" {
  description = "Private IP address of the EC2 instance (N/A for EKS)"
  value       = var.deploy_mode == "ec2" ? aws_instance.app[0].private_ip : (var.deploy_mode == "k8s" ? aws_instance.k8s[0].private_ip : "N/A - Using EKS")
}

output "public_ip" {
  description = "Public IP address (N/A for EKS - use LoadBalancer service)"
  value       = var.deploy_mode == "ec2" ? aws_eip.app[0].public_ip : (var.deploy_mode == "k8s" ? aws_instance.k8s[0].public_ip : "N/A - Using EKS LoadBalancer")
}

output "public_dns" {
  description = "Public DNS name (N/A for EKS - use LoadBalancer service)"
  value       = var.deploy_mode == "ec2" ? aws_eip.app[0].public_dns : (var.deploy_mode == "k8s" ? aws_instance.k8s[0].public_dns : "N/A - Using EKS LoadBalancer")
}

# ============================================================================
# Application Outputs
# ============================================================================

output "application_url" {
  description = "URL to access the application (for EKS, deploy app first to get LoadBalancer URL)"
  value       = var.deploy_mode == "ec2" ? "http://${aws_eip.app[0].public_ip}:${var.app_port}" : (var.deploy_mode == "k8s" ? "http://${aws_instance.k8s[0].public_ip}" : "Deploy application to get LoadBalancer URL")
}

output "health_check_url" {
  description = "URL for application health check (for EKS, deploy app first)"
  value       = var.deploy_mode == "ec2" ? "http://${aws_eip.app[0].public_ip}:${var.app_port}/SpringMVC/actuator/health" : (var.deploy_mode == "k8s" ? "http://${aws_instance.k8s[0].public_ip}/SpringMVC/actuator/health" : "Deploy application to get LoadBalancer URL")
}

output "swagger_url" {
  description = "URL for Swagger UI (for EKS, deploy app first)"
  value       = var.deploy_mode == "ec2" ? "http://${aws_eip.app[0].public_ip}:${var.app_port}/SpringMVC/swagger-ui/" : (var.deploy_mode == "k8s" ? "http://${aws_instance.k8s[0].public_ip}/SpringMVC/swagger-ui/" : "Deploy application to get LoadBalancer URL")
}

output "kubectl_config_command" {
  description = "Command to configure kubectl for EKS cluster"
  value       = var.deploy_mode == "eks" ? "aws eks update-kubeconfig --region ${var.aws_region} --name ${aws_eks_cluster.main[0].name}" : "N/A - Not using EKS"
}

# ============================================================================
# Deployment Information
# ============================================================================

output "deployment_info" {
  description = "Deployment information"
  value = {
    project     = var.project_name
    environment = var.environment
    region      = var.aws_region
    docker_image = var.docker_image
    deployed_at = timestamp()
  }
}

# ============================================================================
# Connection Information
# ============================================================================

output "ssh_command" {
  description = "SSH command to connect to the instance (if key is configured)"
  value       = "SSH not configured - use AWS Systems Manager Session Manager"
}

output "ssm_connect_command" {
  description = "AWS CLI command to connect via Session Manager"
  value       = var.deploy_mode == "ec2" ? "aws ssm start-session --target ${aws_instance.app[0].id} --region ${var.aws_region}" : (var.deploy_mode == "k8s" ? "aws ssm start-session --target ${aws_instance.k8s[0].id} --region ${var.aws_region}" : "N/A - Using EKS")
}

output "k8s_kubeconfig_command" {
  description = "Command to get kubeconfig (k8s mode only)"
  value       = var.deploy_mode == "k8s" ? "aws ssm start-session --target ${aws_instance.k8s[0].id} --region ${var.aws_region} --document-name AWS-StartInteractiveCommand --parameters command='cat /root/.kube/config'" : "N/A - Not in Kubernetes mode"
}

# ============================================================================
# Summary Output (formatted for easy reading)
# ============================================================================

output "deployment_summary" {
  description = "Summary of the deployment"
  value = <<-EOT
%{if var.deploy_mode == "eks"}╔════════════════════════════════════════════════════════╗
║  AWS EKS DEPLOYMENT SUCCESSFUL                         ║
╚════════════════════════════════════════════════════════╝

Deployment Mode: AWS EKS (Elastic Kubernetes Service)

EKS Cluster Information:
  • Cluster Name: ${aws_eks_cluster.main[0].name}
  • Cluster Endpoint: ${aws_eks_cluster.main[0].endpoint}
  • Kubernetes Version: ${aws_eks_cluster.main[0].version}
  • Region: ${var.aws_region}
  • Node Group: ${aws_eks_node_group.main[0].id}
  • Node Instance Type: ${var.eks_node_instance_type}
  • Desired Nodes: ${var.eks_node_desired_size}

Configure kubectl:
  aws eks update-kubeconfig --region ${var.aws_region} --name ${aws_eks_cluster.main[0].name}

Next Steps:
  1. Configure kubectl using the command above
  2. Deploy application: kubectl apply -f k8s/
  3. Get LoadBalancer URL: kubectl get svc -n achat-app

Kubernetes Commands:
  • kubectl get nodes
  • kubectl get pods -n achat-app
  • kubectl get svc -n achat-app
  • kubectl logs -n achat-app -l app=achat-app

Docker Image:
  → ${var.docker_image}
%{~endif~}
%{if var.deploy_mode == "k8s"}╔════════════════════════════════════════════════════════╗
║  KUBERNETES DEPLOYMENT SUCCESSFUL                      ║
╚════════════════════════════════════════════════════════╝

Deployment Mode: Kubernetes (k3s)

Application URLs:
  → Main: http://${aws_instance.k8s[0].public_ip}/SpringMVC/
  → Health: http://${aws_instance.k8s[0].public_ip}/SpringMVC/actuator/health
  → Swagger: http://${aws_instance.k8s[0].public_ip}/SpringMVC/swagger-ui/

Kubernetes Cluster:
  • Instance ID: ${aws_instance.k8s[0].id}
  • Instance Type: ${aws_instance.k8s[0].instance_type}
  • Public IP: ${aws_instance.k8s[0].public_ip}
  • Region: ${var.aws_region}
  • K8s Distribution: k3s (lightweight Kubernetes)

Connect to Cluster:
  SSM: aws ssm start-session --target ${aws_instance.k8s[0].id} --region ${var.aws_region}

Kubernetes Commands (run on instance):
  • kubectl get pods -n achat-app
  • kubectl get svc -n achat-app
  • kubectl logs -n achat-app -l app=achat-app

Docker Image:
  → ${var.docker_image}
%{~endif~}
%{if var.deploy_mode == "ec2"}╔════════════════════════════════════════════════════════╗
║  EC2 DEPLOYMENT SUCCESSFUL                             ║
╚════════════════════════════════════════════════════════╝

Deployment Mode: Standalone EC2

Application URLs:
  → Main: http://${aws_eip.app[0].public_ip}:${var.app_port}/SpringMVC/
  → Health: http://${aws_eip.app[0].public_ip}:${var.app_port}/SpringMVC/actuator/health
  → Swagger: http://${aws_eip.app[0].public_ip}:${var.app_port}/SpringMVC/swagger-ui/

Instance Details:
  • Instance ID: ${aws_instance.app[0].id}
  • Instance Type: ${aws_instance.app[0].instance_type}
  • Public IP: ${aws_eip.app[0].public_ip}
  • Region: ${var.aws_region}

Connect to Instance:
  SSM: aws ssm start-session --target ${aws_instance.app[0].id} --region ${var.aws_region}

Docker Image:
  → ${var.docker_image}
%{endif}
════════════════════════════════════════════════════════
EOT
}

