---
id: TS-20260119-003
title: Gateway를 통한 로그인 시 403 Forbidden 에러
type: troubleshooting
status: resolved
created: 2026-01-19
updated: 2026-01-19
author: Laze
severity: high
resolved: true
affected_services: [api-gateway, auth-service]
tags: [cors, gateway, login, 403]
---

# Gateway를 통한 로그인 시 403 Forbidden 에러

## 요약

| 항목 | 내용 |
|------|------|
| **심각도** | 🟠 High |
| **발생일** | 2026-01-19 |
| **해결일** | 2026-01-19 |
| **영향 서비스** | api-gateway, auth-service |

## 증상 (Symptoms)

### 현상
- Local 환경에서 Gateway(`http://localhost:8080`)를 통한 로그인 시도 시 403 Forbidden 발생
- 브라우저에 "localhost에 대한 액세스가 거부됨" 메시지 표시
- Auth Service 직접 접근(`http://localhost:9000`)은 정상 동작

### 에러 메시지
```
POST http://localhost:8080/auth-service/login → 403 Forbidden
```

### 모니터링 지표
- Gateway 로그에서 CORS 관련 에러 확인
- 브라우저 개발자 도구 Network 탭에서 Origin 헤더가 `null`로 전송됨

## 원인 분석 (Root Cause)

### 초기 추정
- CSRF 토큰 문제
- Spring Security 인증 설정 문제
- Gateway 라우팅 문제

### 실제 원인
커밋 `32ef3ff`에서 CORS 허용 목록에서 `"null"` origin이 제거됨

**삭제된 코드:**
```java
configuration.addAllowedOrigin("null"); // 로컬 개발 환경에서 Origin이 'null'인 경우 허용
```

**문제 흐름:**
1. 로그인 폼 제출 시 브라우저가 `Origin: null` 헤더 전송
2. Gateway의 CORS 설정에서 `"null"` origin이 허용 목록에서 제거됨
3. CORS 검증 실패 → 403 Forbidden

### 분석 과정
1. Playwright를 사용하여 실제 로그인 요청 시 전송되는 헤더 확인
2. 요청 헤더에서 `origin: "null"` 확인
3. Gateway SecurityConfig.java의 CORS 설정 검토
4. Git 히스토리 분석으로 `32ef3ff` 커밋에서 제거된 것 확인

## 해결 방법 (Solution)

### 즉시 조치 (Immediate Fix)
Gateway SecurityConfig.java에 `"null"` origin 다시 추가

### 영구 조치 (Permanent Fix)
`corsWebFilter()` 메서드에서 `"null"` origin 허용 복원:
```java
configuration.setAllowCredentials(true);
configuration.addAllowedOrigin("null"); // 로컬 개발 환경에서 Origin이 'null'인 경우 허용
configuration.setMaxAge(3600L);
```

### 수정된 파일
| 파일 경로 | 수정 내용 |
|----------|----------|
| `services/api-gateway/src/main/java/com/portal/universe/apigateway/config/SecurityConfig.java` | `"null"` origin 허용 라인 추가 |

## 재발 방지 (Prevention)

### 모니터링
- Gateway 로그에서 CORS 에러 발생 시 알림 설정 고려

### 프로세스 개선
- CORS 설정 변경 시 로컬 환경 테스트 필수화
- 로컬 개발 환경 특성 (Origin: null) 문서화

## 학습 포인트

1. **Origin: null 이해**: 브라우저에서 로컬 파일이나 특정 상황에서 폼 제출 시 `Origin` 헤더가 `"null"` 문자열로 전송될 수 있음
2. **CORS 검증 흐름**: Gateway 단계에서 CORS 검증 실패 시 403 Forbidden 반환
3. **개발 환경 고려**: Production에서는 불필요하지만 Local 개발 환경에서는 `"null"` origin 허용이 필요할 수 있음

## 관련 링크

- [MDN - Origin Header](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Origin)
- [Spring CORS Documentation](https://docs.spring.io/spring-framework/reference/web/webflux-cors.html)

## 관련 이슈

- 원인 커밋: `32ef3ff`
