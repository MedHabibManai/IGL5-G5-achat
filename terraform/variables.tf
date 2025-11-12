# Terraform Variables
# Define all input variables for the infrastructure

# ============================================================================
# General Configuration
# ============================================================================

variable "project_name" {
  description = "Name of the project"
  type        = string
  default     = "achat-app"
}

variable "environment" {
  description = "Environment name (dev, staging, prod)"
  type        = string
  default     = "sandbox"
}

variable "aws_region" {
  description = "AWS region for resources"
  type        = string
  default     = "us-east-1"
}

# ============================================================================
# Network Configuration
# ============================================================================

variable "vpc_cidr" {
  description = "CIDR block for VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "public_subnet_cidr" {
  description = "CIDR block for public subnet"
  type        = string
  default     = "10.0.1.0/24"
}

variable "availability_zone" {
  description = "Availability zone for resources"
  type        = string
  default     = "us-east-1a"
}

# ============================================================================
# EC2 Configuration
# ============================================================================

variable "instance_type" {
  description = "EC2 instance type"
  type        = string
  default     = "t2.micro"  # Free tier eligible
}

variable "ami_id" {
  description = "AMI ID for EC2 instance (Amazon Linux 2023)"
  type        = string
  default     = ""  # Will be fetched dynamically if empty
}

variable "key_name" {
  description = "Name of the SSH key pair (must exist in AWS)"
  type        = string
  default     = ""  # Optional - leave empty if no SSH access needed
}

variable "root_volume_size" {
  description = "Size of root EBS volume in GB"
  type        = number
  default     = 30  # Minimum required by Amazon Linux 2023 AMI
}

# ============================================================================
# Kubernetes Configuration
# ============================================================================

variable "k8s_instance_type" {
  description = "EC2 instance type for Kubernetes cluster"
  type        = string
  default     = "t2.medium"  # Minimum recommended for k3s with application
}

variable "deploy_mode" {
  description = "Deployment mode: 'ec2' for standalone EC2, 'k8s' for Kubernetes cluster"
  type        = string
  default     = "k8s"
}

# ============================================================================
# Application Configuration
# ============================================================================

variable "app_name" {
  description = "Application name"
  type        = string
  default     = "achat"
}

variable "app_port" {
  description = "Application port"
  type        = number
  default     = 8080
}

variable "docker_image" {
  description = "Docker image to deploy"
  type        = string
  default     = "rayenslouma/achat-app:latest"
}

variable "docker_username" {
  description = "Docker Hub username"
  type        = string
  default     = "rayenslouma"
}

# ============================================================================
# Security Configuration
# ============================================================================

variable "allowed_ssh_cidr" {
  description = "CIDR blocks allowed to SSH (leave empty to disable SSH)"
  type        = list(string)
  default     = []  # Empty = no SSH access
  # Example: ["0.0.0.0/0"] for anywhere (not recommended)
  # Example: ["1.2.3.4/32"] for specific IP
}

variable "allowed_http_cidr" {
  description = "CIDR blocks allowed to access application"
  type        = list(string)
  default     = ["0.0.0.0/0"]  # Allow from anywhere
}

# ============================================================================
# Tags
# ============================================================================

variable "common_tags" {
  description = "Common tags to apply to all resources"
  type        = map(string)
  default = {
    Terraform   = "true"
    Project     = "achat-app"
    Environment = "sandbox"
  }
}

# ============================================================================
# Feature Flags
# ============================================================================

variable "enable_monitoring" {
  description = "Enable CloudWatch detailed monitoring"
  type        = bool
  default     = false  # Costs extra
}

variable "enable_auto_scaling" {
  description = "Enable auto-scaling group"
  type        = bool
  default     = false  # For future enhancement
}

variable "create_rds" {
  description = "Create RDS MySQL database"
  type        = bool
  default     = false  # For future enhancement
}

