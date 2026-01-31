# Portal Universe - Terraform Root Module
# Environment-specific configurations are in environments/ directory

terraform {
  required_version = ">= 1.5.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.25"
    }
  }

  # Remote state backend (uncomment and configure for production)
  # backend "s3" {
  #   bucket         = "portal-universe-tfstate"
  #   key            = "terraform.tfstate"
  #   region         = "ap-northeast-2"
  #   dynamodb_table = "terraform-lock"
  #   encrypt        = true
  # }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = "portal-universe"
      ManagedBy   = "terraform"
      Environment = var.environment
    }
  }
}
