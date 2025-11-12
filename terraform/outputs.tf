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
  value       = data.aws_iam_role.lab_role.arn
}

output "iam_instance_profile" {
  description = "Name of the IAM instance profile (AWS Academy LabInstanceProfile)"
  value       = data.aws_iam_instance_profile.lab_profile.name
}

# ============================================================================
# Deployment Mode Output
# ============================================================================

output "deploy_mode" {
  description = "Current deployment mode (ec2 or k8s)"
  value       = var.deploy_mode
}

# ============================================================================
# EC2 Instance Outputs (when deploy_mode = "ec2")
# ============================================================================

output "instance_id" {
  description = "ID of the EC2 instance"
  value       = var.deploy_mode == "ec2" ? aws_instance.app[0].id : (var.deploy_mode == "k8s" ? aws_instance.k8s.id : "N/A")
}

output "instance_type" {
  description = "Type of the EC2 instance"
  value       = var.deploy_mode == "ec2" ? aws_instance.app[0].instance_type : (var.deploy_mode == "k8s" ? aws_instance.k8s.instance_type : "N/A")
}

output "instance_state" {
  description = "State of the EC2 instance"
  value       = var.deploy_mode == "ec2" ? aws_instance.app[0].instance_state : (var.deploy_mode == "k8s" ? aws_instance.k8s.instance_state : "N/A")
}

output "private_ip" {
  description = "Private IP address of the EC2 instance"
  value       = var.deploy_mode == "ec2" ? aws_instance.app[0].private_ip : (var.deploy_mode == "k8s" ? aws_instance.k8s.private_ip : "N/A")
}

output "public_ip" {
  description = "Public IP address of the EC2 instance"
  value       = var.deploy_mode == "ec2" ? aws_eip.app[0].public_ip : (var.deploy_mode == "k8s" ? aws_instance.k8s.public_ip : "N/A")
}

output "public_dns" {
  description = "Public DNS name of the EC2 instance"
  value       = var.deploy_mode == "ec2" ? aws_eip.app[0].public_dns : (var.deploy_mode == "k8s" ? aws_instance.k8s.public_dns : "N/A")
}

# ============================================================================
# Application Outputs
# ============================================================================

output "application_url" {
  description = "URL to access the application"
  value       = var.deploy_mode == "ec2" ? "http://${aws_eip.app[0].public_ip}:${var.app_port}" : "http://${aws_instance.k8s.public_ip}"
}

output "health_check_url" {
  description = "URL for application health check"
  value       = var.deploy_mode == "ec2" ? "http://${aws_eip.app[0].public_ip}:${var.app_port}/SpringMVC/actuator/health" : "http://${aws_instance.k8s.public_ip}/SpringMVC/actuator/health"
}

output "swagger_url" {
  description = "URL for Swagger UI"
  value       = var.deploy_mode == "ec2" ? "http://${aws_eip.app[0].public_ip}:${var.app_port}/SpringMVC/swagger-ui/" : "http://${aws_instance.k8s.public_ip}/SpringMVC/swagger-ui/"
}

output "kubernetes_dashboard_url" {
  description = "URL for Kubernetes dashboard (k8s mode only)"
  value       = var.deploy_mode == "k8s" ? "http://${aws_instance.k8s.public_ip}:6443" : "N/A - Not in Kubernetes mode"
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
  value       = var.deploy_mode == "ec2" ? "aws ssm start-session --target ${aws_instance.app[0].id} --region ${var.aws_region}" : "aws ssm start-session --target ${aws_instance.k8s.id} --region ${var.aws_region}"
}

output "kubectl_config_command" {
  description = "Command to get kubeconfig (k8s mode only)"
  value       = var.deploy_mode == "k8s" ? "aws ssm start-session --target ${aws_instance.k8s.id} --region ${var.aws_region} --document-name AWS-StartInteractiveCommand --parameters command='cat /root/.kube/config'" : "N/A - Not in Kubernetes mode"
}

# ============================================================================
# Summary Output (formatted for easy reading)
# ============================================================================

output "deployment_summary" {
  description = "Summary of the deployment"
  value = var.deploy_mode == "k8s" ? <<-EOT
    ╔════════════════════════════════════════════════════════╗
    ║  KUBERNETES DEPLOYMENT SUCCESSFUL                      ║
    ╚════════════════════════════════════════════════════════╝

    Deployment Mode: Kubernetes (k3s)

    Application URLs:
      → Main: http://${aws_instance.k8s.public_ip}/SpringMVC/
      → Health: http://${aws_instance.k8s.public_ip}/SpringMVC/actuator/health
      → Swagger: http://${aws_instance.k8s.public_ip}/SpringMVC/swagger-ui/

    Kubernetes Cluster:
      • Instance ID: ${aws_instance.k8s.id}
      • Instance Type: ${aws_instance.k8s.instance_type}
      • Public IP: ${aws_instance.k8s.public_ip}
      • Region: ${var.aws_region}
      • K8s Distribution: k3s (lightweight Kubernetes)

    Connect to Cluster:
      SSM: aws ssm start-session --target ${aws_instance.k8s.id} --region ${var.aws_region}

    Kubernetes Commands (run on instance):
      • kubectl get pods -n achat-app
      • kubectl get svc -n achat-app
      • kubectl logs -n achat-app -l app=achat-app

    Docker Image:
      → ${var.docker_image}

    ════════════════════════════════════════════════════════
  EOT
  : <<-EOT
    ╔════════════════════════════════════════════════════════╗
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

    ════════════════════════════════════════════════════════
  EOT
}

