terraform {
  required_version = ">= 1.5"

  required_providers {
    digitalocean = {
      source  = "digitalocean/digitalocean"
      version = "~> 2.40"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.15"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.32"
    }
    local = {
      source  = "hashicorp/local"
      version = "~> 2.5"
    }
  }
}

provider "digitalocean" {
  token = var.do_token
}

provider "kubernetes" {
  host                   = digitalocean_kubernetes_cluster.kafka_services.endpoint
  token                  = digitalocean_kubernetes_cluster.kafka_services.kube_config[0].token
  cluster_ca_certificate = base64decode(digitalocean_kubernetes_cluster.kafka_services.kube_config[0].cluster_ca_certificate)
}

provider "helm" {
  kubernetes {
    host                   = digitalocean_kubernetes_cluster.kafka_services.endpoint
    token                  = digitalocean_kubernetes_cluster.kafka_services.kube_config[0].token
    cluster_ca_certificate = base64decode(digitalocean_kubernetes_cluster.kafka_services.kube_config[0].cluster_ca_certificate)
  }
}

resource "digitalocean_project" "kafka" {
  name        = var.project_name
  description = "Kafka mTLS Platform"
  purpose     = "Service or API"
  environment = var.environment

  resources = concat(
    [for d in digitalocean_droplet.controllers : d.urn],
    [for d in digitalocean_droplet.brokers : d.urn],
    [digitalocean_kubernetes_cluster.kafka_services.urn],
  )
}
