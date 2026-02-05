# Local to Kubernetes 전환 가이드

로컬 개발 환경에서 Kubernetes 클러스터로 점진적으로 전환하는 과정을 학습합니다.

---

## 1. 학습 목표

- 개발 환경의 단계적 전환 프로세스 이해
- Docker 이미지 빌드 및 배포 방법 습득
- docker-compose.yml → Kubernetes manifest 전환 학습
- Kind를 활용한 로컬 Kubernetes 클러스터 구축

---

## 2. 전환 단계 개요

```
Local Development (localhost)
          ↓
Docker Compose (단일 호스트, 다중 컨테이너)
          ↓
Kind (로컬 Kubernetes 클러스터)
          ↓
Production Kubernetes (EKS, GKE, AKS)
```

| 단계 | 목적 | 도구 |
|------|------|------|
| **Local** | 빠른 개발 사이클 | IDE, Spring Boot DevTools |
| **Docker Compose** | 전체 인프라 테스트 | Docker Compose |
| **Kind** | Kubernetes 기능 테스트 | Kind (Kubernetes in Docker) |
| **Production** | 실제 운영 환경 | EKS, GKE, AKS |

---

## 3. Stage 1: Local Development

### 특징

- IDE에서 직접 Spring Boot 애플리케이션 실행
- 외부 인프라는 Docker Compose로 실행

### 실행 방법

```bash
# 1. 인프라 컨테이너만 실행
docker-compose up mongodb kafka redis localstack

# 2. IDE에서 Spring Boot 애플리케이션 실행
# SPRING_PROFILES_ACTIVE=local

# 3. Frontend 개발 서버 실행
cd frontend/portal-shell
npm run dev
```

### 장점

- ✅ 빠른 핫 리로드
- ✅ 쉬운 디버깅
- ✅ 낮은 리소스 사용

### 단점

- ❌ 네트워크 격리 없음
- ❌ 운영 환경과 차이
- ❌ 컨테이너화 이슈 발견 어려움

---

## 4. Stage 2: Docker Compose

### Dockerfile 작성

#### Backend (Spring Boot)

```dockerfile
# services/blog-service/Dockerfile
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# JAR 파일 복사
COPY build/libs/*.jar app.jar

# 비루트 사용자 생성
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# 헬스 체크
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8082/actuator/health || exit 1

# 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### Frontend (Vue 3)

```dockerfile
# frontend/portal-shell/Dockerfile

# Stage 1: Build
FROM node:20-alpine AS builder

WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

# Stage 2: Production
FROM nginx:alpine

COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
```

### Docker Compose 전체 구성

```yaml
# docker-compose.yml
version: '3.8'

networks:
  portal-universe-net:
    driver: bridge

services:
  # ===== Backend Services =====
  blog-service:
    build:
      context: ./services/blog-service
      dockerfile: Dockerfile
    container_name: blog-service
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    ports:
      - "8082:8082"
    depends_on:
      - mongodb
      - kafka
      - localstack
    networks:
      - portal-universe-net
    healthcheck:
      test: ["CMD", "wget", "--spider", "http://localhost:8082/actuator/health"]
      interval: 30s
      timeout: 3s
      retries: 3

  auth-service:
    build:
      context: ./services/auth-service
      dockerfile: Dockerfile
    container_name: auth-service
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    ports:
      - "8081:8081"
    depends_on:
      - mysql
      - kafka
      - redis
    networks:
      - portal-universe-net

  shopping-service:
    build:
      context: ./services/shopping-service
      dockerfile: Dockerfile
    container_name: shopping-service
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    ports:
      - "8083:8083"
    depends_on:
      - mysql
      - kafka
      - redis
    networks:
      - portal-universe-net

  # ===== Frontend =====
  portal-shell:
    build:
      context: ./frontend/portal-shell
      dockerfile: Dockerfile
    container_name: portal-shell
    ports:
      - "30000:80"
    depends_on:
      - blog-service
      - auth-service
      - shopping-service
    networks:
      - portal-universe-net

  # ===== Infrastructure =====
  mongodb:
    image: mongo:7
    container_name: mongodb
    environment:
      - MONGO_INITDB_ROOT_USERNAME=laze
      - MONGO_INITDB_ROOT_PASSWORD=password
    ports:
      - "27017:27017"
    volumes:
      - mongodb_data:/data/db
    networks:
      - portal-universe-net

  mysql:
    image: mysql:8
    container_name: mysql
    environment:
      - MYSQL_ROOT_PASSWORD=password
      - MYSQL_DATABASE=portal_universe
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
    networks:
      - portal-universe-net

  kafka:
    image: confluentinc/cp-kafka:latest
    container_name: kafka
    environment:
      - KAFKA_BROKER_ID=1
      - KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181
      - KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092
      - KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
    ports:
      - "9092:9092"
    depends_on:
      - zookeeper
    networks:
      - portal-universe-net

  zookeeper:
    image: confluentinc/cp-zookeeper:latest
    container_name: zookeeper
    environment:
      - ZOOKEEPER_CLIENT_PORT=2181
    networks:
      - portal-universe-net

  redis:
    image: redis:7-alpine
    container_name: redis
    ports:
      - "6379:6379"
    networks:
      - portal-universe-net

  localstack:
    image: localstack/localstack:latest
    container_name: localstack
    environment:
      - SERVICES=s3
      - AWS_ACCESS_KEY_ID=test
      - AWS_SECRET_ACCESS_KEY=test
      - AWS_DEFAULT_REGION=ap-northeast-2
    ports:
      - "4566:4566"
    volumes:
      - ./localstack_data:/var/lib/localstack
    networks:
      - portal-universe-net

volumes:
  mongodb_data:
  mysql_data:
```

### 빌드 및 실행

```bash
# 1. JAR 파일 빌드
cd services/blog-service
./gradlew clean bootJar

# 2. Docker 이미지 빌드 및 실행
cd ../../
docker-compose build
docker-compose up -d

# 3. 로그 확인
docker-compose logs -f blog-service

# 4. 정지 및 제거
docker-compose down
docker-compose down -v  # 볼륨까지 삭제
```

---

## 5. Stage 3: Kubernetes with Kind

### Kind 설치 및 클러스터 생성

```bash
# Kind 설치 (macOS)
brew install kind

# 클러스터 생성
kind create cluster --name portal-universe

# 클러스터 확인
kubectl cluster-info --context kind-portal-universe
kubectl get nodes
```

### Kind 설정 파일

```yaml
# kind-config.yaml
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
name: portal-universe
nodes:
  - role: control-plane
    kubeadmConfigPatches:
      - |
        kind: InitConfiguration
        nodeRegistration:
          kubeletExtraArgs:
            node-labels: "ingress-ready=true"
    extraPortMappings:
      - containerPort: 80
        hostPort: 80
        protocol: TCP
      - containerPort: 443
        hostPort: 443
        protocol: TCP
  - role: worker
  - role: worker
```

```bash
# 설정 파일로 클러스터 생성
kind create cluster --config kind-config.yaml
```

### Docker 이미지를 Kind로 로드

```bash
# 1. 이미지 빌드
docker build -t blog-service:1.0.0 services/blog-service

# 2. Kind 클러스터로 로드
kind load docker-image blog-service:1.0.0 --name portal-universe

# 3. 확인
docker exec -it portal-universe-control-plane crictl images | grep blog-service
```

---

## 6. Docker Compose → Kubernetes 전환

### MongoDB 예시

#### docker-compose.yml

```yaml
services:
  mongodb:
    image: mongo:7
    environment:
      - MONGO_INITDB_ROOT_USERNAME=laze
      - MONGO_INITDB_ROOT_PASSWORD=password
    ports:
      - "27017:27017"
    volumes:
      - mongodb_data:/data/db

volumes:
  mongodb_data:
```

#### Kubernetes Manifests

```yaml
# k8s/mongodb/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mongodb
  labels:
    app: mongodb
spec:
  replicas: 1
  selector:
    matchLabels:
      app: mongodb
  template:
    metadata:
      labels:
        app: mongodb
    spec:
      containers:
        - name: mongodb
          image: mongo:7
          ports:
            - containerPort: 27017
          env:
            - name: MONGO_INITDB_ROOT_USERNAME
              valueFrom:
                secretKeyRef:
                  name: mongodb-secret
                  key: username
            - name: MONGO_INITDB_ROOT_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: mongodb-secret
                  key: password
          volumeMounts:
            - name: mongodb-storage
              mountPath: /data/db
      volumes:
        - name: mongodb-storage
          persistentVolumeClaim:
            claimName: mongodb-pvc

---
# k8s/mongodb/service.yaml
apiVersion: v1
kind: Service
metadata:
  name: mongodb
spec:
  selector:
    app: mongodb
  ports:
    - protocol: TCP
      port: 27017
      targetPort: 27017
  type: ClusterIP

---
# k8s/mongodb/secret.yaml
apiVersion: v1
kind: Secret
metadata:
  name: mongodb-secret
type: Opaque
stringData:
  username: laze
  password: password

---
# k8s/mongodb/pvc.yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: mongodb-pvc
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 10Gi
```

### Blog Service 예시

#### Kubernetes Manifests

```yaml
# k8s/blog-service/configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: blog-service-config
data:
  SPRING_PROFILES_ACTIVE: "kubernetes"

---
# k8s/blog-service/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: blog-service
  labels:
    app: blog-service
spec:
  replicas: 2
  selector:
    matchLabels:
      app: blog-service
  template:
    metadata:
      labels:
        app: blog-service
    spec:
      containers:
        - name: blog-service
          image: blog-service:1.0.0
          imagePullPolicy: IfNotPresent
          ports:
            - containerPort: 8082
          envFrom:
            - configMapRef:
                name: blog-service-config
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8082
            initialDelaySeconds: 60
            periodSeconds: 10
            timeoutSeconds: 5
            failureThreshold: 3
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8082
            initialDelaySeconds: 30
            periodSeconds: 5
            timeoutSeconds: 3
            failureThreshold: 3
          resources:
            requests:
              memory: "512Mi"
              cpu: "250m"
            limits:
              memory: "1Gi"
              cpu: "500m"

---
# k8s/blog-service/service.yaml
apiVersion: v1
kind: Service
metadata:
  name: blog-service
spec:
  selector:
    app: blog-service
  ports:
    - protocol: TCP
      port: 8082
      targetPort: 8082
  type: ClusterIP
```

### 배포

```bash
# Namespace 생성 (선택)
kubectl create namespace portal-universe
kubectl config set-context --current --namespace=portal-universe

# ConfigMap, Secret 먼저 생성
kubectl apply -f k8s/mongodb/secret.yaml
kubectl apply -f k8s/blog-service/configmap.yaml

# 인프라 리소스 배포
kubectl apply -f k8s/mongodb/
kubectl apply -f k8s/kafka/
kubectl apply -f k8s/redis/
kubectl apply -f k8s/localstack/

# 애플리케이션 배포
kubectl apply -f k8s/blog-service/
kubectl apply -f k8s/auth-service/
kubectl apply -f k8s/shopping-service/

# 확인
kubectl get pods
kubectl get services
kubectl logs -f deployment/blog-service
```

---

## 7. LocalStack Kubernetes 배포

```yaml
# k8s/localstack/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: localstack
  labels:
    app: localstack
spec:
  replicas: 1
  selector:
    matchLabels:
      app: localstack
  template:
    metadata:
      labels:
        app: localstack
    spec:
      containers:
        - name: localstack
          image: localstack/localstack:latest
          ports:
            - containerPort: 4566
          env:
            - name: SERVICES
              value: "s3"
            - name: AWS_ACCESS_KEY_ID
              value: "test"
            - name: AWS_SECRET_ACCESS_KEY
              value: "test"
            - name: AWS_DEFAULT_REGION
              value: "ap-northeast-2"
            - name: PERSISTENCE
              value: "1"
          volumeMounts:
            - name: localstack-storage
              mountPath: /var/lib/localstack
      volumes:
        - name: localstack-storage
          persistentVolumeClaim:
            claimName: localstack-pvc

---
# k8s/localstack/service.yaml
apiVersion: v1
kind: Service
metadata:
  name: localstack
spec:
  selector:
    app: localstack
  ports:
    - protocol: TCP
      port: 4566
      targetPort: 4566
  type: ClusterIP

---
# k8s/localstack/pvc.yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: localstack-pvc
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 5Gi
```

---

## 8. 전환 체크리스트

### Docker Compose → Kubernetes

- [ ] 모든 서비스의 Deployment 작성
- [ ] 각 Deployment에 대응하는 Service 작성
- [ ] 환경 변수를 ConfigMap으로 분리
- [ ] 민감 정보를 Secret으로 분리
- [ ] 데이터 저장소에 PersistentVolumeClaim 추가
- [ ] Liveness/Readiness Probe 설정
- [ ] Resource requests/limits 설정
- [ ] Labels 일관성 확인

---

## 9. 디버깅

### Pod 로그 확인

```bash
# 실시간 로그
kubectl logs -f deployment/blog-service

# 특정 Pod 로그
kubectl logs blog-service-7d9f8b6c4d-xjk2p

# 이전 컨테이너 로그
kubectl logs blog-service-7d9f8b6c4d-xjk2p --previous
```

### Pod 내부 접속

```bash
# Shell 접속
kubectl exec -it blog-service-7d9f8b6c4d-xjk2p -- /bin/sh

# 단일 명령 실행
kubectl exec blog-service-7d9f8b6c4d-xjk2p -- env | grep SPRING
```

### 네트워크 테스트

```bash
# Service DNS 확인
kubectl run -it --rm debug --image=busybox --restart=Never -- nslookup mongodb

# 연결 테스트
kubectl run -it --rm debug --image=curlimages/curl --restart=Never -- \
  curl http://blog-service:8082/actuator/health
```

---

## 10. 핵심 요약

### 전환 단계

1. **Local**: IDE 개발, 빠른 피드백
2. **Docker Compose**: 전체 스택 통합 테스트
3. **Kind**: Kubernetes 기능 검증
4. **Production**: 실제 운영 환경

### 주요 변경 사항

| 항목 | Docker Compose | Kubernetes |
|------|----------------|------------|
| 서비스 정의 | `services:` | `Deployment` + `Service` |
| 환경 변수 | `environment:` | `ConfigMap` + `Secret` |
| 볼륨 | `volumes:` | `PersistentVolumeClaim` |
| 네트워크 | `networks:` | Service DNS |
| 헬스 체크 | `healthcheck:` | `livenessProbe`, `readinessProbe` |

---

## 11. 관련 문서

- [Environment Profiles](./environment-profiles.md) - 환경별 프로필 설정
- [Kubernetes to AWS](./kubernetes-to-aws.md) - 실제 AWS 전환
- [Kubernetes Fundamentals](../../infra/kubernetes-fundamentals.md) - K8s 기초
- [Kind Cluster](../../infra/kubernetes-kind-cluster.md) - Kind 상세 가이드
- [LocalStack S3](../infra/localstack-s3.md) - LocalStack 사용법
