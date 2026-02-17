# -------------------------------------------------------
# NGINX Ingress Controller (DigitalOcean Load Balancer)
# -------------------------------------------------------
resource "helm_release" "ingress_nginx" {
  name             = "ingress-nginx"
  repository       = "https://kubernetes.github.io/ingress-nginx"
  chart            = "ingress-nginx"
  version          = "4.11.3"
  namespace        = "ingress-nginx"
  create_namespace = true

  set {
    name  = "controller.service.type"
    value = "LoadBalancer"
  }

  set {
    name  = "controller.service.annotations.service\\.beta\\.kubernetes\\.io/do-loadbalancer-name"
    value = "${var.project_name}-lb"
  }

  set {
    name  = "controller.service.annotations.service\\.beta\\.kubernetes\\.io/do-loadbalancer-size-unit"
    value = "1"
  }

  depends_on = [digitalocean_kubernetes_cluster.kafka_services]
}

# -------------------------------------------------------
# Kafka namespace
# -------------------------------------------------------
resource "kubernetes_namespace" "kafka" {
  metadata {
    name = "kafka"
  }

  depends_on = [digitalocean_kubernetes_cluster.kafka_services]
}

# -------------------------------------------------------
# Schema Registry
# -------------------------------------------------------
resource "helm_release" "schema_registry" {
  name      = "schema-registry"
  chart     = "${path.module}/../charts/schema-registry"
  namespace = kubernetes_namespace.kafka.metadata[0].name

  set {
    name  = "kafka.bootstrapServers"
    value = join(",", [for b in digitalocean_droplet.brokers : "${b.ipv4_address_private}:9093"])
  }

  set {
    name  = "ingress.enabled"
    value = "true"
  }

  set {
    name  = "ingress.className"
    value = "nginx"
  }

  set {
    name  = "ingress.hosts[0].host"
    value = var.domain != "" ? "schema-registry.${var.domain}" : ""
  }

  set {
    name  = "ingress.hosts[0].paths[0].path"
    value = "/"
  }

  set {
    name  = "ingress.hosts[0].paths[0].pathType"
    value = "Prefix"
  }

  depends_on = [helm_release.ingress_nginx]
}

# -------------------------------------------------------
# Kafka UI
# -------------------------------------------------------
resource "helm_release" "kafka_ui" {
  name      = "kafka-ui"
  chart     = "${path.module}/../charts/kafka-ui"
  namespace = kubernetes_namespace.kafka.metadata[0].name

  set {
    name  = "kafka.bootstrapServers"
    value = join(",", [for b in digitalocean_droplet.brokers : "${b.ipv4_address_private}:9093"])
  }

  set {
    name  = "schemaRegistry.url"
    value = "http://schema-registry:8081"
  }

  set {
    name  = "ingress.enabled"
    value = "true"
  }

  set {
    name  = "ingress.className"
    value = "nginx"
  }

  set {
    name  = "ingress.hosts[0].host"
    value = var.domain != "" ? "kafka-ui.${var.domain}" : ""
  }

  set {
    name  = "ingress.hosts[0].paths[0].path"
    value = "/"
  }

  set {
    name  = "ingress.hosts[0].paths[0].pathType"
    value = "Prefix"
  }

  depends_on = [helm_release.ingress_nginx, helm_release.schema_registry]
}

# -------------------------------------------------------
# Prometheus
# -------------------------------------------------------
resource "helm_release" "prometheus" {
  name      = "prometheus"
  chart     = "${path.module}/../charts/prometheus"
  namespace = kubernetes_namespace.kafka.metadata[0].name

  # Scrape targets: all broker + controller JMX endpoints on private IPs
  set {
    name  = "scrapeTargets.brokers"
    value = join(",", [for b in digitalocean_droplet.brokers : "${b.ipv4_address_private}:7071"])
  }

  set {
    name  = "scrapeTargets.controllers"
    value = join(",", [for c in digitalocean_droplet.controllers : "${c.ipv4_address_private}:7071"])
  }

  set {
    name  = "ingress.enabled"
    value = "true"
  }

  set {
    name  = "ingress.className"
    value = "nginx"
  }

  set {
    name  = "ingress.hosts[0].host"
    value = var.domain != "" ? "prometheus.${var.domain}" : ""
  }

  set {
    name  = "ingress.hosts[0].paths[0].path"
    value = "/"
  }

  set {
    name  = "ingress.hosts[0].paths[0].pathType"
    value = "Prefix"
  }

  depends_on = [helm_release.ingress_nginx]
}
