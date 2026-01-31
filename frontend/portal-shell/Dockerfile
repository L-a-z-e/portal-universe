# ============================================
# Stage 1: Build
# ============================================
FROM node:22-alpine AS build

WORKDIR /workspace

# 1. workspace 루트 package.json 복사
COPY package.json package-lock.json ./

# 2. design-tokens 복사
COPY design-tokens/package.json ./design-tokens/
COPY design-tokens/ ./design-tokens/

# 3. design-types 복사
COPY design-types/package.json ./design-types/
COPY design-types/ ./design-types/

# 4. design-system-vue 복사
COPY design-system-vue/package.json ./design-system-vue/
COPY design-system-vue/ ./design-system-vue/

# 5. portal-shell 복사
COPY portal-shell/package.json ./portal-shell/
COPY portal-shell/ ./portal-shell/

# 6. 의존성 설치
RUN --mount=type=cache,target=/root/.npm npm ci

# 7. design-tokens 빌드
WORKDIR /workspace/design-tokens
RUN npm run build

# 8. design-types 빌드
WORKDIR /workspace/design-types
RUN npm run build

# 9. design-system-vue 빌드
WORKDIR /workspace/design-system-vue
RUN npm run build

# 10. portal-shell 빌드
ARG BUILD_MODE=docker
WORKDIR /workspace/portal-shell
RUN npm run build:${BUILD_MODE}

# ============================================
# Stage 2: Nginx
# ============================================
FROM nginxinc/nginx-unprivileged:stable-alpine
WORKDIR /usr/share/nginx/html

ARG BUILD_MODE=docker

COPY --from=build /workspace/portal-shell/dist ./
COPY portal-shell/nginx.conf /etc/nginx/nginx.conf
COPY portal-shell/default.conf /etc/nginx/conf.d/default.conf
COPY portal-shell/default.k8s.conf /tmp/default.k8s.conf

# K8s 모드에서는 SSL 없는 HTTP-only 설정 사용
# nginx-unprivileged 이미지에서는 /tmp 파일 삭제가 불가하므로 그대로 둠
RUN if [ "$BUILD_MODE" = "k8s" ]; then \
      cp /tmp/default.k8s.conf /etc/nginx/conf.d/default.conf; \
    fi

EXPOSE 8080
EXPOSE 8443
