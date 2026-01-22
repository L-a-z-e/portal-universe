# Kubernetes Fundamentals

컨테이너 오케스트레이션 플랫폼인 Kubernetes의 핵심 개념을 학습합니다.

---

## 1. Kubernetes 개요

### 왜 Kubernetes인가?

**Docker Compose의 한계:**
- 단일 호스트 환경
- 자동 복구 기능 없음
- 수평 확장 어려움
- 롤링 업데이트 지원 제한

**Kubernetes가 제공하는 기능:**
- 다중 노드 클러스터 관리
- 자동 복구 (Self-healing)
- 자동 스케일링 (HPA, VPA)
- 롤링 업데이트 / 롤백
- 서비스 디스커버리 및 로드 밸런싱

### Kubernetes Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Control Plane                                │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌───────────────┐ │
│  │ API Server  │ │   etcd      │ │ Scheduler   │ │ Controller    │ │
│  │             │ │  (Key-Value)│ │             │ │   Manager     │ │
│  └──────┬──────┘ └─────────────┘ └─────────────┘ └───────────────┘ │
└─────────┼───────────────────────────────────────────────────────────┘
          │ kubectl / API
┌─────────┼───────────────────────────────────────────────────────────┐
│         ▼                    Worker Nodes                            │
│  ┌─────────────────────┐      ┌─────────────────────┐               │
│  │     Node 1          │      │     Node 2          │               │
│  │  ┌───────┐ ┌───────┐│      │  ┌───────┐ ┌───────┐│               │
│  │  │ Pod A │ │ Pod B ││      │  │ Pod C │ │ Pod D ││               │
│  │  └───────┘ └───────┘│      │  └───────┘ └───────┘│               │
│  │  ┌─────────────────┐│      │  ┌─────────────────┐│               │
│  │  │ kubelet │ kube- ││      │  │ kubelet │ kube- ││               │
│  │  │         │ proxy ││      │  │         │ proxy ││               │
│  │  └─────────────────┘│      │  └─────────────────┘│               │
│  └─────────────────────┘      └─────────────────────┘               │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 2. 핵심 리소스 계층 구조

```
Namespace
└── Deployment (선언적 배포 관리)
    └── ReplicaSet (복제본 유지)
        └── Pod (컨테이너 그룹)
            └── Container (실행 단위)

Service (네트워크 접근점)
    └── Endpoint (Pod IP 목록)
```

---

## 3. Namespace

클러스터 내 리소스를 논리적으로 분리하는 가상 클러스터입니다.

### Portal Universe Namespace 정의

```yaml
# k8s/base/namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: portal-universe
  labels:
    name: portal-universe
    environment: development
```

### Namespace 활용

```bash
# Namespace 목록
kubectl get namespaces

# 특정 Namespace의 리소스 조회
kubectl get pods -n portal-universe

# 기본 Namespace 설정
kubectl config set-context --current --namespace=portal-universe
```

---

## 4. Pod

Kubernetes에서 배포 가능한 가장 작은 단위입니다.

### Pod 특징

- 하나 이상의 컨테이너를 포함
- 컨테이너들은 네트워크와 스토리지를 공유
- Pod 내 컨테이너는 localhost로 통신
- Pod는 일시적(ephemeral) - 언제든 재생성될 수 있음

### Pod 구조

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: auth-service-pod
  namespace: portal-universe
  labels:
    app: auth-service
spec:
  containers:
    - name: auth-service
      image: portal-universe-auth-service:v1.0.2
      ports:
        - containerPort: 8081
      env:
        - name: SPRING_PROFILES_ACTIVE
          value: "kubernetes"
      resources:
        requests:
          cpu: "100m"
          memory: "256Mi"
        limits:
          cpu: "500m"
          memory: "512Mi"
```

### Multi-Container Pod 패턴

```yaml
# Sidecar Pattern 예시
spec:
  containers:
    - name: main-app
      image: myapp:v1
    - name: log-agent          # Sidecar 컨테이너
      image: fluentd:latest
      volumeMounts:
        - name: logs
          mountPath: /var/log
  volumes:
    - name: logs
      emptyDir: {}
```

| 패턴 | 용도 | 예시 |
|------|------|------|
| Sidecar | 보조 기능 추가 | 로그 수집, 프록시 |
| Ambassador | 외부 통신 대리 | DB 프록시 |
| Adapter | 출력 표준화 | 모니터링 메트릭 변환 |

---

## 5. Deployment

Pod의 선언적 배포 및 업데이트를 관리합니다.

### Portal Universe auth-service Deployment

```yaml
# k8s/services/auth-service.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: auth-service
  namespace: portal-universe
  labels:
    app: auth-service
spec:
  replicas: 1                          # Pod 복제본 수
  selector:
    matchLabels:
      app: auth-service                # 관리할 Pod 선택
  template:                            # Pod 템플릿
    metadata:
      labels:
        app: auth-service
      annotations:
        prometheus.io/scrape: "true"   # Prometheus 메트릭 수집
        prometheus.io/path: "/actuator/prometheus"
        prometheus.io/port: "8081"
    spec:
      containers:
        - name: auth-service
          image: portal-universe-auth-service:v1.0.2
          imagePullPolicy: Never       # 로컬 이미지 사용
          ports:
            - containerPort: 8081
              name: http
          envFrom:
            - configMapRef:
                name: portal-universe-config
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: "kubernetes"
            - name: DB_PASSWORD
              valueFrom:
                secretKeyRef:          # Secret에서 비밀번호 로드
                  name: portal-universe-secret
                  key: MYSQL_PASSWORD
          resources:
            requests:
              cpu: "100m"
              memory: "256Mi"
            limits:
              cpu: "500m"
              memory: "512Mi"
```

### Deployment 전략

```yaml
spec:
  strategy:
    type: RollingUpdate              # 또는 Recreate
    rollingUpdate:
      maxSurge: 1                    # 업데이트 중 추가 허용 Pod 수
      maxUnavailable: 0              # 업데이트 중 불가용 허용 Pod 수
```

| 전략 | 동작 | 사용 사례 |
|------|------|----------|
| RollingUpdate | 점진적 교체 | 무중단 배포 (기본값) |
| Recreate | 전체 종료 후 재생성 | DB 마이그레이션 |

---

## 6. ReplicaSet

지정된 수의 Pod 복제본을 유지합니다.

### ReplicaSet 동작 원리

```
┌──────────────────────────────────────────────────────────────┐
│                    ReplicaSet Controller                      │
│                                                              │
│   Desired: 3 Pods    Current: 2 Pods    → Create 1 Pod      │
│   Desired: 3 Pods    Current: 4 Pods    → Delete 1 Pod      │
│   Desired: 3 Pods    Current: 3 Pods    → No action         │
└──────────────────────────────────────────────────────────────┘
```

**주의:** ReplicaSet은 직접 생성하지 않고 Deployment를 통해 관리합니다.

```bash
# Deployment가 생성한 ReplicaSet 확인
kubectl get rs -n portal-universe

# 출력 예시
NAME                       DESIRED   CURRENT   READY   AGE
auth-service-5f7b8c6d4    1         1         1       5m
```

---

## 7. Service

Pod들에 대한 안정적인 네트워크 엔드포인트를 제공합니다.

### Portal Universe auth-service Service

```yaml
apiVersion: v1
kind: Service
metadata:
  name: auth-service
  namespace: portal-universe
  labels:
    app: auth-service
spec:
  type: ClusterIP                   # Service 유형
  ports:
    - port: 8081                    # Service 포트
      targetPort: 8081              # Pod 포트
      protocol: TCP
      name: http
  selector:
    app: auth-service               # 트래픽을 전달할 Pod 선택
```

### Service 유형

```
┌─────────────────────────────────────────────────────────────────┐
│                         External Traffic                         │
└─────────────────────────────┬───────────────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────────────┐
│  LoadBalancer Service (type: LoadBalancer)                       │
│  - 클라우드 프로바이더의 LB와 연동                                  │
│  - 외부 IP 할당                                                   │
└─────────────────────────────┬───────────────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────────────┐
│  NodePort Service (type: NodePort)                               │
│  - 모든 노드의 특정 포트로 접근 가능                                │
│  - 포트 범위: 30000-32767                                        │
└─────────────────────────────┬───────────────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────────────┐
│  ClusterIP Service (type: ClusterIP) - 기본값                    │
│  - 클러스터 내부에서만 접근 가능                                    │
│  - 내부 DNS: auth-service.portal-universe.svc.cluster.local     │
└─────────────────────────────────────────────────────────────────┘
```

| 유형 | 외부 접근 | 사용 사례 |
|------|----------|----------|
| ClusterIP | 불가 | 내부 마이크로서비스 |
| NodePort | 노드 IP + 포트 | 개발/테스트 환경 |
| LoadBalancer | 외부 LB IP | 프로덕션 (클라우드) |
| ExternalName | DNS CNAME | 외부 서비스 연결 |

---

## 8. Probes (Health Checks)

Pod의 상태를 감시하여 자동 복구를 수행합니다.

### Portal Universe Probe 설정

```yaml
spec:
  containers:
    - name: auth-service
      # Startup Probe: 애플리케이션 초기화 완료 확인
      startupProbe:
        httpGet:
          path: /actuator/health/readiness
          port: 8081
        initialDelaySeconds: 30      # 첫 체크 전 대기
        periodSeconds: 10            # 체크 간격
        failureThreshold: 18         # 실패 허용 횟수 (최대 3분)

      # Liveness Probe: Pod 재시작 필요 여부 확인
      livenessProbe:
        httpGet:
          path: /actuator/health/liveness
          port: 8081
        initialDelaySeconds: 10
        periodSeconds: 10
        failureThreshold: 3

      # Readiness Probe: 트래픽 수신 준비 확인
      readinessProbe:
        httpGet:
          path: /actuator/health/readiness
          port: 8081
        initialDelaySeconds: 5
        periodSeconds: 5
        failureThreshold: 3
```

### Probe 유형 비교

| Probe | 실패 시 동작 | 용도 |
|-------|-------------|------|
| startupProbe | 컨테이너 재시작 | 느린 시작 애플리케이션 |
| livenessProbe | Pod 재시작 | 데드락 감지 |
| readinessProbe | 서비스에서 제외 | 일시적 불가용 처리 |

### Probe 체크 방식

```yaml
# HTTP GET
httpGet:
  path: /health
  port: 8080

# TCP Socket
tcpSocket:
  port: 3306

# Command 실행
exec:
  command:
    - cat
    - /tmp/healthy
```

---

## 9. ConfigMap & Secret

설정과 민감 정보를 분리하여 관리합니다.

### ConfigMap

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: portal-universe-config
  namespace: portal-universe
data:
  MYSQL_HOST: "mysql-db"
  MYSQL_PORT: "3306"
  REDIS_HOST: "redis"
  KAFKA_BOOTSTRAP_SERVERS: "kafka:29092"
```

### Secret

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: portal-universe-secret
  namespace: portal-universe
type: Opaque
data:
  MYSQL_PASSWORD: cGFzc3dvcmQxMjM=     # base64 인코딩
  JWT_SECRET: c2VjcmV0a2V5MTIz
```

### Pod에서 사용

```yaml
spec:
  containers:
    - name: app
      # ConfigMap 전체 로드
      envFrom:
        - configMapRef:
            name: portal-universe-config

      # Secret 특정 키 로드
      env:
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: portal-universe-secret
              key: MYSQL_PASSWORD

      # 파일로 마운트
      volumeMounts:
        - name: config-volume
          mountPath: /etc/config
  volumes:
    - name: config-volume
      configMap:
        name: portal-universe-config
```

---

## 10. 리소스 관리

### Resource Requests & Limits

```yaml
resources:
  requests:                  # 최소 보장 리소스
    cpu: "100m"             # 0.1 CPU core
    memory: "256Mi"         # 256 MiB
  limits:                    # 최대 사용 가능 리소스
    cpu: "500m"             # 0.5 CPU core
    memory: "512Mi"         # 512 MiB
```

### CPU 단위

| 표기 | 의미 |
|------|------|
| `1` | 1 vCPU |
| `500m` | 0.5 vCPU (millicpu) |
| `100m` | 0.1 vCPU |

### Memory 단위

| 표기 | 의미 |
|------|------|
| `Ki, Mi, Gi` | Kibibyte, Mebibyte, Gibibyte (2의 거듭제곱) |
| `K, M, G` | Kilobyte, Megabyte, Gigabyte (10의 거듭제곱) |

---

## 11. 기본 kubectl 명령어

### 리소스 조회

```bash
# 전체 리소스 확인
kubectl get all -n portal-universe

# Pod 상태 확인
kubectl get pods -n portal-universe -o wide

# Pod 상세 정보
kubectl describe pod auth-service-xxx -n portal-universe

# 이벤트 확인
kubectl get events -n portal-universe --sort-by='.lastTimestamp'
```

### 로그 및 디버깅

```bash
# Pod 로그
kubectl logs -f auth-service-xxx -n portal-universe

# 이전 컨테이너 로그 (재시작된 경우)
kubectl logs auth-service-xxx -n portal-universe --previous

# Pod 접속
kubectl exec -it auth-service-xxx -n portal-universe -- /bin/bash

# 포트 포워딩
kubectl port-forward svc/auth-service 8081:8081 -n portal-universe
```

### 리소스 적용

```bash
# YAML 파일 적용
kubectl apply -f k8s/services/auth-service.yaml

# 디렉토리 전체 적용
kubectl apply -f k8s/

# 리소스 삭제
kubectl delete -f k8s/services/auth-service.yaml
```

---

## 12. 관련 문서

- [Kubernetes Networking](./kubernetes-networking.md) - Service, Ingress 상세
- [Kubernetes Kind Cluster](./kubernetes-kind-cluster.md) - 로컬 K8s 환경 구성
- [Portal Universe Infra Guide](./portal-universe-infra-guide.md) - 전체 인프라 가이드
