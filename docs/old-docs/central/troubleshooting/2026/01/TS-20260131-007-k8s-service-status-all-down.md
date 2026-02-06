---
id: TS-20260131-007
title: K8s 환경 Service Status 페이지 전체 서비스 Down 표시
type: troubleshooting
status: resolved
created: 2026-01-31
updated: 2026-01-31
author: Laze
severity: high
resolved: true
affected_services: [portal-shell, api-gateway]
tags: [kubernetes, health-check, network-policy, webflux, module-federation, fabric8, rbac]
---

# K8s 환경 Service Status 페이지 전체 서비스 Down 표시

## 요약

| 항목 | 내용 |
|------|------|
| **심각도** | 🟠 High |
| **발생일** | 2026-01-31 |
| **해결일** | 2026-01-31 ✅ |
| **영향 서비스** | portal-shell, api-gateway |

## 증상 (Symptoms)

### 현상
- K8s 환경에서 Service Status 페이지(`/service-status`) 접속 시 **6개 서비스 모두 "Down"** 표시
- 실제 서비스(auth, blog, shopping, notification, prism, gateway)는 모두 정상 동작 중
- Local/Docker 환경에서는 발생하지 않음

### 에러 메시지

브라우저 콘솔:
```
GET http://localhost:8080/api/health/services net::ERR_CONNECTION_REFUSED
```

또는 K8s 환경에서 올바른 base URL을 사용하더라도:
```
GET http://portal-universe/api/health/services → 504 Gateway Timeout
```

### 재현 조건
1. Kind 클러스터에 전체 서비스 배포
2. `portal-universe` 도메인으로 Portal Shell 접속
3. Service Status 페이지 진입

## 원인 분석 (Root Cause)

3개 레이어의 문제가 복합적으로 작용:

### 원인 1: 프론트엔드 아키텍처 — 브라우저에서 직접 health check 호출

#### 기존 구조 (수정 전)
프론트엔드 `serviceStatus` store가 브라우저에서 **각 서비스의 health endpoint를 직접 호출**하는 구조였음.

```
Browser → http://auth-service:8081/actuator/health     ❌ K8s 내부 DNS 접근 불가
Browser → http://blog-service:8082/actuator/health     ❌
Browser → http://shopping-service:8083/actuator/health ❌
...
```

- Local 환경: `localhost:808X`로 직접 접근 가능하여 정상 동작
- K8s 환경: `auth-service:8081` 등은 클러스터 내부 DNS로, **브라우저에서 resolve 불가**

### 원인 2: API Gateway Self-Call Timeout

Gateway가 자신의 health endpoint를 확인하기 위해 `http://api-gateway:8080/actuator/health`를 WebClient로 호출하면:
- WebFlux는 단일 event loop에서 동작
- 자기 자신에게 HTTP 요청 → 같은 스레드가 요청을 처리해야 하지만 응답을 기다리며 blocking
- **Timeout 발생** (3초 후 down 처리)

### 원인 3: K8s NetworkPolicy — API 서버 egress 차단

`network-policy.yaml`의 기본 egress 정책이 K8s API 서버(`10.96.0.1:443`)로의 통신을 차단:
- fabric8 `KubernetesClient`가 Pod/Deployment 정보를 조회하려면 K8s API에 접근 필요
- NetworkPolicy에 K8s API 서버 egress 규칙 없음 → `connection timeout`

```yaml
# 기존: 외부 인터넷만 허용, 내부 IP 대역 차단
- to:
    - ipBlock:
        cidr: 0.0.0.0/0
        except:
          - 10.0.0.0/8      # ← K8s API 서버(10.96.0.1) 포함!
          - 172.16.0.0/12
          - 192.168.0.0/16
```

## 해결 방법 (Solution)

### 1. API Gateway — Health Aggregation Endpoint 추가

#### 아키텍처 변경

```
수정 전:
  Browser → 각 서비스 health endpoint 직접 호출 (6회 HTTP)

수정 후:
  Browser → GET /api/health/services → API Gateway
  API Gateway → 각 서비스 health endpoint (내부 DNS) + K8s API (Pod/Deployment 정보)
  API Gateway → JSON 응답 (1회 HTTP)
```

#### 신규 파일: DTO

**`services/api-gateway/.../health/dto/PodInfo.java`**
```java
public record PodInfo(String name, String phase, boolean ready, int restarts) {}
```

**`services/api-gateway/.../health/dto/ServiceHealthInfo.java`**
```java
public record ServiceHealthInfo(
        String name, String displayName, String status, Long responseTime,
        Integer replicas, Integer readyReplicas, List<PodInfo> pods
) {
    public static ServiceHealthInfo of(String name, String displayName, String status, long responseTime) {
        return new ServiceHealthInfo(name, displayName, status, responseTime, null, null, null);
    }
    public ServiceHealthInfo withKubernetesInfo(int replicas, int readyReplicas, List<PodInfo> pods) {
        return new ServiceHealthInfo(this.name, this.displayName, this.status, this.responseTime,
                replicas, readyReplicas, pods);
    }
}
```

**`services/api-gateway/.../health/dto/ServiceHealthResponse.java`**
```java
public record ServiceHealthResponse(String overallStatus, Instant timestamp, List<ServiceHealthInfo> services) {
    public static ServiceHealthResponse of(List<ServiceHealthInfo> services) {
        String overall = resolveOverallStatus(services);
        return new ServiceHealthResponse(overall, Instant.now(), services);
    }
}
```

#### 신규 파일: Config

**`services/api-gateway/.../health/config/HealthCheckProperties.java`**
```java
@Data
@Component
@ConfigurationProperties(prefix = "health-check")
public class HealthCheckProperties {
    private List<ServiceConfig> services = new ArrayList<>();

    @Data
    public static class ServiceConfig {
        private String name;
        private String displayName;
        private String url;
        private String healthPath = "/actuator/health";
        private String k8sDeploymentName;
    }
}
```

**`services/api-gateway/.../health/config/KubernetesClientConfig.java`**
```java
@Configuration
@Profile("kubernetes")
public class KubernetesClientConfig {
    @Bean
    public KubernetesClient kubernetesClient() {
        return new KubernetesClientBuilder().build();
    }
}
```

#### 신규 파일: Service & Controller

**`services/api-gateway/.../health/ServiceHealthAggregator.java`**

핵심 로직:
- `HealthCheckProperties`에서 서비스 목록을 읽어 각 서비스의 `/actuator/health`를 WebClient로 호출
- **Self-call 방지**: gateway 자체는 `HealthEndpoint` bean을 직접 호출 (HTTP self-call 대신)
- K8s 환경: `fabric8 KubernetesClient`로 Deployment replica 수, Pod 상태 조회
- Non-K8s 환경: `KubernetesClient`가 null이면 K8s 정보 없이 health만 반환

```java
// Self-call 방지 — HealthEndpoint 직접 호출
private Mono<ServiceHealthInfo> checkSelf(HealthCheckProperties.ServiceConfig config) {
    return Mono.fromCallable(() -> {
        var health = healthEndpoint.health();
        String s = Status.UP.equals(health.getStatus()) ? "up" : "down";
        return ServiceHealthInfo.of(config.getName(), config.getDisplayName(), s, responseTime);
    }).subscribeOn(Schedulers.boundedElastic());
}
```

**`services/api-gateway/.../health/ServiceHealthController.java`**
```java
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class ServiceHealthController {
    private final ServiceHealthAggregator healthAggregator;

    @GetMapping("/services")
    public Mono<ServiceHealthResponse> getServicesHealth() {
        return healthAggregator.aggregateHealth();
    }
}
```

#### 수정 파일: application.yml

**`application.yml`** — health-check 서비스 목록 추가:
```yaml
health-check:
  services:
    - name: api-gateway
      displayName: API Gateway
      url: ${services.gateway.url}
      healthPath: /actuator/health
      k8sDeploymentName: api-gateway
    - name: auth-service
      displayName: Auth Service
      url: ${services.auth.url}
      # ... 나머지 서비스
```

#### 수정 파일: build.gradle

`fabric8 kubernetes-client` 의존성 추가:
```gradle
implementation platform('io.fabric8:kubernetes-client-bom:6.13.4')
implementation 'io.fabric8:kubernetes-client'
```

#### 수정 파일: PublicPathProperties

`/api/health/**` 경로를 `permitAll` 및 `skipJwtParsing`에 추가:
```java
"/api/health/**"  // permitAll
"/api/health/"    // skipJwtParsing
```

### 2. K8s RBAC — ServiceAccount, Role, RoleBinding

**`k8s/services/api-gateway.yaml`** 에 추가:
```yaml
# ServiceAccount
apiVersion: v1
kind: ServiceAccount
metadata:
  name: api-gateway-sa
  namespace: portal-universe

# Role — pods, deployments 조회 권한
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: api-gateway-health-reader
  namespace: portal-universe
rules:
  - apiGroups: [""]
    resources: ["pods"]
    verbs: ["get", "list"]
  - apiGroups: ["apps"]
    resources: ["deployments"]
    verbs: ["get", "list"]

# RoleBinding
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: api-gateway-health-reader-binding
  namespace: portal-universe
subjects:
  - kind: ServiceAccount
    name: api-gateway-sa
roleRef:
  kind: Role
  name: api-gateway-health-reader
  apiGroup: rbac.authorization.k8s.io
```

Deployment에 `serviceAccountName: api-gateway-sa` 추가.

### 3. K8s NetworkPolicy — API 서버 egress 허용

**`k8s/infrastructure/network-policy.yaml`** 에 추가:
```yaml
# api-gateway → K8s API 서버 egress 허용
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-k8s-api-for-gateway
  namespace: portal-universe
spec:
  podSelector:
    matchLabels:
      app: api-gateway
  policyTypes:
    - Egress
  egress:
    - to:
        - ipBlock:
            cidr: 10.96.0.1/32   # K8s API 서버 ClusterIP
      ports:
        - protocol: TCP
          port: 443
```

K8s API 서버 IP 확인:
```bash
kubectl get svc kubernetes -n default -o jsonpath='{.spec.clusterIP}'
# 결과: 10.96.0.1
```

### 4. 프론트엔드 — 단일 API 호출로 변경

**`frontend/portal-shell/src/store/serviceStatus.ts`**

기존: 각 서비스에 개별 HTTP 요청 (6회)
수정 후: `/api/health/services` 단일 호출 (1회)

```typescript
async checkAllServices(): Promise<void> {
  const baseUrl = import.meta.env.VITE_API_BASE_URL || '';
  const response = await fetch(`${baseUrl}/api/health/services`, {
    method: 'GET',
    headers: { Accept: 'application/json' },
  });
  const data = await response.json();
  // data.services 배열을 파싱하여 store에 반영
}
```

**`frontend/portal-shell/src/views/ServiceStatusPage.vue`**

K8s 환경에서 추가 정보 표시:
- Replicas (ready/total)
- Pod 목록 (이름, phase, ready 상태, restart 횟수)
- Collapsible pod detail section

## 수정된 파일 전체 목록

### 신규 생성 (7개)

| 파일 경로 | 내용 |
|----------|------|
| `services/api-gateway/.../health/dto/PodInfo.java` | Pod 상태 DTO |
| `services/api-gateway/.../health/dto/ServiceHealthInfo.java` | 서비스별 health 정보 DTO |
| `services/api-gateway/.../health/dto/ServiceHealthResponse.java` | 전체 응답 DTO |
| `services/api-gateway/.../health/config/HealthCheckProperties.java` | health-check 설정 바인딩 |
| `services/api-gateway/.../health/config/KubernetesClientConfig.java` | K8s 프로파일 전용 KubernetesClient bean |
| `services/api-gateway/.../health/ServiceHealthAggregator.java` | health 수집 서비스 (WebClient + K8s API) |
| `services/api-gateway/.../health/ServiceHealthController.java` | `/api/health/services` endpoint |

### 수정 (8개)

| 파일 경로 | 변경 내용 |
|----------|----------|
| `services/api-gateway/build.gradle` | fabric8 kubernetes-client 의존성 추가 |
| `services/api-gateway/.../config/PublicPathProperties.java` | `/api/health/**` permitAll/skipJwtParsing 추가 |
| `services/api-gateway/.../resources/application.yml` | health-check 서비스 목록 추가 |
| `services/api-gateway/.../resources/application-kubernetes.yml` | K8s 환경 health-check URL 설정 |
| `k8s/services/api-gateway.yaml` | ServiceAccount, Role, RoleBinding 추가, Deployment에 serviceAccountName 설정 |
| `k8s/infrastructure/network-policy.yaml` | K8s API 서버 egress 허용 정책 추가 |
| `frontend/portal-shell/src/store/serviceStatus.ts` | 단일 API 호출 방식으로 변경, K8s 정보(replicas, pods) 지원 |
| `frontend/portal-shell/src/views/ServiceStatusPage.vue` | Pod 상세 정보 UI 추가 |

## 검증

### API Gateway health endpoint
```bash
# K8s 환경
kubectl exec -it deploy/api-gateway -n portal-universe -- \
  curl -s http://localhost:8080/api/health/services | jq .

# 기대 응답
{
  "overallStatus": "up",
  "timestamp": "2026-01-31T...",
  "services": [
    { "name": "api-gateway", "displayName": "API Gateway", "status": "up", "responseTime": 5, "replicas": 1, "readyReplicas": 1, "pods": [...] },
    { "name": "auth-service", "displayName": "Auth Service", "status": "up", "responseTime": 42, ... },
    ...
  ]
}
```

### 브라우저 확인
1. Service Status 페이지 접속 → 전체 서비스 "Healthy" 표시
2. 콘솔에 `net::ERR_CONNECTION_REFUSED` 없음
3. 10초 간격 auto-refresh 정상 동작
4. Pod 정보 토글 정상 작동

## 재발 방지 (Prevention)

### 체크리스트
- [ ] 프론트엔드에서 K8s 내부 DNS를 직접 호출하는 패턴 금지 (반드시 API Gateway 경유)
- [ ] WebFlux 서비스에서 self-call 시 `HealthEndpoint` 직접 호출 패턴 사용
- [ ] NetworkPolicy 변경 시 K8s API 서버 egress 영향 범위 확인
- [ ] 새 서비스 추가 시 `health-check.services` 목록에 등록

### 아키텍처 원칙
```
브라우저 → API Gateway → 내부 서비스
         (유일한 진입점)

❌ 브라우저 → K8s 내부 서비스 직접 호출
```

## 학습 포인트

### 1. 브라우저 vs 서버 사이드 호출 구분
- 브라우저는 K8s 클러스터 네트워크에 접근할 수 없음
- `http://auth-service:8081`은 K8s 내부 DNS — 브라우저에서 resolve 불가
- health check 같은 내부 통신은 반드시 서버(API Gateway)에서 수행

### 2. WebFlux Self-Call 주의
- WebFlux reactor 스레드에서 자기 자신에게 HTTP 요청하면 deadlock/timeout 발생
- Spring Boot `HealthEndpoint` bean을 `@Autowired`하여 직접 호출로 해결
- `Schedulers.boundedElastic()`에서 blocking 호출 실행

### 3. NetworkPolicy와 K8s API 접근
- K8s API 서버는 `10.96.0.1:443` (기본값)에 위치
- `10.0.0.0/8` 대역을 egress에서 차단하면 K8s API 접근도 차단됨
- fabric8 `KubernetesClient`는 Pod 내부의 ServiceAccount 토큰을 자동 사용

### 4. RBAC 최소 권한 원칙
- `ClusterRole` 대신 `Role`(namespace 스코프)로 최소 권한 부여
- `pods: [get, list]`, `deployments: [get, list]`만 허용
- ServiceAccount를 명시적으로 생성하고 Deployment에 연결

## 관련 문서

- [TS-20260121-003: Kubernetes 배포 중 발생한 복합 인프라 이슈](./TS-20260121-003-k8s-deployment-issues.md)
- [Spring Boot Actuator Health Endpoint](https://docs.spring.io/spring-boot/reference/actuator/endpoints.html#actuator.endpoints.health)
- [fabric8 Kubernetes Client](https://github.com/fabric8io/kubernetes-client)
- [Kubernetes NetworkPolicy](https://kubernetes.io/docs/concepts/services-networking/network-policies/)
- [Kubernetes RBAC](https://kubernetes.io/docs/reference/access-authn-authz/rbac/)
