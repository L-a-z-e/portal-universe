# Stage 1: Build
FROM node:22 AS build
WORKDIR /app

# 패키지 파일 복사 및 의존성 설치
COPY package*.json ./
RUN npm install

# 소스 코드 복사
COPY . .

# Docker 모드로 빌드
RUN npm run build:docker

# Stage 2: Nginx로 서빙
FROM nginx:1.29-alpine
WORKDIR /usr/share/nginx/html

# 빌드 결과물 복사
COPY --from=build /app/dist ./

# Nginx 설정 복사
COPY frontend/portal-shell/nginx.conf /etc/nginx/conf.d/default.conf

# 포트 노출
EXPOSE 80

# Nginx 실행
CMD ["nginx", "-g", "daemon off;"]