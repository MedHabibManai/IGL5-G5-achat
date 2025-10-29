#!/bin/bash

# Jenkins Quick Start Script for IGL5-G5-Achat CI/CD Pipeline
# This script helps you quickly start Jenkins and get the initial admin password

echo "========================================="
echo "Jenkins CI/CD Pipeline - Quick Start"
echo "========================================="
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Error: Docker is not running!"
    echo "Please start Docker and try again."
    exit 1
fi

echo "✓ Docker is running"
echo ""

# Check if docker-compose.yml exists
if [ ! -f "docker-compose.yml" ]; then
    echo "❌ Error: docker-compose.yml not found!"
    echo "Please run this script from the project root directory."
    exit 1
fi

echo "✓ docker-compose.yml found"
echo ""

# Start Jenkins container
echo "Starting Jenkins container..."
docker-compose up -d jenkins

# Wait for Jenkins to start
echo ""
echo "Waiting for Jenkins to start (this may take 1-2 minutes)..."
sleep 30

# Check if Jenkins container is running
if ! docker ps | grep -q jenkins-cicd; then
    echo "❌ Error: Jenkins container failed to start!"
    echo "Check logs with: docker-compose logs jenkins"
    exit 1
fi

echo "✓ Jenkins container is running"
echo ""

# Get initial admin password
echo "========================================="
echo "Jenkins Initial Setup Information"
echo "========================================="
echo ""
echo "Jenkins URL: http://localhost:8080"
echo ""
echo "Initial Admin Password:"
echo "----------------------------------------"

# Try to get the password (may take a moment for the file to be created)
for i in {1..10}; do
    if docker exec jenkins-cicd test -f /var/jenkins_home/secrets/initialAdminPassword 2>/dev/null; then
        docker exec jenkins-cicd cat /var/jenkins_home/secrets/initialAdminPassword
        echo ""
        echo "----------------------------------------"
        echo ""
        echo "✓ Copy the password above and paste it into Jenkins setup wizard"
        echo ""
        echo "Next steps:"
        echo "1. Open http://localhost:8080 in your browser"
        echo "2. Paste the initial admin password"
        echo "3. Follow the setup wizard"
        echo "4. Refer to JENKINS_SETUP.md for detailed configuration"
        echo ""
        echo "========================================="
        exit 0
    fi
    echo "Waiting for Jenkins to initialize... ($i/10)"
    sleep 5
done

echo ""
echo "⚠ Warning: Could not retrieve initial admin password automatically"
echo "Please run this command manually:"
echo "docker exec jenkins-cicd cat /var/jenkins_home/secrets/initialAdminPassword"
echo ""
echo "========================================="

