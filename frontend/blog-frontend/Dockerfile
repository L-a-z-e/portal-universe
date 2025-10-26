# ====================================================================
# Stage 1: Build Stage
# 역할: Vue.js 소스 코드를 컴파일하고 정적 파일(HTML, JS, CSS)을 생성합니다.
# ====================================================================
FROM node:22-alpine AS build

WORKDIR /workspace

# 1. 의존성 설치를 위해 package.json 파일을 먼저 복사합니다.
COPY package.json package-lock.json ./

# 2. design-system과 blog-frontend의 package.json을 복사합니다.
COPY design-system/package.json ./design-system/
COPY blog-frontend/package.json ./blog-frontend/

# 3. 전체 의존성을 설치합니다.
RUN npm ci

# 4. design-system과 blog-frontend의 전체 소스 코드를 복사합니다.
COPY design-system/ ./design-system/
COPY blog-frontend/ ./blog-frontend/

# 5. design-system을 먼저 빌드하여 타입 정의 파일(.d.ts) 등을 생성합니다.
WORKDIR /workspace/design-system
RUN npm run build

# 6. blog-frontend를 빌드합니다.
#    BUILD_MODE 인자에 따라 다른 환경(.env) 파일을 사용하여 빌드합니다. (기본값: docker)
ARG BUILD_MODE=docker
WORKDIR /workspace/blog-frontend
RUN npm run build:${BUILD_MODE}

# ====================================================================
# Stage 2: Serve Stage
# 역할: 빌드된 정적 파일을 Nginx를 통해 서비스합니다.
# ====================================================================
FROM nginx:stable-alpine

# Nginx의 기본 웹 루트 디렉토리로 작업 디렉토리 변경
WORKDIR /usr/share/nginx/html

# Build Stage에서 생성된 dist 디렉토리의 모든 파일을 복사합니다.
COPY --from=build /workspace/blog-frontend/dist ./

# 커스텀 Nginx 설정을 복사하여 기본 설정을 덮어씁니다.
COPY blog-frontend/nginx.conf /etc/nginx/conf.d/default.conf

# Nginx가 사용할 80 포트를 노출합니다.
EXPOSE 80

# Nginx를 foreground로 실행하여 컨테이너가 바로 종료되지 않도록 합니다.
CMD ["nginx", "-g", "daemon off;"]
