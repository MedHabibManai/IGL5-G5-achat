# Terraform AWS Provider Configuration
# This file configures the AWS provider and Terraform backend

terraform {
  required_version = ">= 1.0"
  
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
  
  # Backend configuration for state management
  # For production, use S3 backend. For sandbox, local is fine.
  backend "local" {
    path = "terraform.tfstate"
  }
  
  # Uncomment below for S3 backend (production)
  # backend "s3" {
  #   bucket         = "your-terraform-state-bucket"
  #   key            = "achat-app/terraform.tfstate"
  #   region         = "us-east-1"
  #   encrypt        = true
  #   dynamodb_table = "terraform-state-lock"
  # }
}

# AWS Provider
provider "aws" {
  region = var.aws_region
  
  # Credentials are provided via environment variables or AWS CLI config
  # AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, AWS_SESSION_TOKEN
  
  default_tags {
    tags = {
      Project     = var.project_name
      Environment = var.environment
      ManagedBy   = "Terraform"
      Owner       = "DevOps-Team"
    }
  }
}

