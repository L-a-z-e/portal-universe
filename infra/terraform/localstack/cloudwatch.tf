# ===== CloudWatch Alarms =====
# 07-init-cloudwatch.sh에 대응

resource "aws_cloudwatch_metric_alarm" "email_dlq" {
  alarm_name          = "email-dlq-messages"
  alarm_description   = "Email DLQ has messages - email processing failures detected"
  namespace           = "AWS/SQS"
  metric_name         = "ApproximateNumberOfMessagesVisible"
  statistic           = "Sum"
  period              = 60
  evaluation_periods  = 1
  threshold           = 1
  comparison_operator = "GreaterThanOrEqualToThreshold"
  treat_missing_data  = "notBreaching"
  alarm_actions       = [aws_sns_topic.cloudwatch_alarms.arn]

  dimensions = {
    QueueName = aws_sqs_queue.email_dlq.name
  }
}

resource "aws_cloudwatch_metric_alarm" "image_thumbnail_dlq" {
  alarm_name          = "image-thumbnail-dlq-messages"
  alarm_description   = "Image thumbnail DLQ has messages - Lambda processing failures"
  namespace           = "AWS/SQS"
  metric_name         = "ApproximateNumberOfMessagesVisible"
  statistic           = "Sum"
  period              = 60
  evaluation_periods  = 1
  threshold           = 1
  comparison_operator = "GreaterThanOrEqualToThreshold"
  treat_missing_data  = "notBreaching"
  alarm_actions       = [aws_sns_topic.cloudwatch_alarms.arn]

  dimensions = {
    QueueName = aws_sqs_queue.image_thumbnail_dlq.name
  }
}

resource "aws_cloudwatch_metric_alarm" "lambda_errors" {
  alarm_name          = "lambda-image-thumbnail-errors"
  alarm_description   = "Lambda image-thumbnail error rate too high"
  namespace           = "AWS/Lambda"
  metric_name         = "Errors"
  statistic           = "Sum"
  period              = 300
  evaluation_periods  = 1
  threshold           = 3
  comparison_operator = "GreaterThanOrEqualToThreshold"
  treat_missing_data  = "notBreaching"
  alarm_actions       = [aws_sns_topic.cloudwatch_alarms.arn]

  dimensions = {
    FunctionName = aws_lambda_function.image_thumbnail.function_name
  }
}

resource "aws_cloudwatch_metric_alarm" "order_saga_failures" {
  alarm_name          = "order-saga-failures"
  alarm_description   = "Order saga failure rate too high"
  namespace           = "PortalUniverse/Shopping"
  metric_name         = "OrderSagaFailure"
  statistic           = "Sum"
  period              = 300
  evaluation_periods  = 1
  threshold           = 5
  comparison_operator = "GreaterThanOrEqualToThreshold"
  treat_missing_data  = "notBreaching"
  alarm_actions       = [aws_sns_topic.cloudwatch_alarms.arn]
}
