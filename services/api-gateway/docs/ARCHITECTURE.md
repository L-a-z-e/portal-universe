# API Gateway 아키텍처

## 시스템 개요

```
┌─────────────────┐
│  Frontend       │
│  (MFA Modules)  │
└────────┬────────┘
         │
         ▼
┌─────────────────┐    JWT 검증    ┌─────────────────┐
│   API Gateway   │───────────────▶│   Auth Service  │
│   (8080)        │◀───────────────│   (/oauth2/jwks)│
└────────┬────────┘                └─────────────────┘
         │
    라우팅 분기
         │
    ┌────┴────┬────────────┬────────────┐
    ▼         ▼            ▼            ▼
┌──────┐  ┌──────┐    ┌──────────┐  ┌──────────────┐
│ Auth │  │ Blog │    │ Shopping │  │ Notification │
│ 8081 │  │ 8082 │    │   8083   │  │     8084     │
└──────┘  └──────┘    └──────────┘  └──────────────┘
```

## 요청 처리 흐름

```
1. 클라이언트 요청 수신
2. CORS 필터 적용
3. JWT 토큰 검증 (Authorization 헤더)
4. 경로 기반 라우팅 결정
5. 서비스로 요청 전달 (+ 사용자 정보 헤더)
6. 응답 반환
```

## 보안 설정

### JWT 검증
- **방식**: OAuth2 Resource Server
- **공개키**: `http://auth-service:8081/oauth2/jwks`
- **알고리즘**: RS256

### 인증 예외 경로
```java
.pathMatchers("/api/v1/auth/signup").permitAll()
.pathMatchers("/api/v1/blog/posts").permitAll()
.pathMatchers("/actuator/**").permitAll()
.anyExchange().authenticated()
```

## 라우팅 구성

### application.yml
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: auth-service
          uri: lb://auth-service
          predicates:
            - Path=/api/v1/auth/**
          filters:
            - StripPrefix=2

        - id: blog-service
          uri: lb://blog-service
          predicates:
            - Path=/api/v1/blog/**
          filters:
            - StripPrefix=2
```

### 필터 체인

| 순서 | 필터 | 역할 |
|------|------|------|
| 1 | CORS Filter | Cross-Origin 처리 |
| 2 | JWT Filter | 토큰 검증 |
| 3 | StripPrefix | 경로 prefix 제거 |
| 4 | AddRequestHeader | 사용자 정보 헤더 추가 |

## 서비스 디스커버리

### Eureka 연동
```yaml
eureka:
  client:
    service-url:
      defaultZone: http://discovery-service:8761/eureka
```

### 로드 밸런싱
- **방식**: Round Robin (기본)
- **URI 형식**: `lb://service-name`

## CORS 설정

```java
@Bean
public CorsWebFilter corsWebFilter() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(Arrays.asList(
        "http://localhost:30000",  // Portal Shell
        "http://localhost:30001",  // Blog Frontend
        "http://localhost:30002"   // Shopping Frontend
    ));
    config.setAllowedMethods(Arrays.asList("*"));
    config.setAllowedHeaders(Arrays.asList("*"));
    config.setAllowCredentials(true);
    return new CorsWebFilter(source);
}
```

## 사용자 정보 전파

JWT에서 추출한 사용자 정보를 다운스트림 서비스로 전달:

```
X-User-Id: {user-uuid}
X-User-Role: ROLE_USER
```

## 에러 처리

### 인증 실패
```json
{
  "success": false,
  "code": "C003",
  "message": "Unauthorized"
}
```

### 서비스 불가
```json
{
  "success": false,
  "code": "C004",
  "message": "Service Unavailable"
}
```

## 모니터링

### Actuator 엔드포인트
- `/actuator/health` - 헬스 체크
- `/actuator/routes` - 라우팅 정보
- `/actuator/metrics` - 메트릭

### 분산 추적
- Brave (Zipkin 연동)
- 각 요청에 Trace ID 부여

## 확장 고려사항

- **Rate Limiting**: 요청 제한 (Redis 활용)
- **Circuit Breaker**: Resilience4j 연동
- **API Versioning**: 버전별 라우팅
- **Request/Response Logging**: 감사 로그
