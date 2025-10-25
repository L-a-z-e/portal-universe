FROM node:22-alpine AS build

WORKDIR /workspace

# workspace 루트
COPY package.json package-lock.json ./

# design-system
COPY design-system/package.json ./design-system/
COPY design-system/ ./design-system/

# blog-frontend
COPY blog-frontend/package.json ./blog-frontend/
COPY blog-frontend/ ./blog-frontend/

RUN npm ci

ARG BUILD_MODE=docker
WORKDIR /workspace/blog-frontend
RUN npm run build:${BUILD_MODE}

FROM nginx:stable-alpine
WORKDIR /usr/share/nginx/html

COPY --from=build /workspace/blog-frontend/dist ./
COPY blog-frontend/nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]