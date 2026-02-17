# Internal Kafka traffic within VPC
resource "digitalocean_firewall" "kafka_internal" {
  name = "${var.project_name}-internal"

  droplet_ids = concat(
    digitalocean_droplet.controllers[*].id,
    digitalocean_droplet.brokers[*].id,
  )

  # Controller quorum (9094)
  inbound_rule {
    protocol         = "tcp"
    port_range       = "9094"
    source_addresses = [digitalocean_vpc.kafka.ip_range]
  }

  # Inter-broker (9092)
  inbound_rule {
    protocol         = "tcp"
    port_range       = "9092"
    source_addresses = [digitalocean_vpc.kafka.ip_range]
  }

  # Client listener (9093) - VPC + DOKS nodes
  inbound_rule {
    protocol         = "tcp"
    port_range       = "9093"
    source_addresses = [digitalocean_vpc.kafka.ip_range]
  }

  # JMX exporter (7071) - Prometheus scraping
  inbound_rule {
    protocol         = "tcp"
    port_range       = "7071"
    source_addresses = [digitalocean_vpc.kafka.ip_range]
  }

  # All outbound
  outbound_rule {
    protocol              = "tcp"
    port_range            = "1-65535"
    destination_addresses = ["0.0.0.0/0", "::/0"]
  }

  outbound_rule {
    protocol              = "udp"
    port_range            = "1-65535"
    destination_addresses = ["0.0.0.0/0", "::/0"]
  }

  outbound_rule {
    protocol              = "icmp"
    destination_addresses = ["0.0.0.0/0", "::/0"]
  }
}

# SSH access from admin CIDRs
resource "digitalocean_firewall" "ssh" {
  name = "${var.project_name}-ssh"

  droplet_ids = concat(
    digitalocean_droplet.controllers[*].id,
    digitalocean_droplet.brokers[*].id,
  )

  inbound_rule {
    protocol         = "tcp"
    port_range       = "22"
    source_addresses = var.admin_cidrs
  }
}
