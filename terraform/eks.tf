# EKS Cluster Configuration
# This file defines an AWS EKS (Elastic Kubernetes Service) cluster
# Uses existing LabRole for all IAM requirements (AWS Academy compatible)

# Note: This configuration is conditional based on var.deploy_mode
# Only created when deploy_mode = "eks"

# ============================================================================
# Security Group for EKS Cluster
# ============================================================================

resource "aws_security_group" "eks_cluster" {
  count       = var.deploy_mode == "eks" ? 1 : 0
  name        = "${var.project_name}-eks-cluster-sg"
  description = "Security group for ${var.project_name} EKS cluster"
  vpc_id      = aws_vpc.main.id

  # Allow all traffic within VPC (for node-to-node and node-to-control-plane communication)
  ingress {
    description = "Allow all from VPC"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = [aws_vpc.main.cidr_block]
  }

  # Allow HTTPS from anywhere (for kubectl access)
  ingress {
    description = "HTTPS from anywhere"
    from_port   = 443
    to_port     = 443
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
      Name = "${var.project_name}-eks-cluster-sg"
    }
  )
}

# ============================================================================
# Security Group for EKS Worker Nodes
# ============================================================================

resource "aws_security_group" "eks_nodes" {
  count       = var.deploy_mode == "eks" ? 1 : 0
  name        = "${var.project_name}-eks-nodes-sg"
  description = "Security group for ${var.project_name} EKS worker nodes"
  vpc_id      = aws_vpc.main.id

  # Allow all traffic from cluster security group
  ingress {
    description     = "Allow all from cluster"
    from_port       = 0
    to_port         = 0
    protocol        = "-1"
    security_groups = [aws_security_group.eks_cluster[0].id]
  }

  # Allow all traffic between nodes
  ingress {
    description = "Allow all from nodes"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    self        = true
  }

  # Allow HTTP from anywhere (for LoadBalancer services)
  ingress {
    description = "HTTP from anywhere"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Allow HTTPS from anywhere
  ingress {
    description = "HTTPS from anywhere"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Allow NodePort range from anywhere
  ingress {
    description = "NodePort range"
    from_port   = 30000
    to_port     = 32767
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
      Name = "${var.project_name}-eks-nodes-sg"
    }
  )
}

# ============================================================================
# EKS Cluster
# ============================================================================

resource "aws_eks_cluster" "main" {
  count    = var.deploy_mode == "eks" ? 1 : 0
  name     = "${var.project_name}-eks-cluster"
  role_arn = data.aws_iam_role.lab_role.arn
  version  = var.eks_cluster_version

  vpc_config {
    subnet_ids              = [aws_subnet.public.id, aws_subnet.private.id]
    security_group_ids      = [aws_security_group.eks_cluster[0].id]
    endpoint_public_access  = true
    endpoint_private_access = true
    public_access_cidrs     = ["0.0.0.0/0"]
  }

  # Enable control plane logging
  enabled_cluster_log_types = ["api", "audit", "authenticator", "controllerManager", "scheduler"]

  tags = merge(
    var.common_tags,
    {
      Name = "${var.project_name}-eks-cluster"
    }
  )

  depends_on = [
    aws_subnet.public,
    aws_subnet.private
  ]
}

# ============================================================================
# EKS Node Group
# ============================================================================

resource "aws_eks_node_group" "main" {
  count           = var.deploy_mode == "eks" ? 1 : 0
  cluster_name    = aws_eks_cluster.main[0].name
  node_group_name = "${var.project_name}-eks-node-group"
  node_role_arn   = data.aws_iam_role.lab_role.arn
  subnet_ids      = [aws_subnet.public.id, aws_subnet.private.id]

  scaling_config {
    desired_size = var.eks_node_desired_size
    max_size     = var.eks_node_max_size
    min_size     = var.eks_node_min_size
  }

  instance_types = [var.eks_node_instance_type]
  capacity_type  = "ON_DEMAND"
  disk_size      = 30

  # Use latest EKS optimized AMI
  ami_type = "AL2_x86_64"

  # Remote access configuration (optional - only if key_name is provided)
  dynamic "remote_access" {
    for_each = var.key_name != "" ? [1] : []
    content {
      ec2_ssh_key               = var.key_name
      source_security_group_ids = [aws_security_group.eks_nodes[0].id]
    }
  }

  tags = merge(
    var.common_tags,
    {
      Name = "${var.project_name}-eks-node-group"
    }
  )

  depends_on = [
    aws_eks_cluster.main
  ]

  lifecycle {
    create_before_destroy = true
    ignore_changes        = [scaling_config[0].desired_size]
  }
}

# ============================================================================
# EKS Add-ons
# ============================================================================

# VPC CNI Add-on (for pod networking)
resource "aws_eks_addon" "vpc_cni" {
  count        = var.deploy_mode == "eks" ? 1 : 0
  cluster_name = aws_eks_cluster.main[0].name
  addon_name   = "vpc-cni"

  depends_on = [
    aws_eks_node_group.main
  ]
}

# CoreDNS Add-on (for DNS resolution)
resource "aws_eks_addon" "coredns" {
  count        = var.deploy_mode == "eks" ? 1 : 0
  cluster_name = aws_eks_cluster.main[0].name
  addon_name   = "coredns"

  depends_on = [
    aws_eks_node_group.main
  ]
}

# kube-proxy Add-on (for service networking)
resource "aws_eks_addon" "kube_proxy" {
  count        = var.deploy_mode == "eks" ? 1 : 0
  cluster_name = aws_eks_cluster.main[0].name
  addon_name   = "kube-proxy"

  depends_on = [
    aws_eks_node_group.main
  ]
}

# ============================================================================
# Private Subnet for EKS Nodes (required for EKS best practices)
# ============================================================================

resource "aws_subnet" "private" {
  vpc_id                  = aws_vpc.main.id
  cidr_block              = "10.0.2.0/24"
  availability_zone       = data.aws_availability_zones.available.names[0]
  map_public_ip_on_launch = false

  tags = merge(
    var.common_tags,
    {
      Name                              = "${var.project_name}-private-subnet"
      "kubernetes.io/role/internal-elb" = "1"
      "kubernetes.io/cluster/${var.project_name}-eks-cluster" = "shared"
    }
  )
}

# Route table for private subnet
resource "aws_route_table" "private" {
  vpc_id = aws_vpc.main.id

  tags = merge(
    var.common_tags,
    {
      Name = "${var.project_name}-private-rt"
    }
  )
}

# Associate private subnet with private route table
resource "aws_route_table_association" "private" {
  subnet_id      = aws_subnet.private.id
  route_table_id = aws_route_table.private.id
}

# NAT Gateway for private subnet (allows nodes to pull images from internet)
resource "aws_eip" "nat" {
  count  = var.deploy_mode == "eks" ? 1 : 0
  domain = "vpc"

  tags = merge(
    var.common_tags,
    {
      Name = "${var.project_name}-nat-eip"
    }
  )
}

resource "aws_nat_gateway" "main" {
  count         = var.deploy_mode == "eks" ? 1 : 0
  allocation_id = aws_eip.nat[0].id
  subnet_id     = aws_subnet.public.id

  tags = merge(
    var.common_tags,
    {
      Name = "${var.project_name}-nat-gateway"
    }
  )

  depends_on = [aws_internet_gateway.main]
}

# Route for private subnet to use NAT gateway
resource "aws_route" "private_nat" {
  count                  = var.deploy_mode == "eks" ? 1 : 0
  route_table_id         = aws_route_table.private.id
  destination_cidr_block = "0.0.0.0/0"
  nat_gateway_id         = aws_nat_gateway.main[0].id
}

