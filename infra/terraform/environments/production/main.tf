# Production Environment
# Usage: terraform init && terraform plan -var-file="production.tfvars"

module "root" {
  source = "../../"

  environment  = "production"
  project_name = "portal-universe"
  aws_region   = "ap-northeast-2"
}
