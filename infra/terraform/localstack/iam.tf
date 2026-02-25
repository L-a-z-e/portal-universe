# ===== IAM Users & Policies =====
# 01-init-iam.sh에 대응

# --- blog-service ---

resource "aws_iam_user" "blog_service" {
  name = "blog-service"
}

resource "aws_iam_policy" "blog_service" {
  name = "blog-service-policy"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid      = "BlogBucketAccess"
        Effect   = "Allow"
        Action   = ["s3:PutObject", "s3:GetObject", "s3:DeleteObject"]
        Resource = "${aws_s3_bucket.blog.arn}/*"
      },
      {
        Sid      = "BlogBucketList"
        Effect   = "Allow"
        Action   = "s3:ListBucket"
        Resource = aws_s3_bucket.blog.arn
      },
      {
        Sid      = "BlogBucketHead"
        Effect   = "Allow"
        Action   = "s3:HeadBucket"
        Resource = "*"
      }
    ]
  })
}

resource "aws_iam_user_policy_attachment" "blog_service" {
  user       = aws_iam_user.blog_service.name
  policy_arn = aws_iam_policy.blog_service.arn
}

# --- drive-service ---

resource "aws_iam_user" "drive_service" {
  name = "drive-service"
}

resource "aws_iam_policy" "drive_service" {
  name = "drive-service-policy"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "DriveBucketAccess"
        Effect = "Allow"
        Action = [
          "s3:PutObject", "s3:GetObject", "s3:DeleteObject",
          "s3:CreateMultipartUpload", "s3:UploadPart",
          "s3:CompleteMultipartUpload", "s3:AbortMultipartUpload",
          "s3:ListMultipartUploadParts"
        ]
        Resource = "arn:aws:s3:::drive-bucket/*"
      },
      {
        Sid    = "DriveBucketList"
        Effect = "Allow"
        Action = [
          "s3:ListBucket", "s3:ListBucketMultipartUploads",
          "s3:GetBucketVersioning", "s3:GetLifecycleConfiguration"
        ]
        Resource = "arn:aws:s3:::drive-bucket"
      },
      {
        Sid      = "DriveBucketHead"
        Effect   = "Allow"
        Action   = "s3:HeadBucket"
        Resource = "*"
      }
    ]
  })
}

resource "aws_iam_user_policy_attachment" "drive_service" {
  user       = aws_iam_user.drive_service.name
  policy_arn = aws_iam_policy.drive_service.arn
}

# --- notification-service ---

resource "aws_iam_user" "notification_service" {
  name = "notification-service"
}

resource "aws_iam_policy" "notification_service" {
  name = "notification-service-policy"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "SQSSend"
        Effect = "Allow"
        Action = [
          "sqs:SendMessage", "sqs:ReceiveMessage", "sqs:DeleteMessage",
          "sqs:GetQueueUrl", "sqs:GetQueueAttributes", "sqs:ChangeMessageVisibility"
        ]
        Resource = "arn:aws:sqs:${var.aws_region}:000000000000:notification-*"
      },
      {
        Sid    = "SNSPublish"
        Effect = "Allow"
        Action = ["sns:Publish", "sns:Subscribe", "sns:Unsubscribe"]
        Resource = "arn:aws:sns:${var.aws_region}:000000000000:notification-*"
      }
    ]
  })
}

resource "aws_iam_user_policy_attachment" "notification_service" {
  user       = aws_iam_user.notification_service.name
  policy_arn = aws_iam_policy.notification_service.arn
}

# --- shopping-service ---

resource "aws_iam_user" "shopping_service" {
  name = "shopping-service"
}

resource "aws_iam_policy" "shopping_service" {
  name = "shopping-service-policy"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "SQSOrderQueues"
        Effect = "Allow"
        Action = [
          "sqs:SendMessage", "sqs:ReceiveMessage", "sqs:DeleteMessage",
          "sqs:GetQueueUrl", "sqs:GetQueueAttributes"
        ]
        Resource = "arn:aws:sqs:${var.aws_region}:000000000000:order-*"
      },
      {
        Sid      = "EventBridgePutEvents"
        Effect   = "Allow"
        Action   = "events:PutEvents"
        Resource = aws_cloudwatch_event_bus.portal_universe.arn
      }
    ]
  })
}

resource "aws_iam_user_policy_attachment" "shopping_service" {
  user       = aws_iam_user.shopping_service.name
  policy_arn = aws_iam_policy.shopping_service.arn
}

# --- Lambda Execution Role ---

resource "aws_iam_role" "lambda_execution" {
  name = "image-thumbnail-lambda-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect    = "Allow"
        Principal = { Service = "lambda.amazonaws.com" }
        Action    = "sts:AssumeRole"
      }
    ]
  })
}

resource "aws_iam_role_policy" "lambda_s3_logs" {
  name = "image-thumbnail-policy"
  role = aws_iam_role.lambda_execution.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = ["s3:GetObject", "s3:PutObject"]
        Resource = "${aws_s3_bucket.blog.arn}/*"
      },
      {
        Effect   = "Allow"
        Action   = ["logs:CreateLogGroup", "logs:CreateLogStream", "logs:PutLogEvents"]
        Resource = "arn:aws:logs:*:*:*"
      }
    ]
  })
}
