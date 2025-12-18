#!/bin/bash
# Setup script for ML service on EC2
# Run this ONCE on the EC2 instance to prepare for ML deployment

echo "🔧 Setting up ML service environment..."

# 1. Create 2GB swap file to prevent OOM kills during pip install
echo "📦 Creating 2GB swap file..."
sudo dd if=/dev/zero of=/swapfile bs=128M count=16
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile

# Make swap permanent across reboots
echo '/swapfile swap swap defaults 0 0' | sudo tee -a /etc/fstab

# 2. Verify swap is active
echo "✅ Swap status:"
free -h

# 3. Install Python dependencies with no cache to save memory
echo "📥 Installing ML dependencies..."
cd /home/ec2-user/ml-service
pip3 install --no-cache-dir -r requirements.txt

echo "✅ ML service setup complete!"
echo "🚀 Start the service with: python3 app.py"
