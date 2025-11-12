# Main Terraform Configuration
# Defines AWS infrastructure for deploying the Spring Boot application

# ============================================================================
# Data Sources
# ============================================================================

# Get latest Amazon Linux 2023 AMI
data "aws_ami" "amazon_linux_2023" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-*-x86_64"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

# Get current AWS account ID
data "aws_caller_identity" "current" {}

# Get available availability zones
data "aws_availability_zones" "available" {
  state = "available"
}

# ============================================================================
# VPC and Networking
# ============================================================================

# Create VPC
resource "aws_vpc" "main" {
  cidr_block           = var.vpc_cidr
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = merge(
    var.common_tags,
    {
      Name = "${var.project_name}-vpc"
    }
  )
}

# Create Internet Gateway
resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id

  tags = merge(
    var.common_tags,
    {
      Name = "${var.project_name}-igw"
    }
  )
}

# Create Public Subnet
resource "aws_subnet" "public" {
  vpc_id                  = aws_vpc.main.id
  cidr_block              = var.public_subnet_cidr
  availability_zone       = var.availability_zone != "" ? var.availability_zone : data.aws_availability_zones.available.names[0]
  map_public_ip_on_launch = true

  tags = merge(
    var.common_tags,
    {
      Name = "${var.project_name}-public-subnet"
      Type = "Public"
      # EKS LoadBalancer tags for automatic subnet discovery
      "kubernetes.io/role/elb" = "1"
      "kubernetes.io/cluster/${var.project_name}-eks-cluster" = "shared"
    }
  )
}

# Create Route Table
resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }

  tags = merge(
    var.common_tags,
    {
      Name = "${var.project_name}-public-rt"
    }
  )
}

# Associate Route Table with Subnet
resource "aws_route_table_association" "public" {
  subnet_id      = aws_subnet.public.id
  route_table_id = aws_route_table.public.id
}

# ============================================================================
# Security Groups
# ============================================================================

# Security Group for EC2 Instance
resource "aws_security_group" "app" {
  name        = "${var.project_name}-app-sg"
  description = "Security group for ${var.project_name} application"
  vpc_id      = aws_vpc.main.id

  # Allow HTTP traffic to application
  ingress {
    description = "HTTP from allowed IPs"
    from_port   = var.app_port
    to_port     = var.app_port
    protocol    = "tcp"
    cidr_blocks = var.allowed_http_cidr
  }

  # Allow SSH (only if allowed_ssh_cidr is not empty)
  dynamic "ingress" {
    for_each = length(var.allowed_ssh_cidr) > 0 ? [1] : []
    content {
      description = "SSH from allowed IPs"
      from_port   = 22
      to_port     = 22
      protocol    = "tcp"
      cidr_blocks = var.allowed_ssh_cidr
    }
  }

  # Allow all outbound traffic
  egress {
    description = "Allow all outbound"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(
    var.common_tags,
    {
      Name = "${var.project_name}-app-sg"
    }
  )
}

# ============================================================================
# IAM Role for EC2 - Using AWS Academy LabRole
# ============================================================================
# Note: AWS Academy Learner Lab does not allow IAM role creation
# We use the existing LabRole and LabInstanceProfile

# Data source to get the existing LabRole
data "aws_iam_role" "lab_role" {
  name = "LabRole"
}

# Data source to get the existing LabInstanceProfile
data "aws_iam_instance_profile" "lab_profile" {
  name = "LabInstanceProfile"
}

# ============================================================================
# EC2 Instance
# ============================================================================

# User Data Script to install Docker and run application
locals {
  user_data = <<-EOF
    #!/bin/bash
    set -e
    
    # Update system
    yum update -y
    
    # Install Docker
    yum install -y docker
    systemctl start docker
    systemctl enable docker
    
    # Add ec2-user to docker group
    usermod -a -G docker ec2-user
    
    # Install Docker Compose
    curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    chmod +x /usr/local/bin/docker-compose
    
    # Pull and run the application
    docker pull ${var.docker_image}
    
    # Run the application container
    # Note: Application runs on port 8089 inside container, exposed on port 8080
    # Using H2 in-memory database instead of MySQL
    docker run -d \
      --name ${var.app_name} \
      --restart unless-stopped \
      -p ${var.app_port}:8089 \
      -e SPRING_DATASOURCE_URL='jdbc:h2:mem:achatdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE' \
      -e SPRING_DATASOURCE_DRIVER_CLASS_NAME='org.h2.Driver' \
      -e SPRING_DATASOURCE_USERNAME='sa' \
      -e SPRING_DATASOURCE_PASSWORD='' \
      -e SPRING_JPA_HIBERNATE_DDL_AUTO='create-drop' \
      -e SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT='org.hibernate.dialect.H2Dialect' \
      ${var.docker_image}
    
    # Create a simple health check script
    # Note: Application runs on port 8089 inside container
    cat > /usr/local/bin/health-check.sh << 'HEALTH'
    #!/bin/bash
    curl -f http://localhost:8089/actuator/health || exit 1
    HEALTH
    chmod +x /usr/local/bin/health-check.sh
    
    # Log deployment
    echo "Application deployed successfully at $(date)" >> /var/log/app-deployment.log
    docker ps >> /var/log/app-deployment.log
  EOF
}

# EC2 Instance (only created when deploy_mode = "ec2")
resource "aws_instance" "app" {
  count = var.deploy_mode == "ec2" ? 1 : 0

  ami                    = var.ami_id != "" ? var.ami_id : data.aws_ami.amazon_linux_2023.id
  instance_type          = var.instance_type
  subnet_id              = aws_subnet.public.id
  vpc_security_group_ids = [aws_security_group.app.id]
  iam_instance_profile   = data.aws_iam_instance_profile.lab_profile.name
  key_name               = var.key_name != "" ? var.key_name : null

  user_data = local.user_data

  monitoring = var.enable_monitoring

  root_block_device {
    volume_size           = var.root_volume_size
    volume_type           = "gp3"
    delete_on_termination = true
    encrypted             = true
  }

  metadata_options {
    http_endpoint               = "enabled"
    http_tokens                 = "required"  # IMDSv2
    http_put_response_hop_limit = 1
  }

  tags = merge(
    var.common_tags,
    {
      Name = "${var.project_name}-instance"
    }
  )
}

# ============================================================================
# Elastic IP (Optional - for static IP) - Only for EC2 mode
# ============================================================================

resource "aws_eip" "app" {
  count = var.deploy_mode == "ec2" ? 1 : 0

  instance = aws_instance.app[0].id
  domain   = "vpc"

  tags = merge(
    var.common_tags,
    {
      Name = "${var.project_name}-eip"
    }
  )

  depends_on = [aws_internet_gateway.main]
}

