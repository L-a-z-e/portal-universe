#!/bin/bash

DOCKER_USERNAME="laze8771"

SERVICES=(
    "config-service"
    "discovery-service"
    "api-gateway"
    "auth-service"
    "blog-service"
    "shopping-service"
    "notification-service"
    "portal-shell"
)

echo "🚀 Tagging and pushing images to Docker Hub..."

for SERVICE in "${SERVICES[@]}"; do
    LOCAL_IMAGE="portal-universe-${SERVICE}:latest"
    HUB_IMAGE="${DOCKER_USERNAME}/portal-universe-${SERVICE}:latest"

    echo ""
    echo "📦 Processing: ${SERVICE}"

    # 태그 추가
    docker tag "${LOCAL_IMAGE}" "${HUB_IMAGE}"

    # Push
    docker push "${HUB_IMAGE}"

    echo "✅ ${SERVICE} pushed!"
done

echo ""
echo "🎉 All images pushed to Docker Hub!"