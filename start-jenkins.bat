@echo off
REM Jenkins Quick Start Script for IGL5-G5-Achat CI/CD Pipeline
REM This script helps you quickly start Jenkins and get the initial admin password

echo =========================================
echo Jenkins CI/CD Pipeline - Quick Start
echo =========================================
echo.

REM Check if Docker is running
docker info >nul 2>&1
if errorlevel 1 (
    echo X Error: Docker is not running!
    echo Please start Docker Desktop and try again.
    pause
    exit /b 1
)

echo [OK] Docker is running
echo.

REM Check if docker-compose.yml exists
if not exist "docker-compose.yml" (
    echo X Error: docker-compose.yml not found!
    echo Please run this script from the project root directory.
    pause
    exit /b 1
)

echo [OK] docker-compose.yml found
echo.

REM Start Jenkins container
echo Starting Jenkins container...
docker-compose up -d jenkins

REM Wait for Jenkins to start
echo.
echo Waiting for Jenkins to start (this may take 1-2 minutes)...
timeout /t 30 /nobreak >nul

REM Check if Jenkins container is running
docker ps | findstr "jenkins-cicd" >nul
if errorlevel 1 (
    echo X Error: Jenkins container failed to start!
    echo Check logs with: docker-compose logs jenkins
    pause
    exit /b 1
)

echo [OK] Jenkins container is running
echo.

REM Get initial admin password
echo =========================================
echo Jenkins Initial Setup Information
echo =========================================
echo.
echo Jenkins URL: http://localhost:8080
echo.
echo Initial Admin Password:
echo ----------------------------------------

REM Try to get the password (may take a moment for the file to be created)
set /a counter=0
:wait_loop
if %counter% geq 10 goto password_error

docker exec jenkins-cicd test -f /var/jenkins_home/secrets/initialAdminPassword 2>nul
if errorlevel 1 (
    set /a counter+=1
    echo Waiting for Jenkins to initialize... (%counter%/10)
    timeout /t 5 /nobreak >nul
    goto wait_loop
)

docker exec jenkins-cicd cat /var/jenkins_home/secrets/initialAdminPassword
echo.
echo ----------------------------------------
echo.
echo [OK] Copy the password above and paste it into Jenkins setup wizard
echo.
echo Next steps:
echo 1. Open http://localhost:8080 in your browser
echo 2. Paste the initial admin password
echo 3. Follow the setup wizard
echo 4. Refer to JENKINS_SETUP.md for detailed configuration
echo.
echo =========================================
pause
exit /b 0

:password_error
echo.
echo ! Warning: Could not retrieve initial admin password automatically
echo Please run this command manually:
echo docker exec jenkins-cicd cat /var/jenkins_home/secrets/initialAdminPassword
echo.
echo =========================================
pause
exit /b 1

