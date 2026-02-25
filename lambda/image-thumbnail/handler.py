"""
S3 이미지 업로드 → 썸네일 자동 생성 Lambda 함수.

트리거: S3 PutObject 이벤트 (blog-bucket)
동작: 원본 이미지를 읽어 300x300 썸네일 생성 → thumbnails/ 프리픽스로 저장
"""

import json
import logging
import os
import urllib.parse
from io import BytesIO

import boto3
from PIL import Image

logger = logging.getLogger()
logger.setLevel(logging.INFO)

THUMBNAIL_MAX_SIZE = (300, 300)
THUMBNAIL_PREFIX = "thumbnails/"
THUMBNAIL_QUALITY = 85

# LocalStack endpoint (환경변수로 주입)
ENDPOINT_URL = os.environ.get("AWS_ENDPOINT_URL")

s3_client = boto3.client(
    "s3",
    endpoint_url=ENDPOINT_URL,
    region_name=os.environ.get("AWS_DEFAULT_REGION", "ap-northeast-2"),
)

SUPPORTED_FORMATS = {"image/jpeg", "image/png", "image/webp"}

# PIL format → Content-Type 매핑
FORMAT_TO_CONTENT_TYPE = {
    "JPEG": "image/jpeg",
    "PNG": "image/png",
    "WEBP": "image/webp",
}


def handler(event, context):
    """S3 이벤트를 받아 썸네일을 생성합니다."""
    logger.info("Received event: %s", json.dumps(event, default=str))

    for record in event.get("Records", []):
        bucket = record["s3"]["bucket"]["name"]
        key = urllib.parse.unquote_plus(record["s3"]["object"]["key"])

        # thumbnails/ 프리픽스는 무한 루프 방지를 위해 스킵
        if key.startswith(THUMBNAIL_PREFIX):
            logger.info("Skipping thumbnail: %s", key)
            continue

        try:
            process_image(bucket, key)
        except Exception:
            logger.exception("Failed to process %s/%s", bucket, key)
            raise

    return {"statusCode": 200, "body": "Thumbnails generated"}


def process_image(bucket: str, key: str) -> None:
    """원본 이미지를 다운로드하고 썸네일을 생성합니다."""
    # 원본 다운로드
    response = s3_client.get_object(Bucket=bucket, Key=key)
    content_type = response["ContentType"]

    if content_type not in SUPPORTED_FORMATS:
        logger.info("Unsupported format %s for %s, skipping", content_type, key)
        return

    original_bytes = response["Body"].read()
    logger.info("Downloaded %s (%d bytes, %s)", key, len(original_bytes), content_type)

    # 썸네일 생성
    image = Image.open(BytesIO(original_bytes))

    # EXIF 회전 정보 반영
    try:
        from PIL import ImageOps
        image = ImageOps.exif_transpose(image)
    except Exception:
        pass

    # RGBA → RGB 변환 (JPEG 저장 시 필요)
    if image.mode in ("RGBA", "P") and content_type == "image/jpeg":
        image = image.convert("RGB")

    image.thumbnail(THUMBNAIL_MAX_SIZE, Image.LANCZOS)

    # 저장
    output = BytesIO()
    pil_format = _content_type_to_pil_format(content_type)
    save_kwargs = {"quality": THUMBNAIL_QUALITY, "optimize": True}
    if pil_format == "PNG":
        save_kwargs = {"optimize": True}

    image.save(output, format=pil_format, **save_kwargs)
    output.seek(0)

    # 썸네일 키: thumbnails/{원본 키}
    thumbnail_key = f"{THUMBNAIL_PREFIX}{key}"

    s3_client.put_object(
        Bucket=bucket,
        Key=thumbnail_key,
        Body=output.getvalue(),
        ContentType=content_type,
    )

    logger.info(
        "Thumbnail created: %s (%d → %d bytes, %dx%d)",
        thumbnail_key,
        len(original_bytes),
        output.tell(),
        image.width,
        image.height,
    )


def _content_type_to_pil_format(content_type: str) -> str:
    """Content-Type을 PIL format 문자열로 변환합니다."""
    mapping = {
        "image/jpeg": "JPEG",
        "image/png": "PNG",
        "image/webp": "WEBP",
    }
    return mapping.get(content_type, "JPEG")
