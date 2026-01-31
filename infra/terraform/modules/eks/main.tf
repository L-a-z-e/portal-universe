# EKS Cluster Module
# Usage:
#   module "eks" {
#     source             = "./modules/eks"
#     environment        = var.environment
#     project_name       = var.project_name
#     kubernetes_version = "1.29"
#     subnet_ids         = module.vpc.private_subnet_ids
#     vpc_id             = module.vpc.vpc_id
#   }

resource "aws_eks_cluster" "this" {
  name     = "${var.environment}-${var.project_name}-eks"
  role_arn = aws_iam_role.cluster.arn
  version  = var.kubernetes_version

  vpc_config {
    subnet_ids              = var.subnet_ids
    endpoint_private_access = true
    endpoint_public_access  = var.environment == "staging" ? true : false
  }

  tags = {
    Environment = var.environment
    Project     = var.project_name
  }

  depends_on = [
    aws_iam_role_policy_attachment.cluster_policy,
  ]
}

resource "aws_iam_role" "cluster" {
  name = "${var.environment}-${var.project_name}-eks-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "eks.amazonaws.com"
      }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "cluster_policy" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSClusterPolicy"
  role       = aws_iam_role.cluster.name
}
