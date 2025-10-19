# Stage 1: Build
FROM node:22-alpine AS build
WORKDIR /app

ARG BUILD_MODE=docker

# 단순한 경로
COPY package.json package-lock.json ./
RUN npm ci

COPY . ./
RUN npm run build:${BUILD_MODE}

# Stage 2: Nginx
FROM nginx:stable-alpine
WORKDIR /usr/share/nginx/html

COPY --from=build /app/dist ./
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
