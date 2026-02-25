#!/bin/bash
# Lambda 배포 패키지 빌드 (Docker 기반 Linux x86_64 호환)
# LocalStack은 Lambda를 Docker 컨테이너(Linux)에서 실행하므로
# macOS에서 빌드한 Python C extension은 동작하지 않음.
# 반드시 Linux 환경에서 Pillow를 빌드해야 함.

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
OUTPUT="$SCRIPT_DIR/package.zip"

echo "Building Lambda package for linux/x86_64..."

docker run --rm \
  -v "$SCRIPT_DIR":/build \
  -w /build \
  --platform linux/amd64 \
  python:3.11-slim \
  bash -c '
    set -e
    apt-get update -qq && apt-get install -y -qq zip libjpeg-dev zlib1g-dev > /dev/null 2>&1
    rm -rf /tmp/package && mkdir -p /tmp/package
    pip install Pillow -t /tmp/package --quiet --no-cache-dir
    cp handler.py /tmp/package/
    cd /tmp/package
    find . -name "*.pyc" -delete
    find . -name "__pycache__" -type d -exec rm -rf {} + 2>/dev/null || true
    find . -name "tests" -type d -exec rm -rf {} + 2>/dev/null || true
    zip -r9 /build/package.zip . > /dev/null 2>&1
    echo "Done: $(du -sh /build/package.zip | cut -f1)"
  '

echo "Package built: $OUTPUT"
ls -lh "$OUTPUT"
