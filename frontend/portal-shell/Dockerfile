# ============================================
# Stage 1: Build
# ============================================
FROM node:22-alpine AS build

# 작업 디렉토리 (루트 기준)
WORKDIR /workspace

# ✅ 1. workspace 루트의 package.json들 복사
COPY package.json package-lock.json ./

# ✅ 2. design-system 복사 (의존성)
COPY design-system/package.json ./design-system/
COPY design-system/ ./design-system/

# ✅ 3. portal-shell 복사
COPY portal-shell/package.json ./portal-shell/
COPY portal-shell/ ./portal-shell/

# ✅ 4. 통합 npm install (workspace 전체)
RUN npm ci

# ✅ 5. 빌드 (portal-shell만)
ARG BUILD_MODE=docker
WORKDIR /workspace/portal-shell
RUN npm run build:${BUILD_MODE}

# ============================================
# Stage 2: Nginx
# ============================================
FROM nginx:stable-alpine
WORKDIR /usr/share/nginx/html

# ✅ 빌드 결과물만 복사
COPY --from=build /workspace/portal-shell/dist ./
COPY portal-shell/nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]