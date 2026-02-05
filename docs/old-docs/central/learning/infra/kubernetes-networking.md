# Kubernetes Networking

Kubernetes 네트워킹의 핵심 개념인 Service, Ingress, Network Policy를 학습합니다.

---

## 1. Kubernetes 네트워킹 모델

### 기본 원칙

1. **모든 Pod는 NAT 없이 서로 통신 가능**
2. **모든 Node는 NAT 없이 모든 Pod와 통신 가능**
3. **Pod가 보는 자신의 IP = 다른 Pod가 보는 해당 Pod의 IP**

### 네트워크 계층

```
┌─────────────────────────────────────────────────────────────────┐
│  External Traffic (Users, External Services)                     │
└─────────────────────────────┬───────────────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────────────┐
│  Layer 7: Ingress (HTTP/HTTPS 라우팅)                            │
│  - Path-based routing                                            │
│  - TLS termination                                               │
└─────────────────────────────┬───────────────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────────────┐
│  Layer 4: Service (TCP/UDP 로드 밸런싱)                          │
│  - ClusterIP, NodePort, LoadBalancer                             │
└─────────────────────────────┬───────────────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────────────┐
│  Layer 3: Pod Network (CNI)                                      │
│  - Calico, Flannel, Cilium 등                                    │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. Service Types

### ClusterIP (기본값)

클러스터 내부에서만 접근 가능한 가상 IP를 할당합니다.

```yaml
apiVersion: v1
kind: Service
metadata:
  name: auth-service
  namespace: portal-universe
spec:
  type: ClusterIP
  ports:
    - port: 8081            # Service 포트
      targetPort: 8081      # Pod 포트
      protocol: TCP
  selector:
    app: auth-service
```

**DNS 접근:**
```
# 같은 Namespace
auth-service:8081

# 다른 Namespace
auth-service.portal-universe.svc.cluster.local:8081
```

### NodePort

모든 노드의 특정 포트로 외부 접근을 허용합니다.

```yaml
apiVersion: v1
kind: Service
metadata:
  name: api-gateway
  namespace: portal-universe
spec:
  type: NodePort
  ports:
    - port: 8080
      targetPort: 8080
      nodePort: 30080       # 노드 포트 (30000-32767)
  selector:
    app: api-gateway
```

```
┌─────────────────────────────────────────────────────────────────┐
│  External Request → NodeIP:30080                                 │
└─────────────────────────────┬───────────────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────────────┐
│  Node 1 (192.168.1.10:30080)    Node 2 (192.168.1.11:30080)    │
└─────────────────────────────┬───────────────────────────────────┘
                              │ kube-proxy
┌─────────────────────────────▼───────────────────────────────────┐
│  Service (ClusterIP:8080)                                        │
└─────────────────────────────┬───────────────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────────────┐
│  Pod 1 (10.0.0.5:8080)          Pod 2 (10.0.0.6:8080)          │
└─────────────────────────────────────────────────────────────────┘
```

### LoadBalancer

클라우드 프로바이더의 로드 밸런서와 연동합니다.

```yaml
apiVersion: v1
kind: Service
metadata:
  name: api-gateway
  namespace: portal-universe
spec:
  type: LoadBalancer
  ports:
    - port: 80
      targetPort: 8080
  selector:
    app: api-gateway
```

### Headless Service

ClusterIP 없이 Pod IP를 직접 반환합니다 (StatefulSet에 사용).

```yaml
apiVersion: v1
kind: Service
metadata:
  name: mysql-headless
spec:
  clusterIP: None           # Headless Service
  selector:
    app: mysql
  ports:
    - port: 3306
```

---

## 3. Ingress

HTTP/HTTPS 트래픽을 클러스터 내부 서비스로 라우팅합니다.

### Ingress Controller

Ingress 리소스를 실제로 처리하는 컨트롤러입니다.

**Portal Universe NGINX Ingress Controller:**

```yaml
# k8s/infrastructure/ingress-controller.yaml (핵심 부분)
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ingress-nginx-controller
  namespace: ingress-nginx
spec:
  template:
    spec:
      containers:
      - name: controller
        image: registry.k8s.io/ingress-nginx/controller:v1.13.3
        args:
          - /nginx-ingress-controller
          - --ingress-class=nginx
          - --watch-ingress-without-class=true
        ports:
        - containerPort: 80
          hostPort: 80           # 노드 80 포트와 직접 연결
        - containerPort: 443
          hostPort: 443
```

### IngressClass

```yaml
apiVersion: networking.k8s.io/v1
kind: IngressClass
metadata:
  name: nginx
spec:
  controller: k8s.io/ingress-nginx
```

### Portal Universe Ingress 설정

```yaml
# k8s/infrastructure/ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: portal-universe-ingress
  namespace: portal-universe
  annotations:
    nginx.ingress.kubernetes.io/ssl-redirect: "false"
spec:
  ingressClassName: nginx
  rules:
    - host: portal-universe          # 호스트명 기반 라우팅
      http:
        paths:
          # 모니터링 서비스 라우팅
          - path: /grafana
            pathType: Prefix
            backend:
              service:
                name: grafana
                port:
                  number: 3000

          - path: /prometheus
            pathType: Prefix
            backend:
              service:
                name: prometheus
                port:
                  number: 9090

          - path: /zipkin
            pathType: Prefix
            backend:
              service:
                name: zipkin
                port:
                  number: 9411

          # API 서비스 라우팅
          - path: /auth-service
            pathType: Prefix
            backend:
              service:
                name: api-gateway
                port:
                  number: 8080

          - path: /api
            pathType: Prefix
            backend:
              service:
                name: api-gateway
                port:
                  number: 8080

          # 기본 라우팅 (Frontend)
          - path: /
            pathType: Prefix
            backend:
              service:
                name: portal-shell
                port:
                  number: 80
```

### 라우팅 흐름

```
┌─────────────────────────────────────────────────────────────────┐
│  Client Request: http://portal-universe/api/v1/posts            │
└─────────────────────────────┬───────────────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────────────┐
│  Ingress Controller                                              │
│  Rule: host=portal-universe, path=/api → api-gateway:8080       │
└─────────────────────────────┬───────────────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────────────┐
│  api-gateway Service (ClusterIP)                                 │
└─────────────────────────────┬───────────────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────────────┐
│  api-gateway Pod → blog-service → Response                       │
└─────────────────────────────────────────────────────────────────┘
```

### Path Type

| 유형 | 매칭 방식 |
|------|----------|
| `Exact` | 정확히 일치 (`/foo`만 매칭) |
| `Prefix` | 접두사 일치 (`/foo`, `/foo/bar` 매칭) |
| `ImplementationSpecific` | IngressClass에 따라 다름 |

### TLS 설정

```yaml
spec:
  tls:
    - hosts:
        - portal-universe
      secretName: portal-universe-tls      # TLS 인증서 Secret
  rules:
    - host: portal-universe
      http:
        paths:
          ...
```

---

## 4. Network Policy

Pod 간 네트워크 트래픽을 제어합니다.

### 기본 개념

```yaml
# k8s/infrastructure/network-policy.yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: backend-isolation
  namespace: portal-universe
spec:
  podSelector:
    matchLabels:
      tier: backend              # 적용 대상 Pod

  policyTypes:
    - Ingress
    - Egress

  ingress:
    - from:
        - podSelector:
            matchLabels:
              tier: gateway      # api-gateway에서만 접근 허용
      ports:
        - protocol: TCP
          port: 8080

  egress:
    - to:
        - podSelector:
            matchLabels:
              tier: database     # database로만 나가는 트래픽 허용
      ports:
        - protocol: TCP
          port: 3306
```

### 트래픽 제어 예시

```
┌─────────────────────────────────────────────────────────────────┐
│  NetworkPolicy: backend-isolation                                │
│                                                                  │
│  Ingress (허용):                                                 │
│  ┌──────────────┐         ┌──────────────┐                      │
│  │ api-gateway  │ ───────►│ auth-service │                      │
│  │ tier=gateway │   ✓     │ tier=backend │                      │
│  └──────────────┘         └──────────────┘                      │
│                                                                  │
│  Ingress (차단):                                                 │
│  ┌──────────────┐         ┌──────────────┐                      │
│  │ other-pod    │ ───X───►│ auth-service │                      │
│  │ tier=other   │         │ tier=backend │                      │
│  └──────────────┘         └──────────────┘                      │
└─────────────────────────────────────────────────────────────────┘
```

### Default Deny 정책

```yaml
# 모든 Ingress 트래픽 차단
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: default-deny-ingress
spec:
  podSelector: {}            # 모든 Pod에 적용
  policyTypes:
    - Ingress

# 모든 Egress 트래픽 차단
---
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: default-deny-egress
spec:
  podSelector: {}
  policyTypes:
    - Egress
```

---

## 5. DNS in Kubernetes

### CoreDNS

```
┌─────────────────────────────────────────────────────────────────┐
│  Service DNS Resolution                                          │
│                                                                  │
│  auth-service                                                    │
│  ↓                                                               │
│  auth-service.portal-universe                                    │
│  ↓                                                               │
│  auth-service.portal-universe.svc                                │
│  ↓                                                               │
│  auth-service.portal-universe.svc.cluster.local                  │
│                                                                  │
│  → ClusterIP: 10.96.45.123                                      │
└─────────────────────────────────────────────────────────────────┘
```

### DNS 조회 테스트

```bash
# Pod에서 DNS 테스트
kubectl run -it --rm debug --image=busybox -- nslookup auth-service.portal-universe

# 결과:
# Server:    10.96.0.10
# Address:   10.96.0.10:53
# Name:      auth-service.portal-universe.svc.cluster.local
# Address:   10.96.45.123
```

---

## 6. Service Discovery

### Environment Variables

```bash
# Pod 내에서 자동 주입되는 환경 변수
AUTH_SERVICE_SERVICE_HOST=10.96.45.123
AUTH_SERVICE_SERVICE_PORT=8081
```

### DNS 기반 (권장)

```yaml
# Spring Boot application.yml
spring:
  datasource:
    url: jdbc:mysql://mysql-db.portal-universe:3306/auth_db
  redis:
    host: redis.portal-universe
```

---

## 7. Service Mesh 개요

### Istio / Linkerd

```
┌─────────────────────────────────────────────────────────────────┐
│  Without Service Mesh                                            │
│                                                                  │
│  ┌──────────┐              ┌──────────┐                         │
│  │  Pod A   │──────────────│  Pod B   │                         │
│  │ (App)    │   Direct     │ (App)    │                         │
│  └──────────┘              └──────────┘                         │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  With Service Mesh (Sidecar Pattern)                             │
│                                                                  │
│  ┌───────────────────┐     ┌───────────────────┐                │
│  │  Pod A            │     │  Pod B            │                │
│  │ ┌───────┐ ┌─────┐│     │┌─────┐ ┌───────┐ │                │
│  │ │ App   │→│Proxy││─────►││Proxy│→│ App   │ │                │
│  │ └───────┘ └─────┘│     │└─────┘ └───────┘ │                │
│  └───────────────────┘     └───────────────────┘                │
│                                                                  │
│  Proxy: Envoy (Istio) / Linkerd-proxy                           │
│  기능: mTLS, 트래픽 제어, 관측성, 장애 복구                        │
└─────────────────────────────────────────────────────────────────┘
```

### Service Mesh 기능

| 기능 | 설명 |
|------|------|
| mTLS | 서비스 간 암호화 통신 |
| Traffic Splitting | 카나리 배포, A/B 테스트 |
| Circuit Breaker | 장애 전파 방지 |
| Retry/Timeout | 자동 재시도 및 타임아웃 |
| Observability | 분산 추적, 메트릭 수집 |

---

## 8. 실습: 네트워크 디버깅

### 연결 테스트

```bash
# 임시 Pod로 네트워크 테스트
kubectl run -it --rm debug --image=nicolaka/netshoot -n portal-universe -- bash

# 내부에서 테스트
curl http://auth-service:8081/actuator/health
nslookup mysql-db
telnet mysql-db 3306
```

### Service Endpoint 확인

```bash
# Service의 Endpoint 확인
kubectl get endpoints auth-service -n portal-universe

# 출력:
# NAME           ENDPOINTS           AGE
# auth-service   10.244.1.5:8081     5m
```

### 트래픽 모니터링

```bash
# kube-proxy 로그 확인
kubectl logs -n kube-system -l k8s-app=kube-proxy

# Ingress Controller 로그
kubectl logs -n ingress-nginx -l app.kubernetes.io/name=ingress-nginx
```

---

## 9. 관련 문서

- [Kubernetes Fundamentals](./kubernetes-fundamentals.md) - K8s 기초
- [Kubernetes Kind Cluster](./kubernetes-kind-cluster.md) - 로컬 K8s 환경
- [Portal Universe Infra Guide](./portal-universe-infra-guide.md) - 전체 인프라 가이드
