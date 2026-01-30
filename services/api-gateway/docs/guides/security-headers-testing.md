---
id: api-gateway-security-headers-testing
title: Security Headers Filter 테스트 가이드
type: guide
status: current
created: 2026-01-23
updated: 2026-01-30
author: Portal Universe Team
tags: [api-gateway, security, headers, testing]
---

# Security Headers Filter - 테스트 가이드

## 구현 내용

### 1. SecurityHeadersProperties
위치: `config/SecurityHeadersProperties.java`
- application.yml의 `security.headers` 설정과 매핑
- CSP, HSTS, Cache-Control 등 세부 설정 관리

### 2. SecurityHeadersFilter
위치: `config/SecurityHeadersFilter.java`
- GlobalFilter로 구현 (Order: HIGHEST_PRECEDENCE)
- 모든 응답에 보안 헤더 자동 추가
- 환경별 조건부 적용 (HSTS)

### 3. 적용된 보안 헤더

| 헤더 | 기본값 | 설명 |
|------|--------|------|
| X-Content-Type-Options | nosniff | MIME 타입 스니핑 방지 |
| X-Frame-Options | DENY | 클릭재킹 방지 |
| X-XSS-Protection | 1; mode=block | XSS 공격 방지 |
| Referrer-Policy | strict-origin-when-cross-origin | Referrer 정보 제어 |
| Permissions-Policy | geolocation=(), microphone=(), camera=() | 브라우저 기능 제한 |
| Content-Security-Policy | 설정 참조 | 리소스 로딩 제한 |
| Strict-Transport-Security | max-age=31536000; includeSubDomains | HTTPS 강제 (프로덕션) |
| Cache-Control | 인증 경로에만 | 캐시 제어 |

---

## 테스트 방법

### 1. API Gateway 실행

```bash
cd services/api-gateway
./gradlew bootRun --args='--spring.profiles.active=local'
```

### 2. curl로 헤더 확인

```bash
# 일반 API 엔드포인트
curl -I http://localhost:8080/api/blog/posts

# 인증 엔드포인트 (Cache-Control 확인)
curl -I http://localhost:8080/api/auth/check

# 예상 응답 헤더:
# X-Content-Type-Options: nosniff
# X-Frame-Options: DENY
# X-XSS-Protection: 1; mode=block
# Referrer-Policy: strict-origin-when-cross-origin
# Permissions-Policy: geolocation=(), microphone=(), camera=()
# Content-Security-Policy: default-src 'self'; ...
# Cache-Control: no-store, no-cache, must-revalidate (인증 경로만)
```

### 3. 브라우저 개발자 도구 확인

1. 브라우저에서 http://localhost:30000 접속
2. F12 > Network 탭 열기
3. 임의의 API 요청 후 Response Headers 확인
4. 보안 헤더가 모두 포함되어 있는지 검증

### 4. 환경별 동작 확인

#### Local (HTTP)
- HSTS: **비활성화** (application-local.yml에서 오버라이드)
- 나머지 헤더: 활성화

```bash
# Local 환경
curl -I http://localhost:8080/api/blog/posts | grep -i strict-transport-security
# (출력 없음 - HSTS 비활성화)
```

#### Kubernetes (HTTPS)
- HSTS: **활성화** (max-age=31536000, includeSubDomains)
- 모든 헤더: 활성화

---

## 설정 커스터마이징

### application.yml 수정

```yaml
security:
  headers:
    # 전체 비활성화
    enabled: false

    # 개별 헤더 제어
    frame-options: SAMEORIGIN  # DENY 대신 SAMEORIGIN
    xss-protection: false      # X-XSS-Protection 비활성화

    # CSP 정책 변경
    csp:
      enabled: true
      report-only: true  # 위반 보고만, 차단하지 않음
      policy: "default-src 'self' 'unsafe-inline'"

    # HSTS 설정
    hsts:
      max-age: 63072000  # 2년
      preload: true      # HSTS preload 리스트 등록용

    # 캐시 제어 경로 추가
    cache-control:
      no-cache-paths:
        - "/api/admin/**"
        - "/api/sensitive/**"
```

---

## 로그 확인

```bash
# 보안 헤더 적용 로그
grep "Security headers added" logs/api-gateway.log

# 디버그 레벨 로깅 활성화 (application-local.yml)
logging:
  level:
    com.portal.universe.apigateway.config.SecurityHeadersFilter: DEBUG
```

---

## 예상 결과

### 성공 케이스
```
HTTP/1.1 200 OK
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
Referrer-Policy: strict-origin-when-cross-origin
Permissions-Policy: geolocation=(), microphone=(), camera=()
Content-Security-Policy: default-src 'self'; script-src 'self' 'unsafe-inline'; ...
Content-Type: application/json
```

### 인증 경로 (추가 헤더)
```
HTTP/1.1 200 OK
Cache-Control: no-store, no-cache, must-revalidate
Pragma: no-cache
Expires: 0
X-Content-Type-Options: nosniff
...
```

---

## 문제 해결

### 헤더가 보이지 않는 경우

1. **필터가 등록되었는지 확인**
   ```bash
   # Application 로그에서 확인
   grep "SecurityHeadersFilter" logs/api-gateway.log
   ```

2. **설정이 로드되었는지 확인**
   ```bash
   curl http://localhost:8080/actuator/env | jq '.propertySources[] | select(.name | contains("security"))'
   ```

3. **Order 우선순위 확인**
   - SecurityHeadersFilter: HIGHEST_PRECEDENCE
   - 다른 필터보다 먼저 실행되어야 함

### CSP 위반 오류

브라우저 콘솔에 CSP 위반 메시지가 보이는 경우:
1. `report-only: true`로 변경하여 모니터링
2. 필요한 도메인/리소스를 policy에 추가
3. 안정화 후 `report-only: false`로 전환

---

## 참고 자료

- [OWASP Security Headers](https://owasp.org/www-project-secure-headers/)
- [MDN Content Security Policy](https://developer.mozilla.org/en-US/docs/Web/HTTP/CSP)
- [MDN HTTP Strict Transport Security](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Strict-Transport-Security)
