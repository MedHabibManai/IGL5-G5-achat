// BOM Fix
// jenkins/stages/refreshEC2.groovy
def call() {
    stage('Refresh EC2 Instance Only') {
        if (params.DEPLOYMENT_MODE != 'REUSE_INFRASTRUCTURE' || !fileExists('terraform/main.tf')) {
            echo 'Skipping EC2 refresh: not in REUSE_INFRASTRUCTURE mode or terraform config missing'
            return
        }

                echo '========================================='
                echo 'Stage 9.6: Refreshing EC2 Instance Only'
                echo '========================================='
                echo 'Mode: REUSE_INFRASTRUCTURE'
                echo 'This will keep VPC, RDS, and other resources'
                echo 'Only the EC2 instance will be recreated with new user-data'

            withCredentials([file(credentialsId: "${AWS_CREDENTIAL_ID}", variable: 'AWS_CREDENTIALS_FILE')]) {
                dir(TERRAFORM_DIR) {
                    sh '''
                        echo "Setting up AWS credentials..."
                        mkdir -p ~/.aws
                        cp $AWS_CREDENTIALS_FILE ~/.aws/credentials
                        chmod 600 ~/.aws/credentials
                        
                        echo "======================================"
                        echo "Importing existing infrastructure state..."
                        echo "======================================"
                        
                        # Initialize Terraform first
                        terraform init -input=false
                        
                        # Import existing resources (ignore errors if already in state)
                        echo "Importing VPC and network resources..."
                        VPC_ID=$(aws ec2 describe-vpcs --region ${AWS_REGION} --filters "Name=tag:Name,Values=achat-app-vpc" --query "Vpcs[0].VpcId" --output text 2>/dev/null || echo "")
                        
                        if [ -n "$VPC_ID" ] && [ "$VPC_ID" != "None" ]; then
                            echo "Found existing VPC: $VPC_ID"
                            terraform import -var="docker_image=${TF_VAR_docker_image}" aws_vpc.main $VPC_ID 2>/dev/null || echo "  (already in state)"
                            
                            # Import Internet Gateway
                            IGW_ID=$(aws ec2 describe-internet-gateways --region ${AWS_REGION} --filters "Name=attachment.vpc-id,Values=$VPC_ID" --query "InternetGateways[0].InternetGatewayId" --output text 2>/dev/null || echo "")
                            if [ -n "$IGW_ID" ] && [ "$IGW_ID" != "None" ]; then
                                echo "Importing IGW: $IGW_ID"
                                terraform import -var="docker_image=${TF_VAR_docker_image}" aws_internet_gateway.main $IGW_ID 2>/dev/null || echo "  (already in state)"
                            fi
                            
                            # Import public subnet
                            PUBLIC_SUBNET_ID=$(aws ec2 describe-subnets --region ${AWS_REGION} --filters "Name=vpc-id,Values=$VPC_ID" "Name=tag:Name,Values=achat-app-public-subnet" --query "Subnets[0].SubnetId" --output text 2>/dev/null || echo "")
                            if [ -n "$PUBLIC_SUBNET_ID" ] && [ "$PUBLIC_SUBNET_ID" != "None" ]; then
                                echo "Importing public subnet: $PUBLIC_SUBNET_ID"
                                terraform import -var="docker_image=${TF_VAR_docker_image}" aws_subnet.public $PUBLIC_SUBNET_ID 2>/dev/null || echo "  (already in state)"
                            fi
                            
                            # Import private subnet
                            PRIVATE_SUBNET_ID=$(aws ec2 describe-subnets --region ${AWS_REGION} --filters "Name=vpc-id,Values=$VPC_ID" "Name=tag:Name,Values=achat-app-private-subnet" --query "Subnets[0].SubnetId" --output text 2>/dev/null || echo "")
                            if [ -n "$PRIVATE_SUBNET_ID" ] && [ "$PRIVATE_SUBNET_ID" != "None" ]; then
                                echo "Importing private subnet: $PRIVATE_SUBNET_ID"
                                terraform import -var="docker_image=${TF_VAR_docker_image}" aws_subnet.private $PRIVATE_SUBNET_ID 2>/dev/null || echo "  (already in state)"
                            fi
                            
                            # Import route table
                            RTB_ID=$(aws ec2 describe-route-tables --region ${AWS_REGION} --filters "Name=vpc-id,Values=$VPC_ID" "Name=tag:Name,Values=achat-app-public-rt" --query "RouteTables[0].RouteTableId" --output text 2>/dev/null || echo "")
                            if [ -n "$RTB_ID" ] && [ "$RTB_ID" != "None" ]; then
                                echo "Importing route table: $RTB_ID"
                                terraform import -var="docker_image=${TF_VAR_docker_image}" aws_route_table.public $RTB_ID 2>/dev/null || echo "  (already in state)"
                                
                                # Import route table association
                                if [ -n "$PUBLIC_SUBNET_ID" ]; then
                                    RTB_ASSOC_ID=$(aws ec2 describe-route-tables --region ${AWS_REGION} --route-table-id $RTB_ID --query "RouteTables[0].Associations[?SubnetId=='\''$PUBLIC_SUBNET_ID'\''].RouteTableAssociationId | [0]" --output text 2>/dev/null || echo "")
                                    if [ -n "$RTB_ASSOC_ID" ] && [ "$RTB_ASSOC_ID" != "None" ]; then
                                        echo "Importing route table association: $RTB_ASSOC_ID"
                                        terraform import -var="docker_image=${TF_VAR_docker_image}" aws_route_table_association.public $RTB_ASSOC_ID 2>/dev/null || echo "  (already in state)"
                                    fi
                                fi
                            fi
                            
                            # Import security groups
                            APP_SG_ID=$(aws ec2 describe-security-groups --region ${AWS_REGION} --filters "Name=vpc-id,Values=$VPC_ID" "Name=group-name,Values=achat-app-app-sg" --query "SecurityGroups[0].GroupId" --output text 2>/dev/null || echo "")
                            if [ -n "$APP_SG_ID" ] && [ "$APP_SG_ID" != "None" ]; then
                                echo "Importing app security group: $APP_SG_ID"
                                terraform import -var="docker_image=${TF_VAR_docker_image}" aws_security_group.app $APP_SG_ID 2>/dev/null || echo "  (already in state)"
                            fi
                            
                            RDS_SG_ID=$(aws ec2 describe-security-groups --region ${AWS_REGION} --filters "Name=vpc-id,Values=$VPC_ID" "Name=group-name,Values=achat-app-rds-sg" --query "SecurityGroups[0].GroupId" --output text 2>/dev/null || echo "")
                            if [ -n "$RDS_SG_ID" ] && [ "$RDS_SG_ID" != "None" ]; then
                                echo "Importing RDS security group: $RDS_SG_ID"
                                terraform import -var="docker_image=${TF_VAR_docker_image}" aws_security_group.rds $RDS_SG_ID 2>/dev/null || echo "  (already in state)"
                            fi
                            
                            # Import RDS
                            DB_ID=$(aws rds describe-db-instances --region ${AWS_REGION} --query "DBInstances[?DBName=='achatdb'].DBInstanceIdentifier | [0]" --output text 2>/dev/null || echo "")
                            if [ -n "$DB_ID" ] && [ "$DB_ID" != "None" ]; then
                                echo "Importing RDS instance: $DB_ID"
                                terraform import -var="docker_image=${TF_VAR_docker_image}" aws_db_instance.mysql $DB_ID 2>/dev/null || echo "  (already in state)"
                            fi
                            
                            # Import DB subnet group
                            DB_SUBNET_GROUP=$(aws rds describe-db-subnet-groups --region ${AWS_REGION} --query "DBSubnetGroups[?DBSubnetGroupName=='achat-app-db-subnet-group'].DBSubnetGroupName | [0]" --output text 2>/dev/null || echo "")
                            if [ -n "$DB_SUBNET_GROUP" ] && [ "$DB_SUBNET_GROUP" != "None" ]; then
                                echo "Importing DB subnet group: $DB_SUBNET_GROUP"
                                terraform import -var="docker_image=${TF_VAR_docker_image}" aws_db_subnet_group.main $DB_SUBNET_GROUP 2>/dev/null || echo "  (already in state)"
                            fi
                            
                            # Import EKS security groups if they exist
                            EKS_CLUSTER_SG_ID=$(aws ec2 describe-security-groups --region ${AWS_REGION} --filters "Name=vpc-id,Values=$VPC_ID" "Name=group-name,Values=achat-app-eks-cluster-sg" --query "SecurityGroups[0].GroupId" --output text 2>/dev/null || echo "")
                            if [ -n "$EKS_CLUSTER_SG_ID" ] && [ "$EKS_CLUSTER_SG_ID" != "None" ]; then
                                echo "Importing EKS cluster security group: $EKS_CLUSTER_SG_ID"
                                terraform import -var="docker_image=${TF_VAR_docker_image}" aws_security_group.eks_cluster[0] $EKS_CLUSTER_SG_ID 2>/dev/null || echo "  (already in state or not in config)"
                            fi
                            
                            EKS_NODES_SG_ID=$(aws ec2 describe-security-groups --region ${AWS_REGION} --filters "Name=vpc-id,Values=$VPC_ID" "Name=group-name,Values=achat-app-eks-nodes-sg" --query "SecurityGroups[0].GroupId" --output text 2>/dev/null || echo "")
                            if [ -n "$EKS_NODES_SG_ID" ] && [ "$EKS_NODES_SG_ID" != "None" ]; then
                                echo "Importing EKS nodes security group: $EKS_NODES_SG_ID"
                                terraform import -var="docker_image=${TF_VAR_docker_image}" aws_security_group.eks_nodes[0] $EKS_NODES_SG_ID 2>/dev/null || echo "  (already in state or not in config)"
                            fi
                            
                            # Check for existing EIP and import it if it exists
                            EXISTING_EIP=$(aws ec2 describe-addresses --region ${AWS_REGION} --filters "Name=tag:Name,Values=achat-app-eip" --query "Addresses[0].AllocationId" --output text 2>/dev/null || echo "")
                            if [ -n "$EXISTING_EIP" ] && [ "$EXISTING_EIP" != "None" ]; then
                                echo "Found existing EIP: $EXISTING_EIP - importing..."
                                terraform import -var="docker_image=${TF_VAR_docker_image}" aws_eip.app $EXISTING_EIP 2>/dev/null || echo "  (already in state)"
                            fi
                        fi
                        
                        echo ""
                        echo "======================================"
                        echo "Destroying only EC2 instance..."
                        echo "======================================"
                        
                        # Destroy only EC2 and EIP
                        terraform destroy -auto-approve \
                          -target=aws_eip_association.app \
                          -target=aws_eip.app \
                          -target=aws_instance.app \
                          -var="docker_image=${TF_VAR_docker_image}" 2>/dev/null || echo "EC2 resources don'\''t exist yet"
                        
                        echo ""
                        echo "======================================"
                        echo "Creating new EC2 instance..."
                        echo "======================================"
                        
                        # Check if EIP exists, if not create it (but only if limit not exceeded)
                        EXISTING_EIP_CHECK=$(terraform state show aws_eip.app 2>/dev/null | grep -q "id" && echo "exists" || echo "missing")
                        if [ "$EXISTING_EIP_CHECK" = "missing" ]; then
                            echo "EIP not in state, checking if we can create one..."
                            EIP_COUNT=$(aws ec2 describe-addresses --region ${AWS_REGION} --query "length(Addresses)" --output text 2>/dev/null || echo "0")
                            if [ "$EIP_COUNT" -ge 5 ]; then
                                echo "WARNING: EIP limit reached. Attempting to reuse existing unassociated EIP..."
                                UNASSOCIATED_EIP=$(aws ec2 describe-addresses --region ${AWS_REGION} --filters "Name=domain,Values=vpc" "Name=association-id,Values=" --query "Addresses[0].AllocationId" --output text 2>/dev/null || echo "")
                                if [ -n "$UNASSOCIATED_EIP" ] && [ "$UNASSOCIATED_EIP" != "None" ]; then
                                    echo "Found unassociated EIP: $UNASSOCIATED_EIP - importing..."
                                    terraform import -var="docker_image=${TF_VAR_docker_image}" aws_eip.app $UNASSOCIATED_EIP 2>/dev/null || echo "  (import failed, will try to create)"
                                fi
                            fi
                        fi
                        
                        # Apply only EC2-related resources to avoid conflicts with existing EKS/NAT resources
                        # Use -target to only apply EC2 instance, EIP (if not imported), and EIP association
                        # IMPORTANT: Use single line to avoid shell variable expansion issues
                        terraform apply -auto-approve -target=aws_eip.app -target=aws_eip_association.app -target=aws_instance.app -var="docker_image=${TF_VAR_docker_image}" || {
                            echo "Apply failed. Checking if EIP is the issue..."
                            # If EIP creation failed due to limit, try to apply without EIP (instance will get auto-assigned IP)
                            terraform apply -auto-approve -target=aws_instance.app -var="docker_image=${TF_VAR_docker_image}" || {
                                echo "ERROR: Failed to create EC2 instance"
                                exit 1
                            }
                        }
                        
                        echo "======================================"
                        echo "EC2 instance refreshed successfully"
                        echo "======================================"
                    '''
                }
            }

                echo 'EC2 instance recreated with new configuration'
                echo 'VPC and RDS remain unchanged (faster deployment!)'
    }
}
return this