region             = "us-west-1"
name_prefix        = "bankapp-prod"
cluster_name       = "bankapp"
kubernetes_version = "1.31"

vpc_cidr = "10.0.0.0/16"
azs      = ["us-west-1a", "us-west-1c"]

# Private-only API endpoint. To allow bastion/CI access, set this to true and
# provide the specific corporate egress CIDRs below (never 0.0.0.0/0).
endpoint_public_access = false
public_access_cidrs    = []

node_instance_types = ["m6i.large"]
db_instance_class   = "db.t3.medium"

owner               = "platform-engineering"
cost_center         = "public-cloud-platform"
data_classification = "confidential"
