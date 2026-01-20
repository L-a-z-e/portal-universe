---
id: TS-20260121-003
title: Kubernetes 배포 중 발생한 복합 인프라 이슈
type: troubleshooting
status: resolved
created: 2026-01-21
updated: 2026-01-21
author: Laze
severity: high
resolved: true
affected_services: [notification-service, auth-service, shopping-service, all-services]
tags: [kubernetes, redis, kind, docker, image-pull, oauth2, spring-security, elasticsearch, nori]
---

# Kubernetes 배포 중 발생한 복합 인프라 이슈

## 요약

| 항목 | 내용 |
|------|------|
| **심각도** | 🟠 High |
| **발생일** | 2026-01-21 |
| **해결일** | 2026-01-21 ✅ |
| **영향 서비스** | notification-service, auth-service, shopping-service, 전체 배포 프로세스 |

## 증상 (Symptoms)

### 문제 1: Notification Service CrashLoopBackOff
- Notification Service Pod가 시작 직후 CrashLoopBackOff 상태 반복
- Pod가 Redis 연결을 시도하지만 실패

### 문제 2: ErrImageNeverPull
- 모든 서비스 Pod가 `ErrImageNeverPull` 에러 발생
- Pod가 로컬에서 빌드된 이미지를 찾지 못함

### 에러 메시지 (문제 2)
```
Failed to pull image "portal-universe/notification-service:latest": rpc error: code = Unknown desc = Error response from daemon: pull access denied for portal-universe/notification-service, repository does not exist or may require 'docker login'
```

또는 imagePullPolicy: Never 설정 시:
```
ErrImageNeverPull: Container image "portal-universe/notification-service:v1.0.1" is not present with pull policy of Never
```

### 문제 3: Auth Service ClientRegistrationRepository 에러

#### 현상
- Auth Service Pod가 Error 상태로 재시작 반복
- OAuth2 Client 설정 관련 Bean을 찾지 못함

#### 에러 메시지
```
Parameter 0 of method setFilterChains in org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration required a bean of type 'org.springframework.security.oauth2.client.registration.ClientRegistrationRepository' that could not be found.
```

#### 긍정적 확인
- `redirect-uri` 설정은 올바르게 적용됨: `http://portal-universe/callback`

---

### 문제 4: Shopping Service Elasticsearch Nori Tokenizer 에러

#### 현상
- Shopping Service Pod가 CrashLoopBackOff 상태로 5회 이상 재시작
- Elasticsearch 인덱스 생성 시 한국어 형태소 분석기(Nori) 관련 에러 발생

#### 에러 메시지
```
ElasticsearchException: [es/indices.create] failed: [illegal_argument_exception]
Custom Analyzer [korean] failed to find tokenizer under name [nori_tokenizer]
```

## 원인 분석 (Root Cause)

### 문제 1: Redis 미배포

#### 실제 원인
- Redis Deployment/Service 매니페스트가 K8s 인프라에 존재하지 않음
- Notification Service가 Spring Boot 시작 시 Redis 연결 필수 설정으로 되어 있어 연결 실패 시 Pod 종료

#### 분석 과정
```bash
# Pod 로그 확인
kubectl logs -n portal-universe notification-service-xxxxx

# Redis Service 확인
kubectl get svc -n portal-universe | grep redis
# (결과: Redis 없음)
```

---

### 문제 2: Docker Desktop K8s vs Kind 이미지 동기화

#### 초기 추정
- `imagePullPolicy: Never`와 `:latest` 태그 조합 문제
- 이미지 ID 불일치

#### 실제 원인
- **Kind 클러스터는 Docker Desktop K8s와 독립적인 containerd 사용**
- 로컬 Docker에서 빌드한 이미지가 Kind 클러스터의 containerd에 존재하지 않음
- `docker images`로 보이는 이미지는 Docker Desktop의 이미지이며, Kind는 이를 직접 접근할 수 없음

#### 분석 과정
1. 현재 K8s 컨텍스트 확인
```bash
kubectl config current-context
# 결과: kind-portal-universe
```

2. Docker 이미지 ID 확인
```bash
docker images | grep notification-service
# 결과: portal-universe/notification-service  latest  abc123...
```

3. Kind 클러스터 내부 이미지 확인 (없음)
```bash
docker exec -it portal-universe-control-plane crictl images
# Kind의 containerd에는 이미지가 없음
```

4. 버전 태그 시도 (실패)
```bash
# v1.0.1 태그로 빌드했지만 동일한 에러 발생
```

---

### 문제 3: Auth Service ClientRegistrationRepository 에러

#### 초기 추정
- Spring Security OAuth2 Client 의존성 누락
- `application-kubernetes.yml`에 OAuth2 Client 설정 누락

#### 실제 원인
- `application-kubernetes.yml`에 OAuth2 Client 설정이 완전히 누락됨
- `SecurityConfig.java`에서 `.oauth2Login()`이 활성화되어 있어 Spring Security가 `ClientRegistrationRepository` bean을 필수로 요구함
- 다른 프로파일(`application-docker.yml`, `application.yml`)에는 OAuth2 설정이 존재하지만, K8s 프로파일에는 누락

#### 분석 과정
1. Auth Service 로그 확인
```bash
kubectl logs -n portal-universe auth-service-xxxxx
# OAuth2 Client bean 찾을 수 없음 확인
```

2. 설정 파일 비교
- `application.yml`: OAuth2 설정 존재 ✓
- `application-docker.yml`: OAuth2 설정 존재 ✓
- `application-kubernetes.yml`: OAuth2 설정 누락 ✗

3. SecurityConfig 확인
```java
http.oauth2Login(...)  // 활성화됨
```

---

### 문제 4: Shopping Service Elasticsearch Nori Tokenizer 에러

#### 초기 추정
- Shopping Service 설정 오류
- Elasticsearch 연결 문제

#### 실제 원인
- K8s Elasticsearch가 기본 이미지(`docker.elastic.co/elasticsearch/elasticsearch:8.18.5`)를 사용
- 기본 이미지에는 **Nori(한국어 형태소 분석기) 플러그인이 포함되지 않음**
- Shopping Service가 인덱스 생성 시 `nori_tokenizer`를 사용하는 커스텀 Analyzer를 정의했으나, Elasticsearch에 해당 플러그인이 없어 실패

#### 분석 과정
1. Shopping Service 로그 확인
```bash
kubectl logs -n portal-universe shopping-service-xxxxx
# Nori tokenizer not found 에러 확인
```

2. Elasticsearch 플러그인 확인
```bash
kubectl exec -it elasticsearch-0 -n portal-universe -- bin/elasticsearch-plugin list
# (결과: analysis-nori 없음)
```

3. 인덱스 설정 확인
- Shopping Service가 `korean` analyzer 정의 시 `nori_tokenizer` 사용
- Elasticsearch에 해당 플러그인 미설치

## 해결 방법 (Solution)

### 문제 1: Redis 미배포 ✅ 해결됨

#### 즉시 조치
```bash
# Redis 매니페스트 생성
cat > k8s/infrastructure/redis.yaml << 'EOF'
apiVersion: v1
kind: Service
metadata:
  name: redis
  namespace: portal-universe
spec:
  selector:
    app: redis
  ports:
    - protocol: TCP
      port: 6379
      targetPort: 6379
  type: ClusterIP
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: redis
  namespace: portal-universe
spec:
  replicas: 1
  selector:
    matchLabels:
      app: redis
  template:
    metadata:
      labels:
        app: redis
    spec:
      containers:
      - name: redis
        image: redis:7-alpine
        ports:
        - containerPort: 6379
        resources:
          requests:
            memory: "128Mi"
            cpu: "100m"
          limits:
            memory: "256Mi"
            cpu: "200m"
EOF

# Redis 배포
kubectl apply -f k8s/infrastructure/redis.yaml
```

#### 영구 조치
- Redis 매니페스트를 Git에 커밋하여 인프라의 일부로 유지

#### 수정된 파일
| 파일 경로 | 수정 내용 |
|----------|----------|
| `k8s/infrastructure/redis.yaml` | 신규 생성 - Redis Deployment/Service 정의 |

---

### 문제 2: Docker Desktop K8s vs Kind 이미지 동기화 ✅ 해결됨

#### 즉시 조치
```bash
# 모든 서비스 이미지를 Kind 클러스터로 로드
kind load docker-image portal-universe/auth-service:v1.0.1 --name portal-universe
kind load docker-image portal-universe/blog-service:v1.0.1 --name portal-universe
kind load docker-image portal-universe/shopping-service:v1.0.1 --name portal-universe
kind load docker-image portal-universe/notification-service:v1.0.1 --name portal-universe
kind load docker-image portal-universe/gateway:v1.0.1 --name portal-universe

# 또는 전체 이미지 일괄 로드 스크립트
for service in auth-service blog-service shopping-service notification-service gateway; do
  kind load docker-image portal-universe/$service:v1.0.1 --name portal-universe
done
```

#### 영구 조치
1. **빌드 스크립트에 Kind 로드 추가**
```bash
# scripts/build-and-load.sh
#!/bin/bash
VERSION=${1:-latest}

# 빌드
docker build -t portal-universe/auth-service:$VERSION ./services/auth-service
docker build -t portal-universe/blog-service:$VERSION ./services/blog-service
# ... 다른 서비스들

# Kind 클러스터로 로드
if [ "$(kubectl config current-context)" = "kind-portal-universe" ]; then
  echo "Loading images to Kind cluster..."
  for service in auth-service blog-service shopping-service notification-service gateway; do
    kind load docker-image portal-universe/$service:$VERSION --name portal-universe
  done
fi
```

2. **Deployment 매니페스트 수정**
```yaml
spec:
  containers:
  - name: auth-service
    image: portal-universe/auth-service:v1.0.1
    imagePullPolicy: IfNotPresent  # Never 대신 IfNotPresent 사용
```

#### 수정된 파일
| 파일 경로 | 수정 내용 |
|----------|----------|
| `k8s/services/*.yaml` | `imagePullPolicy: IfNotPresent` 적용, 버전 태그 사용 |
| `scripts/build-and-load.sh` | (예정) Kind 이미지 로드 자동화 스크립트 |

---

### 문제 3: Auth Service ClientRegistrationRepository 에러 ✅ 해결됨

#### 즉시 조치
`application-kubernetes.yml`에 OAuth2 Client 설정 추가:

```yaml
# services/auth-service/src/main/resources/application-kubernetes.yml
spring.security.oauth2.client:
  registration:
    google:
      client-id: ${GOOGLE_CLIENT_ID:dummy}
      client-secret: ${GOOGLE_CLIENT_SECRET:dummy}
      redirect-uri: http://portal-universe/auth-service/login/oauth2/code/google
      scope:
        - email
        - profile
  provider:
    google:
      authorization-uri: https://accounts.google.com/o/oauth2/v2/auth
      token-uri: https://oauth2.googleapis.com/token
      user-info-uri: https://www.googleapis.com/oauth2/v3/userinfo
      user-name-attribute: sub
```

#### 재배포
```bash
# Auth Service 이미지 재빌드
cd services/auth-service
./gradlew clean bootJar
docker build -t portal-universe/auth-service:v1.0.2 .

# Kind 클러스터로 로드
kind load docker-image portal-universe/auth-service:v1.0.2 --name portal-universe

# Deployment 업데이트
kubectl set image deployment/auth-service auth-service=portal-universe/auth-service:v1.0.2 -n portal-universe
```

#### 영구 조치
- 모든 프로파일(`local`, `docker`, `k8s`)에 OAuth2 설정 포함 확인
- K8s ConfigMap이나 Secret으로 실제 OAuth2 Credentials 관리

#### 수정된 파일
| 파일 경로 | 수정 내용 |
|----------|----------|
| `services/auth-service/src/main/resources/application-kubernetes.yml` | OAuth2 Client 설정 추가 |
| `k8s/services/auth-service.yaml` | 이미지 버전 v1.0.2로 업데이트 |

---

### 문제 4: Shopping Service Elasticsearch Nori Tokenizer 에러 ✅ 해결됨

#### 즉시 조치

**1. 커스텀 Elasticsearch Dockerfile 작성**
```dockerfile
# infrastructure/elasticsearch/Dockerfile
FROM docker.elastic.co/elasticsearch/elasticsearch:8.18.5
RUN bin/elasticsearch-plugin install --batch analysis-nori
```

**2. 커스텀 이미지 빌드 및 Kind 로드**
```bash
# 이미지 빌드
docker build -t portal-universe-elasticsearch:v1.0.0 \
  -f infrastructure/elasticsearch/Dockerfile \
  infrastructure/elasticsearch/

# Kind 클러스터로 로드
kind load docker-image portal-universe-elasticsearch:v1.0.0 --name portal-universe
```

**3. K8s 매니페스트 수정**
```yaml
# k8s/infrastructure/elasticsearch.yaml
spec:
  containers:
  - name: elasticsearch
    image: portal-universe-elasticsearch:v1.0.0
    imagePullPolicy: Never  # Kind 로컬 이미지 사용
```

**4. StatefulSet 재배포**
```bash
# 기존 StatefulSet 삭제 (PVC는 유지)
kubectl delete statefulset elasticsearch -n portal-universe

# 새 매니페스트 적용
kubectl apply -f k8s/infrastructure/elasticsearch.yaml

# Elasticsearch 준비 대기
kubectl wait --for=condition=ready pod/elasticsearch-0 -n portal-universe --timeout=120s
```

**5. Shopping Service 재시작**
```bash
kubectl rollout restart deployment/shopping-service -n portal-universe
```

#### 영구 조치
1. **커스텀 이미지 관리**
   - `infrastructure/elasticsearch/Dockerfile` 유지
   - 버전 업그레이드 시 Nori 플러그인 포함 확인

2. **문서화**
   - README에 커스텀 Elasticsearch 이미지 빌드 절차 추가
   - 로컬 개발 환경 가이드에 Nori 플러그인 필수 명시

#### 수정된 파일
| 파일 경로 | 수정 내용 |
|----------|----------|
| `infrastructure/elasticsearch/Dockerfile` | 신규 생성 - Nori 플러그인 포함 |
| `k8s/infrastructure/elasticsearch.yaml` | 커스텀 이미지 사용, imagePullPolicy: Never |

## 재발 방지 (Prevention)

### 인프라 체크리스트
- [ ] 모든 필수 인프라 컴포넌트 매니페스트 존재 확인 (Redis, Kafka, MySQL 등)
- [ ] Kind 클러스터 사용 시 이미지 로드 자동화 스크립트 실행
- [ ] 배포 전 `kubectl get all -n portal-universe` 로 의존성 서비스 확인

### 모니터링
```yaml
# Prometheus Alert 추가 (예정)
- alert: PodCrashLooping
  expr: rate(kube_pod_container_status_restarts_total[5m]) > 0
  annotations:
    description: "Pod {{ $labels.pod }} is crash looping"
```

### 프로세스 개선
1. **배포 전 체크리스트 문서 작성**
   - 필수 인프라 컴포넌트 목록
   - Kind 환경 전용 이미지 로드 절차
   - 환경별 설정 파일 검증

2. **CI/CD 파이프라인 강화**
   - 배포 전 의존성 서비스 health check
   - Kind 환경 감지 및 자동 이미지 로드

3. **문서화**
   - Kind vs Docker Desktop K8s 차이점 문서화
   - Local K8s 개발 환경 가이드 작성

## 학습 포인트

### 1. Kind는 독립적인 containerd 사용
- Kind 클러스터는 Docker Desktop과 별도의 컨테이너 런타임 사용
- `docker images`로 보이는 이미지가 Kind에서 자동으로 사용 가능한 것이 아님
- `kind load docker-image` 명령으로 명시적 로드 필요

### 2. imagePullPolicy 전략
- `Never`: 로컬에만 의존, 이미지가 없으면 실패
- `IfNotPresent`: 로컬에 없으면 pull 시도 (Kind에는 권장)
- `Always`: 항상 pull 시도 (프로덕션 권장)

### 3. 인프라 의존성 선언적 관리
- 모든 인프라 컴포넌트는 매니페스트로 관리
- 암묵적 의존성(Redis, Kafka 등)도 명시적으로 배포
- Helm Chart 또는 Kustomize 사용 고려

### 4. 로컬 K8s 환경 차이 인지
- Docker Desktop K8s: Docker 이미지 직접 사용 가능
- Kind: `kind load docker-image` 필수
- Minikube: `minikube image load` 또는 `eval $(minikube docker-env)` 필요

### 5. Spring Boot 설정 프로파일별 검증
- 각 프로파일(`local`, `docker`, `k8s`)별 설정 완전성 검증 필요
- OAuth2 같은 선택적 기능도 환경별 활성화/비활성화 명확히

### 6. 프로파일별 설정 완전성
- `application-{profile}.yml`에서 모든 필수 설정이 포함되어 있는지 확인 필요
- 한 프로파일에서 동작하는 설정이 다른 프로파일에서 누락될 수 있음
- 프로파일 간 설정 비교 자동화 고려

### 7. Spring Security OAuth2 의존성
- `.oauth2Login()` 사용 시 반드시 OAuth2 Client 설정 필요
- 설정이 없으면 `ClientRegistrationRepository` bean 생성 실패
- 개발 환경에서는 dummy 값이라도 설정 필요

### 8. 커스텀 플러그인 이미지 관리
- Elasticsearch, Kibana 등 플러그인이 필요한 서비스는 커스텀 이미지로 관리
- 기본 이미지는 최소한의 기능만 포함
- Dockerfile을 명확한 경로에 관리하고 README에 빌드 절차 문서화

### 9. Dockerfile 위치 및 문서화
- 커스텀 이미지용 Dockerfile은 `infrastructure/[service]/` 경로에 관리
- README에 이미지 빌드 및 Kind 로드 절차 명시
- 버전 관리 및 CI/CD 파이프라인에 통합

## 관련 링크

- [Kind - Quick Start](https://kind.sigs.k8s.io/docs/user/quick-start/)
- [Kind - Loading an Image Into Your Cluster](https://kind.sigs.k8s.io/docs/user/quick-start/#loading-an-image-into-your-cluster)
- [Spring Security OAuth2 Client](https://docs.spring.io/spring-security/reference/servlet/oauth2/client/index.html)
- [Kubernetes imagePullPolicy](https://kubernetes.io/docs/concepts/containers/images/#image-pull-policy)
- [Elasticsearch Analysis Nori Plugin](https://www.elastic.co/guide/en/elasticsearch/plugins/current/analysis-nori.html)
- [Elasticsearch Plugin Management](https://www.elastic.co/guide/en/elasticsearch/reference/current/modules-plugins.html)

## 최종 결과

모든 문제가 해결되어 전체 시스템이 정상 운영 중입니다.

### Pod 상태 (전체 Running ✅)

```bash
$ kubectl get pods -n portal-universe

NAME                                      READY   STATUS    RESTARTS   AGE
api-gateway-xxx                           1/1     Running   0          10m
auth-service-xxx                          1/1     Running   0          5m   # v1.0.2
blog-service-xxx                          1/1     Running   0          10m
elasticsearch-0                           1/1     Running   0          8m   # v1.0.0 (Nori)
notification-service-xxx                  1/1     Running   0          10m
shopping-service-xxx                      1/1     Running   0          7m   # v1.0.1
redis-xxx                                 1/1     Running   0          10m
kafka-0                                   1/1     Running   0          10m
mongodb-0                                 1/1     Running   0          10m
mysql-0                                   1/1     Running   0          10m
```

### 해결된 이슈 요약

| 문제 | 상태 | 버전 | 해결 방법 |
|------|------|------|----------|
| **문제 1**: Redis 미배포 | ✅ 해결 | - | Redis Deployment/Service 생성 |
| **문제 2**: Kind 이미지 동기화 | ✅ 해결 | v1.0.1 | `kind load docker-image` 실행 |
| **문제 3**: Auth OAuth2 설정 누락 | ✅ 해결 | v1.0.2 | `application-kubernetes.yml` OAuth2 설정 추가 |
| **문제 4**: Elasticsearch Nori 플러그인 | ✅ 해결 | v1.0.0 | 커스텀 이미지 (Nori 포함) |

### 주요 변경 사항

1. **Redis 인프라 추가**
   - 파일: `k8s/infrastructure/redis.yaml`

2. **Auth Service 설정 수정**
   - 파일: `services/auth-service/src/main/resources/application-kubernetes.yml`
   - 이미지: `portal-universe/auth-service:v1.0.2`

3. **Elasticsearch 커스텀 이미지**
   - 파일: `infrastructure/elasticsearch/Dockerfile`
   - 이미지: `portal-universe-elasticsearch:v1.0.0`
   - 플러그인: `analysis-nori`

### 다음 개선 사항

1. [ ] 빌드 및 배포 자동화 스크립트 작성 (`scripts/build-and-load.sh`)
2. [ ] Local K8s 개발 환경 가이드 문서 작성
3. [ ] CI/CD 파이프라인에 환경별 이미지 로드 로직 추가
4. [ ] 프로파일별 설정 검증 자동화 도구 개발
5. [ ] Elasticsearch 커스텀 이미지 CI/CD 통합
