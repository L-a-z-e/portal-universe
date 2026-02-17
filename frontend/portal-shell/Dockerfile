# ============================================
# Stage 1: Build
# ============================================
FROM node:22-alpine AS build

WORKDIR /workspace

# 1. workspace 루트 package.json 복사
COPY package.json package-lock.json ./

# 2. design-core 복사
COPY design-core/package.json ./design-core/
COPY design-core/ ./design-core/

# 3. design-vue 복사
COPY design-vue/package.json ./design-vue/
COPY design-vue/ ./design-vue/

# 4. portal-shell 복사
COPY portal-shell/package.json ./portal-shell/
COPY portal-shell/ ./portal-shell/

# 6. 의존성 설치
RUN --mount=type=cache,target=/root/.npm npm ci

# 7. design-core 빌드
WORKDIR /workspace/design-core
RUN npm run build

# 8. design-vue 빌드
WORKDIR /workspace/design-vue
RUN npm run build

# 9. portal-shell 빌드
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
