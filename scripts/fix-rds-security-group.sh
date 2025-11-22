#!/bin/bash
# Fix RDS Security Group to Allow EKS Nodes
# This script manually updates the RDS security group to allow MySQL traffic from EKS nodes

echo "========================================="
echo "Fixing RDS Security Group for EKS"
echo "========================================="
echo ""

# Set AWS region
export AWS_DEFAULT_REGION=us-east-1

# Get VPC ID from Terraform output
echo "Getting VPC ID from Terraform..."
VPC_ID=$(cd terraform && terraform output -raw vpc_id 2>/dev/null)

if [ -z "$VPC_ID" ] || [ "$VPC_ID" = "" ]; then
    echo "ERROR: Could not get VPC ID from Terraform"
    echo "Make sure you're in the project root and Terraform has been applied"
    exit 1
fi

echo "VPC ID: $VPC_ID"
echo ""

# Get RDS Security Group ID
echo "Getting RDS Security Group ID..."
RDS_SG_ID=$(aws ec2 describe-security-groups \
    --region us-east-1 \
    --filters "Name=vpc-id,Values=$VPC_ID" "Name=group-name,Values=achat-app-rds-sg" \
    --query "SecurityGroups[0].GroupId" \
    --output text 2>/dev/null)

if [ -z "$RDS_SG_ID" ] || [ "$RDS_SG_ID" = "None" ]; then
    echo "ERROR: Could not find RDS security group"
    exit 1
fi

echo "RDS Security Group ID: $RDS_SG_ID"
echo ""

# Get EKS Nodes Security Group ID
echo "Getting EKS Nodes Security Group ID..."
EKS_NODES_SG_ID=$(aws ec2 describe-security-groups \
    --region us-east-1 \
    --filters "Name=vpc-id,Values=$VPC_ID" "Name=group-name,Values=achat-app-eks-nodes-sg" \
    --query "SecurityGroups[0].GroupId" \
    --output text 2>/dev/null)

if [ -z "$EKS_NODES_SG_ID" ] || [ "$EKS_NODES_SG_ID" = "None" ]; then
    echo "ERROR: Could not find EKS nodes security group"
    exit 1
fi

echo "EKS Nodes Security Group ID: $EKS_NODES_SG_ID"
echo ""

# Create JSON for security group rule
IP_PERMISSIONS_JSON="[{\"IpProtocol\": \"tcp\", \"FromPort\": 3306, \"ToPort\": 3306, \"UserIdGroupPairs\": [{\"GroupId\": \"$EKS_NODES_SG_ID\"}]}]"

echo "Adding security group rule..."
echo "Allowing MySQL (port 3306) from EKS nodes ($EKS_NODES_SG_ID) to RDS ($RDS_SG_ID)"
echo ""

# Add the security group rule
OUTPUT=$(aws ec2 authorize-security-group-ingress \
    --region us-east-1 \
    --group-id "$RDS_SG_ID" \
    --ip-permissions "$IP_PERMISSIONS_JSON" 2>&1)
EXIT_CODE=$?

if echo "$OUTPUT" | grep -qiE "(already exists|Duplicate)"; then
    echo "✓ Security group rule already exists (this is OK)"
elif [ $EXIT_CODE -eq 0 ]; then
    echo "✓ Security group rule added successfully!"
else
    echo "Result: $OUTPUT"
    echo "Note: If rule already exists, this is expected and safe to ignore"
fi

echo ""
echo "========================================="
echo "Next Steps:"
echo "1. Wait 30 seconds for the security group rule to propagate"
echo "2. Restart the backend pods to reconnect to RDS:"
echo "   kubectl rollout restart deployment/achat-app -n achat-app"
echo "3. Check pod status:"
echo "   kubectl get pods -n achat-app -l app=achat-app"
echo "========================================="




