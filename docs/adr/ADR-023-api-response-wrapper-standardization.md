# ADR-023: API Response Wrapper 표준화

**Status**: Deprecated
**Date**: 2026-02-07
**Author**: Laze
**Superseded by**: [ADR-031: Unified API Response Strategy](./ADR-031-unified-api-response-strategy.md)

## Context

Portal Universe는 마이크로서비스 아키텍처 기반 플랫폼으로, 6개의 백엔드 서비스가 독립적으로 개발됩니다. 클라이언트(프론트엔드)는 일관된 응답 형식을 기대하며, common-library의 `ApiResponse<T>` wrapper를 사용하도록 Spring Rules에 명시되어 있습니다.

그러나 blog-service의 FileController에서 파일 업로드/삭제 endpoint가 `ResponseEntity<FileUploadResponse>` 및 `ResponseEntity<Void>`를 직접 반환하는 패턴이 발견되었습니다. 나머지 5개 Controller(Post, Comment, Like, Series, Tag)는 모두 `ApiResponse`를 준수하지만, 파일 처리 로직만 예외로 취급되고 있습니다.

이로 인해 프론트엔드 API 클라이언트에서 응답 파싱 로직에 분기 처리가 필요하며, 신규 서비스 개발 시 "파일 관련 API는 다른 형식을 써도 되나?"라는 혼란을 야기할 수 있습니다.

## Decision

**모든 REST API endpoint는 `ApiResponse<T>`로 응답합니다 (HTTP 200 기준).**

- 성공 응답: `ApiResponse.success(data)`
- 삭제 성공: `ApiResponse.success(null)` (HTTP 204 대신 200)
- 에러 응답: `ApiResponse.error(code, message)` (GlobalExceptionHandler 처리)

**예외**: 파일 다운로드(바이너리 스트림)는 `ResponseEntity<Resource>` 허용

## Rationale

- **일관성**: GlobalExceptionHandler가 에러를 `ApiResponse` 형식으로 반환하는데, 정상 응답만 다른 형식을 사용하면 클라이언트 로직이 복잡해집니다.
- **프론트엔드 단순화**: 모든 API 호출 결과를 `response.success`, `response.data`, `response.error`로 통일 처리 가능합니다.
- **자동화 테스트**: E2E/통합 테스트에서 응답 검증 로직을 단일화할 수 있습니다.
- **신규 개발자 가이드**: Spring Rules에 명시된 규칙을 모든 Controller가 예외 없이 따르면, 학습 비용이 줄어듭니다.

## Trade-offs

✅ **장점**:
- 프론트엔드 API 클라이언트의 응답 파싱 로직 단일화
- GlobalExceptionHandler와 정상 응답의 형식 일치
- 자동화 테스트 assertion 단순화
- 신규 서비스 개발 시 혼란 방지

⚠️ **단점 및 완화**:
- HTTP 의미론 약간 위반 (삭제 성공 시 204 No Content 대신 200 OK 사용)
  → (완화: `success: true` 필드로 의미 보존, RESTful 원칙보다 클라이언트 일관성 우선)
- 파일 업로드 응답이 한 단계 감싸짐 (`{success: true, data: {url: "..."}}`)
  → (완화: 프론트엔드는 이미 다른 API에서 이 구조에 익숙함, 추가 복잡도 없음)

## Implementation

### 수정 대상
- `services/blog-service/src/main/java/.../file/controller/FileController.java`
  - `uploadFile()`: `ApiResponse<FileUploadResponse>` 반환으로 변경
  - `deleteFile()`: `ApiResponse<Void>` 반환으로 변경 (HTTP 200)

### 향후 강제 방안
- 정적 분석: ArchUnit 또는 Checkstyle로 `ResponseEntity<T>` 직접 반환 금지 (바이너리 다운로드 제외)
- CI lint rule 추가 가능성

## References

- [ADR-002: API Endpoint Design](./ADR-002-api-endpoint-design.md)
- [Spring Rules](../../.claude/rules/spring.md)
- [common-library ApiResponse.java](../../common/common-library/src/main/java/com/portal/universe/common/response/ApiResponse.java)

---

## 변경 이력

| 날짜 | 변경 내용 | 작성자 |
|------|----------|--------|
| 2026-02-07 | 초안 작성 | Laze |
