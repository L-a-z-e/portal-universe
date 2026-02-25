# ===== SQS Queues =====
# 03-init-sqs.sh + 05-init-eventbridge.sh (SQS targets) + 07-init-cloudwatch.sh에 대응

# --- Email Processing ---

resource "aws_sqs_queue" "email_dlq" {
  name = "email-dlq"
}

resource "aws_sqs_queue" "email_queue" {
  name                       = "email-queue"
  visibility_timeout_seconds = 60
  message_retention_seconds  = 86400

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.email_dlq.arn
    maxReceiveCount     = 3
  })
}

# --- EventBridge Targets ---

resource "aws_sqs_queue" "high_value_order_dlq" {
  name = "high-value-order-dlq"
}

resource "aws_sqs_queue" "high_value_order" {
  name = "high-value-order-queue"

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.high_value_order_dlq.arn
    maxReceiveCount     = 3
  })
}

resource "aws_sqs_queue" "order_confirmation_dlq" {
  name = "order-confirmation-dlq"
}

resource "aws_sqs_queue" "order_confirmation" {
  name = "order-confirmation-queue"

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.order_confirmation_dlq.arn
    maxReceiveCount     = 3
  })
}

# --- Lambda DLQ ---

resource "aws_sqs_queue" "image_thumbnail_dlq" {
  name = "image-thumbnail-dlq"
}

# --- CloudWatch Alarm Verification ---

resource "aws_sqs_queue" "cloudwatch_alarm" {
  name = "cloudwatch-alarm-queue"
}
