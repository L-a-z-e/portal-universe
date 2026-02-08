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

# 5. admin-frontend 복사
COPY admin-frontend/package.json ./admin-frontend/
COPY admin-frontend/ ./admin-frontend/

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
