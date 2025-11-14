// BOM Fix
// jenkins/stages/deployToEKS.groovy
def call() {
    stage('Deploy to EKS') {
        if (!fileExists('k8s/deployment.yaml')) {
            echo 'Skipping EKS deploy: k8s manifests not found'
            return
        }

                echo '========================================='
                echo 'Stage 13: Deploying to AWS EKS'
                echo '========================================='
            
            // Configure kubectl for EKS - using withCredentials instead of withAWS
            withCredentials([file(credentialsId: "${AWS_CREDENTIAL_ID}", variable: 'AWS_CREDENTIALS_FILE')]) {
                dir("${TERRAFORM_DIR}") {
                        // Get EKS cluster name from Terraform, fallback to AWS API if not in state
                        def eksClusterName = sh(
                            script: '''
                                # Parse and export AWS credentials
                                export AWS_ACCESS_KEY_ID=$(grep aws_access_key_id "$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                export AWS_SECRET_ACCESS_KEY=$(grep aws_secret_access_key "$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                export AWS_SESSION_TOKEN=$(grep aws_session_token "$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                export AWS_DEFAULT_REGION=us-east-1
                                
                                # Try to get from Terraform output first
                                CLUSTER_NAME=$(terraform output -raw eks_cluster_name 2>/dev/null || echo "")
                                
                                # If not in Terraform state (e.g., REUSE_INFRASTRUCTURE mode), check AWS directly
                                if [ -z "$CLUSTER_NAME" ] || [ "$CLUSTER_NAME" = "" ]; then
                                    echo "EKS cluster not in Terraform state, checking AWS directly..." >&2
                                    # Try the default cluster name pattern
                                    CLUSTER_NAME="achat-app-eks-cluster"
                                    # Verify cluster exists and is active in AWS
                                    CLUSTER_STATUS=$(aws eks describe-cluster --region us-east-1 --name "$CLUSTER_NAME" --query 'cluster.status' --output text 2>/dev/null || echo "NOT_FOUND")
                                    if [ "$CLUSTER_STATUS" = "NOT_FOUND" ]; then
                                        # Try to find any EKS cluster with matching name pattern
                                        echo "Cluster 'achat-app-eks-cluster' not found, searching for matching clusters..." >&2
                                        CLUSTER_NAME=$(aws eks list-clusters --region us-east-1 --output text 2>/dev/null | grep "achat-app" | head -1 || echo "")
                                        if [ -z "$CLUSTER_NAME" ] || [ "$CLUSTER_NAME" = "None" ]; then
                                            echo "No EKS cluster found matching 'achat-app' pattern" >&2
                                            echo ""
                                            exit 0
                                        fi
                                        # Verify the found cluster is active
                                        CLUSTER_STATUS=$(aws eks describe-cluster --region us-east-1 --name "$CLUSTER_NAME" --query 'cluster.status' --output text 2>/dev/null || echo "NOT_FOUND")
                                    fi
                                    if [ "$CLUSTER_STATUS" != "ACTIVE" ]; then
                                        echo "EKS cluster '$CLUSTER_NAME' exists but is not ACTIVE (status: $CLUSTER_STATUS)" >&2
                                        echo "Cannot deploy to non-active cluster" >&2
                                        echo ""
                                        exit 0
                                    fi
                                    echo "Found active EKS cluster in AWS: $CLUSTER_NAME (status: $CLUSTER_STATUS)" >&2
                                fi
                                
                                # Only output the cluster name to stdout (for returnStdout capture)
                                echo "$CLUSTER_NAME"
                            ''',
                            returnStdout: true
                        ).trim()
                        
                        if (eksClusterName && eksClusterName != "") {
                            echo "Configuring kubectl for EKS cluster: ${eksClusterName}"
                            
                            // Update kubeconfig and deploy
                            sh """
                                # Parse and export AWS credentials
                                export AWS_ACCESS_KEY_ID=\$(grep aws_access_key_id "\$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                export AWS_SECRET_ACCESS_KEY=\$(grep aws_secret_access_key "\$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                export AWS_SESSION_TOKEN=\$(grep aws_session_token "\$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                export AWS_DEFAULT_REGION=us-east-1
                                
                                echo "AWS credentials configured from file"
                                
                                # Update kubeconfig
                                aws eks update-kubeconfig \\
                                    --region ${AWS_REGION} \\
                                    --name ${eksClusterName}
                            """
                            
                            // Get RDS endpoint from Terraform
                            def rdsEndpoint = sh(
                                script: '''
                                    # Parse and export AWS credentials
                                    export AWS_ACCESS_KEY_ID=$(grep aws_access_key_id "$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                    export AWS_SECRET_ACCESS_KEY=$(grep aws_secret_access_key "$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                    export AWS_SESSION_TOKEN=$(grep aws_session_token "$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                    export AWS_DEFAULT_REGION=us-east-1
                                    
                                    terraform output -raw rds_endpoint 2>/dev/null || echo ""
                                ''',
                                returnStdout: true
                            ).trim()
                            
                            if (rdsEndpoint) {
                                echo "RDS Endpoint: ${rdsEndpoint}"
                                echo "Updating ConfigMap with RDS endpoint..."
                            } else {
                                echo "WARNING: Could not retrieve RDS endpoint!"
                            }
                            
                            // Ensure RDS security group allows traffic from EKS nodes
                            echo "Ensuring RDS security group allows EKS nodes access..."
                            sh """
                                # Parse and export AWS credentials
                                export AWS_ACCESS_KEY_ID=\$(grep aws_access_key_id "\$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                export AWS_SECRET_ACCESS_KEY=\$(grep aws_secret_access_key "\$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                export AWS_SESSION_TOKEN=\$(grep aws_session_token "\$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                export AWS_DEFAULT_REGION=us-east-1
                                
                                # Get VPC ID
                                VPC_ID=\$(aws ec2 describe-vpcs --region us-east-1 --filters "Name=tag:Name,Values=achat-app-vpc" --query "Vpcs[0].VpcId" --output text 2>/dev/null || echo "")
                                if [ -z "\$VPC_ID" ] || [ "\$VPC_ID" = "None" ]; then
                                    echo "WARNING: Could not find VPC, skipping RDS security group update"
                                else
                                    # Get RDS security group ID
                                    RDS_SG_ID=\$(aws ec2 describe-security-groups --region us-east-1 --filters "Name=vpc-id,Values=\$VPC_ID" "Name=group-name,Values=achat-app-rds-sg" --query "SecurityGroups[0].GroupId" --output text 2>/dev/null || echo "")
                                    
                                    # Get EKS nodes security group ID
                                    EKS_NODES_SG_ID=\$(aws ec2 describe-security-groups --region us-east-1 --filters "Name=vpc-id,Values=\$VPC_ID" "Name=group-name,Values=achat-app-eks-nodes-sg" --query "SecurityGroups[0].GroupId" --output text 2>/dev/null || echo "")
                                    
                                    if [ -n "\$RDS_SG_ID" ] && [ "\$RDS_SG_ID" != "None" ] && [ -n "\$EKS_NODES_SG_ID" ] && [ "\$EKS_NODES_SG_ID" != "None" ]; then
                                        echo "RDS Security Group: \$RDS_SG_ID"
                                        echo "EKS Nodes Security Group: \$EKS_NODES_SG_ID"
                                        
                                        # Try to add the rule (will fail silently if it already exists)
                                        echo "Ensuring RDS security group allows MySQL (port 3306) from EKS nodes..."
                                        # Use JSON format for ip-permissions (required by newer AWS CLI)
                                        # Create JSON payload with proper escaping
                                        IP_PERMISSIONS_JSON='[{"IpProtocol": "tcp", "FromPort": 3306, "ToPort": 3306, "UserIdGroupPairs": [{"GroupId": "'"\$EKS_NODES_SG_ID"'"}]}]'
                                        OUTPUT=\$(aws ec2 authorize-security-group-ingress \\
                                            --region us-east-1 \\
                                            --group-id \$RDS_SG_ID \\
                                            --ip-permissions "\$IP_PERMISSIONS_JSON" 2>&1) || true
                                        EXIT_CODE=\$?
                                        if echo "\$OUTPUT" | grep -qi "already exists\|Duplicate"; then
                                            echo "RDS security group rule already exists (this is OK)"
                                        elif echo "\$OUTPUT" | grep -q "SecurityGroupIngress"; then
                                            echo "RDS security group rule added successfully"
                                        elif [ \$EXIT_CODE -eq 0 ]; then
                                            echo "RDS security group rule updated successfully"
                                        else
                                            echo "RDS security group update result: \$OUTPUT"
                                            echo "Note: If rule already exists, this is expected and safe to ignore"
                                        fi
                                        echo "RDS security group rule check/update completed"
                                    else
                                        echo "WARNING: Could not find RDS or EKS nodes security groups"
                                        echo "RDS SG ID: \$RDS_SG_ID"
                                        echo "EKS Nodes SG ID: \$EKS_NODES_SG_ID"
                                    fi
                                fi
                            """
                            
                            // Deploy to EKS
                            sh """
                                # Helper function for kubectl with retry (only for connection errors)
                                kubectl_retry() {
                                    local cmd="\$@"
                                    local attempt=0
                                    local max_attempts=3
                                    while [ \$attempt -lt \$max_attempts ]; do
                                        attempt=\$((attempt + 1))
                                        echo "Attempt \$attempt/\$max_attempts: \$cmd"
                                        if eval "\$cmd"; then
                                            echo "Success"
                                            return 0
                                        else
                                            local exit_code=\$?
                                            # Check if it's a connection/TLS error (exit code 1 with specific error messages)
                                            if echo "\$cmd" | grep -q "rollout status" && [ \$exit_code -eq 1 ]; then
                                                # Rollout status failures are deployment issues, not connection errors
                                                echo "Deployment rollout failed (not a connection error)"
                                                return 1
                                            fi
                                            echo "Failed (connection error likely), retrying in 15s..."
                                            sleep 15
                                        fi
                                    done
                                    echo "Failed after \$max_attempts attempts"
                                    return 1
                                }
                                
                                # Helper function to diagnose deployment failures
                                diagnose_deployment() {
                                    local deployment=\$1
                                    local namespace=\$2
                                    echo ""
                                    echo "=========================================="
                                    echo "Diagnosing deployment failure: \$deployment"
                                    echo "=========================================="
                                    echo ""
                                    echo "=== Deployment Status ==="
                                    kubectl get deployment \$deployment -n \$namespace -o wide || true
                                    echo ""
                                    echo "=== Deployment Details ==="
                                    kubectl describe deployment \$deployment -n \$namespace | tail -50 || true
                                    echo ""
                                    echo "=== Pod Status (all pods in namespace) ==="
                                    kubectl get pods -n \$namespace -o wide || true
                                    echo ""
                                    echo "=== Pod Status (matching deployment) ==="
                                    # Try to get pods using app label (works for both achat-app and achat-frontend)
                                    kubectl get pods -n \$namespace -l app=\$deployment -o wide || true
                                    echo ""
                                    echo "=== Recent Events (all in namespace) ==="
                                    kubectl get events -n \$namespace --sort-by='.lastTimestamp' | tail -30 || true
                                    echo ""
                                    echo "=== Pod Logs (all pods, including failed/not ready) ==="
                                    # Get pods using app label
                                    pods=\$(kubectl get pods -n \$namespace -l app=\$deployment -o jsonpath='{.items[*].metadata.name}' 2>/dev/null || echo "")
                                    if [ -z "\$pods" ]; then
                                        # Fallback: get all pods in namespace
                                        pods=\$(kubectl get pods -n \$namespace -o jsonpath='{.items[*].metadata.name}' 2>/dev/null || echo "")
                                    fi
                                    for pod in \$pods; do
                                        if [ -n "\$pod" ]; then
                                            pod_status=\$(kubectl get pod \$pod -n \$namespace -o jsonpath='{.status.phase}' 2>/dev/null || echo "Unknown")
                                            echo "--- Pod: \$pod (status: \$pod_status) ---"
                                            echo "--- Logs ---"
                                            kubectl logs -n \$namespace \$pod --tail=50 || true
                                            echo ""
                                            echo "--- Describe Pod ---"
                                            kubectl describe pod \$pod -n \$namespace | tail -40 || true
                                            echo ""
                                        fi
                                    done
                                    echo ""
                                    echo "=== Container Status Details ==="
                                    for pod in \$pods; do
                                        if [ -n "\$pod" ]; then
                                            echo "--- Container statuses for pod: \$pod ---"
                                            kubectl get pod \$pod -n \$namespace -o jsonpath='{range .status.containerStatuses[*]}{"  Name: "}{.name}{"\\n"}{"  Image: "}{.image}{"\\n"}{"  Ready: "}{.ready}{"\\n"}{"  Restart Count: "}{.restartCount}{"\\n"}{"  State: "}{.state}{"\\n"}{"  Last State: "}{.lastState}{"\\n"}{end}' || true
                                            echo ""
                                        fi
                                    done
                                }
                                
                                # Create namespace first
                                echo "Creating namespace..."
                                kubectl_retry kubectl apply -f ../k8s/namespace.yaml
                                
                                # Apply secrets and configmaps
                                echo "Applying secrets and configmaps..."
                                kubectl_retry kubectl apply -f ../k8s/secret.yaml
                                
                                # Update ConfigMap with actual RDS endpoint
                                if [ -n "${rdsEndpoint}" ]; then
                                    echo "Updating ConfigMap with RDS endpoint: ${rdsEndpoint}"
                                    cat ../k8s/configmap.yaml | \\
                                        sed "s|RDS_ENDPOINT_PLACEHOLDER|${rdsEndpoint}|g" | \\
                                        kubectl apply -f -
                                else
                                    echo "WARNING: Applying ConfigMap without RDS endpoint update!"
                                    kubectl_retry kubectl apply -f ../k8s/configmap.yaml
                                fi
                                
                                # Apply all other Kubernetes manifests (deployments, services, etc.)
                                echo "Applying deployments and services..."
                                kubectl_retry kubectl apply -f ../k8s/deployment.yaml
                                kubectl_retry kubectl apply -f ../k8s/frontend-deployment.yaml
                                kubectl_retry kubectl apply -f ../k8s/service.yaml
                                kubectl_retry kubectl apply -f ../k8s/hpa.yaml
                                
                                # Now update deployment images to specific build versions
                                echo "Updating deployment images to build ${BUILD_NUMBER}..."
                                kubectl_retry kubectl set image deployment/achat-app -n achat-app \\
                                    achat-app=${DOCKER_REGISTRY}/habibmanai/${DOCKER_IMAGE_NAME}:${BUILD_NUMBER} \\
                                    --record
                                
                                kubectl_retry kubectl set image deployment/achat-frontend -n achat-app \\
                                    frontend=${DOCKER_REGISTRY}/habibmanai/achat-frontend:${BUILD_NUMBER} \\
                                    --record
                                
                                # Wait for backend rollout with diagnostics on failure
                                echo "Waiting for backend deployment (timeout: 10 minutes)..."
                                if ! kubectl rollout status deployment/achat-app -n achat-app --timeout=10m; then
                                    diagnose_deployment "achat-app" "achat-app"
                                    echo "ERROR: Backend deployment failed!"
                                    exit 1
                                fi
                                echo "Backend deployment successful!"
                                
                                # Wait for frontend rollout with diagnostics on failure
                                echo "Waiting for frontend deployment (timeout: 10 minutes)..."
                                if ! kubectl rollout status deployment/achat-frontend -n achat-app --timeout=10m; then
                                    diagnose_deployment "achat-frontend" "achat-app"
                                    echo "ERROR: Frontend deployment failed!"
                                    exit 1
                                fi
                                echo "Frontend deployment successful!"
                                
                                # Get service endpoints
                                echo ""
                                echo "=== EKS Services ==="
                                kubectl_retry kubectl get svc -n achat-app
                                
                                # Get Load Balancer URLs
                                echo ""
                                echo "=== Frontend LoadBalancer URL ==="
                                FRONTEND_URL=\$(kubectl get svc achat-frontend -n achat-app -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || echo "Pending...")
                                echo "Frontend will be accessible at: http://\${FRONTEND_URL}"
                                
                                echo ""
                                echo "=== Backend LoadBalancer URL ==="
                                BACKEND_URL=\$(kubectl get svc achat-app -n achat-app -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || echo "Pending...")
                                echo "Backend will be accessible at: http://\${BACKEND_URL}/SpringMVC"
                            """
                            
                            echo 'Application deployed to EKS successfully!'
                        } else {
                            echo 'EKS cluster not found. Skipping EKS deployment.'
                            echo 'Checked both Terraform state and AWS directly.'
                            echo 'If cluster exists, ensure it is named "achat-app-eks-cluster" or contains "achat-app" in the name.'
                            echo 'Otherwise, run terraform apply with create_eks=true to create EKS cluster first.'
                        }
                }
            }

    }
}
return this


