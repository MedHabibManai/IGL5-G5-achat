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
# IAM Role for EC2 - Use existing LabRole from AWS Learner Lab
# ============================================================================

# Use the existing LabRole provided by AWS Learner Lab
data "aws_iam_role" "lab_role" {
  name = "LabRole"
}

# Use existing LabInstanceProfile from AWS Learner Lab
# Most learner labs provide this pre-configured instance profile
data "aws_iam_instance_profile" "lab_profile" {
  name = "LabInstanceProfile"
}

# ============================================================================
# EC2 Instance
# ============================================================================

# ============================================================================
# RDS MySQL Database
# ============================================================================

# Security Group for RDS
resource "aws_security_group" "rds" {
  name        = "${var.project_name}-rds-sg"
  description = "Security group for ${var.project_name} RDS MySQL"
  vpc_id      = aws_vpc.main.id

  # Allow MySQL traffic from application security group
  ingress {
    description     = "MySQL from application"
    from_port       = 3306
    to_port         = 3306
    protocol        = "tcp"
    security_groups = [aws_security_group.app.id]
  }

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
      Name = "${var.project_name}-rds-sg"
    }
  )
}

# DB Subnet Group (RDS requires at least 2 subnets in different AZs)
resource "aws_subnet" "private" {
  vpc_id            = aws_vpc.main.id
  cidr_block        = "10.0.2.0/24"
  availability_zone = data.aws_availability_zones.available.names[1]

  tags = merge(
    var.common_tags,
    {
      Name = "${var.project_name}-private-subnet"
    }
  )
}

resource "aws_db_subnet_group" "main" {
  name       = "${var.project_name}-db-subnet-group"
  subnet_ids = [aws_subnet.public.id, aws_subnet.private.id]

  tags = merge(
    var.common_tags,
    {
      Name = "${var.project_name}-db-subnet-group"
    }
  )
}

# RDS MySQL Instance - db.t3.micro (cheapest option, ~$0.017/hour = ~$12.24/month)
resource "aws_db_instance" "mysql" {
  identifier        = "${var.project_name}-db"
  engine            = "mysql"
  engine_version    = "8.0.39"
  instance_class    = "db.t3.micro"
  allocated_storage = 20
  storage_type      = "gp2"

  db_name  = "achatdb"
  username = "admin"
  password = "Admin123456!" # Change this to a secure password

  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds.id]
  publicly_accessible    = false

  # Performance and cost optimization
  skip_final_snapshot       = true
  backup_retention_period   = 1 # Minimum backup retention
  multi_az                  = false
  deletion_protection       = false
  storage_encrypted         = false
  auto_minor_version_upgrade = true

  tags = merge(
    var.common_tags,
    {
      Name = "${var.project_name}-mysql-db"
    }
  )
}

# User Data Script to install Docker and run application
locals {
  user_data = <<-EOF
    #!/bin/bash
    # Redirect all output to console and log file
    exec > >(tee /var/log/user-data.log|logger -t user-data -s 2>/dev/console) 2>&1
    
    echo "=========================================="
    echo "Starting user-data script at $(date)"
    echo "=========================================="
    
    set -e
    
    # Update system
    echo "Step 1: Updating system packages..."
    yum update -y
    
    # Install Docker and netcat for database connectivity check
    echo "Step 2: Installing Docker..."
    yum install -y docker nc
    systemctl start docker
    systemctl enable docker
    
    # Add ec2-user to docker group
    usermod -a -G docker ec2-user
    
    # Install Docker Compose
    echo "Step 3: Installing Docker Compose..."
    curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    chmod +x /usr/local/bin/docker-compose
    
    # Wait for RDS to be ready (with timeout of 10 minutes)
    echo "Step 4: Waiting for RDS MySQL to be ready..."
    echo "RDS Endpoint: ${aws_db_instance.mysql.address}"
    MAX_WAIT=600
    ELAPSED=0
    until timeout 5 bash -c "cat < /dev/null > /dev/tcp/${aws_db_instance.mysql.address}/3306" 2>/dev/null; do
      if [ $ELAPSED -ge $MAX_WAIT ]; then
        echo "ERROR: Timeout waiting for database after $MAX_WAIT seconds"
        exit 1
      fi
      echo "Waiting for database connection... ($ELAPSED/$MAX_WAIT seconds)"
      sleep 10
      ELAPSED=$((ELAPSED + 10))
    done
    echo "SUCCESS: RDS MySQL is ready!"
    
    # Pull application image
    echo "Step 5: Pulling Docker image: ${var.docker_image}"
    docker pull ${var.docker_image}
    
    # Run the application container with RDS connection
    echo "Step 6: Starting application container..."
    echo "Database URL: jdbc:mysql://${aws_db_instance.mysql.address}:3306/achatdb"
    docker run -d \
      --name ${var.app_name} \
      --restart unless-stopped \
      -p ${var.app_port}:${var.app_port} \
      -e SPRING_DATASOURCE_URL="jdbc:mysql://${aws_db_instance.mysql.address}:3306/achatdb?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true" \
      -e SPRING_DATASOURCE_USERNAME="admin" \
      -e SPRING_DATASOURCE_PASSWORD="Admin123456!" \
      -e SPRING_JPA_HIBERNATE_DDL_AUTO="update" \
      ${var.docker_image}
    
    # Wait a bit for container to start
    echo "Step 7: Waiting for container to initialize..."
    sleep 10
    
    # Check if container is running
    echo "Step 8: Checking container status..."
    if docker ps | grep ${var.app_name}; then
      echo "SUCCESS: Application container is running"
      docker ps | grep ${var.app_name}
    else
      echo "ERROR: Application container failed to start"
      echo "Container logs:"
      docker logs ${var.app_name} || true
      exit 1
    fi
    
    # Show container logs
    echo "=========================================="
    echo "Container Logs (last 100 lines):"
    echo "=========================================="
    docker logs --tail 100 ${var.app_name}
    
    # Create a simple health check script
    cat > /usr/local/bin/health-check.sh << 'HEALTH'
    #!/bin/bash
    curl -f http://localhost:${var.app_port}/SpringMVC/actuator/health || exit 1
    HEALTH
    chmod +x /usr/local/bin/health-check.sh
    
    # Log deployment summary
    echo "=========================================="
    echo "Deployment Summary"
    echo "=========================================="
    echo "Deployment completed at: $(date)"
    echo "Database endpoint: ${aws_db_instance.mysql.address}"
    echo "Application port: ${var.app_port}"
    echo "Docker image: ${var.docker_image}"
    echo "Container status:"
    docker ps
    echo "=========================================="
    echo "User-data script completed successfully!"
    echo "=========================================="
    
    # Also save to log file
    echo "Deployment completed successfully at $(date)" >> /var/log/app-deployment.log
    docker logs ${var.app_name} >> /var/log/app-deployment.log 2>&1 || true
  EOF
}

# EC2 Instance
resource "aws_instance" "app" {
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
# Elastic IP (for static public IP)
# ============================================================================

resource "aws_eip" "app" {
  domain = "vpc"

  tags = merge(
    var.common_tags,
    {
      Name = "${var.project_name}-eip"
    }
  )

  depends_on = [aws_internet_gateway.main]
}

# Associate Elastic IP with EC2 instance
resource "aws_eip_association" "app" {
  instance_id   = aws_instance.app.id
  allocation_id = aws_eip.app.id
}
