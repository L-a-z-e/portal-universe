# RDS Module (MySQL for portal-universe services)
# Usage:
#   module "rds" {
#     source       = "./modules/rds"
#     environment  = var.environment
#     project_name = var.project_name
#     subnet_ids   = module.vpc.private_subnet_ids
#     vpc_id       = module.vpc.vpc_id
#   }

resource "aws_db_instance" "this" {
  identifier = "${var.environment}-${var.project_name}-mysql"

  engine               = "mysql"
  engine_version       = "8.0"
  instance_class       = var.instance_class
  allocated_storage    = var.allocated_storage
  storage_encrypted    = true

  db_name  = var.db_name
  username = var.db_username
  password = var.db_password

  vpc_security_group_ids = [aws_security_group.rds.id]
  db_subnet_group_name   = aws_db_subnet_group.this.name

  multi_az            = var.environment == "production" ? true : false
  skip_final_snapshot = var.environment == "staging" ? true : false

  backup_retention_period = var.environment == "production" ? 7 : 1

  tags = {
    Environment = var.environment
    Project     = var.project_name
  }
}

resource "aws_db_subnet_group" "this" {
  name       = "${var.environment}-${var.project_name}-db-subnet"
  subnet_ids = var.subnet_ids
}

resource "aws_security_group" "rds" {
  name_prefix = "${var.environment}-${var.project_name}-rds-"
  vpc_id      = var.vpc_id

  ingress {
    from_port   = 3306
    to_port     = 3306
    protocol    = "tcp"
    cidr_blocks = var.allowed_cidr_blocks
  }
}
