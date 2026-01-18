# Discovery Service

Netflix Eureka 기반 서비스 레지스트리입니다.

## 개요

마이크로서비스의 등록, 발견, 상태 관리를 담당합니다.

## 포트

- 서비스: `8761`
- Dashboard: `http://localhost:8761`

## 주요 기능

| 기능 | 설명 |
|------|------|
| 서비스 등록 | 마이크로서비스 자동 등록 |
| 서비스 발견 | 이름 기반 서비스 조회 |
| Health Check | 주기적 상태 확인 |
| Self-Preservation | 네트워크 장애 대응 |

## 클라이언트 설정

```yaml
# 다른 서비스의 application.yml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
    fetch-registry: true
    register-with-eureka: true
  instance:
    prefer-ip-address: true
    instance-id: ${spring.application.name}:${random.value}
```

## 등록된 서비스

| 서비스 | Application Name |
|--------|------------------|
| API Gateway | `api-gateway` |
| Auth Service | `auth-service` |
| Blog Service | `blog-service` |
| Shopping Service | `shopping-service` |
| Notification Service | `notification-service` |

## API 엔드포인트

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/eureka/apps` | 전체 서비스 목록 |
| GET | `/eureka/apps/{appId}` | 특정 서비스 정보 |
| DELETE | `/eureka/apps/{appId}/{instanceId}` | 서비스 등록 해제 |

## 실행

```bash
./gradlew :services:discovery-service:bootRun
```

## 관련 문서

- [ARCHITECTURE.md](./ARCHITECTURE.md) - 아키텍처 상세
