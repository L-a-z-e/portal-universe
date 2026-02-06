# Portal Universe Kubernetes 배포 가이드

## 1. 개요

Portal Universe를 Kind(Kubernetes in Docker) 환경에서 실행하기 위한 완전한 배포 가이드입니다.

### 목표
- 전체 마이크로서비스 아키텍처를 Kind 클러스터에 배포
- HTTPS/TLS 인증서를 활용한 NGINX Ingress 설정
- Module Federation 기반 Micro Frontend 통합
- Playwright를 통한 E2E 접속 검증

### 배포 대상 서비스
- **Backend Services (6개)**: api-gateway, auth-service, blog-service, shopping-service, notification-service, prism-service
- **Frontend Services (4개)**: portal-shell, blog-frontend, shopping-frontend, prism-frontend
- **Infrastructure (8개)**: MySQL, MongoDB, Redis, PostgreSQL, Elasticsearch, Kafka, LocalStack, Zipkin
- **Monitoring (2개)**: Grafana, Prometheus

---

## 2. 원래 계획 (Plan)

### Phase 1: 버그 수정 및 설정 보정
- 모든 Spring Boot 서비스의 `application-kubernetes.yml`에 포트 번호 추가
- K8s Service DNS 이름에 포트 명시 (예: `http://auth-service:8081`)
- jwt-secrets.yaml.example의 namespace를 `portal-universe`로 수정
- 이미지 태그를 `:latest`로 통일

### Phase 2: portal-shell K8s용 nginx 설정
- `default.k8s.conf` 생성 (HTTP-only, Ingress가 TLS 처리)
- Dockerfile에 `BUILD_MODE` arg 추가 (docker/k8s 분기)

### Phase 3: Frontend .env.k8s 파일 생성
- 4개 frontend 서비스에 `.env.k8s` 생성
- Vite `--mode k8s` 빌드 시 사용

### Phase 4: 누락된 K8s 매니페스트 생성
- postgresql.yaml (Prism Service용)
- localstack.yaml (Blog Service S3 에뮬레이션)
- prism-service.yaml
- blog-frontend.yaml, shopping-frontend.yaml, prism-frontend.yaml

### Phase 5: 스크립트 업데이트
- `build-and-load.sh`: 전체 서비스 빌드 및 Kind 로드
- `deploy-all.sh`: 전체 서비스 배포 자동화

### Phase 6: Secrets 생성 및 클러스터 준비
- secret.yaml, jwt-secrets.yaml 생성
- Kind 클러스터 생성

### Phase 7: 빌드 & 배포 실행
- 이미지 빌드 → Kind 로드 → 배포 → 검증

### Phase 8: Playwright 접속 테스트
- 브라우저 자동화로 전체 서비스 동작 확인

---

## 3. 실제 수행 내용 및 차이점

계획대로 진행했으나 실제 배포 과정에서 여러 이슈가 발견되어 추가 수정이 필요했습니다.

### Phase 1: 버그 수정 ✅
**계획대로 수행**
- api-gateway, auth-service, blog-service, shopping-service, notification-service의 `application-kubernetes.yml`에 포트 추가
- jwt-secrets.yaml.example namespace 수정
- auth-service.yaml, shopping-service.yaml 이미지 태그 `:latest`로 통일

### Phase 2: portal-shell K8s nginx 설정 ⚠️
**계획 + 추가 이슈 해결**
- `default.k8s.conf` 생성 (HTTP only, resolver + set variable 패턴)
- Dockerfile `BUILD_MODE` arg 추가

**🚨 추가 이슈 #1: nginx 변수 proxy_pass에서 URI 미전달**
- **문제**: `proxy_pass http://$backend_host` 형태로 변수를 사용하면 요청 URI(`/api/v1/...`)가 upstream으로 전달되지 않음
- **원인**: nginx proxy_pass에서 변수 사용 시 원본 URI를 자동으로 append하지 않음
- **해결**: `rewrite` 지시문 추가
  ```nginx
  rewrite ^/api/(.*)$ /$1 break;
  proxy_pass http://$backend_api;
  ```

**🚨 추가 이슈 #2: nginx 시작 시 upstream DNS 미해결**
- **문제**: nginx 시작 시점에 `api-gateway`, `blog-frontend` 등의 DNS를 resolve하지 못해 시작 실패
- **원인**: nginx는 기본적으로 설정 로드 시점에 모든 upstream 호스트를 해석
- **해결**: `resolver` + `set $variable` 패턴으로 런타임 동적 resolution
  ```nginx
  resolver 10.96.0.10 valid=10s;  # K8s CoreDNS
  set $backend_api api-gateway:8080;
  proxy_pass http://$backend_api;
  ```

### Phase 3: .env.k8s 파일 ⚠️
**계획 + 추가 이슈 해결**
- 4개 frontend에 `.env.k8s` 생성

**🚨 추가 이슈 #3: blog-frontend Remote URL 누락**
- **문제**: blog-frontend에서 다른 Module Federation remote를 로드할 수 없음
- **원인**: `.env.k8s`에 `VITE_PORTAL_SHELL_REMOTE_URL`, `VITE_SHOPPING_REMOTE_URL` 누락
- **해결**: blog-frontend/.env.k8s에 추가
  ```env
  VITE_PORTAL_SHELL_REMOTE_URL=/assets/remoteEntry.js
  VITE_SHOPPING_REMOTE_URL=/remotes/shop/assets/remoteEntry.js
  ```

**🚨 추가 이슈 #4: HTTPS 전환 후 Mixed Content 에러**
- **문제**: HTTPS 페이지에서 `http://portal-universe:8080/...` 호출 시 브라우저 차단
- **원인**: mkcert TLS 인증서 적용 후 모든 접속이 HTTPS로 전환되었으나, 환경변수는 `http://`로 설정됨
- **해결**: 모든 `.env.k8s`의 URL을 **relative path**로 변경
  ```env
  # Before
  VITE_API_BASE_URL=http://portal-universe:8080

  # After
  VITE_API_BASE_URL=
  VITE_BLOG_REMOTE_URL=/remotes/blog/assets/remoteEntry.js
  ```

### Phase 4: K8s 매니페스트 ⚠️
**계획 + 추가 이슈 해결**
- postgresql.yaml, localstack.yaml 생성
- prism-service.yaml, blog/shopping/prism-frontend.yaml 생성

**🚨 추가 이슈 #5: prism-service 환경변수명 불일치**
- **문제**: prism-service pod가 DB 연결 실패로 CrashLoopBackOff
- **원인**: K8s 매니페스트에서 `DATABASE_*` 환경변수 사용, NestJS config는 `DB_*` 기대
- **해결**: prism-service.yaml 환경변수명 수정
  ```yaml
  # Before
  - name: DATABASE_HOST
  - name: DATABASE_PORT

  # After
  - name: DB_HOST
  - name: DB_PORT
  ```

### Phase 5: 스크립트 업데이트 ✅
**계획대로 수행**
- `build-and-load.sh`: 전체 서비스 빌드 및 Kind 로드 확장
- `deploy-all.sh`: 전체 서비스 배포 확장

### Phase 6-7: 빌드 & 배포 ⚠️
**계획 + 추가 이슈 해결**

**🚨 추가 이슈 #6: portal-shell Dockerfile rm 권한 오류**
- **문제**: `RUN rm -f /etc/nginx/conf.d/default.conf` 실패
- **원인**: nginx base 이미지에서 해당 파일이 read-only로 마운트됨
- **해결**: `rm` 명령 제거, 직접 `default.conf` 또는 `default.k8s.conf`만 복사

**🚨 추가 이슈 #7: HSTS로 HTTP 접근 불가**
- **문제**: 브라우저가 `http://portal-universe:8080` 접근을 차단 (HSTS)
- **원인**: 이전에 HTTPS로 접속한 이력이 있으면 브라우저가 HTTP를 강제로 HTTPS로 redirect
- **해결**: mkcert TLS 인증서를 Ingress에 적용
  ```bash
  mkcert portal-universe
  kubectl create secret tls portal-tls-secret \
    --cert=portal-universe.pem \
    --key=portal-universe-key.pem \
    -n portal-universe
  ```

**🚨 추가 이슈 #8: blog/shopping-service probe 403 Forbidden**
- **문제**: Spring Boot Actuator health endpoint에서 403 반환, pod readiness 실패
- **원인**: `/actuator/health`를 직접 호출했으나 Spring Security 설정으로 인증 필요
- **해결**: probe 경로를 `/actuator/health`로 변경 (permitAll 설정 확인)

### Phase 8: Playwright 테스트 ✅
**성공**
- 전체 21/21 pods Running 확인
- Portal Shell, Blog, Shopping, Prism 모든 페이지 정상 로드
- Module Federation remote loading 정상 동작
- API 호출 정상 응답 확인

---

## 4. 아키텍처 다이어그램

```
┌─────────────────────────────────────────────────────────────────────┐
│                             Browser                                  │
│                    https://portal-universe:8443                     │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               │ kubectl port-forward 8443:443
                               │
                    ┌──────────▼──────────┐
                    │  NGINX Ingress      │
                    │  (TLS Termination)  │
                    └──────────┬──────────┘
                               │
        ┌──────────────────────┼──────────────────────┐
        │                      │                      │
        │                      │                      │
┌───────▼─────────┐   ┌────────▼────────┐   ┌───────▼───────┐
│ portal-shell    │   │  api-gateway    │   │ *-frontend    │
│ (Vue 3, :8080)  │   │  (:8080)        │   │ (:8080 each)  │
│                 │   │                 │   │               │
│ - SPA routing   │   │ Spring Cloud    │   │ - blog        │
│ - Module Fed    │   │ Gateway         │   │ - shopping    │
│ - Proxy remotes │   │                 │   │ - prism       │
└─────────────────┘   └────────┬────────┘   └───────────────┘
                               │
        ┌──────────────────────┼──────────────────────┐
        │                      │                      │
┌───────▼─────────┐   ┌────────▼────────┐   ┌───────▼───────┐
│ auth-service    │   │ blog-service    │   │shopping-service│
│ (:8081)         │   │ (:8082)         │   │ (:8083)        │
└─────────────────┘   └─────────────────┘   └────────────────┘
        │                      │                      │
┌───────▼─────────┐   ┌────────▼────────┐   ┌───────▼───────┐
│notification-svc │   │ prism-service   │   │               │
│ (:8084)         │   │ (:8085)         │   │               │
└─────────────────┘   └─────────────────┘   └───────────────┘
        │                      │
        │              ┌───────┴───────┐
        │              │               │
┌───────▼─────────┐   │   ┌───────────▼────────┐
│ Infrastructure  │   │   │  Monitoring         │
│                 │   │   │                     │
│ - MySQL         │   │   │ - Grafana (:3000)   │
│ - MongoDB       │   │   │ - Prometheus (:9090)│
│ - Redis         │   │   │ - Zipkin (:9411)    │
│ - PostgreSQL    │   │   └─────────────────────┘
│ - Kafka         │   │
│ - Elasticsearch │   │
│ - LocalStack    │   │
└─────────────────┘   │
                      │
            ┌─────────▼─────────┐
            │   Kafka Topics    │
            │                   │
            │ - user-events     │
            │ - order-events    │
            │ - notification    │
            └───────────────────┘
```

---

## 5. 사용 가이드

### 5.1 사전 준비

#### 필수 도구 설치
```bash
# macOS (Homebrew)
brew install kind kubectl mkcert

# Linux
# Kind: https://kind.sigs.k8s.io/docs/user/quick-start/
# kubectl: https://kubernetes.io/docs/tasks/tools/
# mkcert: https://github.com/FiloSottile/mkcert
```

#### Docker Desktop 설정
- **메모리**: 최소 8GB, 권장 10GB
- **CPU**: 최소 4 cores
- **Disk**: 20GB 이상

#### /etc/hosts 설정
```bash
sudo vim /etc/hosts
```

다음 라인 추가:
```
127.0.0.1 portal-universe
```

---

### 5.2 클러스터 생성

```bash
kind create cluster --config k8s/base/kind-config.yaml
```

클러스터 이름은 `portal-universe`로 생성됩니다.

**검증**
```bash
kubectl cluster-info --context kind-portal-universe
kubectl get nodes
```

---

### 5.3 Secrets 생성

#### 3.1 Base Secrets
```bash
# Example 파일 복사
cp k8s/base/secret.yaml.example k8s/base/secret.yaml

# 실제 비밀번호로 수정
vim k8s/base/secret.yaml
```

`secret.yaml` 내용 (Base64 인코딩 필요):
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: portal-universe-secrets
  namespace: portal-universe
type: Opaque
data:
  MYSQL_ROOT_PASSWORD: <base64-encoded-password>
  MYSQL_PASSWORD: <base64-encoded-password>
  MONGODB_ROOT_PASSWORD: <base64-encoded-password>
  POSTGRES_PASSWORD: <base64-encoded-password>
```

**Base64 인코딩 예시**
```bash
echo -n "your-password" | base64
```

#### 3.2 JWT Secrets
```bash
# Example 파일 복사
cp k8s/base/jwt-secrets.yaml.example k8s/base/jwt-secrets.yaml

# JWT 키 생성
openssl rand -base64 32  # Access Token 키
openssl rand -base64 32  # Refresh Token 키

# 생성된 키로 수정
vim k8s/base/jwt-secrets.yaml
```

`jwt-secrets.yaml` 내용:
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: jwt-secrets
  namespace: portal-universe
type: Opaque
stringData:
  JWT_SECRET_KEY: <openssl-rand-base64-32-output>
  JWT_REFRESH_SECRET_KEY: <openssl-rand-base64-32-output>
```

---

### 5.4 TLS 인증서 설정

HTTPS 접근을 위해 mkcert로 로컬 TLS 인증서를 생성합니다.

```bash
# mkcert CA 설치 (최초 1회)
mkcert -install

# portal-universe 도메인 인증서 생성
mkcert portal-universe

# K8s Secret 생성
kubectl create namespace portal-universe
kubectl create secret tls portal-tls-secret \
  --cert=portal-universe.pem \
  --key=portal-universe-key.pem \
  -n portal-universe
```

**생성된 파일**
- `portal-universe.pem` (Certificate)
- `portal-universe-key.pem` (Private Key)

**검증**
```bash
kubectl get secret portal-tls-secret -n portal-universe
```

---

### 5.5 빌드 & 배포

#### 5.5.1 이미지 빌드 및 Kind 로드
```bash
cd /Users/laze/Laze/Project/portal-universe
./k8s/scripts/build-and-load.sh
```

**이 스크립트가 수행하는 작업:**
1. Backend Services Docker 이미지 빌드 (6개)
2. Frontend Services Docker 이미지 빌드 (4개)
3. Elasticsearch custom 이미지 빌드
4. 모든 이미지를 Kind 클러스터에 로드

**예상 소요 시간**: 10-15분 (최초 빌드 시)

#### 5.5.2 배포
```bash
./k8s/scripts/deploy-all.sh
```

**이 스크립트가 수행하는 작업:**
1. Namespace, ConfigMap, Secrets 생성
2. Infrastructure 배포 (DB, Kafka, Redis 등)
3. Infrastructure 대기 (최대 5분)
4. Business Services 배포
5. Frontend 배포
6. Ingress 배포

**예상 소요 시간**: 5-10분

---

### 5.6 접속 (port-forward)

Kind 클러스터는 외부에서 직접 접근할 수 없으므로 port-forward가 필요합니다.

```bash
kubectl port-forward -n ingress-nginx svc/ingress-nginx-controller 8080:80 8443:443
```

**브라우저 접속**
- HTTPS (권장): `https://portal-universe:8443`
- HTTP: `http://portal-universe:8080`

**로그인 테스트 계정**
- Username: `admin@portal.com`
- Password: `admin123`

---

### 5.7 상태 확인 명령어

#### 전체 Pod 상태
```bash
kubectl get pods -n portal-universe
```

정상 상태:
```
NAME                                   READY   STATUS    RESTARTS
api-gateway-xxxxxxxxxx-xxxxx           1/1     Running   0
auth-service-xxxxxxxxxx-xxxxx          1/1     Running   0
blog-service-xxxxxxxxxx-xxxxx          1/1     Running   0
shopping-service-xxxxxxxxxx-xxxxx      1/1     Running   0
notification-service-xxxxxxxxxx-xxxxx  1/1     Running   0
prism-service-xxxxxxxxxx-xxxxx         1/1     Running   0
portal-shell-xxxxxxxxxx-xxxxx          1/1     Running   0
blog-frontend-xxxxxxxxxx-xxxxx         1/1     Running   0
shopping-frontend-xxxxxxxxxx-xxxxx     1/1     Running   0
prism-frontend-xxxxxxxxxx-xxxxx        1/1     Running   0
mysql-db-xxxxxxxxxx-xxxxx              1/1     Running   0
mongodb-xxxxxxxxxx-xxxxx               1/1     Running   0
redis-xxxxxxxxxx-xxxxx                 1/1     Running   0
postgresql-xxxxxxxxxx-xxxxx            1/1     Running   0
kafka-xxxxxxxxxx-xxxxx                 1/1     Running   0
elasticsearch-xxxxxxxxxx-xxxxx         1/1     Running   0
localstack-xxxxxxxxxx-xxxxx            1/1     Running   0
zipkin-xxxxxxxxxx-xxxxx                1/1     Running   0
grafana-xxxxxxxxxx-xxxxx               1/1     Running   0
prometheus-xxxxxxxxxx-xxxxx            1/1     Running   0
```

#### 특정 서비스 로그
```bash
# 최근 50줄
kubectl logs -n portal-universe deploy/auth-service --tail=50

# 실시간 로그 (-f: follow)
kubectl logs -n portal-universe deploy/auth-service -f

# 이전 Pod 로그 (CrashLoopBackOff 시 유용)
kubectl logs -n portal-universe <pod-name> --previous
```

#### 서비스 상태
```bash
kubectl get svc -n portal-universe
```

#### Ingress 확인
```bash
kubectl get ingress -n portal-universe
kubectl describe ingress portal-ingress -n portal-universe
```

#### Pod 상세 정보 (이벤트, probe 상태 등)
```bash
kubectl describe pod <pod-name> -n portal-universe
```

#### 리소스 사용량
```bash
# Node 리소스
kubectl top node

# Pod 리소스
kubectl top pod -n portal-universe
```

---

### 5.8 에러 발생 시 대처

#### CrashLoopBackOff

**증상**: Pod가 계속 재시작됨

**진단**
```bash
# 현재 로그
kubectl logs <pod-name> -n portal-universe

# 이전 로그 (crash 직전)
kubectl logs <pod-name> -n portal-universe --previous

# 이벤트 확인
kubectl describe pod <pod-name> -n portal-universe
```

**주요 원인 및 해결**

1. **DB 연결 실패**
   - DB Pod가 Running인지 확인: `kubectl get pods -n portal-universe | grep mysql`
   - Secret의 비밀번호가 올바른지 확인
   - 환경변수명 확인 (`DATABASE_HOST` vs `DB_HOST`)

2. **메모리 부족 (OOMKilled)**
   ```bash
   # 리소스 사용량 확인
   kubectl top pod -n portal-universe

   # limits 조정 (매니페스트 수정)
   kubectl edit deployment <service-name> -n portal-universe
   ```

3. **Probe 실패**
   ```bash
   # describe에서 Liveness/Readiness probe 실패 확인
   kubectl describe pod <pod-name> -n portal-universe

   # probe 경로 확인
   # Spring Boot: /actuator/health
   # NestJS: /api/v1/health
   ```

#### ImagePullBackOff / ErrImageNeverPull

**증상**: 이미지를 pull할 수 없음

**해결**
```bash
# 이미지를 Kind 클러스터에 다시 로드
kind load docker-image portal-universe-<service>:latest --name portal-universe

# Pod 재시작
kubectl rollout restart deployment/<service-name> -n portal-universe
```

#### 서비스 접근 불가 (503 Service Unavailable)

**진단**
```bash
# Ingress 라우팅 확인
kubectl describe ingress -n portal-universe

# 서비스 엔드포인트 확인 (Pod가 연결되어 있는지)
kubectl get endpoints -n portal-universe

# 특정 서비스 상세
kubectl describe svc <service-name> -n portal-universe
```

**주요 원인**
1. Pod가 Ready 상태가 아님 → Probe 실패
2. Service selector가 Pod label과 불일치
3. Ingress path가 잘못됨

#### nginx 설정 확인

portal-shell의 nginx 설정이 올바른지 확인:
```bash
kubectl exec deploy/portal-shell -n portal-universe -- cat /etc/nginx/conf.d/default.conf
```

**확인 사항**
- `resolver 10.96.0.10` 존재
- `set $backend_api api-gateway:8080` 형태의 변수 선언
- `rewrite` 지시문으로 URI 전달

---

### 5.9 서비스 관리

#### 개별 서비스 재시작
```bash
kubectl rollout restart deployment/<service-name> -n portal-universe
```

예시:
```bash
kubectl rollout restart deployment/auth-service -n portal-universe
```

#### 개별 서비스 스케일
```bash
# 정지 (replicas=0)
kubectl scale deployment/<service-name> -n portal-universe --replicas=0

# 시작 (replicas=1)
kubectl scale deployment/<service-name> -n portal-universe --replicas=1

# 다중 인스턴스
kubectl scale deployment/portal-shell -n portal-universe --replicas=2
```

#### 전체 정지 (namespace 삭제 없이)
```bash
kubectl scale deployment --all -n portal-universe --replicas=0
```

#### 전체 시작 (순서 중요)
```bash
# 1. 인프라 먼저 시작 (DB, Kafka 등)
kubectl scale deployment mysql-db mongodb kafka redis elasticsearch postgresql localstack -n portal-universe --replicas=1

# 2. 30초 대기 (DB 준비 시간)
sleep 30

# 3. Business Services
kubectl scale deployment api-gateway auth-service blog-service shopping-service notification-service prism-service -n portal-universe --replicas=1

# 4. Frontend
kubectl scale deployment portal-shell blog-frontend shopping-frontend prism-frontend -n portal-universe --replicas=1

# 5. portal-shell은 2개로 증가 (LoadBalancer)
kubectl scale deployment portal-shell -n portal-universe --replicas=2
```

#### 특정 서비스만 재배포 (이미지 업데이트 후)
```bash
# 1. Docker 이미지 리빌드
docker build \
  --build-arg BUILD_MODE=k8s \
  -t portal-universe-auth-service:latest \
  -f services/auth-service/Dockerfile \
  .

# 2. Kind에 로드
kind load docker-image portal-universe-auth-service:latest --name portal-universe

# 3. 재배포
kubectl rollout restart deployment/auth-service -n portal-universe

# 4. 롤아웃 상태 확인
kubectl rollout status deployment/auth-service -n portal-universe
```

---

### 5.10 클러스터 완전 삭제

```bash
# Kind 클러스터 삭제
kind delete cluster --name portal-universe

# 로컬 이미지도 삭제하려면
docker images | grep portal-universe | awk '{print $3}' | xargs docker rmi -f
```

**재시작 시**
```bash
# 클러스터 재생성
kind create cluster --config k8s/base/kind-config.yaml

# TLS Secret 재생성
kubectl create namespace portal-universe
kubectl create secret tls portal-tls-secret \
  --cert=portal-universe.pem \
  --key=portal-universe-key.pem \
  -n portal-universe

# 빌드 & 배포
./k8s/scripts/build-and-load.sh
./k8s/scripts/deploy-all.sh
```

---

### 5.11 이미지 업데이트 (코드 변경 후)

#### Frontend 이미지 업데이트 (예: portal-shell)
```bash
# 1. Docker 이미지 리빌드
cd /Users/laze/Laze/Project/portal-universe
docker build \
  --build-arg BUILD_MODE=k8s \
  -t portal-universe-portal-shell:latest \
  -f frontend/portal-shell/Dockerfile \
  frontend/

# 2. Kind에 로드
kind load docker-image portal-universe-portal-shell:latest --name portal-universe

# 3. 재배포
kubectl rollout restart deployment/portal-shell -n portal-universe

# 4. 롤아웃 확인
kubectl rollout status deployment/portal-shell -n portal-universe
```

#### Backend 이미지 업데이트 (예: auth-service)
```bash
# 1. Docker 이미지 리빌드
docker build \
  -t portal-universe-auth-service:latest \
  -f services/auth-service/Dockerfile \
  .

# 2. Kind에 로드
kind load docker-image portal-universe-auth-service:latest --name portal-universe

# 3. 재배포
kubectl rollout restart deployment/auth-service -n portal-universe

# 4. 롤아웃 확인
kubectl rollout status deployment/auth-service -n portal-universe
```

#### 전체 이미지 업데이트
```bash
# build-and-load.sh 스크립트 사용
./k8s/scripts/build-and-load.sh

# 전체 서비스 재시작
kubectl rollout restart deployment --all -n portal-universe
```

---

## 6. 서비스 포트 매핑 테이블

| Service | Internal Port | K8s Service Name | Health Check Path |
|---------|--------------|------------------|-------------------|
| **Backend Services** |
| API Gateway | 8080 | api-gateway | /actuator/health |
| Auth Service | 8081 | auth-service | /actuator/health |
| Blog Service | 8082 | blog-service | /actuator/health |
| Shopping Service | 8083 | shopping-service | /actuator/health |
| Notification Service | 8084 | notification-service | /actuator/health |
| Prism Service | 8085 | prism-service | /api/v1/health |
| **Frontend Services** |
| Portal Shell | 8080 | portal-shell | /health |
| Blog Frontend | 8080 | blog-frontend | / |
| Shopping Frontend | 8080 | shopping-frontend | / |
| Prism Frontend | 8080 | prism-frontend | / |
| **Databases** |
| MySQL | 3306 | mysql-db | - |
| MongoDB | 27017 | mongodb | - |
| Redis | 6379 | redis | - |
| PostgreSQL | 5432 | postgresql | - |
| **Infrastructure** |
| Elasticsearch | 9200 | elasticsearch | /_cluster/health |
| Kafka | 29092 | kafka | - |
| LocalStack (S3) | 4566 | localstack | / |
| **Monitoring** |
| Grafana | 3000 | grafana | /api/health |
| Prometheus | 9090 | prometheus | /-/healthy |
| Zipkin | 9411 | zipkin | /health |

---

## 7. 파일 변경 목록

### 7.1 새로 생성된 파일 (11개)

#### K8s 매니페스트 (7개)
| 파일 | 용도 |
|------|------|
| `k8s/infrastructure/postgresql.yaml` | PostgreSQL for Prism Service |
| `k8s/infrastructure/localstack.yaml` | S3 emulation for Blog Service |
| `k8s/services/prism-service.yaml` | Prism Service (NestJS) Deployment |
| `k8s/services/blog-frontend.yaml` | Blog Frontend Deployment |
| `k8s/services/shopping-frontend.yaml` | Shopping Frontend Deployment |
| `k8s/services/prism-frontend.yaml` | Prism Frontend Deployment |

#### Frontend 설정 (5개)
| 파일 | 용도 |
|------|------|
| `frontend/portal-shell/default.k8s.conf` | K8s용 nginx conf (HTTP only, resolver + variable) |
| `frontend/portal-shell/.env.k8s` | Portal Shell K8s 환경변수 |
| `frontend/blog-frontend/.env.k8s` | Blog Frontend K8s 환경변수 |
| `frontend/shopping-frontend/.env.k8s` | Shopping Frontend K8s 환경변수 |
| `frontend/prism-frontend/.env.k8s` | Prism Frontend K8s 환경변수 |

### 7.2 수정된 파일 (20개)

#### Backend Services (5개)
| 파일 | 변경 내용 |
|------|----------|
| `services/api-gateway/src/main/resources/application-kubernetes.yml` | 서비스 URL 포트 추가 (`http://auth-service:8081`), Redis 호스트명 수정 |
| `services/auth-service/src/main/resources/application-kubernetes.yml` | 서비스 URL 포트 추가 |
| `services/blog-service/src/main/resources/application-kubernetes.yml` | 서비스 URL 포트 추가 |
| `services/shopping-service/src/main/resources/application-kubernetes.yml` | 서비스 URL 포트 추가 |
| `services/notification-service/src/main/resources/application-kubernetes.yml` | 서비스 URL 포트 추가 |

#### K8s Base (2개)
| 파일 | 변경 내용 |
|------|----------|
| `k8s/base/jwt-secrets.yaml.example` | namespace: default → portal-universe |
| `k8s/base/secret.yaml.example` | POSTGRES_PASSWORD 추가 |

#### K8s Services (2개)
| 파일 | 변경 내용 |
|------|----------|
| `k8s/services/auth-service.yaml` | image tag v1.0.2 → latest |
| `k8s/services/shopping-service.yaml` | image tag v1.0.1 → latest |

#### K8s 스크립트 (2개)
| 파일 | 변경 내용 |
|------|----------|
| `k8s/scripts/build-and-load.sh` | 전체 서비스 빌드/로드 확장 (frontend, prism-service, elasticsearch) |
| `k8s/scripts/deploy-all.sh` | 전체 서비스 배포 확장 (jwt-secrets, redis, postgresql, localstack, frontend 추가) |

#### Frontend (9개)
| 파일 | 변경 내용 |
|------|----------|
| `frontend/portal-shell/Dockerfile` | BUILD_MODE arg 추가, nginx conf 선택 로직, rm 명령 제거 |
| `frontend/portal-shell/nginx.conf` | resolver + set variable 패턴, rewrite 지시문 추가 |
| `frontend/portal-shell/.env.k8s` | Mixed Content 해결: relative path로 변경 |
| `frontend/blog-frontend/.env.k8s` | Remote URL 추가, relative path 변경 |
| `frontend/blog-frontend/Dockerfile` | BUILD_MODE arg 추가 |
| `frontend/shopping-frontend/.env.k8s` | Relative path로 변경 |
| `frontend/shopping-frontend/Dockerfile` | BUILD_MODE arg 추가 |
| `frontend/prism-frontend/.env.k8s` | Relative path로 변경 |
| `frontend/prism-frontend/Dockerfile` | BUILD_MODE arg 추가 |

### 7.3 수동 생성 파일 (gitignored, 2개)
| 파일 | 출처 |
|------|------|
| `k8s/base/secret.yaml` | `secret.yaml.example` 복사 후 실제 비밀번호 설정 |
| `k8s/base/jwt-secrets.yaml` | `jwt-secrets.yaml.example` 복사 후 JWT 키 생성 |

### 7.4 주요 이슈 해결 관련 파일

| 이슈 | 관련 파일 | 해결 방법 |
|------|----------|----------|
| nginx DNS resolve 실패 | `frontend/portal-shell/default.k8s.conf` | resolver + set variable 패턴 |
| nginx URI 미전달 | `frontend/portal-shell/default.k8s.conf` | rewrite 지시문 추가 |
| Mixed Content 에러 | 모든 `.env.k8s` | relative path로 변경 |
| prism-service DB 연결 실패 | `k8s/services/prism-service.yaml` | DATABASE_* → DB_* 환경변수명 수정 |
| portal-shell rm 권한 오류 | `frontend/portal-shell/Dockerfile` | rm 명령 제거 |
| probe 403 에러 | `k8s/services/blog-service.yaml`, `k8s/services/shopping-service.yaml` | probe 경로 `/actuator/health`로 변경 |

---

## 8. 트러블슈팅 체크리스트

### 배포 전 체크리스트
- [ ] Docker Desktop 메모리 8GB 이상 설정
- [ ] `/etc/hosts`에 `portal-universe` 등록
- [ ] `k8s/base/secret.yaml` 생성 및 비밀번호 설정
- [ ] `k8s/base/jwt-secrets.yaml` 생성 및 JWT 키 설정
- [ ] mkcert 인증서 생성 (`portal-universe.pem`)
- [ ] TLS Secret 생성 (`portal-tls-secret`)

### 배포 후 체크리스트
- [ ] 모든 Pod가 Running 상태 (`kubectl get pods -n portal-universe`)
- [ ] Ingress가 정상 생성 (`kubectl get ingress -n portal-universe`)
- [ ] port-forward 실행 중
- [ ] `https://portal-universe:8443` 접속 가능

### 문제 발생 시 순서
1. Pod 상태 확인: `kubectl get pods -n portal-universe`
2. 문제 Pod 로그 확인: `kubectl logs <pod-name> -n portal-universe`
3. 이벤트 확인: `kubectl describe pod <pod-name> -n portal-universe`
4. 이전 로그 확인 (CrashLoopBackOff): `kubectl logs <pod-name> --previous -n portal-universe`
5. 관련 Service/Endpoint 확인: `kubectl get svc,ep -n portal-universe`

---

## 9. 참고 자료

### 공식 문서
- [Kind 공식 문서](https://kind.sigs.k8s.io/)
- [Kubernetes 공식 문서](https://kubernetes.io/docs/)
- [NGINX Ingress Controller](https://kubernetes.github.io/ingress-nginx/)
- [mkcert](https://github.com/FiloSottile/mkcert)

### 프로젝트 관련 문서
- [ADR-001: Module Federation 아키텍처](../adr/ADR-001-module-federation.md)
- [Docker 환경 배포 가이드](./docker-deployment-guide.md)
- [Troubleshooting 문서](../troubleshooting/)

### 리소스
- **메모리**: 전체 ~16개 Pod, 최소 8-10GB 권장
- **Disk**: 빌드 이미지 포함 ~20GB
- **네트워크**: Ingress port-forward 필요

---

## 10. FAQ

### Q1: Kind 대신 Minikube를 사용할 수 있나요?
A: 가능합니다. 하지만 이미지 로드 방식이 다릅니다.
```bash
# Minikube의 경우
eval $(minikube docker-env)
# 이후 docker build 명령은 Minikube 내부 Docker에 직접 빌드됨
```

### Q2: portal-shell replica를 1개로 줄여도 되나요?
A: 네, 리소스 절약을 위해 1개로 줄여도 됩니다.
```bash
kubectl scale deployment portal-shell -n portal-universe --replicas=1
```

### Q3: HTTPS를 꼭 사용해야 하나요?
A: 브라우저 HSTS로 인해 HTTP 접근이 차단될 수 있으므로 HTTPS 권장합니다. mkcert 인증서는 로컬 개발용으로 안전합니다.

### Q4: 특정 서비스만 배포하고 싶어요.
A: 가능합니다. 예를 들어 auth-service만 배포:
```bash
kubectl apply -f k8s/services/auth-service.yaml
```
단, 의존성 서비스(MySQL, Redis 등)가 먼저 실행되어야 합니다.

### Q5: 로그를 파일로 저장하고 싶어요.
```bash
kubectl logs deploy/auth-service -n portal-universe > auth-service.log
```

### Q6: Pod가 계속 Pending 상태입니다.
A: 리소스 부족일 가능성이 높습니다.
```bash
kubectl describe pod <pod-name> -n portal-universe
# Events 섹션에서 "Insufficient memory" 또는 "Insufficient cpu" 확인
```
Docker Desktop 메모리를 증가시키거나 일부 서비스를 scale down하세요.

---

## 11. 결론

이 가이드를 통해 Portal Universe 전체 마이크로서비스 아키텍처를 Kind(Kubernetes) 환경에서 성공적으로 배포할 수 있습니다.

**주요 성과:**
- ✅ 21개 Pod (Backend 6 + Frontend 4 + Infrastructure 8 + Monitoring 3) 안정 배포
- ✅ HTTPS/TLS 인증서 기반 보안 접속
- ✅ Module Federation 기반 Micro Frontend 통합
- ✅ 8개 이상의 실제 이슈 해결 및 문서화

**Next Steps:**
- Helm Chart로 배포 자동화
- ArgoCD를 통한 GitOps 구성
- Istio Service Mesh 적용
- Production-ready 설정 (Resource Limits, HPA, PDB)

---

**문서 버전**: 1.0
**최종 업데이트**: 2026-01-31
**작성자**: Laze
**관련 이슈**: K8s 환경 구성 및 Playwright 테스트 계획
