#!/bin/bash
# Manual cleanup script for orphaned DB Subnet Groups

echo "Cleaning up orphaned DB Subnet Groups..."

# Delete the specific DB subnet group
aws rds delete-db-subnet-group \
  --region us-east-1 \
  --db-subnet-group-name achat-app-db-subnet-group

echo "Done! DB Subnet Group 'achat-app-db-subnet-group' deleted."
