// BOM Fix
// jenkins/stages/preTerraformValidation.groovy
def call() {
    stage('Pre-Terraform Validation') {
        when {
        expression {
        return fileExists('terraform/main.tf') && params.DEPLOYMENT_MODE != 'REUSE_INFRASTRUCTURE'
        }
        echo '========================================='
        echo 'Stage 10.5: Pre-Terraform Validation'
        echo '========================================='
        echo 'Checking for resources that could cause conflicts...'
        withCredentials([file(credentialsId: "${AWS_CREDENTIAL_ID}", variable: 'AWS_CREDENTIALS_FILE')]) {
        dir(TERRAFORM_DIR) {
        sh '''
        echo "Setting up AWS credentials..."
        mkdir -p ~/.aws
        cp $AWS_CREDENTIALS_FILE ~/.aws/credentials
        chmod 600 ~/.aws/credentials
        AWS_REGION="us-east-1"
        echo ""
        echo "=========================================="
        echo "Pre-Check 1: Verifying EKS Cluster Status"
        echo "=========================================="
        CLUSTER_NAME="achat-app-eks-cluster"
        # Check if cluster exists
        CLUSTER_STATUS=$(aws eks describe-cluster \
        --region ${AWS_REGION} \
        --name ${CLUSTER_NAME} \
        --query 'cluster.status' \
        --output text 2>/dev/null || echo "NOT_FOUND")
        echo "  Cluster status: $CLUSTER_STATUS"
        if [ "$CLUSTER_STATUS" = "ACTIVE" ]; then
        echo "  ERROR: EKS cluster '${CLUSTER_NAME}' already exists!"
        echo "  This will cause Terraform to fail with 'cluster already exists' error."
        echo "  Please run CLEANUP_AND_DEPLOY mode to delete existing resources first."
        exit 1
        elif [ "$CLUSTER_STATUS" = "CREATING" ]; then
        echo "  ERROR: EKS cluster '${CLUSTER_NAME}' is currently CREATING!"
        echo "  Please wait for creation to complete or delete it manually."
        exit 1
        elif [ "$CLUSTER_STATUS" = "DELETING" ]; then
        echo "  WARNING: EKS cluster '${CLUSTER_NAME}' is currently DELETING."
        echo "  This is expected if cleanup just ran. Terraform Apply will wait for deletion."
        elif [ "$CLUSTER_STATUS" = "NOT_FOUND" ]; then
        echo "  EKS cluster does not exist - ready to create"
        else
        echo "  Unexpected cluster status: $CLUSTER_STATUS"
        fi
        echo ""
        echo "=========================================="
        echo "Pre-Check 2: Verifying DB Subnet Group"
        echo "=========================================="
        DB_SUBNET_GROUP_NAME="achat-app-db-subnet-group"
        # Check if DB subnet group exists
        DB_SG_EXISTS=$(aws rds describe-db-subnet-groups \
        --region ${AWS_REGION} \
        --db-subnet-group-name ${DB_SUBNET_GROUP_NAME} \
        --query 'DBSubnetGroups[0].DBSubnetGroupName' \
        --output text 2>/dev/null || echo "NOT_FOUND")
        if [ "$DB_SG_EXISTS" != "NOT_FOUND" ] && [ -n "$DB_SG_EXISTS" ]; then
        echo "  ERROR: DB subnet group '${DB_SUBNET_GROUP_NAME}' already exists!"
        echo "  This will cause Terraform to fail with 'DBSubnetGroupAlreadyExists' error."
        echo "  Please run CLEANUP_AND_DEPLOY mode to delete existing resources first."
        echo ""
        echo "  Or delete manually with:"
        echo "    aws rds delete-db-subnet-group --db-subnet-group-name ${DB_SUBNET_GROUP_NAME} --region ${AWS_REGION}"
        exit 1
        else
        echo "  DB subnet group does not exist - ready to create"
        fi
        echo ""
        echo "=========================================="
        echo "Pre-Check 3: Checking for RDS Instances"
        echo "=========================================="
        # Check for any achat-app RDS instances
        RDS_INSTANCES=$(aws rds describe-db-instances \
        --region ${AWS_REGION} \
        --query 'DBInstances[?contains(DBInstanceIdentifier, `achat-app`) == `true`].DBInstanceIdentifier' \
        --output text 2>/dev/null || echo "")
        if [ -n "$RDS_INSTANCES" ]; then
        echo "  WARNING: Found existing RDS instances: $RDS_INSTANCES"
        for rds_id in $RDS_INSTANCES; do
        RDS_STATUS=$(aws rds describe-db-instances \
        --region ${AWS_REGION} \
        --db-instance-identifier $rds_id \
        --query 'DBInstances[0].DBInstanceStatus' \
        --output text 2>/dev/null || echo "unknown")
        echo "    - $rds_id: $RDS_STATUS"
        done
        # Only fail if RDS is in a problematic state
        if echo "$RDS_STATUS" | grep -qE "available|creating|backing-up"; then
        echo "  ERROR: Active RDS instance(s) found!"
        echo "  Please run CLEANUP_AND_DEPLOY mode to delete existing resources first."
        exit 1
        fi
        else
        echo "  No RDS instances found - ready to create"
        fi
        echo ""
        echo "=========================================="
        echo "All Pre-Checks Passed!"
        echo "=========================================="
        echo "  - EKS cluster: Ready"
        echo "  - DB subnet group: Ready"
        echo "  - RDS instances: Ready"
        echo ""
        echo "Proceeding with Terraform Plan..."
        '''
        echo 'Pre-Terraform validation completed successfully'
        }
    }
}
return this