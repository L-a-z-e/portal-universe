---
id: API-003
title: Common Library - Security Validation API 명세
type: api
status: current
version: v1
created: 2026-02-06
updated: 2026-02-06
author: Portal Universe Team
tags: [api, common-library, security, xss, sql-injection, validation]
related:
  - API-001
  - API-002
  - API-004
---

# Common Library - Security Validation API 명세

> XSS, SQL Injection 방어를 위한 Bean Validation 어노테이션과 유틸리티를 제공합니다.

---

## 목차

- [XSS 방어](#xss-방어)
  - [@NoXss](#noxss)
  - [@SafeHtml](#safehtml)
  - [XssUtils](#xssutils)
  - [XssFilter](#xssfilter)
- [SQL Injection 방어](#sql-injection-방어)
  - [@NoSqlInjection](#nosqlinjection)
  - [SqlInjectionUtils](#sqlinjectionutils)
- [유틸리티](#유틸리티)
  - [IpUtils](#iputils)

---

## XSS 방어

### @NoXss

XSS(Cross-Site Scripting) 공격 패턴이 포함되지 않았는지 검증하는 Bean Validation 어노테이션입니다. 일반 텍스트 입력 필드에 사용하며, HTML 태그 및 스크립트가 허용되지 않습니다.

**위치:** `com.portal.universe.commonlibrary.security.xss.NoXss`

**검증기:** `NoXssValidator`

```java
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NoXssValidator.class)
@Documented
public @interface NoXss {
    String message() default "HTML/Script 태그는 허용되지 않습니다";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

#### 속성

| 속성 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `message` | String | "HTML/Script 태그는 허용되지 않습니다" | 검증 실패 메시지 |
| `groups` | Class<?>[] | {} | 검증 그룹 |
| `payload` | Class<? extends Payload>[] | {} | 페이로드 |

#### 검증 규칙

- `null`: 통과 (`@NotNull`/`@NotBlank`에서 처리)
- 빈 문자열: 통과
- `XssUtils.isSafe(value)` 호출 → XSS 패턴이 없으면 통과

#### 사용 예시

```java
public record UserRequest(
    @NoXss
    @NotBlank(message = "이름은 필수입니다")
    String username,

    @NoXss
    String comment,

    @NoXss
    @Size(max = 200)
    String bio
) {}
```

---

### @SafeHtml

허용된 HTML 태그만 포함하는지 검증하는 Bean Validation 어노테이션입니다. 블로그 게시글처럼 제한적인 HTML 마크업이 필요한 경우 사용합니다.

**위치:** `com.portal.universe.commonlibrary.security.xss.SafeHtml`

**검증기:** `SafeHtmlValidator`

```java
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SafeHtmlValidator.class)
@Documented
public @interface SafeHtml {
    String message() default "허용되지 않은 HTML 태그가 포함되어 있습니다";
    String[] allowedTags() default {"p", "br", "b", "i", "u", "strong", "em", "ul", "ol", "li"};
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

#### 속성

| 속성 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `message` | String | "허용되지 않은 HTML 태그가 포함되어 있습니다" | 검증 실패 메시지 |
| `allowedTags` | String[] | `{"p", "br", "b", "i", "u", "strong", "em", "ul", "ol", "li"}` | 허용할 HTML 태그 |
| `groups` | Class<?>[] | {} | 검증 그룹 |
| `payload` | Class<? extends Payload>[] | {} | 페이로드 |

#### 검증 규칙

1. `null` / 빈 문자열: 통과
2. XSS 위험 패턴(script, iframe, event handler 등) 감지 시: **실패**
3. `allowedTags`에 없는 HTML 태그 감지 시: **실패**

#### 사용 예시

```java
public record PostCreateRequest(
    @NotBlank
    String title,

    @SafeHtml(allowedTags = {"p", "br", "b", "i", "a", "img", "h1", "h2", "h3"})
    String content
) {}

// 기본 태그만 허용
public record CommentRequest(
    @SafeHtml  // 기본 allowedTags: p, br, b, i, u, strong, em, ul, ol, li
    String content
) {}
```

#### @NoXss vs @SafeHtml 비교

| 항목 | @NoXss | @SafeHtml |
|------|--------|-----------|
| 용도 | 순수 텍스트 필드 | HTML 허용 필드 |
| HTML 허용 | 전혀 불가 | allowedTags만 가능 |
| XSS 패턴 검사 | O | O |
| 사용 예 | 이름, 검색어, 댓글 | 블로그 본문, 에디터 콘텐츠 |

---

### XssUtils

XSS 방어를 위한 유틸리티 클래스입니다.

**위치:** `com.portal.universe.commonlibrary.security.xss.XssUtils`

#### 감지되는 XSS 패턴

| 패턴 | 설명 |
|------|------|
| `<script>...</script>` | Script 태그 |
| `on\w+=` | 이벤트 핸들러 (onclick, onerror 등) |
| `javascript:` | JavaScript 프로토콜 |
| `vbscript:` | VBScript 프로토콜 |
| `<iframe>` | Inline Frame |
| `<embed>`, `<object>` | 플러그인 삽입 |
| `<style>...</style>` | Style 태그 |
| `expression(` | CSS expression |
| `<meta>`, `<link>`, `<base>` | 위험 메타 태그 |

#### 메서드

##### escape(String input)

HTML 특수 문자를 이스케이프 처리합니다.

```java
public static String escape(String input)
```

| 원본 | 변환 |
|------|------|
| `&` | `&amp;` |
| `<` | `&lt;` |
| `>` | `&gt;` |
| `"` | `&quot;` |
| `'` | `&#x27;` |
| `/` | `&#x2F;` |

**사용 예시:**

```java
String safe = XssUtils.escape("<script>alert('xss')</script>");
// → "&lt;script&gt;alert(&#x27;xss&#x27;)&lt;&#x2F;script&gt;"
```

##### stripTags(String input)

모든 HTML 태그를 제거하고 순수 텍스트만 반환합니다.

```java
public static String stripTags(String input)
```

**사용 예시:**

```java
String text = XssUtils.stripTags("<p>Hello <b>World</b></p>");
// → "Hello World"
```

##### sanitize(String input, String... allowedTags)

허용된 태그만 남기고 나머지를 제거합니다. XSS 위험 패턴도 제거됩니다.

```java
public static String sanitize(String input, String... allowedTags)
```

**사용 예시:**

```java
String safe = XssUtils.sanitize(
    "<p>Hello</p><script>alert('xss')</script><b>World</b>",
    "p", "b"
);
// → "<p>Hello</p><b>World</b>"
```

##### containsXssPattern(String input)

XSS 공격 패턴이 포함되어 있는지 검사합니다.

```java
public static boolean containsXssPattern(String input)
```

##### isSafe(String input)

입력값이 안전한지 검사합니다 (`!containsXssPattern(input)`).

```java
public static boolean isSafe(String input)
```

---

### XssFilter

모든 요청 파라미터에 자동으로 XSS 필터링을 적용하는 Servlet Filter입니다.

**위치:** `com.portal.universe.commonlibrary.security.filter.XssFilter`

**상태:** **비활성화** (기본값, `@NoXss` 어노테이션 사용 권장)

**구현:** `Filter`

#### 활성화 방법

`XssFilter.java`에서 `@Component` 어노테이션 주석을 해제합니다:

```java
@Component  // 주석 해제로 활성화
@Order(Ordered.HIGHEST_PRECEDENCE)
public class XssFilter implements Filter { ... }
```

#### 동작 방식

`HttpServletRequestWrapper`를 사용하여 다음을 자동 필터링합니다:
- `getParameter()` - 요청 파라미터
- `getParameterValues()` - 다중값 파라미터
- `getParameterMap()` - 파라미터 맵
- `getHeader()` - 요청 헤더

XSS 패턴이 감지되면 `XssUtils.stripTags()`로 태그를 제거합니다.

> `@NoXss` 어노테이션이 더 세밀한 제어를 제공하므로, 어노테이션 방식을 권장합니다.

---

## SQL Injection 방어

### @NoSqlInjection

SQL Injection 공격 패턴이 포함되지 않았는지 검증하는 Bean Validation 어노테이션입니다. 검색어, 정렬 필드 등 동적 쿼리에 사용되는 파라미터에 적용합니다.

**위치:** `com.portal.universe.commonlibrary.security.sql.NoSqlInjection`

**검증기:** `NoSqlInjectionValidator`

```java
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NoSqlInjectionValidator.class)
@Documented
public @interface NoSqlInjection {
    String message() default "SQL Injection 위험 패턴이 감지되었습니다";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

#### 사용 예시

```java
public record SearchRequest(
    @NoSqlInjection
    String keyword,

    @NoSqlInjection
    String sortField,

    @NoSqlInjection
    String sortDirection
) {}
```

---

### SqlInjectionUtils

SQL Injection 방어를 위한 유틸리티 클래스입니다. JPA/MyBatis가 기본적으로 Prepared Statement를 통해 방어하지만, 동적 쿼리나 정렬 필드 같은 특수 케이스에 추가 검증 레이어를 제공합니다.

**위치:** `com.portal.universe.commonlibrary.security.sql.SqlInjectionUtils`

#### 감지되는 SQL Injection 패턴

| 패턴 | 설명 |
|------|------|
| `--` | SQL 주석 |
| `/* */` | 블록 주석 |
| `UNION ... SELECT` | Union 기반 공격 |
| `; DROP/DELETE/...` | 다중 쿼리 실행 |
| `EXEC/EXECUTE(` | 프로시저 실행 |
| `xp_cmdshell` | OS 명령 실행 |
| `OR 1=1`, `AND 1=1` | 항상 참인 조건 |
| `CHAR(`, `CONCAT(` | 문자열 연결 우회 |
| `SLEEP(`, `BENCHMARK(`, `WAITFOR(` | 시간 기반 Blind SQLi |
| `information_schema` | 메타데이터 접근 |
| `LOAD_FILE(`, `INTO OUTFILE` | 파일 읽기/쓰기 |

#### 메서드

##### containsSqlInjection(String input)

SQL Injection 패턴이 포함되어 있는지 검사합니다. 입력값을 정규화(공백 통합, trim) 후 패턴을 검사합니다.

```java
public static boolean containsSqlInjection(String input)
```

##### isSafe(String input)

입력값이 안전한지 검사합니다 (`!containsSqlInjection(input)`).

```java
public static boolean isSafe(String input)
```

##### normalize(String input)

입력값을 정규화합니다 (여러 공백을 하나로, 앞뒤 공백 제거).

```java
public static String normalize(String input)
```

##### isSafeSortField(String fieldName)

정렬 필드명이 안전한 형식인지 검증합니다. ORDER BY 절에 동적으로 필드명을 넣을 때 사용합니다. 알파벳, 숫자, 언더스코어, 점만 허용합니다.

```java
public static boolean isSafeSortField(String fieldName)
```

**사용 예시:**

```java
if (!SqlInjectionUtils.isSafeSortField(request.getSortField())) {
    throw new CustomBusinessException(CommonErrorCode.SQL_INJECTION_DETECTED);
}
```

##### isSafeSortDirection(String direction)

정렬 방향이 안전한지 검증합니다. `ASC` 또는 `DESC`만 허용합니다.

```java
public static boolean isSafeSortDirection(String direction)
```

---

## 유틸리티

### IpUtils

프록시나 로드 밸런서 환경에서 실제 클라이언트 IP를 추출하는 유틸리티입니다.

**위치:** `com.portal.universe.commonlibrary.util.IpUtils`

#### getClientIp(HttpServletRequest request)

```java
public static String getClientIp(HttpServletRequest request)
```

다음 헤더를 순서대로 확인하여 클라이언트 IP를 추출합니다:

| 순서 | 헤더 | 설명 |
|------|------|------|
| 1 | `X-Forwarded-For` | 표준 프록시 헤더 |
| 2 | `Proxy-Client-IP` | Apache 프록시 |
| 3 | `WL-Proxy-Client-IP` | WebLogic 프록시 |
| 4 | `HTTP_CLIENT_IP` | HTTP 클라이언트 IP |
| 5 | `HTTP_X_FORWARDED_FOR` | HTTP 포워딩 |
| 6 | `getRemoteAddr()` | 직접 연결 IP |

`X-Forwarded-For`에 여러 IP가 포함된 경우(쉼표 구분) 첫 번째 IP(원본 클라이언트)를 반환합니다.

**사용 예시:**

```java
@PostMapping("/login")
public ResponseEntity<ApiResponse<TokenResponse>> login(
        @RequestBody LoginRequest request,
        HttpServletRequest httpRequest) {
    String clientIp = IpUtils.getClientIp(httpRequest);
    TokenResponse token = authService.login(request, clientIp);
    return ResponseEntity.ok(ApiResponse.success(token));
}
```

---

## 관련 문서

- [API-001: Core](./API-001-common-library.md) - 응답 포맷, 예외 처리
- [API-002: 인증 시스템](./API-002-security-auth.md) - JWT, Gateway, 사용자 컨텍스트
- [API-004: 감사 로그](./API-004-security-audit.md) - 보안 이벤트 추적

---

**최종 수정:** 2026-02-06
**API 버전:** v1
**문서 버전:** 1.0
