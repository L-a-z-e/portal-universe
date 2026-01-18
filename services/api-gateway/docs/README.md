# API Gateway

Portal Universe 플랫폼의 API Gateway 서비스입니다.

## 개요

Spring Cloud Gateway 기반의 단일 진입점으로, JWT 검증, 라우팅, CORS 처리를 담당합니다.

## 핵심 기능

- **JWT 검증**: auth-service의 공개키로 토큰 검증
- **서비스 라우팅**: 경로 기반 마이크로서비스 라우팅
- **CORS 처리**: 마이크로 프론트엔드 Cross-Origin 지원
- **부하 분산**: 서비스 인스턴스 간 로드 밸런싱

## 기술 스택

| 구분 | 기술 |
|------|------|
| Framework | Spring Boot 3.5.5 |
| Gateway | Spring Cloud Gateway |
| Security | OAuth2 Resource Server |
| Discovery | Eureka Client |

## 라우팅 규칙

| 경로 | 대상 서비스 | 설명 |
|------|------------|------|
| `/api/v1/auth/**` | auth-service | 인증 API |
| `/api/v1/blog/**` | blog-service | 블로그 API |
| `/api/v1/shopping/**` | shopping-service | 쇼핑 API |
| `/api/v1/notifications/**` | notification-service | 알림 API |

## 포트

- 개발: `8080`
- Docker: `api-gateway:8080`

## 환경 변수

| 변수 | 설명 | 기본값 |
|------|------|--------|
| `AUTH_SERVICE_JWKS_URI` | JWT 공개키 URI | `http://localhost:8081/oauth2/jwks` |
| `EUREKA_CLIENT_SERVICE_URL` | Eureka 서버 | `http://localhost:8761/eureka` |

## CORS 설정

```yaml
allowed-origins:
  - http://localhost:30000  # Portal Shell
  - http://localhost:30001  # Blog Frontend
  - http://localhost:30002  # Shopping Frontend
allowed-methods: GET, POST, PUT, DELETE, OPTIONS
allowed-headers: "*"
```

## 관련 문서

- [ARCHITECTURE.md](./ARCHITECTURE.md) - Gateway 아키텍처
- [API-ROUTING.md](./API-ROUTING.md) - 라우팅 상세 설정
