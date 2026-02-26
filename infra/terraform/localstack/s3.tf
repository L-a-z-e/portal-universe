# ===== S3 Bucket =====
# 02-init-s3.sh에 대응

resource "aws_s3_bucket" "blog" {
  bucket = "blog-bucket"
}

resource "aws_s3_bucket_versioning" "blog" {
  bucket = aws_s3_bucket.blog.id

  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "blog" {
  bucket = aws_s3_bucket.blog.id

  rule {
    id     = "delete-old-versions"
    status = "Enabled"

    noncurrent_version_expiration {
      noncurrent_days = 30
    }
  }
}

resource "aws_s3_bucket_cors_configuration" "blog" {
  bucket = aws_s3_bucket.blog.id

  cors_rule {
    allowed_headers = ["*"]
    allowed_methods = ["GET", "PUT", "POST"]
    allowed_origins = ["http://localhost:30000"]
    max_age_seconds = 3600
  }
}

# ===== Drive Bucket =====

resource "aws_s3_bucket" "drive" {
  bucket = "drive-bucket"
}

resource "aws_s3_bucket_versioning" "drive" {
  bucket = aws_s3_bucket.drive.id

  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "drive" {
  bucket = aws_s3_bucket.drive.id

  rule {
    id     = "delete-old-versions"
    status = "Enabled"

    noncurrent_version_expiration {
      noncurrent_days = 30
    }
  }
}

resource "aws_s3_bucket_cors_configuration" "drive" {
  bucket = aws_s3_bucket.drive.id

  cors_rule {
    allowed_headers = ["*"]
    allowed_methods = ["GET", "PUT", "POST", "DELETE"]
    allowed_origins = ["http://localhost:30000"]
    max_age_seconds = 3600
  }
}
