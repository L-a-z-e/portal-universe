# Kubernetes 배포 가이드

Portal Universe 프로젝트의 Kubernetes 배포를 위한 통합 가이드입니다.

## 개요

이 디렉토리는 Portal Universe 마이크로서비스 아키텍처를 Kubernetes 환경에 배포하기 위한 모든 리소스와 문서를 포함하고 있습니다. Kind(Kubernetes in Docker)를 사용한 로컬 개발 환경부터 프로덕션 배포까지 지원합니다.

## 폴더 구조

```
k8s/
├── base/              # 기본 설정
│   ├── namespace.yaml       # portal-universe 네임스페이스 정의
│   ├── secret.yaml          # 공통 Secret 리소스
│   └── kind-config.yaml     # Kind 클러스터 설정
│
├── docs/              # 문서
│   ├── README.md            # 이 파일
│   ├── deployment-guide.md  # 상세 배포 가이드
│   ├── network-policy.md    # 네트워크 정책 설명
│   └── monitoring.md        # 모니터링 설정 가이드
│
├── infrastructure/    # 인프라 서비스
│   ├── mysql/               # MySQL 데이터베이스
│   ├── mongodb/             # MongoDB 데이터베이스
│   ├── kafka/               # Apache Kafka 메시지 브로커
│   ├── zipkin/              # 분산 추적 시스템
│   ├── prometheus/          # 메트릭 수집 시스템
│   ├── grafana/             # 메트릭 시각화 대시보드
│   ├── ingress/             # Ingress Controller 및 규칙
│   ├── configmap/           # 공통 ConfigMap
│   └── network-policy/      # 네트워크 보안 정책
│
├── scripts/           # 빌드 및 배포 스크립트
│   ├── build-and-load.sh    # Docker 이미지 빌드 및 Kind 로드
│   └── deploy-all.sh        # 전체 서비스 배포 스크립트
│
└── services/          # 애플리케이션 서비스
    ├── api-gateway/         # API Gateway 서비스
    ├── auth/                # 인증/인가 서비스
    ├── blog/                # 블로그 서비스
    ├── shopping/            # 쇼핑 서비스
    ├── notification/        # 알림 서비스
    ├── config/              # Config Server
    ├── discovery/           # Eureka Service Discovery
    └── portal-shell/        # Frontend Shell Application
```

## 빠른 시작 가이드

### 사전 요구사항

- Docker Desktop 설치
- Kind 설치: `brew install kind` (macOS)
- kubectl 설치: `brew install kubectl` (macOS)
- Ingress Controller를 위한 포트 80, 8080 사용 가능

### 1단계: Kind 클러스터 생성

```bash
kind create cluster --config k8s/base/kind-config.yaml
```

이 명령은 다음과 같은 설정으로 클러스터를 생성합니다:
- 클러스터 이름: `portal-universe`
- 포트 매핑: 80:80, 8080:8080
- Ingress 지원 활성화

### 2단계: Docker 이미지 빌드 및 로드

```bash
./k8s/scripts/build-and-load.sh
```

이 스크립트는:
1. 각 서비스의 Docker 이미지를 빌드합니다
2. 빌드된 이미지를 Kind 클러스터에 로드합니다
3. 진행 상황을 실시간으로 표시합니다

### 3단계: 전체 서비스 배포

```bash
./k8s/scripts/deploy-all.sh
```

배포 순서:
1. 네임스페이스 및 기본 리소스
2. 데이터베이스 (MySQL, MongoDB)
3. 메시지 브로커 (Kafka, Zookeeper)
4. 모니터링 스택 (Prometheus, Grafana, Zipkin)
5. Spring Cloud 인프라 (Config Server, Eureka)
6. 애플리케이션 서비스 (Auth, Blog, Shopping, Notification)
7. API Gateway 및 Frontend
8. Ingress 및 Network Policy

### 4단계: 배포 확인

```bash
# 모든 Pod 상태 확인
kubectl get pods -n portal-universe

# 서비스 상태 확인
kubectl get svc -n portal-universe

# Ingress 확인
kubectl get ingress -n portal-universe
```

모든 Pod가 `Running` 상태가 될 때까지 대기합니다 (약 2-3분 소요).

## 접속 정보

| 서비스 | URL | 설명 |
|--------|-----|------|
| Main Application | http://portal-universe:8080 | Portal Shell (Frontend) |
| Eureka Dashboard | http://portal-universe:8080/eureka | 서비스 디스커버리 대시보드 |
| Grafana | http://portal-universe:8080/grafana | 메트릭 시각화 (admin/admin) |
| Prometheus | http://portal-universe:8080/prometheus | 메트릭 수집 시스템 |
| Zipkin | http://portal-universe:8080/zipkin | 분산 추적 시스템 |

**참고**: `portal-universe` 도메인은 로컬 환경에서 `/etc/hosts` 설정이 필요할 수 있습니다:
```bash
sudo sh -c 'echo "127.0.0.1 portal-universe" >> /etc/hosts'
```

## 유용한 명령어

### 로그 확인
```bash
# 특정 서비스 로그 확인
kubectl logs -n portal-universe deployment/auth-service -f

# 모든 서비스 로그 확인
kubectl logs -n portal-universe -l app.kubernetes.io/part-of=portal-universe --tail=100
```

### 재배포
```bash
# 특정 서비스만 재배포
kubectl rollout restart deployment/auth-service -n portal-universe

# 전체 재배포
./k8s/scripts/deploy-all.sh
```

### 클러스터 삭제
```bash
kind delete cluster --name portal-universe
```

## 관련 문서

- [deployment-guide.md](./deployment-guide.md) - 상세 배포 가이드 및 트러블슈팅
- [network-policy.md](./network-policy.md) - 네트워크 보안 정책 설명
- [monitoring.md](./monitoring.md) - Prometheus/Grafana 모니터링 설정

## 아키텍처 다이어그램

```
┌─────────────────────────────────────────────────────────────┐
│                      Ingress Controller                      │
│                    (portal-universe:8080)                   │
└──────────────────────────────┬──────────────────────────────┘
                               │
              ┌────────────────┼────────────────┐
              │                │                │
              ▼                ▼                ▼
      ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
      │ Portal Shell │  │ API Gateway  │  │  Monitoring  │
      │  (Frontend)  │  │              │  │  (Grafana)   │
      └──────────────┘  └──────┬───────┘  └──────────────┘
                               │
              ┌────────────────┼────────────────┐
              │                │                │
              ▼                ▼                ▼
      ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
      │ Auth Service │  │ Blog Service │  │Shop Service  │
      └──────┬───────┘  └──────┬───────┘  └──────┬───────┘
             │                 │                  │
             └─────────────────┼──────────────────┘
                               │
              ┌────────────────┼────────────────┐
              │                │                │
              ▼                ▼                ▼
      ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
      │    MySQL     │  │   MongoDB    │  │    Kafka     │
      └──────────────┘  └──────────────┘  └──────────────┘
```

## 지원

문제가 발생하면 다음을 확인하세요:
1. 모든 Pod가 `Running` 상태인지 확인
2. `kubectl describe pod <pod-name> -n portal-universe`로 상세 정보 확인
3. 서비스 로그 확인
4. [deployment-guide.md](./deployment-guide.md)의 트러블슈팅 섹션 참조
