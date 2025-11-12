# Kubernetes Cluster Configuration (k3s on EC2)
# This file defines a lightweight Kubernetes cluster using k3s
# k3s is a certified Kubernetes distribution perfect for AWS Academy constraints

# ============================================================================
# Data Sources for Existing IAM Resources
# ============================================================================

# Use existing AWS Academy LabRole
data "aws_iam_role" "lab_role" {
  name = "LabRole"
}

# Use existing AWS Academy LabInstanceProfile
data "aws_iam_instance_profile" "lab_profile" {
  name = "LabInstanceProfile"
}

# ============================================================================
# Security Group for Kubernetes Cluster
# ============================================================================

resource "aws_security_group" "k8s" {
  name        = "${var.project_name}-k8s-sg"
  description = "Security group for ${var.project_name} Kubernetes cluster"
  vpc_id      = aws_vpc.main.id

  # Allow HTTP traffic to application (NodePort range)
  ingress {
    description = "HTTP from anywhere"
    from_port   = 30000
    to_port     = 32767
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Allow HTTP traffic on port 80 (for LoadBalancer simulation)
  ingress {
    description = "HTTP on port 80"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Allow HTTPS traffic on port 443
  ingress {
    description = "HTTPS on port 443"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Allow Kubernetes API server
  ingress {
    description = "Kubernetes API"
    from_port   = 6443
    to_port     = 6443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
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
      Name = "${var.project_name}-k8s-sg"
    }
  )
}

# ============================================================================
# EC2 Instance for k3s Kubernetes Cluster
# ============================================================================

resource "aws_instance" "k8s" {
  ami                    = data.aws_ami.amazon_linux_2023.id
  instance_type          = var.k8s_instance_type
  subnet_id              = aws_subnet.public.id
  vpc_security_group_ids = [aws_security_group.k8s.id]
  iam_instance_profile   = data.aws_iam_instance_profile.lab_profile.name

  # Increased volume size for Kubernetes and Docker images
  root_block_device {
    volume_size           = 30
    volume_type           = "gp3"
    delete_on_termination = true
    encrypted             = false

    tags = merge(
      var.common_tags,
      {
        Name = "${var.project_name}-k8s-volume"
      }
    )
  }

  user_data = base64encode(local.k8s_user_data)

  tags = merge(
    var.common_tags,
    {
      Name = "${var.project_name}-k8s-cluster"
      Type = "Kubernetes"
    }
  )

  lifecycle {
    create_before_destroy = false
    ignore_changes        = []
  }
}

# ============================================================================
# User Data Script for k3s Installation
# ============================================================================

locals {
  k8s_user_data = <<-EOF
    #!/bin/bash
    set -e
    
    # Log all output
    exec > >(tee /var/log/user-data.log)
    exec 2>&1
    
    echo "=========================================="
    echo "Starting k3s Kubernetes Cluster Setup"
    echo "=========================================="
    
    # Update system
    echo "Updating system packages..."
    yum update -y
    
    # Install required packages
    echo "Installing required packages..."
    yum install -y curl wget git jq
    
    # Install k3s (lightweight Kubernetes)
    echo "Installing k3s..."
    curl -sfL https://get.k3s.io | INSTALL_K3S_EXEC="server --write-kubeconfig-mode=644" sh -
    
    # Wait for k3s to be ready
    echo "Waiting for k3s to be ready..."
    sleep 30
    
    # Verify k3s installation
    echo "Verifying k3s installation..."
    k3s kubectl get nodes
    
    # Create kubeconfig for external access
    echo "Setting up kubeconfig..."
    mkdir -p /root/.kube
    cp /etc/rancher/k3s/k3s.yaml /root/.kube/config
    chmod 600 /root/.kube/config
    
    # Get public IP
    PUBLIC_IP=$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4)
    echo "Public IP: $PUBLIC_IP"
    
    # Update kubeconfig with public IP
    sed -i "s/127.0.0.1/$PUBLIC_IP/g" /root/.kube/config
    
    # Create namespace for application
    echo "Creating application namespace..."
    k3s kubectl create namespace achat-app || true
    
    # Create deployment manifest
    echo "Creating Kubernetes deployment manifest..."
    cat > /root/deployment.yaml <<'DEPLOY_EOF'
apiVersion: apps/v1
kind: Deployment
metadata:
  name: achat-app
  namespace: achat-app
  labels:
    app: achat-app
spec:
  replicas: 1
  selector:
    matchLabels:
      app: achat-app
  template:
    metadata:
      labels:
        app: achat-app
    spec:
      containers:
      - name: achat-app
        image: ${var.docker_image}
        ports:
        - containerPort: 8089
          name: http
        env:
        - name: SPRING_DATASOURCE_URL
          value: "jdbc:h2:mem:achatdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
        - name: SPRING_DATASOURCE_DRIVER_CLASS_NAME
          value: "org.h2.Driver"
        - name: SPRING_DATASOURCE_USERNAME
          value: "sa"
        - name: SPRING_DATASOURCE_PASSWORD
          value: ""
        - name: SPRING_JPA_HIBERNATE_DDL_AUTO
          value: "create-drop"
        - name: SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT
          value: "org.hibernate.dialect.H2Dialect"
        livenessProbe:
          httpGet:
            path: /SpringMVC/actuator/health
            port: 8089
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /SpringMVC/actuator/health
            port: 8089
          initialDelaySeconds: 30
          periodSeconds: 5
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
DEPLOY_EOF
    
    # Create service manifest
    echo "Creating Kubernetes service manifest..."
    cat > /root/service.yaml <<'SERVICE_EOF'
apiVersion: v1
kind: Service
metadata:
  name: achat-app-service
  namespace: achat-app
  labels:
    app: achat-app
spec:
  type: NodePort
  selector:
    app: achat-app
  ports:
  - port: 8080
    targetPort: 8089
    nodePort: 30080
    protocol: TCP
    name: http
SERVICE_EOF
    
    # Deploy application
    echo "Deploying application to Kubernetes..."
    k3s kubectl apply -f /root/deployment.yaml
    k3s kubectl apply -f /root/service.yaml
    
    # Set up nginx as reverse proxy (simulates LoadBalancer)
    echo "Installing nginx as reverse proxy..."
    yum install -y nginx
    
    # Configure nginx
    cat > /etc/nginx/conf.d/k8s-proxy.conf <<'NGINX_EOF'
server {
    listen 80;
    server_name _;
    
    location / {
        proxy_pass http://localhost:30080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
NGINX_EOF
    
    # Start nginx
    systemctl enable nginx
    systemctl start nginx
    
    # Wait for deployment to be ready
    echo "Waiting for deployment to be ready..."
    k3s kubectl wait --for=condition=available --timeout=300s deployment/achat-app -n achat-app || true
    
    # Show deployment status
    echo "=========================================="
    echo "Deployment Status:"
    echo "=========================================="
    k3s kubectl get all -n achat-app
    
    echo "=========================================="
    echo "k3s Kubernetes Cluster Setup Complete!"
    echo "=========================================="
    echo "Application URL: http://$PUBLIC_IP/SpringMVC/"
    echo "Health Check: http://$PUBLIC_IP/SpringMVC/actuator/health"
    echo "Swagger UI: http://$PUBLIC_IP/SpringMVC/swagger-ui/"
    echo "=========================================="
  EOF
}

