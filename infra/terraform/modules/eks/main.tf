# ---------------------------------------------------------------------------
# Control-plane IAM role
# ---------------------------------------------------------------------------
data "aws_iam_policy_document" "cluster_assume" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["eks.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "cluster" {
  name               = "${var.cluster_name}-cluster-role"
  assume_role_policy = data.aws_iam_policy_document.cluster_assume.json

  tags = var.tags
}

resource "aws_iam_role_policy_attachment" "cluster_policy" {
  role       = aws_iam_role.cluster.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSClusterPolicy"
}

resource "aws_iam_role_policy_attachment" "cluster_vpc_controller" {
  role       = aws_iam_role.cluster.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSVPCResourceController"
}

# ---------------------------------------------------------------------------
# Control-plane log group (must exist before the cluster so logs are captured)
# ---------------------------------------------------------------------------
resource "aws_cloudwatch_log_group" "cluster" {
  name              = "/aws/eks/${var.cluster_name}/cluster"
  retention_in_days = var.log_retention_days
  kms_key_id        = var.kms_key_arn

  tags = var.tags
}

# ---------------------------------------------------------------------------
# Cluster security group
# ---------------------------------------------------------------------------
resource "aws_security_group" "cluster" {
  name        = "${var.cluster_name}-cluster-sg"
  description = "EKS control-plane / node shared security group"
  vpc_id      = data.aws_subnet.first.vpc_id

  tags = merge(var.tags, {
    Name = "${var.cluster_name}-cluster-sg"
  })
}

data "aws_subnet" "first" {
  id = var.subnet_ids[0]
}

# Worker nodes need outbound internet (via NAT) to pull container images from
# ECR/registries, reach the EKS API, and fetch OS/security updates. Egress is
# unavoidable for a functioning managed node group; ingress remains locked down.
# tfsec:ignore:aws-ec2-no-public-egress-sgr
resource "aws_security_group_rule" "cluster_egress" {
  type              = "egress"
  description       = "Allow all outbound (nodes pull images / reach AWS APIs via NAT)"
  security_group_id = aws_security_group.cluster.id
  protocol          = "-1"
  from_port         = 0
  to_port           = 0
  cidr_blocks       = ["0.0.0.0/0"]
  # checkov:skip=CKV_AWS_382: EKS worker nodes require outbound internet egress via NAT.
}

resource "aws_security_group_rule" "cluster_ingress_self" {
  type                     = "ingress"
  description              = "Allow nodes and control plane to communicate with each other"
  security_group_id        = aws_security_group.cluster.id
  protocol                 = "-1"
  from_port                = 0
  to_port                  = 0
  source_security_group_id = aws_security_group.cluster.id
}

# ---------------------------------------------------------------------------
# EKS cluster - secrets envelope encryption + full control-plane logging +
# private endpoint by default.
# ---------------------------------------------------------------------------
resource "aws_eks_cluster" "this" {
  name     = var.cluster_name
  version  = var.kubernetes_version
  role_arn = aws_iam_role.cluster.arn

  enabled_cluster_log_types = var.enabled_cluster_log_types

  vpc_config {
    subnet_ids              = var.subnet_ids
    security_group_ids      = [aws_security_group.cluster.id]
    endpoint_private_access = true
    endpoint_public_access  = var.endpoint_public_access
    public_access_cidrs     = var.endpoint_public_access ? var.public_access_cidrs : null
  }

  encryption_config {
    provider {
      key_arn = var.kms_key_arn
    }
    resources = ["secrets"]
  }

  tags = var.tags

  depends_on = [
    aws_iam_role_policy_attachment.cluster_policy,
    aws_iam_role_policy_attachment.cluster_vpc_controller,
    aws_cloudwatch_log_group.cluster,
  ]
}

# ---------------------------------------------------------------------------
# IRSA - OIDC provider so service accounts can assume least-privilege IAM roles
# ---------------------------------------------------------------------------
data "tls_certificate" "oidc" {
  url = aws_eks_cluster.this.identity[0].oidc[0].issuer
}

resource "aws_iam_openid_connect_provider" "this" {
  url             = aws_eks_cluster.this.identity[0].oidc[0].issuer
  client_id_list  = ["sts.amazonaws.com"]
  thumbprint_list = [data.tls_certificate.oidc.certificates[0].sha1_fingerprint]

  tags = var.tags
}

# ---------------------------------------------------------------------------
# Node group IAM role
# ---------------------------------------------------------------------------
data "aws_iam_policy_document" "node_assume" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "node" {
  name               = "${var.cluster_name}-node-role"
  assume_role_policy = data.aws_iam_policy_document.node_assume.json

  tags = var.tags
}

resource "aws_iam_role_policy_attachment" "node_worker" {
  role       = aws_iam_role.node.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSWorkerNodePolicy"
}

resource "aws_iam_role_policy_attachment" "node_cni" {
  role       = aws_iam_role.node.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKS_CNI_Policy"
}

resource "aws_iam_role_policy_attachment" "node_ecr" {
  role       = aws_iam_role.node.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
}

# ---------------------------------------------------------------------------
# Launch template - encrypted EBS, IMDSv2 required, no public IPs
# ---------------------------------------------------------------------------
resource "aws_launch_template" "node" {
  name_prefix = "${var.cluster_name}-node-"

  block_device_mappings {
    device_name = "/dev/xvda"

    ebs {
      volume_size           = var.node_disk_size
      volume_type           = "gp3"
      encrypted             = true
      kms_key_id            = var.kms_key_arn
      delete_on_termination = true
    }
  }

  metadata_options {
    http_endpoint = "enabled"
    http_tokens   = "required"
    # Hop limit 1 stops pods from reaching the node IMDS; workloads must use
    # IRSA for AWS access instead of the node instance role.
    http_put_response_hop_limit = 1
  }

  monitoring {
    enabled = true
  }

  tag_specifications {
    resource_type = "instance"
    tags = merge(var.tags, {
      Name = "${var.cluster_name}-node"
    })
  }

  tags = var.tags
}

# ---------------------------------------------------------------------------
# Managed node group - private subnets only, approved instance types
# ---------------------------------------------------------------------------
resource "aws_eks_node_group" "this" {
  cluster_name    = aws_eks_cluster.this.name
  node_group_name = "${var.cluster_name}-ng"
  node_role_arn   = aws_iam_role.node.arn
  subnet_ids      = var.subnet_ids
  instance_types  = var.node_instance_types

  scaling_config {
    desired_size = var.node_desired_size
    min_size     = var.node_min_size
    max_size     = var.node_max_size
  }

  update_config {
    max_unavailable = 1
  }

  launch_template {
    id      = aws_launch_template.node.id
    version = aws_launch_template.node.latest_version
  }

  tags = var.tags

  depends_on = [
    aws_iam_role_policy_attachment.node_worker,
    aws_iam_role_policy_attachment.node_cni,
    aws_iam_role_policy_attachment.node_ecr,
  ]
}
