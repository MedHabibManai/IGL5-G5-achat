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
  description = "ARN of the EC2 IAM role (using existing LabRole)"
  value       = data.aws_iam_role.lab_role.arn
}

output "iam_instance_profile_name" {
  description = "Name of the IAM instance profile (using existing LabInstanceProfile)"
  value       = data.aws_iam_instance_profile.lab_profile.name
}

# ============================================================================
# EC2 Instance Outputs
# ============================================================================

output "instance_id" {
  description = "ID of the EC2 instance"
  value       = aws_instance.app.id
}

output "instance_type" {
  description = "Type of the EC2 instance"
  value       = aws_instance.app.instance_type
}

output "instance_state" {
  description = "State of the EC2 instance"
  value       = aws_instance.app.instance_state
}

output "private_ip" {
  description = "Private IP address of the EC2 instance"
  value       = aws_instance.app.private_ip
}

output "public_ip" {
  description = "Public IP address of the EC2 instance (Elastic IP)"
  value       = aws_eip.app.public_ip
}

output "public_dns" {
  description = "Public DNS name of the EC2 instance"
  value       = aws_eip.app.public_dns
}

# ============================================================================
# Application Outputs
# ============================================================================

output "application_url" {
  description = "URL to access the application"
  value       = "http://${aws_eip.app.public_ip}:${var.app_port}/SpringMVC"
}

output "health_check_url" {
  description = "URL for application health check"
  value       = "http://${aws_eip.app.public_ip}:${var.app_port}/SpringMVC/actuator/health"
}

output "swagger_url" {
  description = "URL for Swagger UI (if enabled)"
  value       = "http://${aws_eip.app.public_ip}:${var.app_port}/SpringMVC/swagger-ui.html"
}

# ============================================================================
# Database Outputs
# ============================================================================

output "rds_endpoint" {
  description = "RDS MySQL endpoint address"
  value       = aws_db_instance.mysql.address
}

output "rds_port" {
  description = "RDS MySQL port"
  value       = aws_db_instance.mysql.port
}

output "database_name" {
  description = "Database name"
  value       = aws_db_instance.mysql.db_name
}

output "rds_instance_class" {
  description = "RDS instance class"
  value       = aws_db_instance.mysql.instance_class
}

output "rds_storage" {
  description = "RDS allocated storage (GB)"
  value       = aws_db_instance.mysql.allocated_storage
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
  value       = var.key_name != "" ? "ssh -i ~/.ssh/${var.key_name}.pem ec2-user@${aws_eip.app.public_ip}" : "SSH not configured - use AWS Systems Manager Session Manager"
}

output "ssm_connect_command" {
  description = "AWS CLI command to connect via Session Manager"
  value       = "aws ssm start-session --target ${aws_instance.app.id} --region ${var.aws_region}"
}

# ============================================================================
# Summary Output (formatted for easy reading)
# ============================================================================

output "deployment_summary" {
  description = "Summary of the deployment"
  value = <<-EOT
    ╔════════════════════════════════════════════════════════╗
    ║  DEPLOYMENT SUCCESSFUL                                 ║
    ╚════════════════════════════════════════════════════════╝
    
    Application URL:
      → http://${aws_eip.app.public_ip}:${var.app_port}/SpringMVC
    
    Health Check:
      → http://${aws_eip.app.public_ip}:${var.app_port}/SpringMVC/actuator/health
    
    Instance Details:
      • Instance ID: ${aws_instance.app.id}
      • Instance Type: ${aws_instance.app.instance_type}
      • Public IP: ${aws_eip.app.public_ip}
      • Region: ${var.aws_region}
    
    Database Details:
      • RDS Endpoint: ${aws_db_instance.mysql.address}
      • Database: ${aws_db_instance.mysql.db_name}
      • Instance Class: ${aws_db_instance.mysql.instance_class} (~$0.017/hour)
      • Storage: ${aws_db_instance.mysql.allocated_storage} GB
    
    Connect to Instance:
      ${var.key_name != "" ? "SSH: ssh -i ~/.ssh/${var.key_name}.pem ec2-user@${aws_eip.app.public_ip}" : "SSM: aws ssm start-session --target ${aws_instance.app.id}"}
    
    Docker Image:
      → ${var.docker_image}
    
    ════════════════════════════════════════════════════════
  EOT
}

# ============================================================================
# EKS Outputs
# ============================================================================

output "eks_cluster_name" {
  description = "Name of the EKS cluster"
  value       = var.create_eks ? try(aws_eks_cluster.main[0].name, "") : ""
}

output "eks_cluster_endpoint" {
  description = "Endpoint for EKS cluster"
  value       = var.create_eks ? try(aws_eks_cluster.main[0].endpoint, "") : ""
}

output "eks_cluster_version" {
  description = "Kubernetes version of the EKS cluster"
  value       = var.create_eks ? try(aws_eks_cluster.main[0].version, "") : ""
}

output "eks_cluster_security_group_id" {
  description = "Security group ID attached to the EKS cluster"
  value       = var.create_eks ? try(aws_security_group.eks_cluster[0].id, "") : ""
}

output "eks_node_group_name" {
  description = "Name of the EKS node group"
  value       = var.create_eks ? try(aws_eks_node_group.main[0].node_group_name, "") : ""
}

output "eks_node_group_status" {
  description = "Status of the EKS node group"
  value       = var.create_eks ? try(aws_eks_node_group.main[0].status, "") : ""
}

output "eks_kubeconfig_command" {
  description = "Command to update kubeconfig for EKS cluster"
  value       = var.create_eks ? try("aws eks update-kubeconfig --region ${var.aws_region} --name ${aws_eks_cluster.main[0].name}", "") : "EKS not enabled"
}


