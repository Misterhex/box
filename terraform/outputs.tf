# --- Droplet IPs ---

output "controller_public_ips" {
  description = "Public IPs of Kafka controller Droplets"
  value       = { for d in digitalocean_droplet.controllers : d.name => d.ipv4_address }
}

output "controller_private_ips" {
  description = "Private (VPC) IPs of Kafka controller Droplets"
  value       = { for d in digitalocean_droplet.controllers : d.name => d.ipv4_address_private }
}

output "broker_public_ips" {
  description = "Public IPs of Kafka broker Droplets"
  value       = { for d in digitalocean_droplet.brokers : d.name => d.ipv4_address }
}

output "broker_private_ips" {
  description = "Private (VPC) IPs of Kafka broker Droplets"
  value       = { for d in digitalocean_droplet.brokers : d.name => d.ipv4_address_private }
}

# --- Kubernetes ---

output "k8s_cluster_id" {
  description = "DOKS cluster ID"
  value       = digitalocean_kubernetes_cluster.kafka_services.id
}

output "k8s_endpoint" {
  description = "DOKS cluster API endpoint"
  value       = digitalocean_kubernetes_cluster.kafka_services.endpoint
}

output "kubeconfig_path" {
  description = "Path to generated kubeconfig file"
  value       = local_file.kubeconfig.filename
}

# --- Ansible ---

output "ansible_inventory_path" {
  description = "Path to generated Ansible inventory"
  value       = local_file.ansible_inventory.filename
}

# --- Ingress ---

output "ingress_load_balancer_ip" {
  description = "Load balancer IP for NGINX ingress (use for DNS A records)"
  value       = "Run: kubectl -n ingress-nginx get svc ingress-nginx-controller -o jsonpath='{.status.loadBalancer.ingress[0].ip}'"
}

output "service_urls" {
  description = "URLs for deployed services"
  value = var.domain != "" ? {
    kafka_ui        = "http://kafka-ui.${var.domain}"
    schema_registry = "http://schema-registry.${var.domain}"
    prometheus      = "http://prometheus.${var.domain}"
  } : {
    kafka_ui        = "http://<LOAD_BALANCER_IP> (set Host: kafka-ui header, or configure var.domain)"
    schema_registry = "http://<LOAD_BALANCER_IP> (set Host: schema-registry header)"
    prometheus      = "http://<LOAD_BALANCER_IP> (set Host: prometheus header)"
  }
}
