# ===== EventBridge =====
# 05-init-eventbridge.sh에 대응

resource "aws_cloudwatch_event_bus" "portal_universe" {
  name = "portal-universe"
}

# Rule 1: 고액 주문 (totalAmount >= 100000)
resource "aws_cloudwatch_event_rule" "high_value_order" {
  name           = "high-value-order-rule"
  event_bus_name = aws_cloudwatch_event_bus.portal_universe.name

  event_pattern = jsonencode({
    source      = ["portal-universe.shopping-service"]
    detail-type = ["OrderCreated"]
    detail = {
      totalAmount = [{ numeric = [">=", 100000] }]
    }
  })
}

resource "aws_cloudwatch_event_target" "high_value_order_sqs" {
  rule           = aws_cloudwatch_event_rule.high_value_order.name
  event_bus_name = aws_cloudwatch_event_bus.portal_universe.name
  target_id      = "high-value-order-queue"
  arn            = aws_sqs_queue.high_value_order.arn

  dead_letter_config {
    arn = aws_sqs_queue.high_value_order_dlq.arn
  }
}

# Rule 2: 모든 주문 확인
resource "aws_cloudwatch_event_rule" "order_confirmation" {
  name           = "order-confirmation-rule"
  event_bus_name = aws_cloudwatch_event_bus.portal_universe.name

  event_pattern = jsonencode({
    source      = ["portal-universe.shopping-service"]
    detail-type = ["OrderCreated"]
  })
}

resource "aws_cloudwatch_event_target" "order_confirmation_sqs" {
  rule           = aws_cloudwatch_event_rule.order_confirmation.name
  event_bus_name = aws_cloudwatch_event_bus.portal_universe.name
  target_id      = "order-confirmation-queue"
  arn            = aws_sqs_queue.order_confirmation.arn

  dead_letter_config {
    arn = aws_sqs_queue.order_confirmation_dlq.arn
  }
}
