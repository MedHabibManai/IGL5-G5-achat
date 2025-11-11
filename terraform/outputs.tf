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
  value       = "http://${aws_eip.app.public_ip}:${var.app_port}"
}

output "health_check_url" {
  description = "URL for application health check"
  value       = "http://${aws_eip.app.public_ip}:${var.app_port}/actuator/health"
}

output "swagger_url" {
  description = "URL for Swagger UI (if enabled)"
  value       = "http://${aws_eip.app.public_ip}:${var.app_port}/swagger-ui.html"
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
      → http://${aws_eip.app.public_ip}:${var.app_port}
    
    Health Check:
      → http://${aws_eip.app.public_ip}:${var.app_port}/actuator/health
    
    Instance Details:
      • Instance ID: ${aws_instance.app.id}
      • Instance Type: ${aws_instance.app.instance_type}
      • Public IP: ${aws_eip.app.public_ip}
      • Region: ${var.aws_region}
    
    Connect to Instance:
      ${var.key_name != "" ? "SSH: ssh -i ~/.ssh/${var.key_name}.pem ec2-user@${aws_eip.app.public_ip}" : "SSM: aws ssm start-session --target ${aws_instance.app.id}"}
    
    Docker Image:
      → ${var.docker_image}
    
    ════════════════════════════════════════════════════════
  EOT
}

