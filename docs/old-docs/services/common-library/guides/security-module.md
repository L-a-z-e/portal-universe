---
id: common-library-security-module
title: Security Module 가이드
type: guide
status: current
created: 2026-01-23
updated: 2026-01-30
author: Laze
tags: [common-library, security, xss, sql-injection]
---

# Security Module

common-library의 보안 모듈은 XSS(Cross-Site Scripting) 및 SQL Injection 공격을 방어하기 위한 유틸리티와 Bean Validation 어노테이션을 제공합니다.

## 목차

- [XSS 방어](#xss-방어)
  - [XssUtils](#xssutils)
  - [@NoXss 어노테이션](#noxss-어노테이션)
  - [@SafeHtml 어노테이션](#safehtml-어노테이션)
- [SQL Injection 방어](#sql-injection-방어)
  - [SqlInjectionUtils](#sqlinjectionutils)
  - [@NoSqlInjection 어노테이션](#nosqlinjection-어노테이션)
- [에러 코드](#에러-코드)
- [사용 예시](#사용-예시)

---

## XSS 방어

### XssUtils

HTML 이스케이프, 태그 제거, 허용된 태그 필터링 기능을 제공하는 유틸리티 클래스입니다.

```java
// HTML 특수 문자 이스케이프
String escaped = XssUtils.escape("<script>alert('XSS')</script>");
// 결과: &lt;script&gt;alert(&#x27;XSS&#x27;)&lt;&#x2F;script&gt;

// 모든 HTML 태그 제거
String stripped = XssUtils.stripTags("<p>Hello <b>World</b></p>");
// 결과: Hello World

// 허용된 태그만 유지
String sanitized = XssUtils.sanitize(
    "<p>Hello</p><script>alert('XSS')</script>",
    "p", "b", "i"
);
// 결과: <p>Hello</p> (script 태그는 제거됨)

// XSS 패턴 감지
boolean isSafe = XssUtils.isSafe("<script>alert(1)</script>");
// 결과: false
```

### @NoXss 어노테이션

일반 텍스트 입력 필드에 사용하여 HTML/Script 태그를 차단합니다.

```java
public record UserRequest(
    @NotBlank
    @NoXss
    String username,

    @NoXss
    String comment
) {}
```

**감지되는 패턴:**
- `<script>` 태그
- 이벤트 핸들러 (`onclick`, `onerror` 등)
- JavaScript 프로토콜 (`javascript:`, `vbscript:`)
- `<iframe>`, `<embed>`, `<object>` 태그
- `<style>` 태그 및 CSS expression
- `<meta>`, `<link>`, `<base>` 태그

### @SafeHtml 어노테이션

블로그 게시글처럼 제한적인 HTML 마크업이 필요한 경우 사용합니다.

```java
public record PostCreateRequest(
    @NotBlank
    @NoXss
    String title,

    @SafeHtml(allowedTags = {"p", "br", "b", "i", "a", "img"})
    String content
) {}
```

**기본 허용 태그:**
`p`, `br`, `b`, `i`, `u`, `strong`, `em`, `ul`, `ol`, `li`

**동작 방식:**
1. XSS 위험 패턴 먼저 검사 (script, iframe 등)
2. 허용되지 않은 태그 검사
3. 검증 실패 시 구체적인 메시지 제공

---

## SQL Injection 방어

### SqlInjectionUtils

SQL Injection 패턴 탐지 및 동적 쿼리 파라미터 검증 기능을 제공합니다.

```java
// SQL Injection 패턴 감지
boolean isSafe = SqlInjectionUtils.isSafe("admin' OR '1'='1");
// 결과: false

// 정렬 필드명 검증 (알파벳, 숫자, 언더스코어, 점만 허용)
boolean isValidField = SqlInjectionUtils.isSafeSortField("user_name");
// 결과: true

boolean isInvalidField = SqlInjectionUtils.isSafeSortField("name; DROP TABLE");
// 결과: false

// 정렬 방향 검증 (ASC 또는 DESC만 허용)
boolean isValidDirection = SqlInjectionUtils.isSafeSortDirection("DESC");
// 결과: true

// 입력값 정규화 (공백 정리)
String normalized = SqlInjectionUtils.normalize("  hello    world  ");
// 결과: "hello world"
```

**감지되는 패턴:**
- SQL 주석 (`--`, `/*`, `*/`)
- UNION SELECT 공격
- OR/AND 항상 참인 조건 (`OR 1=1`, `AND '1'='1'`)
- 세미콜론을 통한 다중 쿼리
- 위험한 SQL 함수 (`EXEC`, `SLEEP`, `BENCHMARK`)
- Information Schema 접근
- `LOAD_FILE`, `INTO OUTFILE`

### @NoSqlInjection 어노테이션

검색어, 정렬 필드 등 동적 쿼리에 사용되는 파라미터에 적용합니다.

```java
public record SearchRequest(
    @NoSqlInjection
    String keyword,

    @Pattern(regexp = "^[a-zA-Z0-9_.]+$")
    @NoSqlInjection
    String sortBy
) {}
```

**참고:** JPA/MyBatis가 Prepared Statement를 통해 기본적으로 방어하지만, ORDER BY나 동적 테이블명 같은 특수 케이스에 추가 검증 레이어를 제공합니다.

---

## 에러 코드

`CommonErrorCode`에 다음 보안 관련 에러 코드가 추가되었습니다:

| 코드 | HttpStatus | 메시지 |
|------|-----------|--------|
| `C006` | 400 BAD_REQUEST | Potential XSS attack detected |
| `C007` | 400 BAD_REQUEST | Potential SQL Injection detected |
| `C008` | 400 BAD_REQUEST | Invalid HTML content |

검증 실패 시 자동으로 `MethodArgumentNotValidException`이 발생하며, `GlobalExceptionHandler`에서 처리됩니다.

---

## 사용 예시

### 1. 블로그 게시글 생성

```java
public record PostCreateRequest(
    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 200)
    @NoXss
    String title,

    @NotBlank(message = "본문은 필수입니다")
    @SafeHtml(allowedTags = {"p", "br", "b", "i", "a", "img", "h1", "h2", "h3"})
    String content,

    @NoXss
    @NoSqlInjection
    String category
) {}

@PostMapping
public ResponseEntity<ApiResponse<PostResponse>> create(
        @Valid @RequestBody PostCreateRequest request) {
    return ResponseEntity.ok(ApiResponse.success(postService.create(request)));
}
```

### 2. 검색 API

```java
public record SearchRequest(
    @NoXss
    @NoSqlInjection
    String keyword,

    @Pattern(regexp = "^[a-zA-Z0-9_.]+$")
    @NoSqlInjection
    String sortBy,

    @Pattern(regexp = "^(ASC|DESC)$")
    String sortDirection
) {}

@GetMapping("/search")
public ResponseEntity<ApiResponse<Page<PostResponse>>> search(
        @Valid SearchRequest request) {
    return ResponseEntity.ok(ApiResponse.success(postService.search(request)));
}
```

### 3. 동적 쿼리 (QueryDSL)

```java
public Page<Post> search(SearchRequest request) {
    JPAQuery<Post> query = queryFactory.selectFrom(post);

    // 검색 키워드 (이미 @NoSqlInjection으로 검증됨)
    if (request.keyword() != null) {
        query.where(post.title.contains(request.keyword()));
    }

    // 정렬 필드 추가 검증
    if (SqlInjectionUtils.isSafeSortField(request.sortBy()) &&
        SqlInjectionUtils.isSafeSortDirection(request.sortDirection())) {
        // 동적 정렬 적용
        OrderSpecifier<?> orderSpecifier = createOrderSpecifier(
            request.sortBy(),
            request.sortDirection()
        );
        query.orderBy(orderSpecifier);
    }

    return query.fetch();
}
```

### 4. 수동 검증

```java
@Service
public class PostService {

    public void processUserInput(String input) {
        // XSS 위험 패턴이 있으면 예외 발생
        if (!XssUtils.isSafe(input)) {
            throw new CustomBusinessException(CommonErrorCode.XSS_DETECTED);
        }

        // SQL Injection 위험 패턴이 있으면 예외 발생
        if (!SqlInjectionUtils.isSafe(input)) {
            throw new CustomBusinessException(CommonErrorCode.SQL_INJECTION_DETECTED);
        }

        // 안전한 입력 처리
        processInput(input);
    }
}
```

---

## XssFilter (선택)

모든 요청 파라미터에 자동으로 XSS 필터링을 적용하는 전역 필터입니다.

**주의:** 필터는 선택적으로 사용하며, **@NoXss 어노테이션을 사용하는 것을 권장합니다.**

### 활성화 방법

`XssFilter.java`의 `@Component` 주석을 제거하면 활성화됩니다:

```java
@Component  // 주석 제거
@Order(Ordered.HIGHEST_PRECEDENCE)
public class XssFilter implements Filter {
    // ...
}
```

### 동작 방식

1. HttpServletRequestWrapper를 사용하여 요청 감싸기
2. getParameter(), getParameterValues() 호출 시 자동 필터링
3. XSS 위험 패턴이 있으면 태그 제거

---

## 테스트

모든 유틸리티와 어노테이션은 단위 테스트 및 통합 테스트가 작성되어 있습니다:

```bash
# XSS 테스트 실행
./gradlew :services:common-library:test --tests "*Xss*"

# SQL Injection 테스트 실행
./gradlew :services:common-library:test --tests "*SqlInjection*"

# 전체 보안 테스트 실행
./gradlew :services:common-library:test --tests "*Xss*" --tests "*SqlInjection*"
```

---

## 참고 자료

- [OWASP XSS Prevention Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Cross_Site_Scripting_Prevention_Cheat_Sheet.html)
- [OWASP SQL Injection Prevention Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/SQL_Injection_Prevention_Cheat_Sheet.html)
- [Spring Bean Validation](https://docs.spring.io/spring-framework/reference/core/validation/beanvalidation.html)
