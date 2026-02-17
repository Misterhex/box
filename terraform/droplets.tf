resource "digitalocean_droplet" "controllers" {
  count = var.controller_count

  name     = "kafka-controller-${count.index + 1}"
  region   = var.region
  size     = var.controller_size
  image    = "ubuntu-24-04-x64"
  vpc_uuid = digitalocean_vpc.kafka.id
  ssh_keys = var.ssh_key_fingerprints

  tags = [
    "kafka",
    "controller",
    var.project_name,
  ]

  user_data = <<-EOF
    #!/bin/bash
    set -euo pipefail

    # Set hostname
    hostnamectl set-hostname kafka-controller-${count.index + 1}

    # Install Java 21 (LTS)
    apt-get update -qq
    apt-get install -y -qq openjdk-21-jre-headless

    # Create kafka user
    useradd -r -m -s /bin/false kafka || true

    # Create data directories
    mkdir -p /opt/kafka /var/lib/kafka/data
    chown -R kafka:kafka /var/lib/kafka
  EOF
}

resource "digitalocean_droplet" "brokers" {
  count = var.broker_count

  name     = "kafka-broker-${count.index + 1}"
  region   = var.region
  size     = var.broker_size
  image    = "ubuntu-24-04-x64"
  vpc_uuid = digitalocean_vpc.kafka.id
  ssh_keys = var.ssh_key_fingerprints

  tags = [
    "kafka",
    "broker",
    var.project_name,
  ]

  user_data = <<-EOF
    #!/bin/bash
    set -euo pipefail

    # Set hostname
    hostnamectl set-hostname kafka-broker-${count.index + 1}

    # Install Java 21 (LTS)
    apt-get update -qq
    apt-get install -y -qq openjdk-21-jre-headless

    # Create kafka user
    useradd -r -m -s /bin/false kafka || true

    # Create data directories
    mkdir -p /opt/kafka /var/lib/kafka/data
    chown -R kafka:kafka /var/lib/kafka
  EOF
}
