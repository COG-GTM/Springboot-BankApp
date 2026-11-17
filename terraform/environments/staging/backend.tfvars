bucket         = "bankapp-terraform-state-staging"
key            = "staging/terraform.tfstate"
region         = "us-east-1"
dynamodb_table = "bankapp-terraform-locks-staging"
encrypt        = true
