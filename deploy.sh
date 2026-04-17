#!/bin/bash
set -e

# ── Config ────────────────────────────────────────────────────────────────────
PEM_FILE="$HOME/Downloads/Diamler-Dev.pem"
EC2_USER="ec2-user"
EC2_HOST="65.2.143.213"
REMOTE_DIR="/opt/harness"
JAR_NAME="daimler-ar-wiring-harness.jar"
SERVICE_NAME="harness"

SSH="ssh -i $PEM_FILE -o StrictHostKeyChecking=no $EC2_USER@$EC2_HOST"
SCP="scp -i $PEM_FILE -o StrictHostKeyChecking=no"

# ── Step 1: Build ─────────────────────────────────────────────────────────────
echo ">>> Building JAR..."
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
./mvnw clean package -DskipTests -q
echo ">>> Build complete."

# ── Step 2: Find the JAR ──────────────────────────────────────────────────────
JAR_PATH=$(ls target/*.jar | head -1)
echo ">>> JAR: $JAR_PATH"

# ── Step 3: Upload to EC2 ─────────────────────────────────────────────────────
echo ">>> Uploading to EC2..."
$SSH "mkdir -p $REMOTE_DIR"
$SCP "$JAR_PATH" "$EC2_USER@$EC2_HOST:$REMOTE_DIR/$JAR_NAME"
echo ">>> Upload complete."

# ── Step 4: Restart service ───────────────────────────────────────────────────
echo ">>> Restarting service on EC2..."
$SSH "sudo systemctl restart $SERVICE_NAME"
echo ">>> Service restarted."

# ── Step 5: Check status ──────────────────────────────────────────────────────
sleep 3
$SSH "sudo systemctl status $SERVICE_NAME --no-pager"

echo ""
echo ">>> Deploy complete! App running at http://$EC2_HOST:8080"
