# Load Balancing

Kubernetes Service Discovery를 활용한 로드 밸런싱을 학습합니다.

## 개요

Spring Cloud Gateway에서 Kubernetes 환경의 Service Discovery와 로드 밸런싱을 구성합니다.

```
              ┌─────────────────┐
              │   API Gateway   │
              └────────┬────────┘
                       │
              ┌────────▼────────┐
              │  K8s Service    │  ← Service Discovery
              │ (blog-service)  │
              └────────┬────────┘
                       │
         ┌─────────────┼─────────────┐
         ▼             ▼             ▼
    ┌─────────┐   ┌─────────┐   ┌─────────┐
    │  Pod 1  │   │  Pod 2  │   │  Pod 3  │
    └─────────┘   └─────────┘   └─────────┘
```

## Kubernetes DNS 기반 라우팅

### Portal Universe 구성

Kubernetes 환경에서는 Service DNS명을 직접 사용합니다.

```yaml
# application-kubernetes.yml
services:
  gateway:
    url: "http://api-gateway"         # K8s Service DNS
  auth:
    url: "http://auth-service"
  blog:
    url: "http://blog-service"
  shopping:
    url: "http://shopping-service"
  notification:
    url: "http://notification-service"
```

### K8s Service 정의

```yaml
# k8s/blog-service.yaml
apiVersion: v1
kind: Service
metadata:
  name: blog-service
  namespace: portal-universe
spec:
  selector:
    app: blog-service
  ports:
    - port: 80                        # Service Port
      targetPort: 8082                # Container Port
  type: ClusterIP
```

### DNS 해석 흐름

```
api-gateway → blog-service (DNS) → 10.96.0.15 (ClusterIP) → Pod IP
                     ↓
          kube-dns / CoreDNS
```

## Spring Cloud Kubernetes Discovery

### 의존성 추가 (선택적)

```gradle
dependencies {
    implementation 'org.springframework.cloud:spring-cloud-starter-kubernetes-client-all'
}
```

### Discovery 설정

```yaml
spring:
  cloud:
    kubernetes:
      discovery:
        enabled: true
        all-namespaces: false          # 같은 namespace만
      loadbalancer:
        enabled: true

    gateway:
      discovery:
        locator:
          enabled: true                # Service Discovery 자동 라우팅
          lower-case-service-id: true
```

### 동적 Service Discovery

```yaml
# Discovery 기반 자동 라우팅 활성화 시
spring:
  cloud:
    gateway:
      routes:
        - id: blog-service-route
          uri: lb://blog-service       # lb:// prefix로 로드밸런싱
          predicates:
            - Path=/api/blog/**
```

## 로드 밸런싱 전략

### 1. Kubernetes kube-proxy (기본)

K8s Service를 통해 자동으로 로드 밸런싱됩니다.

```
┌─────────────────────────────────────────────────┐
│                  kube-proxy                      │
├─────────────────────────────────────────────────┤
│  iptables/IPVS 모드                             │
│  - Round Robin (기본)                           │
│  - Session Affinity (설정 시)                   │
└─────────────────────────────────────────────────┘
```

```yaml
# Session Affinity 설정 (필요 시)
apiVersion: v1
kind: Service
metadata:
  name: blog-service
spec:
  sessionAffinity: ClientIP
  sessionAffinityConfig:
    clientIP:
      timeoutSeconds: 10800            # 3시간
```

### 2. Spring Cloud LoadBalancer

```yaml
spring:
  cloud:
    loadbalancer:
      ribbon:
        enabled: false                 # Ribbon 비활성화 (deprecated)
      configurations: default

# 커스텀 로드밸런서 설정
loadbalancer:
  clients:
    blog-service:
      hint: "round-robin"
```

### 커스텀 LoadBalancer 구현

```java
@Configuration
public class CustomLoadBalancerConfig {

    @Bean
    @Primary
    public ReactorLoadBalancer<ServiceInstance> weightedLoadBalancer(
            ServiceInstanceListSupplier instanceSupplier) {

        return new WeightedLoadBalancer(instanceSupplier);
    }
}

public class WeightedLoadBalancer implements ReactorServiceInstanceLoadBalancer {

    private final ServiceInstanceListSupplier instanceSupplier;
    private final AtomicInteger position = new AtomicInteger(0);

    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {
        return instanceSupplier.get()
                .next()
                .map(instances -> {
                    if (instances.isEmpty()) {
                        return new EmptyResponse();
                    }

                    // Weighted Round Robin
                    int index = Math.abs(position.incrementAndGet()
                            % getTotalWeight(instances));

                    ServiceInstance instance = selectByWeight(instances, index);
                    return new DefaultResponse(instance);
                });
    }

    private int getTotalWeight(List<ServiceInstance> instances) {
        return instances.stream()
                .mapToInt(i -> getWeight(i))
                .sum();
    }

    private int getWeight(ServiceInstance instance) {
        String weight = instance.getMetadata().get("weight");
        return weight != null ? Integer.parseInt(weight) : 1;
    }
}
```

## Health Check 기반 로드 밸런싱

### Kubernetes Readiness Probe

```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: blog-service
spec:
  template:
    spec:
      containers:
        - name: blog-service
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8082
            initialDelaySeconds: 30
            periodSeconds: 10
            failureThreshold: 3
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8082
            initialDelaySeconds: 60
            periodSeconds: 30
```

### Gateway에서 Health 확인

```yaml
# application.yml
spring:
  cloud:
    gateway:
      routes:
        - id: blog-service-route
          uri: ${services.blog.url}
          predicates:
            - Path=/api/blog/**
          metadata:
            response-timeout: 5000
            connect-timeout: 2000
```

## 환경별 구성

### Local (개발)

```yaml
# application-local.yml
services:
  blog:
    url: "http://localhost:8082"      # 단일 인스턴스

spring:
  cloud:
    gateway:
      discovery:
        locator:
          enabled: false              # Discovery 비활성화
```

### Docker Compose

```yaml
# application-docker.yml
services:
  blog:
    url: "http://blog-service:8082"   # Docker DNS

# docker-compose.yml
services:
  blog-service:
    image: portal/blog-service
    deploy:
      replicas: 2                     # 다중 인스턴스
```

### Kubernetes

```yaml
# application-kubernetes.yml
services:
  blog:
    url: "http://blog-service"        # K8s Service (port 80)

# HPA로 자동 스케일링
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: blog-service-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: blog-service
  minReplicas: 2
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
```

## Failover 처리

### Circuit Breaker와 연동

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: blog-service-route
          uri: ${services.blog.url}
          predicates:
            - Path=/api/blog/**
          filters:
            - name: CircuitBreaker
              args:
                name: blogCircuitBreaker
                fallbackUri: forward:/fallback/blog
            - name: Retry
              args:
                retries: 3
                statuses: BAD_GATEWAY, SERVICE_UNAVAILABLE
                methods: GET
                backoff:
                  firstBackoff: 100ms
                  maxBackoff: 500ms
                  factor: 2
```

### Fallback Controller

```java
@RestController
public class FallbackController {

    @GetMapping("/fallback/blog")
    public Mono<String> blogServiceFallback() {
        return Mono.just("Blog Service is currently unavailable. " +
                         "Please try again later.");
    }

    @GetMapping("/fallback/shopping")
    public Mono<ResponseEntity<Map<String, Object>>> shoppingServiceFallback() {
        Map<String, Object> response = Map.of(
                "status", "error",
                "message", "Shopping Service is temporarily unavailable",
                "timestamp", Instant.now()
        );
        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(response));
    }
}
```

## 모니터링

### 로드 밸런싱 메트릭

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus

  metrics:
    tags:
      application: ${spring.application.name}
```

### Prometheus 쿼리

```promql
# 서비스별 요청 분포
sum by (uri) (rate(http_server_requests_seconds_count{application="api-gateway"}[5m]))

# 응답 시간 분포
histogram_quantile(0.95, sum by (le, uri)
  (rate(http_server_requests_seconds_bucket{application="api-gateway"}[5m])))
```

## 테스트

```bash
# 로드 밸런싱 확인 (여러 번 요청)
for i in {1..10}; do
  curl -s http://localhost:8080/api/blog/posts | jq '.hostname'
done

# K8s에서 Pod IP 확인
kubectl get pods -l app=blog-service -o wide
```

## 참고 자료

- Portal Universe: `services/api-gateway/src/main/resources/application-kubernetes.yml`
- [Kubernetes Service](https://kubernetes.io/docs/concepts/services-networking/service/)
- [Spring Cloud Kubernetes](https://spring.io/projects/spring-cloud-kubernetes)
