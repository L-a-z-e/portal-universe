# Discovery Service 아키텍처

## 시스템 구조

```
┌─────────────────────────────────────────────────────┐
│              Eureka Server (8761)                    │
│  ┌─────────────────────────────────────────────┐   │
│  │         @EnableEurekaServer                  │   │
│  │  ┌──────────────┐  ┌─────────────────────┐  │   │
│  │  │ Registry     │  │ Dashboard           │  │   │
│  │  │ (In-Memory)  │  │ (Web UI)            │  │   │
│  │  └──────────────┘  └─────────────────────┘  │   │
│  └─────────────────────────────────────────────┘   │
└───────────────────────┬─────────────────────────────┘
                        │
    ┌───────────────────┼───────────────────┐
    │                   │                   │
    ▼                   ▼                   ▼
┌────────┐         ┌────────┐         ┌────────┐
│auth-svc│ ←────── │gateway │ ──────→ │blog-svc│
│ :8081  │   HTTP  │ :8080  │  HTTP   │ :8082  │
└────────┘         └────────┘         └────────┘
     ↑                  ↑                  ↑
     └──────── Heartbeat (30s) ────────────┘
```

## 서비스 등록 흐름

```
1. 서비스 시작
       │
       ▼
2. Eureka Client 초기화
       │
       ▼
3. POST /eureka/apps/{appId}
   (서비스 정보 등록)
       │
       ▼
4. Eureka Server Registry 저장
       │
       ▼
5. Heartbeat 전송 시작 (30초마다)
```

## 서비스 발견 흐름

```
┌──────────────┐                    ┌──────────────┐
│  API Gateway │                    │ Eureka Server│
└──────┬───────┘                    └──────┬───────┘
       │                                   │
       │  1. GET /eureka/apps             │
       │ ─────────────────────────────────▶│
       │                                   │
       │  2. 서비스 목록 반환              │
       │◀─────────────────────────────────│
       │                                   │
       │  3. 캐시에 저장 (30초 유효)       │
       │                                   │
       ▼
┌──────────────┐
│ auth-service │  ← 라우팅 (lb://auth-service)
│    :8081     │
└──────────────┘
```

## Self-Preservation 모드

네트워크 장애 시 서비스를 섣불리 제거하지 않는 보호 모드입니다.

```
정상 상태:
  - 등록된 서비스: 5개
  - 예상 Heartbeat: 5 * 2/min = 10/min
  - 실제 Heartbeat: 10/min

장애 감지:
  - 실제 Heartbeat < 85% of 예상치
  → Self-Preservation 활성화
  → 서비스 제거 중단
```

### 설정

```yaml
eureka:
  server:
    enable-self-preservation: true
    renewal-percent-threshold: 0.85
    eviction-interval-timer-in-ms: 60000
```

## 인스턴스 메타데이터

```yaml
eureka:
  instance:
    metadata-map:
      version: 1.0.0
      zone: zone-a
      weight: 100
```

### 메타데이터 활용

```java
// 버전별 라우팅
@Bean
public ReactorLoadBalancer<ServiceInstance> versionBasedLoadBalancer(
        ServiceInstanceListSupplier supplier) {
    return new VersionAwareLoadBalancer(supplier, "1.0.0");
}
```

## 고가용성 구성

```
┌─────────────┐    replicate    ┌─────────────┐
│ Eureka #1   │ ◀─────────────▶ │ Eureka #2   │
│ :8761       │                 │ :8762       │
└──────┬──────┘                 └──────┬──────┘
       │                               │
       └───────────┬───────────────────┘
                   │
           ┌───────┴───────┐
           ▼               ▼
      ┌────────┐      ┌────────┐
      │Service │      │Service │
      │   A    │      │   B    │
      └────────┘      └────────┘
```

### Peer 설정

```yaml
# Eureka #1
eureka:
  client:
    service-url:
      defaultZone: http://eureka2:8762/eureka/

# Eureka #2
eureka:
  client:
    service-url:
      defaultZone: http://eureka1:8761/eureka/
```

## 헬스 체크

### Eureka Dashboard

```
http://localhost:8761
```

대시보드에서 확인 가능:
- 등록된 애플리케이션 목록
- 인스턴스 상태 (UP/DOWN)
- Self-Preservation 상태
- 최근 리스 갱신 현황

### REST API

```bash
# 전체 애플리케이션 조회
curl http://localhost:8761/eureka/apps

# 특정 서비스 조회
curl http://localhost:8761/eureka/apps/auth-service
```

## Kubernetes 환경

Kubernetes에서는 내장 Service Discovery를 사용하므로 Eureka가 선택적입니다.

```yaml
# k8s 프로파일에서 Eureka 비활성화
eureka:
  client:
    enabled: false
```
