# ===== Lambda =====
# 06-init-lambda.sh에 대응

resource "aws_lambda_function" "image_thumbnail" {
  function_name = "image-thumbnail"
  role          = aws_iam_role.lambda_execution.arn
  handler       = "handler.handler"
  runtime       = "python3.11"
  timeout       = 30
  memory_size   = 256

  filename         = var.lambda_package_path
  source_code_hash = filebase64sha256(var.lambda_package_path)

  environment {
    variables = {
      AWS_ENDPOINT_URL   = "http://host.docker.internal:4566"
      AWS_DEFAULT_REGION = var.aws_region
    }
  }

  dead_letter_config {
    target_arn = aws_sqs_queue.image_thumbnail_dlq.arn
  }
}

# S3 → Lambda 호출 권한
resource "aws_lambda_permission" "s3_trigger" {
  statement_id  = "s3-trigger"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.image_thumbnail.function_name
  principal     = "s3.amazonaws.com"
  source_arn    = aws_s3_bucket.blog.arn
}

# S3 Event Notification → Lambda
resource "aws_s3_bucket_notification" "blog_lambda" {
  bucket = aws_s3_bucket.blog.id

  lambda_function {
    lambda_function_arn = aws_lambda_function.image_thumbnail.arn
    events              = ["s3:ObjectCreated:Put", "s3:ObjectCreated:CompleteMultipartUpload"]
  }

  depends_on = [aws_lambda_permission.s3_trigger]
}
