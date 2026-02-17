# Generate Ansible inventory from Terraform-provisioned Droplets
resource "local_file" "ansible_inventory" {
  filename        = "${path.module}/generated/inventory.yml"
  file_permission = "0644"

  content = yamlencode({
    all = {
      vars = {
        ansible_user                 = "root"
        ansible_ssh_common_args      = "-o StrictHostKeyChecking=no"
        kafka_vpc_network            = digitalocean_vpc.kafka.ip_range
        kafka_broker_private_ips     = [for b in digitalocean_droplet.brokers : b.ipv4_address_private]
        kafka_controller_private_ips = [for c in digitalocean_droplet.controllers : c.ipv4_address_private]
      }
      children = {
        kafka_controllers = {
          hosts = {
            for i, d in digitalocean_droplet.controllers :
            d.name => {
              ansible_host              = d.ipv4_address
              private_ip                = d.ipv4_address_private
              kafka_node_id             = i + 1
              kafka_controller_quorum   = join(",", [
                for j, c in digitalocean_droplet.controllers :
                "${j + 1}@${c.ipv4_address_private}:9094"
              ])
            }
          }
        }
        kafka_brokers = {
          hosts = {
            for i, d in digitalocean_droplet.brokers :
            d.name => {
              ansible_host              = d.ipv4_address
              private_ip                = d.ipv4_address_private
              kafka_node_id             = i + 101
              kafka_controller_quorum   = join(",", [
                for j, c in digitalocean_droplet.controllers :
                "${j + 1}@${c.ipv4_address_private}:9094"
              ])
            }
          }
        }
      }
    }
  })
}

resource "local_file" "kubeconfig" {
  filename        = "${path.module}/generated/kubeconfig.yaml"
  file_permission = "0600"
  content         = digitalocean_kubernetes_cluster.kafka_services.kube_config[0].raw_config
}
