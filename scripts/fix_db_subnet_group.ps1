# Script to manually delete the leftover DB Subnet Group
# Run this before retrying the Jenkins deployment

Write-Host "Deleting leftover DB Subnet Group..." -ForegroundColor Yellow

# You need to set your AWS credentials first
# Option 1: Use AWS CLI with your credentials
# aws configure set aws_access_key_id YOUR_ACCESS_KEY
# aws configure set aws_secret_access_key YOUR_SECRET_KEY
# aws configure set aws_session_token YOUR_SESSION_TOKEN
# aws configure set region us-east-1

# Delete the DB Subnet Group
docker exec jenkins-with-docker bash -c "aws rds delete-db-subnet-group --region us-east-1 --db-subnet-group-name achat-app-db-subnet-group 2>&1"

if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ DB Subnet Group deleted successfully!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Now you can retry the Jenkins deployment with CLEANUP_AND_DEPLOY mode." -ForegroundColor Cyan
} else {
    Write-Host "✗ Failed to delete DB Subnet Group" -ForegroundColor Red
    Write-Host "The subnet group might already be deleted or the credentials might be expired." -ForegroundColor Yellow
}
