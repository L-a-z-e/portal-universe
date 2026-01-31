# Staging Environment
# Usage: terraform init && terraform plan -var-file="staging.tfvars"

module "root" {
  source = "../../"

  environment  = "staging"
  project_name = "portal-universe"
  aws_region   = "ap-northeast-2"
}
