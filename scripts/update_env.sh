#!/bin/bash

# Script to update systemd service environment variables
# Usage: ./update_env.sh <MAIL_USERNAME> <MAIL_PASSWORD>

USERNAME=$1
PASSWORD=$2
SERVICE_FILE="/etc/systemd/system/talentsync-backend.service"

if [ -z "$USERNAME" ] || [ -z "$PASSWORD" ]; then
  echo "Error: Credentials not provided"
  exit 1
fi

echo "Updating Environment variables in $SERVICE_FILE..."

# Remove existing MAIL configurations to avoid duplicates
sudo sed -i '/Environment="MAIL_/d' $SERVICE_FILE

# Insert new configurations after [Service] block
# We use a temporary file to construct the block safely
# Note: sed 'a' appends after the match. We verify [Service] exists.

if grep -q "\[Service\]" "$SERVICE_FILE"; then
    sudo sed -i "/\[Service\]/a Environment=\"MAIL_PASSWORD=$PASSWORD\"" $SERVICE_FILE
    sudo sed -i "/\[Service\]/a Environment=\"MAIL_USERNAME=$USERNAME\"" $SERVICE_FILE
    
    echo "Reloading systemd daemon..."
    sudo systemctl daemon-reload
    
    echo "Restarting service..."
    sudo systemctl restart talentsync-backend
    
    echo "Deployment configuration updated successfully."
else
    echo "Error: [Service] block not found in $SERVICE_FILE"
    exit 1
fi
