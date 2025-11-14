// jenkins/stages/terraformApply.groovy
def call() {
    stage('Terraform Apply') {
        when {
            expression { 
                return fileExists('terraform/main.tf') && params.DEPLOYMENT_MODE != 'REUSE_INFRASTRUCTURE'
            }
        }
        steps {
            script {
                echo '========================================='
                echo 'Stage 12: Terraform Apply (Deploy to AWS)'
                echo '========================================='
            }

            // Ask for approval before deploying to AWS (optional)
            // Uncomment the following lines to require manual approval
            // input {
            //     message "Deploy to AWS?"
            //     ok "Deploy"
            // }

            withCredentials([file(credentialsId: "${AWS_CREDENTIAL_ID}", variable: 'AWS_CREDENTIALS_FILE')]) {
                dir(TERRAFORM_DIR) {
                    sh '''
                        echo "Setting up AWS credentials..."
                        mkdir -p ~/.aws
                        cp $AWS_CREDENTIALS_FILE ~/.aws/credentials
                        chmod 600 ~/.aws/credentials
                        
                        # Pre-check: Wait for any existing EKS cluster to be fully deleted
                        echo ""
                        echo "=========================================="
                        echo "Pre-Check: Verifying EKS cluster name availability"
                        echo "=========================================="
                        
                        CLUSTER_NAME="achat-app-eks-cluster"
                        CLUSTER_STATUS=$(aws eks describe-cluster \
                            --region ${AWS_REGION} \
                            --name $CLUSTER_NAME \
                            --query 'cluster.status' \
                            --output text 2>/dev/null || echo "NOT_FOUND")
                        
                        if [ "$CLUSTER_STATUS" != "NOT_FOUND" ]; then
                            echo "Found existing cluster: $CLUSTER_NAME"
                            echo "  Current status: $CLUSTER_STATUS"
                            
                            if [ "$CLUSTER_STATUS" = "DELETING" ]; then
                                echo ""
                                echo "Cluster is being deleted. Waiting for deletion to complete..."
                                echo "This may take 5-10 minutes..."
                                echo ""
                                
                                WAIT_START=$(date +%s)
                                MAX_WAIT=900  # 15 minutes
                                
                                while true; do
                                    sleep 30
                                    
                                    CURRENT_STATUS=$(aws eks describe-cluster \
                                        --region ${AWS_REGION} \
                                        --name $CLUSTER_NAME \
                                        --query 'cluster.status' \
                                        --output text 2>/dev/null || echo "DELETED")
                                    
                                    if [ "$CURRENT_STATUS" = "DELETED" ] || echo "$CURRENT_STATUS" | grep -q "ResourceNotFoundException"; then
                                        echo "Cluster $CLUSTER_NAME is now fully deleted"
                                        break
                                    fi
                                    
                                    ELAPSED=$(($(date +%s) - WAIT_START))
                                    if [ $ELAPSED -gt $MAX_WAIT ]; then
                                        echo "Timeout waiting for cluster deletion (${MAX_WAIT}s)"
                                        echo "  Current status: $CURRENT_STATUS"
                                        exit 1
                                    fi
                                    
                                    echo "  Status: $CURRENT_STATUS (waited ${ELAPSED}s / ${MAX_WAIT}s)"
                                done
                                echo ""
                            elif [ "$CLUSTER_STATUS" = "ACTIVE" ]; then
                                echo "ERROR: Cluster $CLUSTER_NAME is still ACTIVE!"
                                echo "  This should have been deleted by the cleanup stage."
                                echo "  Please run with CLEANUP_AND_DEPLOY mode or manually delete the cluster."
                                exit 1
                            else
                                echo "Unexpected cluster status: $CLUSTER_STATUS"
                                echo "  Proceeding anyway..."
                            fi
                        else
                            echo "No existing cluster found - name is available"
                        fi
                        
                        echo ""
                        echo "=========================================="
                        echo "Applying Terraform plan..."
                        echo "=========================================="
                        
                        # Retry terraform apply up to 3 times with exponential backoff
                        MAX_RETRIES=3
                        RETRY_COUNT=0
                        SUCCESS=false
                        
                        while [ $RETRY_COUNT -lt $MAX_RETRIES ] && [ "$SUCCESS" = false ]; do
                            RETRY_COUNT=$((RETRY_COUNT + 1))
                            echo ""
                            echo "=========================================="
                            echo "Terraform Apply - Attempt $RETRY_COUNT of $MAX_RETRIES"
                            echo "=========================================="
                            
                            if terraform apply -auto-approve tfplan; then
                                echo ""
                                echo "Terraform apply completed successfully!"
                                SUCCESS=true
                            else
                                EXIT_CODE=$?
                                echo ""
                                echo "Terraform apply failed (exit code: $EXIT_CODE)"
                                
                                if [ $RETRY_COUNT -lt $MAX_RETRIES ]; then
                                    # Calculate delay: 10s, 20s, 40s (using case for sh compatibility)
                                    case $RETRY_COUNT in
                                        1) DELAY=10 ;;
                                        2) DELAY=20 ;;
                                        3) DELAY=40 ;;
                                        *) DELAY=10 ;;
                                    esac
                                    echo "Retrying in ${DELAY} seconds..."
                                    echo "(This may be a temporary network/TLS issue)"
                                    echo ""
                                    echo "Note: Will need to recreate plan after retry..."
                                    sleep $DELAY
                                    
                                    # Recreate plan for retry (plan file may be consumed)
                                    echo "Recreating Terraform plan for retry..."
                                    if ! terraform plan \
                                      -var="docker_image=${TF_VAR_docker_image}" \
                                      -out=tfplan \
                                      -input=false; then
                                        echo "Failed to recreate plan for retry"
                                        exit 1
                                    fi
                                else
                                    echo ""
                                    echo "Terraform apply failed after $MAX_RETRIES attempts"
                                    exit $EXIT_CODE
                                fi
                            fi
                        done

                        echo ""
                        echo "Deployment complete!"
                    '''
                }
            }

            script {
                echo 'Infrastructure deployed to AWS successfully'
            }
        }
    }
}
return this
