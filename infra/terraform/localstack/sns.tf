# ===== SNS Topics =====
# 03-init-sqs.sh + 07-init-cloudwatch.sh에 대응

resource "aws_sns_topic" "notification_events" {
  name = "notification-events"
}

# SNS → email-queue 구독 (팬아웃)
resource "aws_sns_topic_subscription" "email_queue" {
  topic_arn = aws_sns_topic.notification_events.arn
  protocol  = "sqs"
  endpoint  = aws_sqs_queue.email_queue.arn
}

# CloudWatch Alarm 알림 채널
resource "aws_sns_topic" "cloudwatch_alarms" {
  name = "cloudwatch-alarms"
}

# Alarm → cloudwatch-alarm-queue 구독 (검증용)
resource "aws_sns_topic_subscription" "cloudwatch_alarm_queue" {
  topic_arn = aws_sns_topic.cloudwatch_alarms.arn
  protocol  = "sqs"
  endpoint  = aws_sqs_queue.cloudwatch_alarm.arn
}
