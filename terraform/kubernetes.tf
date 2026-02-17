data "digitalocean_kubernetes_versions" "current" {
  version_prefix = var.k8s_version
}

resource "digitalocean_kubernetes_cluster" "kafka_services" {
  name     = "${var.project_name}-k8s"
  region   = var.region
  version  = data.digitalocean_kubernetes_versions.current.latest_version
  vpc_uuid = digitalocean_vpc.kafka.id

  node_pool {
    name       = "default"
    size       = var.k8s_node_size
    node_count = var.k8s_node_count

    tags = [
      "kafka-services",
      var.project_name,
    ]
  }
}
