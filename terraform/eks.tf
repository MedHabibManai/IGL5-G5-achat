# ============================================================================
# EKS Cluster Configuration
# ============================================================================

# EKS Cluster
resource "aws_eks_cluster" "main" {
  count    = var.create_eks ? 1 : 0
  name     = "${var.project_name}-eks-cluster"
  role_arn = data.aws_iam_role.lab_role.arn
  version  = "1.28"

  vpc_config {
    subnet_ids              = [aws_subnet.public.id, aws_subnet.private[0].id]
    endpoint_private_access = false
    endpoint_public_access  = true
    security_group_ids      = [aws_security_group.eks_cluster[0].id]
  }

  enabled_cluster_log_types = ["api", "audit", "authenticator"]

  tags = {
    Name        = "${var.project_name}-eks-cluster"
    Environment = var.environment
    Project     = var.project_name
  }

  depends_on = [
    aws_subnet.public,
    aws_subnet.private
  ]
}

# EKS Node Group
resource "aws_eks_node_group" "main" {
  count           = var.create_eks ? 1 : 0
  cluster_name    = aws_eks_cluster.main[0].name
  node_group_name = "${var.project_name}-node-group"
  node_role_arn   = data.aws_iam_role.lab_role.arn
  subnet_ids      = [aws_subnet.public.id, aws_subnet.private[0].id]

  scaling_config {
    desired_size = 2
    max_size     = 3
    min_size     = 1
  }

  instance_types = ["t3.small"]  # Cheapest option for production
  capacity_type  = "ON_DEMAND"   # Can change to SPOT for even cheaper
  disk_size      = 20

  update_config {
    max_unavailable = 1
  }

  tags = {
    Name        = "${var.project_name}-eks-nodes"
    Environment = var.environment
    Project     = var.project_name
  }

  depends_on = [
    aws_eks_cluster.main[0]
  ]
}

# Security Group for EKS Cluster
resource "aws_security_group" "eks_cluster" {
  count       = var.create_eks ? 1 : 0
  name        = "${var.project_name}-eks-cluster-sg"
  description = "Security group for EKS cluster"
  vpc_id      = aws_vpc.main.id

  # Allow inbound from nodes
  ingress {
    description = "Allow nodes to communicate with cluster API"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = [aws_vpc.main.cidr_block]
  }

  # Allow all outbound
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name        = "${var.project_name}-eks-cluster-sg"
    Environment = var.environment
  }
}

# Security Group for EKS Nodes
resource "aws_security_group" "eks_nodes" {
  count       = var.create_eks ? 1 : 0
  name        = "${var.project_name}-eks-nodes-sg"
  description = "Security group for EKS worker nodes"
  vpc_id      = aws_vpc.main.id

  # Allow nodes to communicate with each other
  ingress {
    description = "Allow nodes to communicate with each other"
    from_port   = 0
    to_port     = 65535
    protocol    = "tcp"
    self        = true
  }

  # Allow pods to communicate with cluster API
  ingress {
    description     = "Allow pods to communicate with cluster API"
    from_port       = 443
    to_port         = 443
    protocol        = "tcp"
    security_groups = var.create_eks ? [aws_security_group.eks_cluster[0].id] : []
  }

  # Allow NodePort services (30000-32767)
  ingress {
    description = "Allow NodePort services"
    from_port   = 30000
    to_port     = 32767
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Allow HTTP
  ingress {
    description = "Allow HTTP"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Allow HTTPS
  ingress {
    description = "Allow HTTPS"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Allow all outbound
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name                                           = "${var.project_name}-eks-nodes-sg"
    Environment                                    = var.environment
    "kubernetes.io/cluster/${var.project_name}-eks-cluster" = "owned"
  }
}

# Private Subnet for EKS (needed for multi-AZ)
resource "aws_subnet" "private" {
  count                   = var.create_eks ? 1 : 0
  vpc_id                  = aws_vpc.main.id
  cidr_block              = "10.0.2.0/24"
  availability_zone       = data.aws_availability_zones.available.names[1]
  map_public_ip_on_launch = false

  tags = {
    Name                                           = "${var.project_name}-private-subnet"
    Environment                                    = var.environment
    "kubernetes.io/cluster/${var.project_name}-eks-cluster" = "shared"
    "kubernetes.io/role/internal-elb"              = "1"
  }
}

# NAT Gateway for private subnet (needed for nodes to pull images)
resource "aws_eip" "nat" {
  count  = var.create_eks ? 1 : 0
  domain = "vpc"

  tags = {
    Name        = "${var.project_name}-nat-eip"
    Environment = var.environment
  }
}

resource "aws_nat_gateway" "main" {
  count         = var.create_eks ? 1 : 0
  allocation_id = aws_eip.nat[0].id
  subnet_id     = aws_subnet.public.id

  tags = {
    Name        = "${var.project_name}-nat-gateway"
    Environment = var.environment
  }

  depends_on = [aws_internet_gateway.main]
}

# Route Table for Private Subnet
resource "aws_route_table" "private" {
  count  = var.create_eks ? 1 : 0
  vpc_id = aws_vpc.main.id

  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.main[0].id
  }

  tags = {
    Name        = "${var.project_name}-private-rt"
    Environment = var.environment
  }
}

resource "aws_route_table_association" "private" {
  count          = var.create_eks ? 1 : 0
  subnet_id      = aws_subnet.private[0].id
  route_table_id = aws_route_table.private[0].id
}

# Update public subnet tags for EKS
resource "aws_ec2_tag" "public_subnet_eks" {
  count       = var.create_eks ? 1 : 0
  resource_id = aws_subnet.public.id
  key         = "kubernetes.io/cluster/${var.project_name}-eks-cluster"
  value       = "shared"
}

resource "aws_ec2_tag" "public_subnet_elb" {
  count       = var.create_eks ? 1 : 0
  resource_id = aws_subnet.public.id
  key         = "kubernetes.io/role/elb"
  value       = "1"
}

# Data source for availability zones
data "aws_availability_zones" "available" {
  state = "available"
}
