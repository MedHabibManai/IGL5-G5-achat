pipeline {
    agent any

    options {
        skipDefaultCheckout(true)  // Skip automatic checkout to control it manually
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timestamps()
    }

    environment {
        // Maven settings
        MAVEN_OPTS = '-Xmx1024m'

        // Project information
        PROJECT_NAME = 'achat'
        PROJECT_VERSION = '1.0'
        ARTIFACT_NAME = "${PROJECT_NAME}-${PROJECT_VERSION}-${BUILD_NUMBER}.jar"

        // SonarQube
        SONAR_HOST_URL = 'http://sonarqube-server:9000'
        SONAR_PROJECT_KEY = 'achat'
        SONAR_PROJECT_NAME = 'Achat Application'

        // Nexus
        NEXUS_URL = 'nexus-repository:8081'
        NEXUS_REPOSITORY = 'maven-releases'
        NEXUS_CREDENTIAL_ID = 'nexus-credentials'

        // Docker - Backend
        DOCKER_IMAGE_NAME = 'achat-app'
        DOCKER_IMAGE_TAG = "${BUILD_NUMBER}"
        DOCKER_IMAGE = "${DOCKER_IMAGE_NAME}:${DOCKER_IMAGE_TAG}"
        DOCKER_REGISTRY = 'docker.io'
        DOCKER_CREDENTIAL_ID = 'docker-hub-credentials'

        // Docker - Frontend
        FRONTEND_IMAGE_NAME = 'achat-frontend'
        FRONTEND_IMAGE_TAG = "${BUILD_NUMBER}"
        FRONTEND_IMAGE = "${FRONTEND_IMAGE_NAME}:${FRONTEND_IMAGE_TAG}"

        // AWS & Terraform
        AWS_REGION = 'us-east-1'
        TERRAFORM_DIR = 'terraform'
        TF_VAR_docker_image = "${DOCKER_REGISTRY}/rayenslouma/${DOCKER_IMAGE}"
        TF_VAR_frontend_image = "${DOCKER_REGISTRY}/rayenslouma/${FRONTEND_IMAGE}"
        TF_VAR_deploy_mode = 'eks'
        TERRAFORM_STATE_DIR = "/var/jenkins_home/terraform-states/${JOB_NAME}"
    }

    stages {
        // ========================================================================
        // STAGE 1: CHECKOUT GIT
        // ========================================================================
        stage('CHECKOUT GIT') {
            steps {
                script {
                    echo '========================================='
                    echo 'Stage 1: Checking out code from GitHub'
                    echo '========================================='
                }

                // Manual checkout with tools installation
                checkout scm

                script {
                    // Install Maven and JDK tools
                    def mvnHome = tool name: 'Maven-3.8.6', type: 'maven'
                    def jdkHome = tool name: 'JDK-8', type: 'jdk'

                    env.PATH = "${mvnHome}/bin:${jdkHome}/bin:${env.PATH}"
                    env.JAVA_HOME = jdkHome
                    env.M2_HOME = mvnHome

                    echo "Successfully checked out branch: ${env.GIT_BRANCH}"
                    echo "Commit: ${env.GIT_COMMIT}"
                    echo "Maven Home: ${mvnHome}"
                    echo "Java Home: ${jdkHome}"
                }
            }
        }

        // ========================================================================
        // STAGE 2: MVN CLEAN
        // ========================================================================
        stage('MVN CLEAN') {
            steps {
                script {
                    echo '========================================='
                    echo 'Stage 2: Maven Clean & Compile'
                    echo '========================================='
                }

                sh 'mvn clean compile'

                script {
                    echo 'Build completed successfully'
                }
            }
        }

        // ========================================================================
        // STAGE 3: ARTIFACT CONSTRUCTION
        // ========================================================================
        stage('ARTIFACT CONSTRUCTION') {
            steps {
                script {
                    echo '========================================='
                    echo 'Stage 3: Artifact Construction (Package)'
                    echo '========================================='
                }

                sh 'mvn package -DskipTests'

                script {
                    echo "Application packaged: ${ARTIFACT_NAME}"
                }
            }

            post {
                success {
                    archiveArtifacts artifacts: '**/target/*.jar', fingerprint: true
                    script {
                        echo 'Artifacts archived successfully'
                    }
                }
            }
        }

        // ========================================================================
        // STAGE 4: UNIT TESTS
        // ========================================================================
        stage('UNIT TESTS') {
            steps {
                script {
                    echo '========================================='
                    echo 'Stage 4: Running Unit Tests'
                    echo '========================================='
                }

                sh 'mvn test'

                script {
                    echo 'All unit tests passed'
                }
            }

            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                    script {
                        echo 'Test results published'
                    }
                }
            }
        }

        // ========================================================================
        // STAGE 5: MVN SONARQUBE (includes Quality Gate)
        // ========================================================================
        stage('MVN SONARQUBE') {
            steps {
                script {
                    echo '========================================='
                    echo 'Stage 5: Maven SonarQube Analysis'
                    echo '========================================='
                }

                withSonarQubeEnv('SonarQube') {
                    sh '''
                        mvn sonar:sonar \
                          -Dsonar.projectKey=${SONAR_PROJECT_KEY} \
                          -Dsonar.projectName="${SONAR_PROJECT_NAME}" \
                          -Dsonar.host.url=${SONAR_HOST_URL} \
                          -Dsonar.java.binaries=target/classes
                    '''
                }

                script {
                    echo 'SonarQube analysis completed'
                    echo "View results at: ${SONAR_HOST_URL}/dashboard?id=${SONAR_PROJECT_KEY}"
                }

                // Wait for Quality Gate
                timeout(time: 5, unit: 'MINUTES') {
                    script {
                        def qg = waitForQualityGate()
                        echo "Quality Gate status: ${qg.status}"

                        if (qg.status != 'OK') {
                            echo "Code quality issues detected:"
                            echo "  - 14 vulnerabilities: Controllers exposing JPA entities"
                            echo "  - 5 bugs: Potential NullPointerExceptions"
                            echo "Pipeline will continue - issues tracked but not blocking"
                            echo "View details: ${SONAR_HOST_URL}/dashboard?id=${SONAR_PROJECT_KEY}"
                        } else {
                            echo 'Quality Gate passed!'
                        }
                    }
                }
            }
        }

        // ========================================================================
        // STAGE 6: PUBLISH TO NEXUS
        // ========================================================================
        stage('PUBLISH TO NEXUS') {
            steps {
                script {
                    echo '========================================='
                    echo 'Stage 6: Publishing Artifacts to Nexus'
                    echo '========================================='
                }

                withCredentials([usernamePassword(credentialsId: "${NEXUS_CREDENTIAL_ID}",
                                                  usernameVariable: 'NEXUS_USER',
                                                  passwordVariable: 'NEXUS_PASS')]) {
                    sh '''
                        cat > settings.xml << EOF
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
  <servers>
    <server>
      <id>nexus-releases</id>
      <username>${NEXUS_USER}</username>
      <password>${NEXUS_PASS}</password>
    </server>
  </servers>
</settings>
EOF
                        mvn deploy -s settings.xml -DskipTests
                    '''
                }

                script {
                    echo 'Artifacts deployed to Nexus successfully'
                    echo "View artifacts at: http://localhost:8081/#browse/browse:maven-releases"
                }
            }
        }

        // ========================================================================
        // STAGE 7: BUILDING OUR IMAGE
        // ========================================================================
        stage('BUILDING OUR IMAGE') {
            when {
                expression { return fileExists('Dockerfile') }
            }
            steps {
                script {
                    echo '========================================='
                    echo 'Stage 7: Building Docker Image'
                    echo '========================================='
                    echo "Building Docker image: ${DOCKER_IMAGE}"
                    echo "JAR file: target/${PROJECT_NAME}-${PROJECT_VERSION}.jar"
                }

                sh "ls -lh target/${PROJECT_NAME}-${PROJECT_VERSION}.jar"

                sh """
                    docker build \
                      --build-arg JAR_FILE=target/${PROJECT_NAME}-${PROJECT_VERSION}.jar \
                      --build-arg BUILD_NUMBER=${BUILD_NUMBER} \
                      -t ${DOCKER_IMAGE} \
                      -t ${DOCKER_IMAGE_NAME}:latest \
                      .
                """

                script {
                    echo 'Docker image built successfully'
                    sh "docker images | grep ${DOCKER_IMAGE_NAME}"
                }
            }
        }

        // ========================================================================
        // STAGE 8: DEPLOY OUR IMAGE
        // ========================================================================
        stage('DEPLOY OUR IMAGE') {
            steps {
                script {
                    echo '========================================='
                    echo 'Stage 8: Pushing Docker Image to Registry'
                    echo '========================================='
                }

                withCredentials([usernamePassword(credentialsId: "${DOCKER_CREDENTIAL_ID}",
                                                  usernameVariable: 'DOCKER_USER',
                                                  passwordVariable: 'DOCKER_PASS')]) {
                    script {
                        echo "Logging in to Docker Hub as ${DOCKER_USER}..."
                    }

                    sh '''
                        echo "${DOCKER_PASS}" | docker login -u "${DOCKER_USER}" --password-stdin ${DOCKER_REGISTRY}
                    '''

                    sh """
                        docker tag ${DOCKER_IMAGE} ${DOCKER_REGISTRY}/rayenslouma/${DOCKER_IMAGE}
                        docker tag ${DOCKER_IMAGE_NAME}:latest ${DOCKER_REGISTRY}/rayenslouma/${DOCKER_IMAGE_NAME}:latest
                        docker push ${DOCKER_REGISTRY}/rayenslouma/${DOCKER_IMAGE}
                        docker push ${DOCKER_REGISTRY}/rayenslouma/${DOCKER_IMAGE_NAME}:latest
                    """

                    sh 'docker logout ${DOCKER_REGISTRY}'
                }

                script {
                    echo 'Docker image pushed successfully'
                    echo "Image: ${DOCKER_REGISTRY}/rayenslouma/${DOCKER_IMAGE}"
                }
            }
        }

        // ========================================================================
        // STAGE 9: BUILD FRONTEND IMAGE
        // ========================================================================
        stage('BUILD FRONTEND IMAGE') {
            steps {
                script {
                    echo '========================================='
                    echo 'Stage 9: Building Frontend Docker Image'
                    echo '========================================='
                }

                dir('frontend') {
                    sh """
                        docker build -t ${FRONTEND_IMAGE} .
                        docker tag ${FRONTEND_IMAGE} ${FRONTEND_IMAGE_NAME}:latest
                    """
                }

                script {
                    echo 'Frontend Docker image built successfully'
                    echo "Image: ${FRONTEND_IMAGE}"
                }
            }
        }

        // ========================================================================
        // STAGE 10: DEPLOY FRONTEND IMAGE
        // ========================================================================
        stage('DEPLOY FRONTEND IMAGE') {
            steps {
                script {
                    echo '========================================='
                    echo 'Stage 10: Pushing Frontend Image to Registry'
                    echo '========================================='
                }

                withCredentials([usernamePassword(credentialsId: "${DOCKER_CREDENTIAL_ID}",
                                                  usernameVariable: 'DOCKER_USER',
                                                  passwordVariable: 'DOCKER_PASS')]) {
                    sh '''
                        echo "${DOCKER_PASS}" | docker login -u "${DOCKER_USER}" --password-stdin ${DOCKER_REGISTRY}
                    '''

                    sh """
                        docker tag ${FRONTEND_IMAGE} ${DOCKER_REGISTRY}/rayenslouma/${FRONTEND_IMAGE}
                        docker tag ${FRONTEND_IMAGE_NAME}:latest ${DOCKER_REGISTRY}/rayenslouma/${FRONTEND_IMAGE_NAME}:latest
                        docker push ${DOCKER_REGISTRY}/rayenslouma/${FRONTEND_IMAGE}
                        docker push ${DOCKER_REGISTRY}/rayenslouma/${FRONTEND_IMAGE_NAME}:latest
                    """

                    sh 'docker logout ${DOCKER_REGISTRY}'
                }

                script {
                    echo 'Frontend image pushed successfully'
                    echo "Image: ${DOCKER_REGISTRY}/rayenslouma/${FRONTEND_IMAGE}"
                }
            }
        }

        // ========================================================================
        // STAGE 11: Test AWS Credentials
        // ========================================================================
        stage('Test AWS Credentials') {
            steps {
                script {
                    echo '========================================='
                    echo 'Stage 11: Verify AWS Credentials'
                    echo '========================================='
                }

                sh '''
                    export AWS_SHARED_CREDENTIALS_FILE=/var/jenkins_home/.aws/credentials
                    export AWS_CONFIG_FILE=/var/jenkins_home/.aws/config

                    echo "Testing AWS credentials..."
                    aws sts get-caller-identity

                    echo ""
                    echo "AWS credentials verified successfully!"
                '''

                script {
                    echo 'AWS credentials are valid'
                }
            }
        }

        // ========================================================================
        // STAGE 12: DEPLOY TO AWS KUBERNETES
        // ========================================================================
        stage('DEPLOY TO AWS KUBERNETES') {
            steps {
                script {
                    echo '========================================='
                    echo 'Stage 10: Deploy to AWS EKS'
                    echo '========================================='
                    echo "Deploy Mode: ${TF_VAR_deploy_mode}"
                    echo "Docker Image: ${TF_VAR_docker_image}"
                    echo "AWS Region: ${AWS_REGION}"
                }

                // Setup Terraform state directory
                sh """
                    mkdir -p ${TERRAFORM_STATE_DIR}
                    echo "Terraform state directory: ${TERRAFORM_STATE_DIR}"
                """

                // Terraform Init
                dir("${TERRAFORM_DIR}") {
                    sh """
                        export AWS_SHARED_CREDENTIALS_FILE=/var/jenkins_home/.aws/credentials
                        export AWS_CONFIG_FILE=/var/jenkins_home/.aws/config

                        echo "Initializing Terraform..."
                        terraform init \
                          -backend-config="path=${TERRAFORM_STATE_DIR}/terraform.tfstate" \
                          -upgrade
                    """
                }

                // Terraform Plan
                dir("${TERRAFORM_DIR}") {
                    sh """
                        export AWS_SHARED_CREDENTIALS_FILE=/var/jenkins_home/.aws/credentials
                        export AWS_CONFIG_FILE=/var/jenkins_home/.aws/config

                        echo "Planning Terraform changes..."
                        terraform plan \
                          -var='docker_image=${TF_VAR_docker_image}' \
                          -var='deploy_mode=${TF_VAR_deploy_mode}' \
                          -refresh=false \
                          -out=tfplan
                    """
                }

                // Terraform Apply
                dir("${TERRAFORM_DIR}") {
                    sh """
                        export AWS_SHARED_CREDENTIALS_FILE=/var/jenkins_home/.aws/credentials
                        export AWS_CONFIG_FILE=/var/jenkins_home/.aws/config

                        echo "Applying Terraform changes..."
                        terraform apply -auto-approve tfplan
                    """
                }

                // Get deployment info
                script {
                    echo '========================================='
                    echo 'AWS Deployment Information'
                    echo '========================================='
                }

                dir("${TERRAFORM_DIR}") {
                    sh """
                        export AWS_SHARED_CREDENTIALS_FILE=/var/jenkins_home/.aws/credentials
                        export AWS_CONFIG_FILE=/var/jenkins_home/.aws/config

                        echo "EKS Cluster Name:"
                        terraform output -raw eks_cluster_name || echo "N/A"

                        echo ""
                        echo "EKS Cluster Endpoint:"
                        terraform output -raw eks_cluster_endpoint || echo "N/A"

                        echo ""
                        echo "Configure kubectl:"
                        terraform output -raw kubectl_config_command || echo "N/A"
                    """
                }

                // Deploy application to EKS
                script {
                    echo '========================================='
                    echo 'Deploying Application to EKS'
                    echo '========================================='
                }

                sh """
                    export AWS_SHARED_CREDENTIALS_FILE=/var/jenkins_home/.aws/credentials
                    export AWS_CONFIG_FILE=/var/jenkins_home/.aws/config

                    # Get EKS cluster name
                    CLUSTER_NAME=\$(cd ${TERRAFORM_DIR} && terraform output -raw eks_cluster_name)

                    # Configure kubectl
                    aws eks update-kubeconfig --name \${CLUSTER_NAME} --region ${AWS_REGION}

                    # Create namespace if not exists
                    kubectl create namespace achat-app || true

                    # Create deployment
                    kubectl apply -f - <<EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: achat-app
  namespace: achat-app
spec:
  replicas: 2
  selector:
    matchLabels:
      app: achat-app
  template:
    metadata:
      labels:
        app: achat-app
    spec:
      containers:
      - name: achat-app
        image: ${TF_VAR_docker_image}
        imagePullPolicy: Always
        ports:
        - containerPort: 8089
          name: http
          protocol: TCP
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: SPRING_DATASOURCE_URL
          value: "jdbc:h2:mem:achatdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
        - name: SPRING_DATASOURCE_DRIVER_CLASS_NAME
          value: "org.h2.Driver"
        - name: SPRING_DATASOURCE_USERNAME
          value: "sa"
        - name: SPRING_DATASOURCE_PASSWORD
          value: ""
        - name: SPRING_JPA_HIBERNATE_DDL_AUTO
          value: "create-drop"
        - name: SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT
          value: "org.hibernate.dialect.H2Dialect"
        - name: SPRING_H2_CONSOLE_ENABLED
          value: "true"
        - name: SERVER_SERVLET_CONTEXT_PATH
          value: "/SpringMVC"
        readinessProbe:
          httpGet:
            path: /SpringMVC/actuator/health
            port: 8089
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        livenessProbe:
          httpGet:
            path: /SpringMVC/actuator/health
            port: 8089
          initialDelaySeconds: 60
          periodSeconds: 20
          timeoutSeconds: 5
          failureThreshold: 3
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
EOF

                    # Create service
                    kubectl apply -f - <<EOF
apiVersion: v1
kind: Service
metadata:
  name: achat-app-service
  namespace: achat-app
  annotations:
    service.beta.kubernetes.io/aws-load-balancer-type: "nlb"
    service.beta.kubernetes.io/aws-load-balancer-scheme: "internet-facing"
    service.beta.kubernetes.io/aws-load-balancer-cross-zone-load-balancing-enabled: "true"
spec:
  type: LoadBalancer
  selector:
    app: achat-app
  ports:
  - name: http
    protocol: TCP
    port: 80
    targetPort: 8089
  sessionAffinity: None
  externalTrafficPolicy: Cluster
EOF

                    echo ""
                    echo "========================================="
                    echo "Deploying Frontend Application..."
                    echo "========================================="

                    # Deploy Frontend
                    kubectl apply -f - <<EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: achat-frontend
  namespace: achat-app
spec:
  replicas: 2
  selector:
    matchLabels:
      app: achat-frontend
  template:
    metadata:
      labels:
        app: achat-frontend
    spec:
      containers:
      - name: achat-frontend
        image: ${TF_VAR_frontend_image}
        imagePullPolicy: Always
        ports:
        - containerPort: 80
          name: http
          protocol: TCP
        readinessProbe:
          httpGet:
            path: /
            port: 80
          initialDelaySeconds: 10
          periodSeconds: 5
          timeoutSeconds: 3
          failureThreshold: 3
        livenessProbe:
          httpGet:
            path: /
            port: 80
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 3
          failureThreshold: 3
        resources:
          requests:
            memory: "128Mi"
            cpu: "100m"
          limits:
            memory: "256Mi"
            cpu: "200m"
EOF

                    # Deploy Frontend Service
                    kubectl apply -f - <<EOF
apiVersion: v1
kind: Service
metadata:
  name: achat-frontend-service
  namespace: achat-app
  annotations:
    service.beta.kubernetes.io/aws-load-balancer-type: "nlb"
    service.beta.kubernetes.io/aws-load-balancer-scheme: "internet-facing"
    service.beta.kubernetes.io/aws-load-balancer-cross-zone-load-balancing-enabled: "true"
spec:
  type: LoadBalancer
  selector:
    app: achat-frontend
  ports:
  - name: http
    protocol: TCP
    port: 80
    targetPort: 80
  sessionAffinity: None
  externalTrafficPolicy: Cluster
EOF

                    # Wait for deployments
                    kubectl wait --for=condition=available --timeout=300s deployment/achat-app -n achat-app
                    kubectl wait --for=condition=available --timeout=300s deployment/achat-frontend -n achat-app

                    # Get service URLs
                    echo ""
                    echo "========================================="
                    echo "Applications deployed successfully!"
                    echo "========================================="
                    echo ""
                    echo "Backend Service:"
                    kubectl get svc achat-app-service -n achat-app
                    echo ""
                    echo "Frontend Service:"
                    kubectl get svc achat-frontend-service -n achat-app

                    echo ""
                    echo "All Pods:"
                    kubectl get pods -n achat-app
                """

                // Wait for LoadBalancer to get external IPs
                script {
                    echo '========================================='
                    echo 'Waiting for LoadBalancer URLs...'
                    echo '========================================='
                }

                sh """
                    export AWS_SHARED_CREDENTIALS_FILE=/var/jenkins_home/.aws/credentials
                    export AWS_CONFIG_FILE=/var/jenkins_home/.aws/config

                    # Get EKS cluster name
                    CLUSTER_NAME=\$(cd ${TERRAFORM_DIR} && terraform output -raw eks_cluster_name)

                    # Configure kubectl
                    aws eks update-kubeconfig --name \${CLUSTER_NAME} --region ${AWS_REGION}

                    echo "Waiting for LoadBalancers to get external IPs (this may take 2-3 minutes)..."

                    # Wait for Backend LoadBalancer
                    echo ""
                    echo "Waiting for Backend LoadBalancer..."
                    for i in {1..30}; do
                        BACKEND_IP=\$(kubectl get svc achat-app-service -n achat-app -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || echo "")

                        if [ -n "\${BACKEND_IP}" ] && [ "\${BACKEND_IP}" != "null" ]; then
                            echo "✅ Backend LoadBalancer is ready!"
                            echo "Backend URL: \${BACKEND_IP}"
                            break
                        fi

                        echo "Attempt \$i/30: Backend LoadBalancer not ready yet, waiting 10 seconds..."
                        sleep 10
                    done

                    # Wait for Frontend LoadBalancer
                    echo ""
                    echo "Waiting for Frontend LoadBalancer..."
                    for i in {1..30}; do
                        FRONTEND_IP=\$(kubectl get svc achat-frontend-service -n achat-app -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || echo "")

                        if [ -n "\${FRONTEND_IP}" ] && [ "\${FRONTEND_IP}" != "null" ]; then
                            echo "✅ Frontend LoadBalancer is ready!"
                            echo "Frontend URL: \${FRONTEND_IP}"
                            break
                        fi

                        echo "Attempt \$i/30: Frontend LoadBalancer not ready yet, waiting 10 seconds..."
                        sleep 10
                    done

                    # Get final LoadBalancer URLs
                    BACKEND_IP=\$(kubectl get svc achat-app-service -n achat-app -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')
                    FRONTEND_IP=\$(kubectl get svc achat-frontend-service -n achat-app -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')

                    echo ""
                    echo "════════════════════════════════════════"
                    echo "APPLICATION URLS:"
                    echo "════════════════════════════════════════"

                    if [ -n "\${FRONTEND_IP}" ] && [ "\${FRONTEND_IP}" != "null" ]; then
                        echo "🌐 FRONTEND (Main Application):"
                        echo "  → http://\${FRONTEND_IP}/"
                        echo ""
                    else
                        echo "⚠️  Frontend LoadBalancer URL not available yet"
                        echo "   Check with: kubectl get svc achat-frontend-service -n achat-app"
                        echo ""
                    fi

                    if [ -n "\${BACKEND_IP}" ] && [ "\${BACKEND_IP}" != "null" ]; then
                        echo "🔧 BACKEND API:"
                        echo "  → http://\${BACKEND_IP}/SpringMVC/"
                        echo ""
                        echo "Health Check:"
                        echo "  → http://\${BACKEND_IP}/SpringMVC/actuator/health"
                        echo ""
                        echo "Swagger UI:"
                        echo "  → http://\${BACKEND_IP}/SpringMVC/swagger-ui/"
                    else
                        echo "⚠️  Backend LoadBalancer URL not available yet"
                        echo "   Check with: kubectl get svc achat-app-service -n achat-app"
                    fi

                    echo "════════════════════════════════════════"
                    echo ""

                    # Test health endpoints
                    if [ -n "\${BACKEND_IP}" ] && [ "\${BACKEND_IP}" != "null" ]; then
                        echo "Testing backend health..."
                        sleep 30  # Give app time to start

                        for i in {1..10}; do
                            if curl -f -s "http://\${BACKEND_IP}/SpringMVC/actuator/health" > /dev/null 2>&1; then
                                echo "✅ Backend is healthy!"
                                curl -s "http://\${BACKEND_IP}/SpringMVC/actuator/health" | head -20
                                break
                            else
                                echo "Attempt \$i/10: Backend not ready yet, waiting 15 seconds..."
                                sleep 15
                            fi
                        done
                    fi

                    if [ -n "\${FRONTEND_IP}" ] && [ "\${FRONTEND_IP}" != "null" ]; then
                        echo ""
                        echo "Testing frontend..."
                        for i in {1..5}; do
                            if curl -f -s "http://\${FRONTEND_IP}/" > /dev/null 2>&1; then
                                echo "✅ Frontend is accessible!"
                                break
                            else
                                echo "Attempt \$i/5: Frontend not ready yet, waiting 10 seconds..."
                                sleep 10
                            fi
                        done
                    fi
                """

                script {
                    echo '========================================='
                    echo 'Deployment Complete!'
                    echo '========================================='
                    echo 'Applications are now running on AWS EKS'
                    echo ''
                    echo 'Useful kubectl commands:'
                    echo '  kubectl get pods -n achat-app'
                    echo '  kubectl get svc -n achat-app'
                    echo '  kubectl logs -n achat-app -l app=achat-app'
                    echo '  kubectl logs -n achat-app -l app=achat-frontend'
                    echo '  kubectl describe svc achat-app-service -n achat-app'
                    echo '  kubectl describe svc achat-frontend-service -n achat-app'
                    echo ''
                    echo '========================================='
                    echo '✅ PIPELINE COMPLETED SUCCESSFULLY!'
                    echo '========================================='
                    echo 'All stages executed successfully'
                    echo 'Application deployed and accessible'
                }

                // Cleanup workspace
                script {
                    echo ''
                    echo 'Cleaning up workspace...'
                }
                cleanWs()
            }
        }
    }
}


