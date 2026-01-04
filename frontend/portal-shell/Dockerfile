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

# 3. portal-shell 복사
COPY portal-shell/package.json ./portal-shell/
COPY portal-shell/ ./portal-shell/

# 4. 의존성 설치
RUN npm ci

# ✅✅✅ 5. design-system 먼저 빌드 (타입 정의 생성)
WORKDIR /workspace/design-system
RUN npm run build

# ✅✅✅ 6. portal-shell 빌드
ARG BUILD_MODE=docker
WORKDIR /workspace/portal-shell
RUN npm run build:${BUILD_MODE}

# ============================================
# Stage 2: Nginx
# ============================================
FROM nginx:stable-alpine
WORKDIR /usr/share/nginx/html

COPY --from=build /workspace/portal-shell/dist ./
COPY portal-shell/nginx.conf /etc/nginx/nginx.conf
COPY portal-shell/default.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
EXPOSE 443
CMD ["nginx", "-g", "daemon off;"]
