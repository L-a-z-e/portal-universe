# Stage 1: Build
FROM node:22 AS build
WORKDIR /app

COPY package*.json ./
RUN npm install

COPY . .
RUN npm run build:docker

# Stage 2: Nginx
FROM nginx:1.29-alpine
WORKDIR /usr/share/nginx/html

COPY --from=build /app/dist ./
COPY frontend/blog-frontend/nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]