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

# 4. vue-bridge 복사
COPY vue-bridge/package.json ./vue-bridge/
COPY vue-bridge/ ./vue-bridge/

# 5. admin-frontend 복사
COPY admin-frontend/package.json ./admin-frontend/
COPY admin-frontend/ ./admin-frontend/

# 6. 의존성 설치
RUN --mount=type=cache,target=/root/.npm npm ci

# 7. design-core 빌드
WORKDIR /workspace/design-core
RUN npm run build

# 8. design-vue 빌드
WORKDIR /workspace/design-vue
RUN npm run build

# 9. vue-bridge 빌드
WORKDIR /workspace/vue-bridge
RUN npm run build

# 10. admin-frontend 빌드
ARG BUILD_MODE=docker
WORKDIR /workspace/admin-frontend
RUN npm run build:${BUILD_MODE}

# ============================================
# Stage 2: Nginx
# ============================================
FROM nginxinc/nginx-unprivileged:stable-alpine
WORKDIR /usr/share/nginx/html

COPY --from=build /workspace/admin-frontend/dist ./
COPY admin-frontend/nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 8080
