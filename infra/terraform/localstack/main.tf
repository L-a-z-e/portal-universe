terraform {
  required_version = ">= 1.5.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

# LocalStack Provider 설정
# tflocal이 endpoint를 자동 오버라이드하지만, 명시적으로도 설정
provider "aws" {
  region                      = var.aws_region
  access_key                  = "test"
  secret_key                  = "test"
  skip_credentials_validation = true
  skip_metadata_api_check     = true
  skip_requesting_account_id  = true

  endpoints {
    s3           = var.localstack_endpoint
    sqs          = var.localstack_endpoint
    sns          = var.localstack_endpoint
    iam          = var.localstack_endpoint
    sts          = var.localstack_endpoint
    lambda       = var.localstack_endpoint
    cloudwatch   = var.localstack_endpoint
    events       = var.localstack_endpoint
  }

  default_tags {
    tags = {
      Project   = var.project_name
      ManagedBy = "terraform"
    }
  }
}
