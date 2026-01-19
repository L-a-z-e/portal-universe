# Auth Service

Portal Universe 플랫폼의 인증/인가 서비스입니다.

## 개요

Spring Authorization Server 기반 OAuth2 Authorization Server로, JWT 토큰 발급 및 사용자 인증을 담당합니다.

## 핵심 기능

- **OAuth2 Authorization Code Flow with PKCE**: 프론트엔드 Public Client 지원
- **JWT 토큰 발급**: Access Token (2분), Refresh Token (7일)
- **소셜 로그인**: Google OAuth2 연동
- **세션 기반 로그인**: 개발/테스트용

## 기술 스택

| 구분 | 기술 |
|------|------|
| Framework | Spring Boot 3.5.5 |
| Security | Spring Authorization Server |
| Database | MySQL |
| Message Queue | Kafka |

## API 엔드포인트

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/api/users/signup` | 회원가입 |
| GET | `/oauth2/authorize` | OAuth2 인증 시작 |
| POST | `/oauth2/token` | 토큰 발급 |
| GET | `/oauth2/jwks` | 공개키 (JWT 검증용) |
| GET | `/.well-known/openid-configuration` | OIDC 설정 |

## 포트

- 개발: `8081`
- Docker: `auth-service:8081`

## 환경 변수

| 변수 | 설명 | 기본값 |
|------|------|--------|
| `SPRING_DATASOURCE_URL` | MySQL URL | `jdbc:mysql://localhost:3306/authdb` |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | Kafka 주소 | `localhost:9092` |

## 연관 서비스

- **api-gateway**: JWT 검증
- **notification-service**: 가입 이벤트 구독 (Kafka)

## 관련 문서

- [ARCHITECTURE.md](./ARCHITECTURE.md) - 인증 플로우, JWT 구조
- [API.md](./API.md) - REST API 상세 명세
