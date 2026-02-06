# Service Discovery

## 목차
1. [개요](#1-개요)
2. [왜 Service Discovery가 필요한가](#2-왜-service-discovery가-필요한가)
3. [Service Discovery 패턴](#3-service-discovery-패턴)
4. [Service Registry 솔루션 비교](#4-service-registry-솔루션-비교)
5. [Kubernetes에서의 Service Discovery](#5-kubernetes에서의-service-discovery)
6. [Portal Universe의 Service Discovery 분석](#6-portal-universe의-service-discovery-분석)
7. [Load Balancing과의 관계](#7-load-balancing과의-관계)
8. [실습 예제](#8-실습-예제)
9. [모범 사례](#9-모범-사례)

---

## 1. 개요

**Service Discovery**는 마이크로서비스 아키텍처에서 서비스 인스턴스의 네트워크 위치(IP 주소와 포트)를 동적으로 찾아내는 메커니즘입니다.

```
+------------------+     "auth-service 어디있지?"     +------------------+
|                  | --------------------------------> |                  |
|  API Gateway     |                                   | Service Registry |
|                  | <-------------------------------- |                  |
+------------------+     "10.0.0.5:8081에 있어!"      +------------------+
        |
        | HTTP Request
        v
+------------------+
|  Auth Service    |
|  (10.0.0.5:8081) |
+------------------+
```

### 핵심 개념

| 용어 | 설명 |
|------|------|
| **Service Registry** | 서비스 인스턴스들의 네트워크 위치 정보를 저장하는 데이터베이스 |
| **Service Registration** | 서비스가 시작될 때 Registry에 자신의 위치를 등록하는 과정 |
| **Service Discovery** | 클라이언트가 서비스의 위치를 찾아내는 과정 |
| **Health Check** | 서비스 인스턴스가 정상적으로 동작하는지 확인하는 과정 |

---

## 2. 왜 Service Discovery가 필요한가

### 2.1 전통적인 환경의 문제점

**정적 환경 (Monolith)**에서는 서비스 위치가 고정적입니다:

```
[ 클라이언트 ]
      |
      | config: api.server.com:80
      v
[ API Server (고정 IP: 192.168.1.100) ]
```

하지만 **동적 환경 (Cloud/Container)**에서는:

```
+---------------+     어떤 인스턴스로?     +---------+---------+---------+
|   클라이언트  | -----------------------> | Pod A   | Pod B   | Pod C   |
+---------------+        ???               | ???     | ???     | ???     |
                                           +---------+---------+---------+

문제점:
- Pod들은 언제든 생성/삭제될 수 있음
- IP 주소가 동적으로 할당됨
- Auto-scaling으로 인스턴스 수가 변동
- Rolling update 시 IP가 변경됨
```

### 2.2 동적 환경의 도전과제

```
┌─────────────────────────────────────────────────────────────────┐
│                    동적 환경의 문제점                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. 동적 IP 할당                                                 │
│     ┌──────────────┐    재시작    ┌──────────────┐             │
│     │ Pod IP:      │  ────────>   │ Pod IP:      │             │
│     │ 10.0.0.5     │              │ 10.0.0.99    │ <- 변경됨   │
│     └──────────────┘              └──────────────┘             │
│                                                                 │
│  2. Auto-Scaling                                                │
│     ┌───┐            Scale-out    ┌───┐ ┌───┐ ┌───┐           │
│     │ A │          ────────────>  │ A │ │ B │ │ C │           │
│     └───┘                         └───┘ └───┘ └───┘           │
│                                   새 인스턴스들의 IP는?          │
│                                                                 │
│  3. 장애 복구                                                    │
│     ┌───┐ ┌───┐ ┌───┐   장애 발생   ┌───┐ ┌───┐ ┌───┐        │
│     │ A │ │ B │ │ C │  ─────────>   │ A │ │ X │ │ D │        │
│     └───┘ └───┘ └───┘               └───┘     └───┘          │
│                                     B 제거, D 추가             │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 2.3 Service Discovery 없이 발생하는 문제

```java
// Anti-Pattern: 하드코딩된 서비스 주소
@FeignClient(name = "auth-service", url = "http://192.168.1.100:8081")
public interface AuthServiceClient {
    @GetMapping("/api/users/{id}")
    UserDto getUser(@PathVariable Long id);
}

// 문제점:
// 1. IP가 변경되면 코드 수정 필요
// 2. 여러 인스턴스 중 하나만 사용
// 3. 인스턴스 장애 시 failover 불가
```

---

## 3. Service Discovery 패턴

Service Discovery는 크게 두 가지 패턴으로 구현됩니다.

### 3.1 Client-Side Discovery

클라이언트가 직접 Service Registry에 질의하여 서비스 위치를 찾고, Load Balancing 결정도 클라이언트가 수행합니다.

```
┌─────────────────────────────────────────────────────────────────┐
│                  Client-Side Discovery                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌────────────────────┐                                        │
│  │   Service Client   │                                        │
│  │  ┌──────────────┐  │    1. Query     ┌──────────────────┐  │
│  │  │ Discovery    │  │ ─────────────>  │ Service Registry │  │
│  │  │ Client       │  │ <───────────── │ (Eureka, Consul) │  │
│  │  └──────────────┘  │    2. Return    └──────────────────┘  │
│  │  ┌──────────────┐  │    Instances              │           │
│  │  │ Load         │  │       List       3. Register/         │
│  │  │ Balancer     │  │                  Heartbeat │           │
│  │  └──────────────┘  │                           │           │
│  └─────────┬──────────┘                           │           │
│            │ 4. Direct Request                    v           │
│            │                         ┌──────────────────┐     │
│            └───────────────────────> │ Service Instance │     │
│                                      │ (10.0.0.5:8081)  │     │
│                                      └──────────────────┘     │
│                                                                │
│  대표 구현: Netflix Eureka + Ribbon                             │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**장점:**
- 클라이언트가 Load Balancing 전략 선택 가능
- 서비스별 맞춤 전략 적용 가능

**단점:**
- 클라이언트에 Discovery 로직이 포함됨
- 언어별로 클라이언트 라이브러리 필요

### 3.2 Server-Side Discovery

전용 Load Balancer/Proxy가 Service Registry와 통신하며, 클라이언트는 고정된 주소로 요청합니다.

```
┌─────────────────────────────────────────────────────────────────┐
│                  Server-Side Discovery                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌────────────┐  1. Request   ┌─────────────────────────────┐  │
│  │  Service   │ ────────────> │        Load Balancer        │  │
│  │  Client    │               │  ┌──────────────────────┐   │  │
│  └────────────┘               │  │   2. Query Registry  │   │  │
│                               │  └──────────┬───────────┘   │  │
│                               │             │               │  │
│                               │             v               │  │
│                               │  ┌──────────────────────┐   │  │
│                               │  │  Service Registry    │   │  │
│                               │  └──────────────────────┘   │  │
│                               └─────────────┬───────────────┘  │
│                                             │                   │
│                                3. Forward   │                   │
│                                             v                   │
│                         ┌──────────────────────────────────┐   │
│                         │  Service Instances               │   │
│                         │  ┌───┐ ┌───┐ ┌───┐              │   │
│                         │  │ A │ │ B │ │ C │              │   │
│                         │  └───┘ └───┘ └───┘              │   │
│                         └──────────────────────────────────┘   │
│                                                                 │
│  대표 구현: Kubernetes Service, AWS ALB, NGINX                  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**장점:**
- 클라이언트가 단순함 (Discovery 로직 불필요)
- 언어 독립적
- 중앙집중식 Load Balancing 정책 관리

**단점:**
- Load Balancer가 Single Point of Failure가 될 수 있음
- 추가적인 네트워크 홉 발생

### 3.3 패턴 비교 요약

| 특성 | Client-Side | Server-Side |
|------|-------------|-------------|
| **Discovery 주체** | 클라이언트 | Load Balancer |
| **Load Balancing** | 클라이언트 | Load Balancer |
| **네트워크 홉** | 적음 | 추가 홉 존재 |
| **클라이언트 복잡도** | 높음 | 낮음 |
| **언어 독립성** | 낮음 | 높음 |
| **대표 구현** | Eureka + Ribbon | K8s Service, AWS ALB |

---

## 4. Service Registry 솔루션 비교

### 4.1 Netflix Eureka

Spring Cloud 생태계에서 가장 널리 사용되는 Service Registry입니다.

```
┌─────────────────────────────────────────────────────────────────┐
│                      Eureka Architecture                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌────────────────┐        ┌────────────────┐                  │
│  │ Eureka Server  │ <────> │ Eureka Server  │  (Peer 복제)     │
│  │  (Primary)     │        │  (Secondary)   │                  │
│  └───────┬────────┘        └────────────────┘                  │
│          │                                                      │
│          │ Registry                                             │
│          │                                                      │
│  ┌───────┴───────────────────────────────────────────┐         │
│  │                    Services                        │         │
│  │  ┌─────────────┐  ┌─────────────┐  ┌───────────┐  │         │
│  │  │ auth-svc    │  │ auth-svc    │  │ blog-svc  │  │         │
│  │  │ Instance-1  │  │ Instance-2  │  │ Instance-1│  │         │
│  │  │ Heartbeat   │  │ Heartbeat   │  │ Heartbeat │  │         │
│  │  │ (30s 주기)  │  │ (30s 주기)  │  │ (30s 주기)│  │         │
│  │  └─────────────┘  └─────────────┘  └───────────┘  │         │
│  └───────────────────────────────────────────────────┘         │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**특징:**
- AP (Availability + Partition Tolerance) 시스템
- Self-preservation 모드로 네트워크 장애 시 인스턴스 유지
- Client-side Load Balancing과 함께 사용

```java
// Eureka Client 설정 예시
@SpringBootApplication
@EnableEurekaClient  // Eureka 클라이언트 활성화
public class AuthServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
```

```yaml
# application.yml
eureka:
  client:
    service-url:
      defaultZone: http://eureka:8761/eureka
  instance:
    prefer-ip-address: true
    lease-renewal-interval-in-seconds: 30  # Heartbeat 주기
```

### 4.2 HashiCorp Consul

Service Discovery와 함께 Configuration Management, Health Checking을 제공하는 솔루션입니다.

```
┌─────────────────────────────────────────────────────────────────┐
│                      Consul Architecture                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌───────────────────────────────────────────────────┐         │
│  │              Consul Server Cluster                 │         │
│  │  ┌─────────┐  ┌─────────┐  ┌─────────┐           │         │
│  │  │ Leader  │  │Follower │  │Follower │  (Raft)   │         │
│  │  └─────────┘  └─────────┘  └─────────┘           │         │
│  └───────────────────────┬───────────────────────────┘         │
│                          │                                      │
│      ┌───────────────────┼───────────────────┐                 │
│      │                   │                   │                 │
│      v                   v                   v                 │
│  ┌───────────┐      ┌───────────┐      ┌───────────┐          │
│  │Consul     │      │Consul     │      │Consul     │          │
│  │Agent      │      │Agent      │      │Agent      │          │
│  │(Service A)│      │(Service B)│      │(Service C)│          │
│  └───────────┘      └───────────┘      └───────────┘          │
│                                                                 │
│  특징: DNS/HTTP API, Health Check, KV Store, Multi-DC          │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**특징:**
- CP (Consistency + Partition Tolerance) 시스템
- DNS 인터페이스 제공 (auth-service.service.consul)
- Key-Value 저장소 내장
- Multi-Datacenter 지원

### 4.3 Kubernetes DNS (CoreDNS)

Kubernetes에 내장된 Service Discovery 메커니즘입니다.

```
┌─────────────────────────────────────────────────────────────────┐
│                  Kubernetes DNS Architecture                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌────────────────────────────────────────────────────────────┐│
│  │                    Control Plane                           ││
│  │  ┌────────────────┐    ┌────────────────────┐             ││
│  │  │  API Server    │ -> │   etcd             │             ││
│  │  │                │    │ (Service 정보 저장) │             ││
│  │  └───────┬────────┘    └────────────────────┘             ││
│  └──────────┼─────────────────────────────────────────────────┘│
│             │                                                   │
│             │ Watch (Service/Endpoint 변경 감지)                │
│             v                                                   │
│  ┌──────────────────┐                                          │
│  │    CoreDNS       │                                          │
│  │  ┌────────────┐  │                                          │
│  │  │ kubernetes │  │  <- Service 정보를 DNS 레코드로 변환      │
│  │  │   plugin   │  │                                          │
│  │  └────────────┘  │                                          │
│  └─────────┬────────┘                                          │
│            │                                                    │
│            │ DNS Query: auth-service.portal-universe.svc.cluster.local
│            v                                                    │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Worker Nodes                                             │  │
│  │  ┌─────────────────────────────────────────────────────┐ │  │
│  │  │ Pod (API Gateway)                                   │ │  │
│  │  │  DNS query -> CoreDNS -> ClusterIP -> Pod          │ │  │
│  │  └─────────────────────────────────────────────────────┘ │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 4.4 솔루션 비교 표

| 특성 | Eureka | Consul | K8s DNS |
|------|--------|--------|---------|
| **CAP** | AP | CP | CP |
| **프로토콜** | HTTP | DNS/HTTP | DNS |
| **Health Check** | Heartbeat | 다양한 방식 | Probe |
| **KV Store** | X | O | ConfigMap |
| **Multi-DC** | 제한적 | O | Federation |
| **설치 복잡도** | 중간 | 높음 | 내장 |
| **Spring 통합** | 최상 | 좋음 | 좋음 |

---

## 5. Kubernetes에서의 Service Discovery

Kubernetes는 **Server-Side Discovery** 패턴을 내장 지원합니다.

### 5.1 Kubernetes Service 개념

```
┌─────────────────────────────────────────────────────────────────┐
│                    Kubernetes Service                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Service (auth-service)                                         │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │  ClusterIP: 10.96.0.10 (가상 IP, 변경되지 않음)              ││
│  │  Port: 8081                                                  ││
│  │  Selector: app=auth-service                                  ││
│  └─────────────────────────────────────────────────────────────┘│
│                          │                                      │
│                          │ Label Selector로 Pod 선택            │
│                          v                                      │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │  Endpoints (자동 생성)                                       ││
│  │  ┌─────────────────────────────────────────────────────────┐││
│  │  │ 10.244.0.5:8081 (Pod A) - Ready                        │││
│  │  │ 10.244.0.6:8081 (Pod B) - Ready                        │││
│  │  │ 10.244.0.7:8081 (Pod C) - NotReady (제외됨)            │││
│  │  └─────────────────────────────────────────────────────────┘││
│  └─────────────────────────────────────────────────────────────┘│
│                                                                 │
│  kube-proxy: ClusterIP로 들어온 트래픽을 Endpoints로 분산       │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 5.2 Service 유형

```yaml
# 1. ClusterIP (기본값) - 클러스터 내부에서만 접근 가능
apiVersion: v1
kind: Service
metadata:
  name: auth-service
  namespace: portal-universe
spec:
  type: ClusterIP
  selector:
    app: auth-service
  ports:
    - port: 8081       # Service 포트
      targetPort: 8081  # Pod 포트
```

```
┌─────────────────────────────────────────────────────────────────┐
│                    Service Types 비교                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. ClusterIP (기본값)                                          │
│     ┌──────────────┐                                           │
│     │  Internal    │    클러스터 내부에서만 접근                 │
│     │  Only        │    auth-service:8081                       │
│     └──────────────┘                                           │
│                                                                 │
│  2. NodePort                                                    │
│     ┌──────────────┐                                           │
│     │ External     │    모든 노드의 특정 포트로 접근             │
│     │ via Node     │    <NodeIP>:30080                         │
│     └──────────────┘                                           │
│                                                                 │
│  3. LoadBalancer                                                │
│     ┌──────────────┐                                           │
│     │ External     │    클라우드 LB가 외부 IP 제공              │
│     │ via Cloud LB │    <External-IP>:8081                     │
│     └──────────────┘                                           │
│                                                                 │
│  4. ExternalName                                                │
│     ┌──────────────┐                                           │
│     │ DNS Alias    │    외부 DNS를 내부 이름으로 매핑           │
│     │              │    CNAME -> external.database.com         │
│     └──────────────┘                                           │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 5.3 CoreDNS와 DNS 레코드

Kubernetes는 CoreDNS를 통해 Service Discovery를 수행합니다.

```
┌─────────────────────────────────────────────────────────────────┐
│                     DNS Record Format                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Service DNS:                                                   │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ <service-name>.<namespace>.svc.cluster.local             │   │
│  │                                                          │   │
│  │ 예시:                                                    │   │
│  │ - auth-service.portal-universe.svc.cluster.local         │   │
│  │ - blog-service.portal-universe.svc.cluster.local         │   │
│  │                                                          │   │
│  │ 같은 Namespace에서는 짧은 이름 사용 가능:                 │   │
│  │ - auth-service                                           │   │
│  │ - blog-service                                           │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  Pod DNS (Headless Service):                                    │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ <pod-ip-dashed>.<namespace>.pod.cluster.local            │   │
│  │                                                          │   │
│  │ 예시:                                                    │   │
│  │ - 10-244-0-5.portal-universe.pod.cluster.local          │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  SRV Record (Port 정보 포함):                                   │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ _<port-name>._<protocol>.<service>.<namespace>.svc...    │   │
│  │                                                          │   │
│  │ 예시:                                                    │   │
│  │ - _http._tcp.auth-service.portal-universe.svc.cluster.local │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 5.4 DNS 검색 도메인 (Search Domain)

```
┌─────────────────────────────────────────────────────────────────┐
│                    Pod의 /etc/resolv.conf                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  nameserver 10.96.0.10  # CoreDNS ClusterIP                    │
│  search portal-universe.svc.cluster.local                       │
│         svc.cluster.local                                       │
│         cluster.local                                           │
│  options ndots:5                                                │
│                                                                 │
│  DNS 조회 순서 (auth-service 요청 시):                          │
│  1. auth-service.portal-universe.svc.cluster.local (우선)       │
│  2. auth-service.svc.cluster.local                             │
│  3. auth-service.cluster.local                                 │
│  4. auth-service (외부 DNS)                                    │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 5.5 Headless Service

StatefulSet이나 직접 Pod에 접근해야 할 때 사용합니다.

```yaml
# Headless Service (ClusterIP: None)
apiVersion: v1
kind: Service
metadata:
  name: kafka-headless
spec:
  clusterIP: None  # Headless!
  selector:
    app: kafka
  ports:
    - port: 9092
```

```
┌─────────────────────────────────────────────────────────────────┐
│                    Headless vs ClusterIP                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ClusterIP Service:                                             │
│  ┌────────────────────────────────────────────────────────────┐│
│  │ DNS Query: auth-service                                    ││
│  │ Response:  10.96.0.10 (ClusterIP - 가상 IP)               ││
│  │                                                            ││
│  │ 트래픽은 kube-proxy가 Pod들로 분산                         ││
│  └────────────────────────────────────────────────────────────┘│
│                                                                 │
│  Headless Service (clusterIP: None):                            │
│  ┌────────────────────────────────────────────────────────────┐│
│  │ DNS Query: kafka-headless                                  ││
│  │ Response:  10.244.0.5 (Pod A IP)                          ││
│  │            10.244.0.6 (Pod B IP)  <- 모든 Pod IP 반환     ││
│  │            10.244.0.7 (Pod C IP)                          ││
│  │                                                            ││
│  │ 클라이언트가 직접 Pod 선택 가능                            ││
│  └────────────────────────────────────────────────────────────┘│
│                                                                 │
│  사용 사례:                                                     │
│  - Kafka: 특정 브로커에 직접 연결                               │
│  - MongoDB ReplicaSet: Primary/Secondary 구분                   │
│  - Elasticsearch: 노드 직접 접근                                │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 6. Portal Universe의 Service Discovery 분석

Portal Universe는 **Kubernetes 내장 DNS 기반 Service Discovery**를 사용합니다.

### 6.1 아키텍처 개요

```
┌─────────────────────────────────────────────────────────────────┐
│              Portal Universe Service Discovery                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  External Traffic                                               │
│        │                                                        │
│        v                                                        │
│  ┌─────────────┐                                               │
│  │   Ingress   │  (nginx-ingress-controller)                   │
│  │   /api/*    │                                               │
│  └──────┬──────┘                                               │
│         │                                                       │
│         v                                                       │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  API Gateway (ClusterIP: api-gateway:8080)              │   │
│  │                                                          │   │
│  │  routes:                                                 │   │
│  │  - /api/auth/**  -> http://auth-service:8081           │   │
│  │  - /api/blog/**  -> http://blog-service:8082           │   │
│  │  - /api/shopping/** -> http://shopping-service:8083    │   │
│  └──────────────────────────────────────────────────────────┘   │
│         │                                                       │
│         │ K8s DNS Resolution                                    │
│         │ (auth-service -> 10.96.x.x -> Pod IPs)               │
│         v                                                       │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │                   Backend Services                        │ │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐  │ │
│  │  │auth-service │  │blog-service │  │shopping-service │  │ │
│  │  │ClusterIP    │  │ClusterIP    │  │ClusterIP        │  │ │
│  │  │:8081        │  │:8082        │  │:8083            │  │ │
│  │  └─────────────┘  └─────────────┘  └─────────────────┘  │ │
│  └───────────────────────────────────────────────────────────┘ │
│         │                                                       │
│         │ K8s DNS Resolution                                    │
│         v                                                       │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │                  Infrastructure Services                  │ │
│  │  ┌───────┐ ┌───────┐ ┌─────────┐ ┌───────┐ ┌──────────┐│ │
│  │  │mysql  │ │mongodb│ │redis    │ │kafka  │ │zipkin    ││ │
│  │  │-db    │ │       │ │         │ │       │ │          ││ │
│  │  └───────┘ └───────┘ └─────────┘ └───────┘ └──────────┘│ │
│  └───────────────────────────────────────────────────────────┘ │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 6.2 Eureka를 사용하지 않는 이유

Portal Universe의 API Gateway 설정에서 Eureka Discovery는 비활성화되어 있습니다:

```yaml
# services/api-gateway/src/main/resources/application.yml
spring:
  cloud:
    gateway:
      discovery:
        locator:
          enabled: false  # Eureka Discovery 비활성화!
```

**Kubernetes 환경에서 Eureka가 불필요한 이유:**

| Eureka | Kubernetes DNS |
|--------|----------------|
| 별도 서버 운영 필요 | 내장 기능 |
| Heartbeat로 상태 관리 | Readiness Probe로 관리 |
| Client 라이브러리 필요 | DNS만 사용 |
| 추가 인프라 비용 | 무료 |

### 6.3 서비스 URL 설정

**ConfigMap (인프라 서비스):**

```yaml
# k8s/infrastructure/configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: portal-universe-config
  namespace: portal-universe
data:
  KAFKA_BOOTSTRAP_SERVERS: "kafka:29092"        # K8s Service 이름
  ELASTICSEARCH_URIS: "http://elasticsearch:9200"
  REDIS_HOST: "redis"                           # K8s Service 이름
  MYSQL_HOST: "mysql-db"                        # K8s Service 이름
  MONGODB_HOST: "mongodb"                       # K8s Service 이름
```

**API Gateway Kubernetes Profile (백엔드 서비스):**

```yaml
# services/api-gateway/src/main/resources/application-kubernetes.yml
services:
  auth:
    url: "http://auth-service"      # K8s Service 이름 (포트 생략 - 80 가정)
  blog:
    url: "http://blog-service"
  shopping:
    url: "http://shopping-service"
  notification:
    url: "http://notification-service"
```

### 6.4 Service 정의 예시

```yaml
# k8s/services/auth-service.yaml
---
apiVersion: v1
kind: Service
metadata:
  name: auth-service          # DNS 이름으로 사용됨
  namespace: portal-universe
  labels:
    app: auth-service
spec:
  type: ClusterIP             # 클러스터 내부 전용
  ports:
    - port: 8081              # Service 포트
      targetPort: 8081        # Container 포트
      protocol: TCP
      name: http
  selector:
    app: auth-service         # 이 label을 가진 Pod로 라우팅
```

### 6.5 Service Discovery 흐름

```
┌─────────────────────────────────────────────────────────────────┐
│             API Gateway -> Auth Service 호출 흐름               │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. API Gateway가 http://auth-service:8081/api/auth/login 호출  │
│     ┌────────────────┐                                         │
│     │  API Gateway   │                                         │
│     │ (10.244.1.5)   │                                         │
│     └───────┬────────┘                                         │
│             │                                                   │
│  2. DNS 조회: auth-service.portal-universe.svc.cluster.local    │
│             │                                                   │
│             v                                                   │
│     ┌────────────────┐                                         │
│     │    CoreDNS     │                                         │
│     │ (10.96.0.10)   │                                         │
│     └───────┬────────┘                                         │
│             │                                                   │
│  3. A 레코드 응답: 10.96.100.50 (auth-service ClusterIP)        │
│             │                                                   │
│             v                                                   │
│     ┌────────────────┐                                         │
│     │  kube-proxy    │  iptables/IPVS 규칙 적용                │
│     └───────┬────────┘                                         │
│             │                                                   │
│  4. Endpoint 선택 (Round-Robin)                                 │
│             │                                                   │
│             v                                                   │
│     ┌────────────────────────────────────────────────┐         │
│     │  Auth Service Endpoints                        │         │
│     │  ┌────────────────┐  ┌────────────────┐       │         │
│     │  │ Pod A          │  │ Pod B          │       │         │
│     │  │ 10.244.2.10    │  │ 10.244.3.15    │       │         │
│     │  │ :8081          │  │ :8081          │       │         │
│     │  └────────────────┘  └────────────────┘       │         │
│     └────────────────────────────────────────────────┘         │
│                                                                 │
│  5. Pod A 또는 B로 트래픽 전달                                   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 6.6 Readiness Probe와 Service Discovery

Pod가 Ready 상태가 아니면 Endpoints에서 자동으로 제외됩니다.

```yaml
# k8s/services/auth-service.yaml (Deployment 부분)
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8081
  initialDelaySeconds: 5
  periodSeconds: 5
  failureThreshold: 3
```

```
┌─────────────────────────────────────────────────────────────────┐
│              Readiness Probe와 Endpoints 관계                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  시나리오: auth-service Pod 3개 중 1개 장애                     │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │  auth-service Service (ClusterIP: 10.96.100.50)            ││
│  └─────────────────────────────────────────────────────────────┘│
│                          │                                      │
│                          v                                      │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │  Endpoints Controller (자동 관리)                           ││
│  │                                                             ││
│  │  [READY]     [READY]      [NOT READY - Probe 실패]          ││
│  │  Pod A       Pod B        Pod C                             ││
│  │  10.244.2.10 10.244.3.15  10.244.4.20                      ││
│  │     O           O              X   <-- Endpoints에서 제외   ││
│  │                                                             ││
│  │  실제 Endpoints: 10.244.2.10, 10.244.3.15                  ││
│  └─────────────────────────────────────────────────────────────┘│
│                                                                 │
│  kube-proxy는 Ready Pod로만 트래픽 전달                         │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 7. Load Balancing과의 관계

Service Discovery와 Load Balancing은 밀접하게 연관되어 있습니다.

### 7.1 개념 구분

```
┌─────────────────────────────────────────────────────────────────┐
│          Service Discovery vs Load Balancing                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Service Discovery:                                             │
│  "auth-service가 어디 있지?" -> "10.0.0.5, 10.0.0.6, 10.0.0.7"  │
│                                                                 │
│  Load Balancing:                                                │
│  "이 3개 중 어디로 보낼까?" -> "10.0.0.6으로!" (알고리즘 기반)   │
│                                                                 │
│  ┌──────────────┐                    ┌──────────────────────┐  │
│  │   Client     │                    │ Service Instances    │  │
│  └──────┬───────┘                    │ ┌──────────────────┐ │  │
│         │                            │ │ 10.0.0.5 (25%)   │ │  │
│         │ 1. Discovery               │ └──────────────────┘ │  │
│         v                            │ ┌──────────────────┐ │  │
│  ┌──────────────┐  2. Get List       │ │ 10.0.0.6 (25%)   │ │  │
│  │  Registry /  │ ─────────────────> │ └──────────────────┘ │  │
│  │  DNS         │                    │ ┌──────────────────┐ │  │
│  └──────────────┘                    │ │ 10.0.0.7 (50%)   │ │  │
│         │                            │ └──────────────────┘ │  │
│         │ 3. Load Balancing          └──────────────────────┘  │
│         v                                      ^               │
│  ┌──────────────┐  4. Forward to one           │               │
│  │ Load         │ ─────────────────────────────┘               │
│  │ Balancer     │                                              │
│  └──────────────┘                                              │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 7.2 Kubernetes의 Load Balancing

```
┌─────────────────────────────────────────────────────────────────┐
│            Kubernetes Load Balancing Layers                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  L7 (Application Layer):                                        │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │  Ingress Controller (NGINX, Traefik, etc.)                 ││
│  │  - Path-based routing: /api/auth/* -> auth-service         ││
│  │  - Host-based routing: api.example.com -> api-gateway      ││
│  │  - TLS termination                                          ││
│  │  - Rate limiting, Authentication                            ││
│  └─────────────────────────────────────────────────────────────┘│
│                          │                                      │
│                          v                                      │
│  L4 (Transport Layer):                                          │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │  Kubernetes Service (kube-proxy)                           ││
│  │  - iptables mode: Random selection                          ││
│  │  - IPVS mode: Round-robin, Least connections, etc.         ││
│  └─────────────────────────────────────────────────────────────┘│
│                          │                                      │
│                          v                                      │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │  Pods                                                       ││
│  │  ┌───────┐  ┌───────┐  ┌───────┐                           ││
│  │  │ Pod A │  │ Pod B │  │ Pod C │                           ││
│  │  └───────┘  └───────┘  └───────┘                           ││
│  └─────────────────────────────────────────────────────────────┘│
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 7.3 kube-proxy 모드 비교

| 모드 | 알고리즘 | 성능 | 기능 |
|------|---------|------|------|
| **iptables** | Random | 중간 | 기본 |
| **IPVS** | RR, LC, WRR 등 | 높음 | 고급 |
| **userspace** | Round-robin | 낮음 | Legacy |

```
┌─────────────────────────────────────────────────────────────────┐
│                    IPVS Load Balancing                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  지원 알고리즘:                                                  │
│  - rr: Round Robin (기본값)                                     │
│  - lc: Least Connections                                        │
│  - dh: Destination Hashing                                      │
│  - sh: Source Hashing (Session Affinity)                        │
│  - sed: Shortest Expected Delay                                 │
│  - nq: Never Queue                                              │
│                                                                 │
│  활성화 방법 (kube-proxy ConfigMap):                            │
│  ┌────────────────────────────────────────────────────────────┐│
│  │ apiVersion: v1                                             ││
│  │ kind: ConfigMap                                            ││
│  │ metadata:                                                  ││
│  │   name: kube-proxy                                         ││
│  │   namespace: kube-system                                   ││
│  │ data:                                                      ││
│  │   config.conf: |                                          ││
│  │     mode: "ipvs"                                          ││
│  │     ipvs:                                                 ││
│  │       scheduler: "lc"  # Least Connections                ││
│  └────────────────────────────────────────────────────────────┘│
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 7.4 Session Affinity (Sticky Session)

동일 클라이언트의 요청을 같은 Pod로 보내야 할 때:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: auth-service
spec:
  selector:
    app: auth-service
  sessionAffinity: ClientIP  # 클라이언트 IP 기반 Session Affinity
  sessionAffinityConfig:
    clientIP:
      timeoutSeconds: 10800   # 3시간
  ports:
    - port: 8081
```

---

## 8. 실습 예제

### 8.1 Service Discovery 동작 확인

**1. DNS 조회 테스트:**

```bash
# portal-universe namespace의 임시 Pod에서 DNS 테스트
kubectl run dns-test --rm -it --image=busybox \
  -n portal-universe -- nslookup auth-service

# 결과 예시:
# Server:    10.96.0.10
# Address 1: 10.96.0.10 kube-dns.kube-system.svc.cluster.local
#
# Name:      auth-service
# Address 1: 10.96.100.50 auth-service.portal-universe.svc.cluster.local
```

**2. Endpoints 확인:**

```bash
# auth-service의 Endpoints 조회
kubectl get endpoints auth-service -n portal-universe

# 결과 예시:
# NAME           ENDPOINTS                          AGE
# auth-service   10.244.2.10:8081,10.244.3.15:8081  5d
```

**3. Service 정보 확인:**

```bash
# Service 상세 정보
kubectl describe svc auth-service -n portal-universe

# 결과 예시:
# Name:              auth-service
# Namespace:         portal-universe
# Labels:            app=auth-service
# Selector:          app=auth-service
# Type:              ClusterIP
# IP:                10.96.100.50
# Port:              http  8081/TCP
# TargetPort:        8081/TCP
# Endpoints:         10.244.2.10:8081,10.244.3.15:8081
```

### 8.2 서비스 간 통신 테스트

**1. API Gateway에서 Auth Service 호출:**

```bash
# API Gateway Pod 내부에서 curl 테스트
kubectl exec -it deploy/api-gateway -n portal-universe -- \
  curl -s http://auth-service:8081/actuator/health | jq

# 결과:
# {
#   "status": "UP",
#   "components": {
#     "db": {"status": "UP"},
#     "redis": {"status": "UP"}
#   }
# }
```

**2. 전체 DNS 이름으로 호출:**

```bash
kubectl exec -it deploy/api-gateway -n portal-universe -- \
  curl -s http://auth-service.portal-universe.svc.cluster.local:8081/actuator/health
```

### 8.3 Headless Service 실습

**Kafka Headless Service 예시:**

```yaml
# headless-service-example.yaml
apiVersion: v1
kind: Service
metadata:
  name: kafka-headless
  namespace: portal-universe
spec:
  clusterIP: None  # Headless Service
  selector:
    app: kafka
  ports:
    - port: 9092
      name: kafka
```

**DNS 조회 차이 확인:**

```bash
# 일반 Service: ClusterIP 반환
kubectl exec -it deploy/api-gateway -n portal-universe -- \
  nslookup kafka

# Headless Service: 모든 Pod IP 반환
kubectl exec -it deploy/api-gateway -n portal-universe -- \
  nslookup kafka-headless
```

### 8.4 Service Discovery 문제 해결

**문제 1: DNS 조회 실패**

```bash
# CoreDNS Pod 상태 확인
kubectl get pods -n kube-system -l k8s-app=kube-dns

# CoreDNS 로그 확인
kubectl logs -n kube-system -l k8s-app=kube-dns

# resolv.conf 확인
kubectl exec -it deploy/api-gateway -n portal-universe -- cat /etc/resolv.conf
```

**문제 2: Service로 연결되지 않음**

```bash
# Endpoints가 비어있는지 확인
kubectl get endpoints auth-service -n portal-universe

# Pod가 Ready 상태인지 확인
kubectl get pods -n portal-universe -l app=auth-service

# Pod Readiness Probe 확인
kubectl describe pod -n portal-universe -l app=auth-service | grep -A5 Readiness
```

**문제 3: 간헐적 연결 실패**

```bash
# 여러 번 연결 테스트 (Load Balancing 동작 확인)
for i in {1..10}; do
  kubectl exec -it deploy/api-gateway -n portal-universe -- \
    curl -s http://auth-service:8081/actuator/health | jq -r '.status'
done
```

### 8.5 Spring Boot에서 K8s Service Discovery 사용

**Feign Client 설정 (URL 직접 지정):**

```java
// Kubernetes 환경에서는 Service 이름으로 직접 호출
@FeignClient(
    name = "auth-service",
    url = "${services.auth.url}"  // http://auth-service
)
public interface AuthServiceClient {

    @GetMapping("/api/users/{id}")
    UserDto getUser(@PathVariable Long id);
}
```

**application-kubernetes.yml:**

```yaml
services:
  auth:
    url: "http://auth-service"  # K8s Service DNS 이름

feign:
  client:
    config:
      auth-service:
        connectTimeout: 5000
        readTimeout: 5000
```

---

## 9. 모범 사례

### 9.1 Service 설계

```
┌─────────────────────────────────────────────────────────────────┐
│                Service Discovery Best Practices                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  DO:                                                            │
│  ✓ Readiness Probe 설정 (트래픽 수신 준비 확인)                 │
│  ✓ Liveness Probe 설정 (비정상 Pod 자동 재시작)                 │
│  ✓ 적절한 timeoutSeconds 설정                                   │
│  ✓ Service 이름에 명확한 네이밍 컨벤션 사용                     │
│  ✓ Namespace로 환경/팀별 격리                                   │
│                                                                 │
│  DON'T:                                                         │
│  ✗ IP 주소 하드코딩                                             │
│  ✗ Readiness Probe 없이 Service 생성                            │
│  ✗ 과도하게 짧은 Probe 간격 (API 서버 부하)                     │
│  ✗ 모든 서비스를 NodePort로 노출                                │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 9.2 Probe 설정 권장값

```yaml
# 권장 Probe 설정
startupProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8081
  initialDelaySeconds: 30   # 애플리케이션 시작 대기
  periodSeconds: 10
  failureThreshold: 18      # 최대 3분 대기 (10초 * 18회)

livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8081
  initialDelaySeconds: 10
  periodSeconds: 10
  failureThreshold: 3

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8081
  initialDelaySeconds: 5
  periodSeconds: 5
  failureThreshold: 3
```

### 9.3 Namespace 전략

```
┌─────────────────────────────────────────────────────────────────┐
│                    Namespace 전략                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  환경별 분리:                                                    │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │ portal-dev   │  │ portal-stage │  │ portal-prod  │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
│                                                                 │
│  팀/도메인별 분리 (대규모 조직):                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │ team-auth    │  │ team-blog    │  │ team-shopping│          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
│                                                                 │
│  Cross-namespace 통신:                                          │
│  http://auth-service.team-auth.svc.cluster.local               │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 9.4 연결 복원력 (Resilience)

```yaml
# Spring Cloud Gateway Circuit Breaker 설정
resilience4j:
  circuitbreaker:
    configs:
      default:
        sliding-window-size: 20
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s
        permitted-number-of-calls-in-half-open-state: 5

  timelimiter:
    configs:
      default:
        timeout-duration: 5s
```

```
Service Discovery 실패 대응:
1. DNS 캐시 활용 (TTL 기반)
2. Connection Pool 유지
3. Circuit Breaker 패턴
4. Retry with Backoff
5. Fallback 메커니즘
```

---

## 참고 자료

### 공식 문서
- [Kubernetes Service](https://kubernetes.io/docs/concepts/services-networking/service/)
- [CoreDNS for Kubernetes](https://coredns.io/plugins/kubernetes/)
- [Spring Cloud Gateway](https://spring.io/projects/spring-cloud-gateway)

### 관련 학습 문서
- [Microservices Overview](./microservices-overview.md)
- [Inter-Service Communication](./inter-service-communication.md)
- [Service Decomposition](./service-decomposition.md)
