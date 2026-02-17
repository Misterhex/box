#!/bin/bash
# Example deployment script for Kafka KRaft cluster
# This script demonstrates the typical deployment workflow

set -e

ANSIBLE_DIR="/home/user/box/ansible"
INVENTORY="${ANSIBLE_DIR}/inventory/hosts.yml"

echo "========================================="
echo "Kafka KRaft Deployment Example"
echo "========================================="
echo ""

# Step 1: Pre-flight checks
echo "Step 1: Pre-flight checks"
echo "-------------------------"

# Check if SSL certificates are in place (example)
echo "⚠️  IMPORTANT: Ensure SSL certificates are deployed to all nodes:"
echo "   - /etc/kafka/ssl/kafka.keystore.jks"
echo "   - /etc/kafka/ssl/kafka.truststore.jks"
echo ""
echo "   See SSL_CERTIFICATE_SETUP.md for instructions."
echo ""

read -p "Have you deployed SSL certificates to all nodes? (yes/no): " SSL_READY
if [ "$SSL_READY" != "yes" ]; then
    echo "❌ Please deploy SSL certificates first!"
    exit 1
fi

# Check if inventory is configured
echo ""
echo "Checking inventory configuration..."
if grep -q "10.0.1.1" "${INVENTORY}"; then
    echo "⚠️  WARNING: Inventory still has placeholder IPs (10.0.x.x)"
    read -p "Continue anyway? (yes/no): " CONTINUE
    if [ "$CONTINUE" != "yes" ]; then
        echo "❌ Please update inventory/hosts.yml with actual IPs"
        exit 1
    fi
fi

# Check if cluster ID is set
CLUSTER_ID=$(grep "kafka_cluster_id:" "${ANSIBLE_DIR}/group_vars/all.yml" | awk '{print $2}')
echo "Cluster ID: ${CLUSTER_ID}"
echo ""

# Step 2: Test connectivity
echo "Step 2: Test connectivity to all nodes"
echo "---------------------------------------"
read -p "Run Ansible ping to verify SSH access? (yes/no): " RUN_PING
if [ "$RUN_PING" == "yes" ]; then
    ansible all -i "${INVENTORY}" -m ping
    echo ""
fi

# Step 3: Deploy controllers
echo "Step 3: Deploy Kafka Controllers"
echo "---------------------------------"
echo "This will deploy Kafka controllers (metadata quorum) to:"
echo "  - controller-1 (node.id=1)"
echo "  - controller-2 (node.id=2)"
echo "  - controller-3 (node.id=3)"
echo ""
read -p "Deploy controllers now? (yes/no): " DEPLOY_CONTROLLERS
if [ "$DEPLOY_CONTROLLERS" == "yes" ]; then
    echo "Deploying controllers..."
    ansible-playbook -i "${INVENTORY}" "${ANSIBLE_DIR}/playbooks/deploy-controllers.yml"
    echo ""
    echo "✓ Controllers deployed"
    echo ""

    # Wait for controllers to stabilize
    echo "Waiting 30 seconds for controller quorum to form..."
    sleep 30

    # Check controller status
    echo "Checking controller status..."
    ansible kafka_controllers -i "${INVENTORY}" -m shell -a "systemctl status kafka" -b || true
    echo ""
fi

# Step 4: Deploy brokers
echo "Step 4: Deploy Kafka Brokers"
echo "-----------------------------"
echo "This will deploy Kafka brokers to:"
echo "  - broker-1 (node.id=101)"
echo "  - broker-2 (node.id=102)"
echo "  - broker-3 (node.id=103)"
echo ""
read -p "Deploy brokers now? (yes/no): " DEPLOY_BROKERS
if [ "$DEPLOY_BROKERS" == "yes" ]; then
    echo "Deploying brokers..."
    ansible-playbook -i "${INVENTORY}" "${ANSIBLE_DIR}/playbooks/deploy-brokers.yml"
    echo ""
    echo "✓ Brokers deployed"
    echo ""

    # Wait for brokers to start
    echo "Waiting 20 seconds for brokers to start..."
    sleep 20

    # Check broker status
    echo "Checking broker status..."
    ansible kafka_brokers -i "${INVENTORY}" -m shell -a "systemctl status kafka" -b || true
    echo ""
fi

# Step 5: Verification
echo "Step 5: Verification"
echo "--------------------"

# Check services
echo "Checking all Kafka services..."
ansible all -i "${INVENTORY}" -m shell -a "systemctl is-active kafka" -b
echo ""

# Check JMX metrics endpoint
echo "Testing JMX metrics endpoints..."
read -p "Enter a controller hostname to test (e.g., controller-1): " CONTROLLER_HOST
if [ ! -z "$CONTROLLER_HOST" ]; then
    echo "Testing http://${CONTROLLER_HOST}:7071/metrics"
    curl -s "http://${CONTROLLER_HOST}:7071/metrics" | head -10 || echo "❌ Failed to reach JMX endpoint"
    echo ""
fi

read -p "Enter a broker hostname to test (e.g., broker-1): " BROKER_HOST
if [ ! -z "$BROKER_HOST" ]; then
    echo "Testing http://${BROKER_HOST}:7071/metrics"
    curl -s "http://${BROKER_HOST}:7071/metrics" | head -10 || echo "❌ Failed to reach JMX endpoint"
    echo ""
fi

# Step 6: View logs
echo "Step 6: View recent logs"
echo "------------------------"
read -p "View recent Kafka logs? (yes/no): " VIEW_LOGS
if [ "$VIEW_LOGS" == "yes" ]; then
    echo "Last 20 lines from each node:"
    ansible all -i "${INVENTORY}" -m shell -a "journalctl -u kafka -n 20 --no-pager" -b
    echo ""
fi

# Summary
echo "========================================="
echo "Deployment Complete!"
echo "========================================="
echo ""
echo "Next steps:"
echo "1. Configure ACLs for your applications"
echo "2. Set up Prometheus to scrape JMX metrics (port 7071)"
echo "3. Create test topics and verify replication"
echo "4. Configure your applications to connect with mTLS"
echo ""
echo "Example: Create a test topic"
echo "----------------------------"
echo "SSH to any broker and run:"
echo ""
cat << 'TOPIC_EXAMPLE'
# Create client SSL properties
cat > /tmp/client-ssl.properties <<EOF
security.protocol=SSL
ssl.truststore.location=/etc/kafka/ssl/kafka.truststore.jks
ssl.truststore.password=changeit
ssl.keystore.location=/etc/kafka/ssl/kafka.keystore.jks
ssl.keystore.password=changeit
ssl.key.password=changeit
EOF

# Create topic
/opt/kafka/kafka_2.13-3.8.1/bin/kafka-topics.sh --create \
  --bootstrap-server localhost:9093 \
  --topic test-topic \
  --partitions 3 \
  --replication-factor 3 \
  --command-config /tmp/client-ssl.properties

# List topics
/opt/kafka/kafka_2.13-3.8.1/bin/kafka-topics.sh --list \
  --bootstrap-server localhost:9093 \
  --command-config /tmp/client-ssl.properties

# Describe topic
/opt/kafka/kafka_2.13-3.8.1/bin/kafka-topics.sh --describe \
  --bootstrap-server localhost:9093 \
  --topic test-topic \
  --command-config /tmp/client-ssl.properties
TOPIC_EXAMPLE

echo ""
echo "For full documentation, see:"
echo "  - README.md"
echo "  - QUICKSTART.md"
echo "  - DEPLOYMENT_SUMMARY.md"
echo ""
