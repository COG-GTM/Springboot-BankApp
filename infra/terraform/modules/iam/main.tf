locals {
  oidc_sub = "${var.oidc_provider_url}:sub"
  oidc_aud = "${var.oidc_provider_url}:aud"
}

# ---------------------------------------------------------------------------
# IRSA trust policy - only the named service account in the named namespace
# on this specific cluster may assume the role.
# ---------------------------------------------------------------------------
data "aws_iam_policy_document" "assume" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [var.oidc_provider_arn]
    }

    condition {
      test     = "StringEquals"
      variable = local.oidc_sub
      values   = ["system:serviceaccount:${var.namespace}:${var.service_account}"]
    }

    condition {
      test     = "StringEquals"
      variable = local.oidc_aud
      values   = ["sts.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "this" {
  name                 = var.role_name
  assume_role_policy   = data.aws_iam_policy_document.assume.json
  max_session_duration = 3600

  tags = var.tags
}

# ---------------------------------------------------------------------------
# Least-privilege policy - read only the exact secrets, decrypt only the exact
# keys. No "*" actions or resources.
# ---------------------------------------------------------------------------
data "aws_iam_policy_document" "permissions" {
  count = length(var.secret_arns) > 0 ? 1 : 0

  statement {
    sid    = "ReadDatabaseSecret"
    effect = "Allow"
    actions = [
      "secretsmanager:GetSecretValue",
      "secretsmanager:DescribeSecret",
    ]
    resources = var.secret_arns
  }

  dynamic "statement" {
    for_each = length(var.kms_key_arns) > 0 ? [1] : []
    content {
      sid    = "DecryptSecret"
      effect = "Allow"
      actions = [
        "kms:Decrypt",
        "kms:DescribeKey",
      ]
      resources = var.kms_key_arns
    }
  }
}

resource "aws_iam_policy" "this" {
  count = length(var.secret_arns) > 0 ? 1 : 0

  name        = "${var.role_name}-policy"
  description = "Least-privilege access for ${var.role_name}"
  policy      = data.aws_iam_policy_document.permissions[0].json

  tags = var.tags
}

resource "aws_iam_role_policy_attachment" "this" {
  count = length(var.secret_arns) > 0 ? 1 : 0

  role       = aws_iam_role.this.name
  policy_arn = aws_iam_policy.this[0].arn
}
