terraform {
  required_version = ">= 1.5.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = ">= 5.60.0, < 6.0.0"
    }
    tls = {
      source  = "hashicorp/tls"
      version = ">= 4.0.0"
    }
  }

  # ---------------------------------------------------------------------------
  # Remote state backend. Commented out intentionally: this repository is
  # generation-only (no apply). Fill in a pre-existing, encrypted, versioned
  # S3 bucket + DynamoDB lock table before running `terraform init` for real.
  # ---------------------------------------------------------------------------
  # backend "s3" {
  #   bucket         = "jefferies-bankapp-tfstate"
  #   key            = "prod/terraform.tfstate"
  #   region         = "us-west-1"
  #   dynamodb_table = "jefferies-bankapp-tflock"
  #   encrypt        = true
  # }
}

provider "aws" {
  region = var.region

  default_tags {
    tags = local.common_tags
  }
}
