# ===== Outputs =====
# terraform apply 후 참조할 수 있는 값들

output "s3_blog_bucket_arn" {
  description = "Blog S3 bucket ARN"
  value       = aws_s3_bucket.blog.arn
}

output "sqs_email_queue_url" {
  description = "Email SQS queue URL"
  value       = aws_sqs_queue.email_queue.url
}

output "sqs_email_dlq_arn" {
  description = "Email DLQ ARN"
  value       = aws_sqs_queue.email_dlq.arn
}

output "sns_notification_topic_arn" {
  description = "Notification events SNS topic ARN"
  value       = aws_sns_topic.notification_events.arn
}

output "sns_cloudwatch_alarms_arn" {
  description = "CloudWatch alarms SNS topic ARN"
  value       = aws_sns_topic.cloudwatch_alarms.arn
}

output "lambda_image_thumbnail_arn" {
  description = "Image thumbnail Lambda function ARN"
  value       = aws_lambda_function.image_thumbnail.arn
}

output "eventbridge_bus_arn" {
  description = "Portal Universe EventBridge bus ARN"
  value       = aws_cloudwatch_event_bus.portal_universe.arn
}

output "cloudwatch_alarm_names" {
  description = "CloudWatch alarm names"
  value = [
    aws_cloudwatch_metric_alarm.email_dlq.alarm_name,
    aws_cloudwatch_metric_alarm.image_thumbnail_dlq.alarm_name,
    aws_cloudwatch_metric_alarm.lambda_errors.alarm_name,
    aws_cloudwatch_metric_alarm.order_saga_failures.alarm_name,
  ]
}
