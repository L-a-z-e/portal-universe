# ============================================
# Stage 1: Build
# ============================================
FROM node:22-slim AS build

WORKDIR /workspace

# 1. workspace 루트 package.json 복사
COPY package.json package-lock.json ./

# 2. design-tokens 복사
COPY design-tokens/package.json ./design-tokens/
COPY design-tokens/ ./design-tokens/

# 3. design-types 복사
COPY design-types/package.json ./design-types/
COPY design-types/ ./design-types/

# 4. design-system-react 복사
COPY design-system-react/package.json ./design-system-react/
COPY design-system-react/ ./design-system-react/

# 5. react-bridge 복사
COPY react-bridge/package.json ./react-bridge/
COPY react-bridge/ ./react-bridge/

# 6. shopping-frontend 복사
COPY shopping-frontend/package.json ./shopping-frontend/
COPY shopping-frontend/ ./shopping-frontend/

# 6. 의존성 설치
RUN --mount=type=cache,target=/root/.npm npm ci

# 7. design-tokens 빌드
WORKDIR /workspace/design-tokens
RUN npm run build

# 8. design-types 빌드
WORKDIR /workspace/design-types
RUN npm run build

# 9. design-system-react 빌드
WORKDIR /workspace/design-system-react
RUN npm run build

# 10. shopping-frontend 빌드
ARG BUILD_MODE=docker
WORKDIR /workspace/shopping-frontend
RUN npm run build:${BUILD_MODE}

# ============================================
# Stage 2: Nginx
# ============================================
FROM nginxinc/nginx-unprivileged:stable-alpine
WORKDIR /usr/share/nginx/html

COPY --from=build /workspace/shopping-frontend/dist ./
COPY shopping-frontend/nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 8080
