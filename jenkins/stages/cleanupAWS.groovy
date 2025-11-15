// BOM Fix
// jenkins/stages/cleanupAWS.groovy
def call() {
    stage('Cleanup AWS Infrastructure') {
        if (params.DEPLOYMENT_MODE != 'CLEANUP_AND_DEPLOY' || !fileExists('terraform/main.tf')) {
            echo 'Skipping AWS cleanup: not in CLEANUP_AND_DEPLOY mode or terraform config missing'
            return
        }

                echo '========================================='
                echo 'Stage 9.5: Cleaning Up Old AWS Infrastructure'
                echo '========================================='
                echo 'WARNING: This will destroy ALL AWS resources'
                echo 'Method: Terraform Destroy (handles all resources including EKS)'

            withCredentials([file(credentialsId: "${AWS_CREDENTIAL_ID}", variable: 'AWS_CREDENTIALS_FILE')]) {
                dir(TERRAFORM_DIR) {
                    sh '''
                        echo "Setting up AWS credentials..."
                        mkdir -p ~/.aws
                        cp $AWS_CREDENTIALS_FILE ~/.aws/credentials
                        chmod 600 ~/.aws/credentials
                        
                        echo "======================================"
                        echo "Using Terraform Destroy for Clean Removal"
                        echo "======================================"
                        echo ""
                        
                        # Setup AWS credentials for AWS CLI
                        export AWS_ACCESS_KEY_ID=\$(grep aws_access_key_id "\$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                        export AWS_SECRET_ACCESS_KEY=\$(grep aws_secret_access_key "\$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                        export AWS_SESSION_TOKEN=\$(grep aws_session_token "\$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                        export AWS_DEFAULT_REGION=\${AWS_REGION}
                        
                        # Check if Terraform state exists
                        if [ -f "terraform.tfstate" ]; then
                            echo "Terraform state found. Using terraform destroy..."
                            echo ""
                            
                            # Initialize Terraform
                            terraform init -upgrade
                            
                            # Destroy all resources
                            echo "Destroying all infrastructure (this may take 10-15 minutes for EKS)..."
                            terraform destroy -auto-approve \
                                -var="docker_image=${DOCKER_REGISTRY}/${DOCKER_HUB_USER}/${DOCKER_IMAGE_NAME}:${BUILD_NUMBER}" || {
                                echo "WARNING: Terraform destroy had errors, but continuing with manual cleanup..."
                            }
                            
                            echo ""
                            echo "Terraform destroy completed"
                        else
                            echo "No Terraform state found. Will use manual cleanup only."
                        fi
                        
                        echo ""
                        echo "======================================"
                        echo "Manual VPC Cleanup (ensures ALL VPCs are deleted)"
                        echo "======================================"
                        echo ""
                        
                        # Find ALL VPCs with achat-app-vpc name (by tag)
                        echo "Step 1: Finding ALL VPCs named 'achat-app-vpc'..."
                        VPC_IDS=\$(aws ec2 describe-vpcs \\
                            --region \${AWS_REGION} \\
                            --filters "Name=tag:Name,Values=achat-app-vpc" \\
                            --query "Vpcs[].VpcId" \\
                            --output text 2>/dev/null || echo "")
                        
                        # Also check for VPCs with achat-app in any tag
                        if [ -z "\$VPC_IDS" ] || [ "\$VPC_IDS" = "None" ]; then
                            echo "  No VPCs found by Name tag, checking for any achat-app VPCs..."
                            VPC_IDS=\$(aws ec2 describe-vpcs \\
                                --region \${AWS_REGION} \\
                                --filters "Name=tag:Project,Values=achat-app" \\
                                --query "Vpcs[].VpcId" \\
                                --output text 2>/dev/null || echo "")
                        fi
                        
                        # Also check for VPCs with CIDR 10.0.0.0/16 (our default)
                        if [ -z "\$VPC_IDS" ] || [ "\$VPC_IDS" = "None" ]; then
                            echo "  No VPCs found by tags, checking for VPCs with CIDR 10.0.0.0/16..."
                            ALL_VPCS=\$(aws ec2 describe-vpcs \\
                                --region \${AWS_REGION} \\
                                --filters "Name=cidr-block,Values=10.0.0.0/16" \\
                                --query "Vpcs[].VpcId" \\
                                --output text 2>/dev/null || echo "")
                            # Only include if we have 5 or fewer VPCs (likely our test VPCs)
                            VPC_COUNT=\$(echo "\$ALL_VPCS" | wc -w)
                            if [ "\$VPC_COUNT" -le 5 ]; then
                                VPC_IDS="\$ALL_VPCS"
                            fi
                        fi
                        
                        if [ -z "\$VPC_IDS" ] || [ "\$VPC_IDS" = "None" ]; then
                            echo "  No achat-app VPCs found. Listing all VPCs to check..."
                            ALL_VPCS=\$(aws ec2 describe-vpcs \\
                                --region \${AWS_REGION} \\
                                --query "Vpcs[].VpcId" \\
                                --output text 2>/dev/null || echo "")
                            echo "  Total VPCs in region: \$(echo \$ALL_VPCS | wc -w)"
                            echo "  VPC IDs: \$ALL_VPCS"
                            echo ""
                            echo "  Since we're at VPC limit, will try to delete any VPCs that look like achat-app VPCs"
                            # Try to delete VPCs that might be ours (check by checking if they have our subnets or security groups)
                            for vpc in \$ALL_VPCS; do
                                # Check if VPC has subnets with our naming pattern
                                HAS_ACHAT_SUBNETS=\$(aws ec2 describe-subnets \\
                                    --region \${AWS_REGION} \\
                                    --filters "Name=vpc-id,Values=\$vpc" "Name=tag:Name,Values=achat-app-*" \\
                                    --query "Subnets[0].SubnetId" \\
                                    --output text 2>/dev/null || echo "")
                                if [ -n "\$HAS_ACHAT_SUBNETS" ] && [ "\$HAS_ACHAT_SUBNETS" != "None" ]; then
                                    echo "    Found VPC \$vpc with achat-app subnets, adding to deletion list"
                                    VPC_IDS="\$VPC_IDS \$vpc"
                                fi
                            done
                        fi
                        
                        if [ -z "\$VPC_IDS" ] || [ "\$VPC_IDS" = "None" ]; then
                            echo "  No achat-app VPCs found to delete"
                        else
                            echo "  Found VPCs to delete: \$VPC_IDS"
                            echo ""
                            
                            for vpc_id in \$VPC_IDS; do
                                if [ -z "\$vpc_id" ] || [ "\$vpc_id" = "None" ]; then
                                    continue
                                fi
                                
                                echo "=========================================="
                                echo "Processing VPC: \$vpc_id"
                                echo "=========================================="
                                
                                # Delete all resources in VPC (same as manual cleanup)
                                # 1. Terminate EC2 instances
                                echo "  1. Terminating EC2 instances..."
                                INSTANCE_IDS=\$(aws ec2 describe-instances \\
                                    --region \${AWS_REGION} \\
                                    --filters "Name=vpc-id,Values=\$vpc_id" "Name=instance-state-name,Values=running,stopped,stopping" \\
                                    --query "Reservations[].Instances[].InstanceId" \\
                                    --output text 2>/dev/null || echo "")
                                if [ -n "\$INSTANCE_IDS" ] && [ "\$INSTANCE_IDS" != "None" ]; then
                                    echo "    Found instances: \$INSTANCE_IDS"
                                    aws ec2 terminate-instances --region \${AWS_REGION} --instance-ids \$INSTANCE_IDS 2>/dev/null || true
                                    echo "    Waiting for instances to terminate..."
                                    aws ec2 wait instance-terminated --region \${AWS_REGION} --instance-ids \$INSTANCE_IDS 2>/dev/null || sleep 30
                                fi
                                
                                # 2. Delete NAT Gateways
                                echo "  2. Deleting NAT Gateways..."
                                NAT_GW_IDS=\$(aws ec2 describe-nat-gateways \\
                                    --region \${AWS_REGION} \\
                                    --filter "Name=vpc-id,Values=\$vpc_id" "Name=state,Values=available,pending" \\
                                    --query "NatGateways[].NatGatewayId" \\
                                    --output text 2>/dev/null || echo "")
                                if [ -n "\$NAT_GW_IDS" ] && [ "\$NAT_GW_IDS" != "None" ]; then
                                    for nat_id in \$NAT_GW_IDS; do
                                        echo "    Deleting NAT Gateway: \$nat_id"
                                        aws ec2 delete-nat-gateway --region \${AWS_REGION} --nat-gateway-id \$nat_id 2>/dev/null || true
                                    done
                                    echo "    Waiting for NAT Gateways to delete..."
                                    sleep 60
                                fi
                                
                                # 3. Detach and delete Internet Gateways
                                echo "  3. Deleting Internet Gateways..."
                                IGW_IDS=\$(aws ec2 describe-internet-gateways \\
                                    --region \${AWS_REGION} \\
                                    --filters "Name=attachment.vpc-id,Values=\$vpc_id" \\
                                    --query "InternetGateways[].InternetGatewayId" \\
                                    --output text 2>/dev/null || echo "")
                                if [ -n "\$IGW_IDS" ] && [ "\$IGW_IDS" != "None" ]; then
                                    for igw_id in \$IGW_IDS; do
                                        echo "    Detaching and deleting IGW: \$igw_id"
                                        aws ec2 detach-internet-gateway --region \${AWS_REGION} --internet-gateway-id \$igw_id --vpc-id \$vpc_id 2>/dev/null || true
                                        aws ec2 delete-internet-gateway --region \${AWS_REGION} --internet-gateway-id \$igw_id 2>/dev/null || true
                                    done
                                fi
                                
                                # 4. Delete Subnets
                                echo "  4. Deleting Subnets..."
                                SUBNET_IDS=\$(aws ec2 describe-subnets \\
                                    --region \${AWS_REGION} \\
                                    --filters "Name=vpc-id,Values=\$vpc_id" \\
                                    --query "Subnets[].SubnetId" \\
                                    --output text 2>/dev/null || echo "")
                                if [ -n "\$SUBNET_IDS" ] && [ "\$SUBNET_IDS" != "None" ]; then
                                    for subnet_id in \$SUBNET_IDS; do
                                        echo "    Deleting subnet: \$subnet_id"
                                        aws ec2 delete-subnet --region \${AWS_REGION} --subnet-id \$subnet_id 2>/dev/null || true
                                    done
                                fi
                                
                                # 5. Delete Route Tables (except main)
                                echo "  5. Deleting Route Tables..."
                                RTB_IDS=\$(aws ec2 describe-route-tables \\
                                    --region \${AWS_REGION} \\
                                    --filters "Name=vpc-id,Values=\$vpc_id" \\
                                    --query 'RouteTables[?Associations[0].Main!=`true`].RouteTableId' \\
                                    --output text 2>/dev/null || echo "")
                                if [ -n "\$RTB_IDS" ] && [ "\$RTB_IDS" != "None" ]; then
                                    for rtb_id in \$RTB_IDS; do
                                        echo "    Deleting route table: \$rtb_id"
                                        aws ec2 delete-route-table --region \${AWS_REGION} --route-table-id \$rtb_id 2>/dev/null || true
                                    done
                                fi
                                
                                # 6. Delete Security Groups (except default)
                                echo "  6. Deleting Security Groups..."
                                SG_IDS=\$(aws ec2 describe-security-groups \\
                                    --region \${AWS_REGION} \\
                                    --filters "Name=vpc-id,Values=\$vpc_id" \\
                                    --query 'SecurityGroups[?GroupName!=`default`].GroupId' \\
                                    --output text 2>/dev/null || echo "")
                                if [ -n "\$SG_IDS" ] && [ "\$SG_IDS" != "None" ]; then
                                    for sg_id in \$SG_IDS; do
                                        echo "    Deleting security group: \$sg_id"
                                        aws ec2 delete-security-group --region \${AWS_REGION} --group-id \$sg_id 2>/dev/null || true
                                    done
                                fi
                                
                                # 7. Delete the VPC
                                echo "  7. Deleting VPC: \$vpc_id"
                                if aws ec2 delete-vpc --region \${AWS_REGION} --vpc-id \$vpc_id 2>/dev/null; then
                                    echo "    VPC \$vpc_id deleted successfully"
                                else
                                    echo "    Failed to delete VPC \$vpc_id (may have dependencies)"
                                fi
                                
                                echo "=========================================="
                            done
                        fi
                        
                        echo ""
                        echo "Verifying VPC deletion (AWS may take a few minutes to fully delete VPCs)..."
                        
                        # Wait for VPCs to be fully deleted to avoid VPC limit issues
                        VPC_WAIT_COUNT=0
                        MAX_VPC_WAIT=30  # 30 * 30 seconds = 15 minutes max
                        
                        # Check for VPCs with achat-app-vpc name
                        while [ \$VPC_WAIT_COUNT -lt \$MAX_VPC_WAIT ]; do
                            REMAINING_VPCS=\$(aws ec2 describe-vpcs \\
                                --region \${AWS_REGION} \\
                                --filters "Name=tag:Name,Values=achat-app-vpc" \\
                                --query "Vpcs[].VpcId" \\
                                --output text 2>/dev/null || echo "")
                            
                            if [ -z "\$REMAINING_VPCS" ] || [ "\$REMAINING_VPCS" = "None" ]; then
                                echo "  All achat-app VPCs confirmed deleted"
                                break
                            else
                                echo "  Waiting for VPCs to delete: \$REMAINING_VPCS (attempt \$VPC_WAIT_COUNT/\$MAX_VPC_WAIT, waiting 30s...)"
                                sleep 30
                                VPC_WAIT_COUNT=\$((VPC_WAIT_COUNT + 1))
                            fi
                        done
                        
                        if [ \$VPC_WAIT_COUNT -ge \$MAX_VPC_WAIT ]; then
                            echo "  WARNING: Some VPCs may still be deleting after 15 minutes"
                            echo "  Current VPCs: \$REMAINING_VPCS"
                            echo "  You may need to wait longer or manually delete them"
                        fi
                        
                        echo ""
                        echo "VPC deletion verification completed"
                        
                            # Function to safely delete resources
                            safe_delete() {
                            eval "$1" 2>/dev/null || echo "  (already deleted or not found)"
                            }
                        
                            # Step 0: Delete EKS Clusters first (if any exist)
                            echo ""
                            echo "Step 0: Deleting EKS Clusters..."
                            EKS_CLUSTERS=$(aws eks list-clusters \
                            --region ${AWS_REGION} \
                            --query 'clusters[?contains(@, `achat-app`) == `true`]' \
                            --output text 2>/dev/null || echo "")
                        
                            if [ -n "$EKS_CLUSTERS" ]; then
                            for cluster_name in $EKS_CLUSTERS; do
                                echo "  Found EKS cluster: $cluster_name"
                                
                                # Check cluster status
                                CLUSTER_STATUS=$(aws eks describe-cluster \
                                    --region ${AWS_REGION} \
                                    --name $cluster_name \
                                    --query 'cluster.status' \
                                    --output text 2>/dev/null || echo "NOT_FOUND")
                                echo "    Current status: $CLUSTER_STATUS"
                                
                                if [ "$CLUSTER_STATUS" = "DELETING" ]; then
                                    echo "    Cluster is already being deleted, waiting..."
                                    aws eks wait cluster-deleted --region ${AWS_REGION} --name $cluster_name 2>&1 || echo "      Wait completed or timed out"
                                    continue
                                fi
                                
                                if [ "$CLUSTER_STATUS" = "NOT_FOUND" ]; then
                                    echo "    Cluster not found, skipping"
                                    continue
                                fi
                                
                                # Delete node groups first
                                echo "    Checking for node groups..."
                                NODE_GROUPS=$(aws eks list-nodegroups \
                                    --region ${AWS_REGION} \
                                    --cluster-name $cluster_name \
                                    --query 'nodegroups[]' \
                                    --output text 2>/dev/null || echo "")
                                
                                if [ -n "$NODE_GROUPS" ] && [ "$NODE_GROUPS" != "None" ]; then
                                    echo "    Found node groups: $NODE_GROUPS"
                                    for node_group in $NODE_GROUPS; do
                                        echo "      Deleting node group: $node_group"
                                        aws eks delete-nodegroup \
                                            --region ${AWS_REGION} \
                                            --cluster-name $cluster_name \
                                            --nodegroup-name $node_group 2>&1 | grep -v "ResourceNotFoundException" || true
                                    done
                                    
                                    # Wait for node groups to delete
                                    echo "      Waiting for node groups to delete (this takes 3-5 minutes)..."
                                    for node_group in $NODE_GROUPS; do
                                        echo "        Waiting for node group: $node_group"
                                        aws eks wait nodegroup-deleted \
                                            --region ${AWS_REGION} \
                                            --cluster-name $cluster_name \
                                            --nodegroup-name $node_group 2>&1 | head -5 || echo "        Node group deleted or not found"
                                    done
                                    echo "      All node groups deleted"
                                else
                                    echo "    No node groups found or already deleted"
                                fi
                                
                                # Delete the cluster
                                echo "    Deleting EKS cluster: $cluster_name"
                                DELETE_OUTPUT=$(aws eks delete-cluster --region ${AWS_REGION} --name $cluster_name 2>&1)
                                if echo "$DELETE_OUTPUT" | grep -q "ResourceNotFoundException"; then
                                    echo "      Cluster already deleted"
                                elif echo "$DELETE_OUTPUT" | grep -q "error"; then
                                    echo "      Error deleting cluster: $DELETE_OUTPUT"
                                else
                                    echo "      Delete command sent successfully"
                                    
                                    # Wait for cluster deletion with timeout
                                    echo "      Waiting for cluster to delete (this takes 5-10 minutes)..."
                                    WAIT_START=$(date +%s)
                                    TIMEOUT=900  # 15 minutes timeout
                                    
                                    while true; do
                                        CURRENT_STATUS=$(aws eks describe-cluster \
                                            --region ${AWS_REGION} \
                                            --name $cluster_name \
                                            --query 'cluster.status' \
                                            --output text 2>/dev/null || echo "DELETED")
                                        
                                        if [ "$CURRENT_STATUS" = "DELETED" ] || echo "$CURRENT_STATUS" | grep -q "ResourceNotFoundException"; then
                                            echo "      Cluster deleted successfully"
                                            break
                                        fi
                                        
                                        ELAPSED=$(($(date +%s) - WAIT_START))
                                        if [ $ELAPSED -gt $TIMEOUT ]; then
                                            echo "      Timeout waiting for cluster deletion (${TIMEOUT}s)"
                                            echo "      Current status: $CURRENT_STATUS"
                                            break
                                        fi
                                        
                                        echo "        Status: $CURRENT_STATUS (${ELAPSED}s elapsed)"
                                        sleep 30
                                    done
                                fi
                                
                                echo "    EKS cluster $cluster_name processed"
                            done
                            else
                            echo "  No EKS clusters found"
                            fi
                        
                            # Step 0.5: Delete RDS Instances FIRST (before DB subnet groups)
                            echo ""
                            echo "Step 0.5: Deleting RDS Instances..."
                        
                            # Find all achat-app RDS instances
                            echo "  Checking for achat-app RDS instances..."
                            RDS_INSTANCES=$(aws rds describe-db-instances \
                            --region ${AWS_REGION} \
                            --query 'DBInstances[?contains(DBInstanceIdentifier, `achat-app`) == `true`].DBInstanceIdentifier' \
                            --output text 2>/dev/null || echo "")
                        
                            if [ -n "$RDS_INSTANCES" ]; then
                            echo "    Found RDS instances: $RDS_INSTANCES"
                            
                            # Delete each RDS instance
                            for db_id in $RDS_INSTANCES; do
                                echo "    Deleting RDS instance: $db_id"
                                DELETE_RDS_OUTPUT=$(aws rds delete-db-instance \
                                    --region ${AWS_REGION} \
                                    --db-instance-identifier $db_id \
                                    --skip-final-snapshot 2>&1)
                                
                                if echo "$DELETE_RDS_OUTPUT" | grep -q "DBInstanceNotFound"; then
                                    echo "      RDS instance already deleted"
                                elif echo "$DELETE_RDS_OUTPUT" | grep -qi "error"; then
                                    echo "      Error deleting RDS instance: $DELETE_RDS_OUTPUT"
                                else
                                    echo "      Delete command sent successfully"
                                fi
                            done
                            
                            # Wait for RDS instances to be fully deleted
                            echo "    Waiting for RDS instances to delete (this may take 5-10 minutes)..."
                            for db_id in $RDS_INSTANCES; do
                                echo "      Waiting for RDS instance: $db_id"
                                WAIT_COUNT=0
                                MAX_WAIT=30  # 30 * 30 seconds = 15 minutes max
                                
                                while [ $WAIT_COUNT -lt $MAX_WAIT ]; do
                                    # Check if instance exists
                                    DB_CHECK=$(aws rds describe-db-instances \
                                        --region ${AWS_REGION} \
                                        --db-instance-identifier $db_id 2>&1 || true)
                                    
                                    if echo "$DB_CHECK" | grep -q "DBInstanceNotFound"; then
                                        echo "        RDS instance $db_id fully deleted (not found)"
                                        break
                                    fi
                                    
                                    # Get status if instance still exists
                                    DB_STATUS=$(echo "$DB_CHECK" | grep -o '"DBInstanceStatus": "[^"]*"' | cut -d'"' -f4 || echo "unknown")
                                    
                                    if [ "$DB_STATUS" = "unknown" ] || [ -z "$DB_STATUS" ]; then
                                        echo "        RDS instance $db_id fully deleted (no status)"
                                        break
                                    else
                                        echo "        Status: $DB_STATUS (attempt $WAIT_COUNT/$MAX_WAIT, waiting 30s...)"
                                        sleep 30
                                        WAIT_COUNT=$((WAIT_COUNT + 1))
                                    fi
                                done
                                
                                if [ $WAIT_COUNT -ge $MAX_WAIT ]; then
                                    echo "        WARNING: Timeout waiting for $db_id deletion, but continuing..."
                                fi
                            done
                            echo "    All RDS instances processed"
                            else
                            echo "    No RDS instances found"
                            fi
                        
                            # Step 0.6: Delete DB Subnet Groups (AFTER RDS instances are deleted)
                            echo ""
                            echo "Step 0.6: Deleting DB Subnet Groups..."
                        
                            # Verify no RDS instances exist before deleting subnet groups
                            echo "  Double-checking that all RDS instances are deleted..."
                            REMAINING_RDS=$(aws rds describe-db-instances \
                            --region ${AWS_REGION} \
                            --query 'DBInstances[?contains(DBInstanceIdentifier, `achat-app`) == `true`].DBInstanceIdentifier' \
                            --output text 2>/dev/null || echo "")
                        
                            if [ -n "$REMAINING_RDS" ]; then
                            echo "    WARNING: Found remaining RDS instances: $REMAINING_RDS"
                            echo "    Waiting additional time for RDS deletion..."
                            
                            EXTRA_WAIT=0
                            while [ $EXTRA_WAIT -lt 10 ] && [ -n "$REMAINING_RDS" ]; do
                                echo "      Extra wait attempt $EXTRA_WAIT/10 (60s)..."
                                sleep 60
                                REMAINING_RDS=$(aws rds describe-db-instances \
                                    --region ${AWS_REGION} \
                                    --query 'DBInstances[?contains(DBInstanceIdentifier, `achat-app`) == `true`].DBInstanceIdentifier' \
                                    --output text 2>/dev/null || echo "")
                                EXTRA_WAIT=$((EXTRA_WAIT + 1))
                            done
                            
                            if [ -n "$REMAINING_RDS" ]; then
                                echo "    ERROR: RDS instances still exist after extended wait: $REMAINING_RDS"
                                echo "    Cannot safely delete DB subnet groups. Skipping subnet group deletion."
                                echo "    Please manually delete these RDS instances and subnet groups later."
                            else
                                echo "    All RDS instances confirmed deleted"
                            fi
                            else
                            echo "    No RDS instances found - safe to delete subnet groups"
                            fi
                        
                            # Only proceed with subnet group deletion if no RDS instances remain
                            if [ -z "$REMAINING_RDS" ]; then
                            # Delete by specific name first (Terraform-managed)
                            echo "  Checking for Terraform-managed DB subnet group: achat-app-db-subnet-group"
                            DB_SG_CHECK=$(aws rds describe-db-subnet-groups \
                            --region ${AWS_REGION} \
                            --db-subnet-group-name achat-app-db-subnet-group \
                            --query 'DBSubnetGroups[0].DBSubnetGroupName' \
                            --output text 2>/dev/null || echo "NOT_FOUND")
                        
                            if [ "$DB_SG_CHECK" != "NOT_FOUND" ] && [ -n "$DB_SG_CHECK" ]; then
                            echo "    Found DB subnet group: $DB_SG_CHECK"
                            DELETE_SG_OUTPUT=$(aws rds delete-db-subnet-group \
                                --region ${AWS_REGION} \
                                --db-subnet-group-name achat-app-db-subnet-group 2>&1)
                            
                            if echo "$DELETE_SG_OUTPUT" | grep -q "DBSubnetGroupNotFoundFault"; then
                                echo "      DB subnet group already deleted"
                            elif echo "$DELETE_SG_OUTPUT" | grep -qi "error"; then
                                echo "      Error deleting DB subnet group: $DELETE_SG_OUTPUT"
                            else
                                echo "      DB subnet group deleted successfully"
                            fi
                            else
                            echo "    DB subnet group not found (already deleted)"
                            fi
                        
                            # Also check for any other achat-app related DB subnet groups
                            echo "  Checking for other achat-app DB subnet groups..."
                            ALL_DB_SUBNET_GROUPS=$(aws rds describe-db-subnet-groups \
                            --region ${AWS_REGION} \
                            --query 'DBSubnetGroups[?contains(DBSubnetGroupName, `achat-app`) == `true`].DBSubnetGroupName' \
                            --output text 2>/dev/null || echo "")
                        
                            if [ -n "$ALL_DB_SUBNET_GROUPS" ]; then
                            for db_sg_name in $ALL_DB_SUBNET_GROUPS; do
                                echo "    Found additional DB subnet group: $db_sg_name"
                                aws rds delete-db-subnet-group \
                                    --region ${AWS_REGION} \
                                    --db-subnet-group-name $db_sg_name 2>&1 | grep -v "DBSubnetGroupNotFoundFault" || true
                                echo "      Processed DB subnet group: $db_sg_name"
                            done
                            else
                            echo "    No additional DB subnet groups found"
                            fi
                        
                            echo "  DB subnet group cleanup completed"
                            else
                            echo "  Skipping DB subnet group deletion due to remaining RDS instances"
                            fi
                        
                        # Note: VPC cleanup is now handled above in the "Manual VPC Cleanup" section
                        # This ensures VPCs are deleted regardless of whether terraform.tfstate exists
                        
                        echo ""
                        echo "======================================"
                        echo "Cleanup completed successfully"
                        echo "======================================"
                    '''
                }
            }

                echo 'AWS infrastructure cleanup completed'
                echo 'Ready for fresh deployment'
    }
}
return this


