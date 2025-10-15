#!/bin/bash

# 실제 이미지 이름에서 태그 및 Kind 로드
for service in discovery-service config-service api-gateway auth-service blog-service shopping-service notification-service; do
  echo "Processing $service..."

  # 실제 이미지 이름 (하이픈)
  local_image="portal-universe-$service:latest"

  # Kind용 이미지 이름
  kind_image="laze8771/portal-universe-$service:latest"

  # 태그 추가
  docker tag $local_image $kind_image

  # Kind에 로드
  kind load docker-image $kind_image --name portal-universe
done

echo "✅ All images loaded to Kind cluster!"