variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "ap-northeast-2"
}

variable "project_name" {
  description = "Project name used for resource naming"
  type        = string
  default     = "portal-universe"
}

variable "localstack_endpoint" {
  description = "LocalStack endpoint URL"
  type        = string
  default     = "http://localhost:4566"
}

variable "lambda_package_path" {
  description = "Path to Lambda deployment package (package.zip)"
  type        = string
  default     = "../../../lambda/image-thumbnail/package.zip"
}
