# ============================================
# Stage 1: Build
# ============================================
FROM node:22-alpine AS build

WORKDIR /workspace

# 1. workspace 루트 package.json 복사
COPY package.json package-lock.json ./

# 2. design-system 복사
COPY design-system/package.json ./design-system/
COPY design-system/ ./design-system/

# 3. blog-frontend 복사
COPY blog-frontend/package.json ./blog-frontend/
COPY blog-frontend/ ./blog-frontend/

# 4. 의존성 설치
RUN npm ci

# ✅✅✅ 5. design-system 먼저 빌드 (타입 정의 생성)
WORKDIR /workspace/design-system
RUN npm run build

# ✅✅✅ 6. blog-frontend 빌드
ARG BUILD_MODE=docker
WORKDIR /workspace/blog-frontend
RUN npm run build:${BUILD_MODE}

# ============================================
# Stage 2: Nginx
# ============================================
FROM nginx:stable-alpine
WORKDIR /usr/share/nginx/html

COPY --from=build /workspace/blog-frontend/dist ./
COPY blog-frontend/nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]