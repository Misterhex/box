variable "do_token" {
  description = "DigitalOcean API token"
  type        = string
  sensitive   = true
}

variable "project_name" {
  description = "DigitalOcean project name"
  type        = string
  default     = "kafka-mtls-platform"
}

variable "environment" {
  description = "Environment (Development, Staging, Production)"
  type        = string
  default     = "Development"
}

variable "region" {
  description = "DigitalOcean region"
  type        = string
  default     = "sgp1"
}

# --- SSH ---

variable "ssh_key_fingerprints" {
  description = "SSH key fingerprints to add to Droplets"
  type        = list(string)
}

variable "admin_cidrs" {
  description = "CIDRs allowed SSH access to Droplets"
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

# --- Droplet sizes ---

variable "controller_size" {
  description = "Droplet size for Kafka controllers"
  type        = string
  default     = "s-1vcpu-2gb"
}

variable "broker_size" {
  description = "Droplet size for Kafka brokers"
  type        = string
  default     = "s-2vcpu-4gb"
}

variable "controller_count" {
  description = "Number of Kafka controllers"
  type        = number
  default     = 3
}

variable "broker_count" {
  description = "Number of Kafka brokers"
  type        = number
  default     = 3
}

# --- Kubernetes ---

variable "k8s_version" {
  description = "DOKS Kubernetes version prefix (latest patch auto-selected)"
  type        = string
  default     = "1.31"
}

variable "k8s_node_size" {
  description = "DOKS worker node size"
  type        = string
  default     = "s-2vcpu-4gb"
}

variable "k8s_node_count" {
  description = "DOKS worker node count"
  type        = number
  default     = 2
}

# --- Domain / Ingress ---

variable "domain" {
  description = "Base domain for ingress (e.g. example.com). Leave empty to use Load Balancer IP directly."
  type        = string
  default     = ""
}
