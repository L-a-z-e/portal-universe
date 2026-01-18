# Common Library

Portal Universe 모든 마이크로서비스가 공유하는 공통 라이브러리입니다.

## 개요

코드 중복을 줄이고 에러 처리, API 응답 형식의 일관성을 보장합니다.

## 주요 기능

### 1. 에러 처리 (Exception Handling)

- **ErrorCode 인터페이스**: 서비스별 에러 코드 표준
- **CustomBusinessException**: 비즈니스 예외
- **GlobalExceptionHandler**: 전역 예외 핸들러

### 2. API 응답 (ApiResponse)

```java
// 성공
ApiResponse.success(data)

// 실패
throw new CustomBusinessException(BlogErrorCode.DUPLICATE_TITLE);
```

### 3. JWT 보안

- `JwtAuthenticationConverterAdapter`: Servlet 환경
- `ReactiveJwtAuthenticationConverterAdapter`: WebFlux 환경

### 4. 이벤트 DTO

- `UserSignedUpEvent`: 회원가입 이벤트
- `OrderCreatedEvent`: 주문 생성 이벤트

## 사용 방법

### 의존성 추가
```gradle
implementation project(':common-library')
```

### ErrorCode 구현
```java
public enum BlogErrorCode implements ErrorCode {
    DUPLICATE_TITLE(HttpStatus.CONFLICT, "B001", "Duplicate Title");
    // ...
}
```

### Controller에서 사용
```java
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<PostDto>> getPost(@PathVariable String id) {
    return ResponseEntity.ok(ApiResponse.success(postService.getPost(id)));
}
```

## 에러 코드 규약

| 서비스 | Prefix |
|--------|--------|
| Common | C |
| Auth | A |
| Blog | B |
| Shopping | S |

## 관련 문서

- [ARCHITECTURE.md](./ARCHITECTURE.md) - 설계 결정
- [API.md](./API.md) - 클래스 명세
