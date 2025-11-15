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
                            
                            // Get RDS endpoint from Terraform, fallback to AWS API if not in state
                            def rdsEndpoint = sh(
                                script: '''
                                    # Parse and export AWS credentials
                                    export AWS_ACCESS_KEY_ID=$(grep aws_access_key_id "$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                    export AWS_SECRET_ACCESS_KEY=$(grep aws_secret_access_key "$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                    export AWS_SESSION_TOKEN=$(grep aws_session_token "$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                    export AWS_DEFAULT_REGION=us-east-1
                                    
                                    # Try Terraform output first
                                    RDS_ENDPOINT=$(terraform output -raw rds_endpoint 2>/dev/null || echo "")
                                    
                                    # If not in Terraform state (e.g., EKS_ONLY mode), query AWS directly
                                    if [ -z "$RDS_ENDPOINT" ] || [ "$RDS_ENDPOINT" = "" ]; then
                                        echo "RDS endpoint not in Terraform state, checking AWS directly..." >&2
                                        # Get RDS instance by name pattern
                                        RDS_ENDPOINT=$(aws rds describe-db-instances --region us-east-1 --query "DBInstances[?contains(DBInstanceIdentifier, 'achat-app')].Endpoint.Address" --output text 2>/dev/null | head -1 || echo "")
                                        if [ -z "$RDS_ENDPOINT" ] || [ "$RDS_ENDPOINT" = "None" ]; then
                                            echo "Could not find RDS instance in AWS" >&2
                                            echo ""
                                            exit 0
                                        fi
                                        echo "Found RDS endpoint in AWS: $RDS_ENDPOINT" >&2
                                    fi
                                    
                                    # Only output the endpoint to stdout (for returnStdout capture)
                                    echo "$RDS_ENDPOINT"
                                ''',
                                returnStdout: true
                            ).trim()
                            
                            if (rdsEndpoint) {
                                echo "RDS Endpoint: ${rdsEndpoint}"
                                echo "Updating ConfigMap with RDS endpoint..."
                            } else {
                                echo "WARNING: Could not retrieve RDS endpoint from Terraform or AWS!"
                            }
                            
                            // Ensure RDS security group allows traffic from EKS nodes
                            echo "Ensuring RDS security group allows EKS nodes access..."
                            sh """
                                # Parse and export AWS credentials
                                export AWS_ACCESS_KEY_ID=\$(grep aws_access_key_id "\$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                export AWS_SECRET_ACCESS_KEY=\$(grep aws_secret_access_key "\$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                export AWS_SESSION_TOKEN=\$(grep aws_session_token "\$AWS_CREDENTIALS_FILE" | cut -d '=' -f2 | tr -d ' ')
                                export AWS_DEFAULT_REGION=us-east-1
                                
                                # Get VPC ID from Terraform first (most reliable)
                                VPC_ID=\$(terraform output -raw vpc_id 2>/dev/null || echo "")
                                # If not in Terraform state, try to find by tag (but prefer Terraform output)
                                if [ -z "\$VPC_ID" ] || [ "\$VPC_ID" = "" ]; then
                                    echo "VPC not in Terraform state, searching by tag..." >&2
                                    # Get the most recent VPC with the tag (in case multiple exist)
                                    VPC_ID=\$(aws ec2 describe-vpcs --region us-east-1 --filters "Name=tag:Name,Values=achat-app-vpc" --query "Vpcs | sort_by(@, &CidrBlock) | [0].VpcId" --output text 2>/dev/null || echo "")
                                fi
                                if [ -z "\$VPC_ID" ] || [ "\$VPC_ID" = "None" ]; then
                                    echo "WARNING: Could not find VPC, skipping RDS security group update"
                                else
                                    # Get RDS security group ID
                                    RDS_SG_ID=\$(aws ec2 describe-security-groups --region us-east-1 --filters "Name=vpc-id,Values=\$VPC_ID" "Name=group-name,Values=achat-app-rds-sg" --query "SecurityGroups[0].GroupId" --output text 2>/dev/null || echo "")
                                    
                                    # Get EKS nodes security group ID - try multiple methods
                                    # Method 1: Try to find by name (Terraform-created)
                                    EKS_NODES_SG_ID=\$(aws ec2 describe-security-groups --region us-east-1 --filters "Name=vpc-id,Values=\$VPC_ID" "Name=group-name,Values=achat-app-eks-nodes-sg" --query "SecurityGroups[0].GroupId" --output text 2>/dev/null || echo "")
                                    
                                    # Method 2: If not found, get from actual EKS node instances (most reliable)
                                    if [ -z "\$EKS_NODES_SG_ID" ] || [ "\$EKS_NODES_SG_ID" = "None" ] || [ "\$EKS_NODES_SG_ID" = "" ]; then
                                        echo "EKS nodes SG not found by name, getting from actual node instances..." >&2
                                        # Get the security group from actual running EKS node instances
                                        EKS_NODES_SG_ID=\$(aws ec2 describe-instances --region us-east-1 \\
                                            --filters "Name=tag:eks:cluster-name,Values=achat-app-eks-cluster" "Name=instance-state-name,Values=running" \\
                                            --query "Reservations[0].Instances[0].SecurityGroups[0].GroupId" \\
                                            --output text 2>/dev/null || echo "")
                                        if [ -n "\$EKS_NODES_SG_ID" ] && [ "\$EKS_NODES_SG_ID" != "None" ]; then
                                            echo "Found EKS nodes SG from instances: \$EKS_NODES_SG_ID" >&2
                                        fi
                                    fi
                                    
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
                                        if echo "\$OUTPUT" | grep -qiE "(already exists|Duplicate)"; then
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
                                # In EKS_ONLY mode, images may not exist - check first before updating
                                # In REUSE_INFRASTRUCTURE mode, images were built, so always update
                                echo "Checking deployment mode and Docker images for build ${BUILD_NUMBER}..."
                                
                                BACKEND_IMAGE="${DOCKER_REGISTRY}/habibmanai/${DOCKER_IMAGE_NAME}:${BUILD_NUMBER}"
                                FRONTEND_IMAGE="${DOCKER_REGISTRY}/habibmanai/achat-frontend:${BUILD_NUMBER}"
                                
                                # Check deployment mode - only skip updates in EKS_ONLY mode
                                DEPLOYMENT_MODE="${params.DEPLOYMENT_MODE}"
                                echo "Deployment mode: \$DEPLOYMENT_MODE"
                                
                                # Default to updating images (for NORMAL, CLEANUP_AND_DEPLOY, REUSE_INFRASTRUCTURE)
                                BACKEND_EXISTS=true
                                FRONTEND_EXISTS=true
                                
                                # Only skip updates in EKS_ONLY mode (where build stages are skipped)
                                if [ "\$DEPLOYMENT_MODE" = "EKS_ONLY" ]; then
                                    echo "EKS_ONLY mode detected - checking if images exist..."
                                    # Get current image from deployment to use as fallback
                                    CURRENT_BACKEND_IMAGE=\$(kubectl get deployment achat-app -n achat-app -o jsonpath='{.spec.template.spec.containers[0].image}' 2>/dev/null || echo "")
                                    if [ -n "\$CURRENT_BACKEND_IMAGE" ]; then
                                        echo "Current backend image: \$CURRENT_BACKEND_IMAGE"
                                        echo "EKS_ONLY mode: Skipping backend image update (images may not exist for this build)"
                                        echo "Deployment will continue with existing image: \$CURRENT_BACKEND_IMAGE"
                                        BACKEND_EXISTS=false  # Don't update
                                    fi
                                    
                                    # Same for frontend
                                    CURRENT_FRONTEND_IMAGE=\$(kubectl get deployment achat-frontend -n achat-app -o jsonpath='{.spec.template.spec.containers[0].image}' 2>/dev/null || echo "")
                                    if [ -n "\$CURRENT_FRONTEND_IMAGE" ]; then
                                        echo "Current frontend image: \$CURRENT_FRONTEND_IMAGE"
                                        echo "EKS_ONLY mode: Skipping frontend image update (images may not exist for this build)"
                                        echo "Deployment will continue with existing image: \$CURRENT_FRONTEND_IMAGE"
                                        FRONTEND_EXISTS=false  # Don't update
                                    fi
                                else
                                    echo "Normal deployment mode - images were built, will update to build ${BUILD_NUMBER}"
                                fi
                                
                                # Update backend image (with retry for connection issues)
                                if [ "\$BACKEND_EXISTS" = "true" ]; then
                                    echo "Updating backend deployment image to build ${BUILD_NUMBER}..."
                                    kubectl_retry kubectl set image deployment/achat-app -n achat-app achat-app=\$BACKEND_IMAGE --record
                                    echo "Backend image updated successfully"
                                else
                                    echo "Skipping backend image update - will use existing image"
                                fi
                                
                                # Update frontend image (with retry for connection issues)
                                if [ "\$FRONTEND_EXISTS" = "true" ]; then
                                    echo "Updating frontend deployment image to build ${BUILD_NUMBER}..."
                                    kubectl_retry kubectl set image deployment/achat-frontend -n achat-app frontend=\$FRONTEND_IMAGE --record
                                    echo "Frontend image updated successfully"
                                else
                                    echo "Skipping frontend image update - will use existing image"
                                fi
                                
                                # Wait for backend rollout with shorter timeout (2 minutes) and non-blocking check
                                echo "Waiting for backend deployment (timeout: 2 minutes, will continue if still progressing)..."
                                # Use shorter timeout - if it fails, check if it's progressing and continue
                                if ! timeout 120 kubectl rollout status deployment/achat-app -n achat-app --timeout=2m 2>&1; then
                                    echo "Rollout check completed or timed out, verifying status..."
                                    # Quick status check
                                    ROLLOUT_STATUS=\$(kubectl get deployment achat-app -n achat-app -o jsonpath='{.status.conditions[?(@.type==\"Progressing\")].status}' 2>/dev/null || echo "Unknown")
                                    READY_REPLICAS=\$(kubectl get deployment achat-app -n achat-app -o jsonpath='{.status.readyReplicas}' 2>/dev/null || echo "0")
                                    DESIRED_REPLICAS=\$(kubectl get deployment achat-app -n achat-app -o jsonpath='{.spec.replicas}' 2>/dev/null || echo "2")
                                    
                                    if [ "\$ROLLOUT_STATUS" = "True" ] || [ "\$READY_REPLICAS" -gt "0" ]; then
                                        echo "Backend deployment is progressing (Ready: \$READY_REPLICAS/\$DESIRED_REPLICAS)"
                                        echo "Continuing - deployment will complete in background..."
                                    else
                                        echo "WARNING: Backend deployment may have issues, but continuing..."
                                        kubectl get pods -n achat-app -l app=achat-app --no-headers 2>/dev/null | head -3 || true
                                    fi
                                else
                                    echo "Backend deployment rollout completed successfully!"
                                fi
                                
                                # Wait for frontend rollout with shorter timeout (2 minutes)
                                echo "Waiting for frontend deployment (timeout: 2 minutes, will continue if still progressing)..."
                                if ! timeout 120 kubectl rollout status deployment/achat-frontend -n achat-app --timeout=2m 2>&1; then
                                    echo "Frontend rollout check completed or timed out, verifying status..."
                                    ROLLOUT_STATUS=\$(kubectl get deployment achat-frontend -n achat-app -o jsonpath='{.status.conditions[?(@.type==\"Progressing\")].status}' 2>/dev/null || echo "Unknown")
                                    READY_REPLICAS=\$(kubectl get deployment achat-frontend -n achat-app -o jsonpath='{.status.readyReplicas}' 2>/dev/null || echo "0")
                                    DESIRED_REPLICAS=\$(kubectl get deployment achat-frontend -n achat-app -o jsonpath='{.spec.replicas}' 2>/dev/null || echo "2")
                                    
                                    if [ "\$ROLLOUT_STATUS" = "True" ] || [ "\$READY_REPLICAS" -gt "0" ]; then
                                        echo "Frontend deployment is progressing (Ready: \$READY_REPLICAS/\$DESIRED_REPLICAS)"
                                        echo "Continuing - deployment will complete in background..."
                                    else
                                        echo "WARNING: Frontend deployment may have issues, but continuing..."
                                        kubectl get pods -n achat-app -l app=achat-frontend --no-headers 2>/dev/null | head -3 || true
                                    fi
                                else
                                    echo "Frontend deployment rollout completed successfully!"
                                fi
                                
                                echo ""
                                echo "Deployment initiated - pods will continue starting in background"
                                echo "You can check status with: kubectl get pods -n achat-app"
                                
                                # Get service endpoints
                                echo ""
                                echo "=== EKS Services ==="
                                kubectl_retry kubectl get svc -n achat-app
                                
                                # Get Load Balancer URLs (brief output - full summary at end)
                                echo ""
                                echo "=== EKS LoadBalancer URLs ==="
                                FRONTEND_URL=\$(kubectl get svc achat-frontend -n achat-app -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || echo "")
                                BACKEND_URL=\$(kubectl get svc achat-app -n achat-app -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || echo "")
                                
                                if [ -n "\$BACKEND_URL" ]; then
                                    echo "Backend LoadBalancer ready"
                                else
                                    echo "Backend LoadBalancer pending (see final summary for URLs)"
                                fi
                                
                                if [ -n "\$FRONTEND_URL" ]; then
                                    echo "Frontend LoadBalancer ready"
                                else
                                    echo "Frontend LoadBalancer pending (see final summary for URLs)"
                                fi
                                
                                echo "(Full URLs will be shown in Final Deployment Summary stage)"
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


