#!/bin/bash
# Run this ONCE on a fresh EC2 instance to set up the server.
# Usage: bash setup-ec2.sh
set -e

REMOTE_DIR="/opt/harness"
JAR_NAME="daimler-ar-wiring-harness.jar"
SERVICE_NAME="harness"

# ── Install Java 17 (Amazon Corretto) ─────────────────────────────────────────
echo ">>> Installing Java 17..."
sudo yum install -y java-17-amazon-corretto

# ── Create app directory ──────────────────────────────────────────────────────
echo ">>> Creating $REMOTE_DIR..."
sudo mkdir -p $REMOTE_DIR
sudo chown ec2-user:ec2-user $REMOTE_DIR

# ── Create environment file (edit with your actual values) ───────────────────
echo ">>> Creating environment config..."
sudo tee /opt/harness/.env > /dev/null <<'EOF'
AWS_ACCESS_KEY_ID=CHANGE_ME
AWS_SECRET_ACCESS_KEY=CHANGE_ME
SPRING_PROFILES_ACTIVE=prod
EOF
sudo chmod 600 /opt/harness/.env

# ── Create systemd service ────────────────────────────────────────────────────
echo ">>> Creating systemd service..."
sudo tee /etc/systemd/system/$SERVICE_NAME.service > /dev/null <<EOF
[Unit]
Description=Daimler AR Wiring Harness Application
After=network.target

[Service]
User=ec2-user
WorkingDirectory=$REMOTE_DIR
EnvironmentFile=$REMOTE_DIR/.env
ExecStart=/usr/bin/java -jar $REMOTE_DIR/$JAR_NAME --spring.profiles.active=prod
SuccessExitStatus=143
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=$SERVICE_NAME

[Install]
WantedBy=multi-user.target
EOF

# ── Enable service ────────────────────────────────────────────────────────────
sudo systemctl daemon-reload
sudo systemctl enable $SERVICE_NAME

echo ""
echo ">>> EC2 setup complete!"
echo ">>> Edit /opt/harness/.env with your AWS credentials, then run deploy.sh"
