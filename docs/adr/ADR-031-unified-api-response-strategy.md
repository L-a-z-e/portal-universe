# ADR-031: Unified API Response Strategy

**Status**: Accepted
**Date**: 2026-02-08
**Author**: Laze
**Supersedes**: ADR-023 (API Response Wrapper 표준화)

## Context

Portal Universe는 Java/Spring, NestJS, Python(FastAPI) 세 가지 백엔드 스택과 Vue 3, React 18 두 가지 프론트엔드 프레임워크로 구성됩니다. ADR-023에서 `ApiResponse<T>` wrapper를 표준화했으나, 페이지네이션 응답과 SSE 이벤트 형식은 여전히 서비스마다 다른 구조를 사용하고 있었습니다.

- Spring `Page<T>`: `content`, `number`(0-based), `last`, `pageable` 등 Spring 내부 구조 노출
- NestJS `PaginatedResult<T>`: `items`, `total`, `page`(1-based) - Spring과 필드명/인덱싱 불일치
- SSE: 서비스마다 raw 데이터, 자체 envelope, 타입 미지정 등 혼재

## Decision

**모든 서비스의 API 응답을 3가지 표준 구조로 통일합니다.**

1. **단건/리스트**: `ApiResponse<T>` (기존 ADR-023)
2. **페이지네이션**: `PageResponse<T>` - 5개 필드, 1-based 페이지
3. **SSE 이벤트**: `SseEnvelope<T>` - `{type, data, timestamp}` 구조

## Alternatives

| 대안 | 장점 | 단점 |
|------|------|------|
| ① Spring `Page<T>` 직접 노출 유지 | 변경 없음 | 30+ 불필요 필드, FE에서 0→1 변환 필요 |
| ② 각 서비스 자체 pagination DTO | 자유도 | 서비스 간 불일치, FE 파싱 분기 |
| ③ **통합 `PageResponse<T>`** (선택) | 일관성, 최소 필드 | 마이그레이션 비용 |

## Rationale

- **프론트엔드 단순화**: `response.data.items`, `response.data.page`로 모든 서비스의 페이지네이션을 동일하게 처리
- **1-based 페이지**: 사용자/프론트엔드 직관에 부합 (URL `?page=1`이 첫 페이지)
- **Spring 내부 숨김**: `Page<T>`의 `pageable`, `sort`, `numberOfElements` 등 30+ 불필요 필드 제거
- **SSE 표준화**: heartbeat 포함 모든 이벤트가 동일한 envelope로 파싱 가능
- **타입 공유**: `@portal/design-types`로 프론트엔드 타입 일원화

## Trade-offs

✅ **장점**:
- 프론트엔드 페이지네이션 로직 통일 (content/items, number/page 분기 제거)
- 새 서비스 추가 시 응답 형식 고민 없음
- SSE 클라이언트 파싱 로직 단일화

⚠️ **단점 및 완화**:
- 전체 서비스 마이그레이션 필요 → (완화: common-library에 `PageResponse.from(Page)` 팩토리 제공)
- Spring `one-indexed-parameters` 설정 필요 → (완화: `application.yml`에 1줄 추가)
- 프론트엔드 전체 수정 필요 → (완화: 일괄 마이그레이션으로 한 번에 처리)

## Implementation

### Backend

- `services/common-library/.../response/PageResponse.java` - Spring `Page<T>` → 5필드 변환
- `services/common-library/.../response/SseEnvelope.java` - SSE 이벤트 표준 envelope
- `services/*/src/main/resources/application.yml` - `one-indexed-parameters: true`
- `services/prism-service/src/common/dto/pagination.dto.ts` - `total` → `totalElements`

### Frontend

- `frontend/design-types/src/api.ts` - `PageResponse<T>`, `SseEvent<T>` 타입 정의
- 각 프론트엔드 앱의 페이지네이션 컴포넌트/스토어 (0-based → 1-based)
- SSE 훅 (`useSse.ts`, `useInventoryStream.ts`, `useQueue.ts`, `useChat.ts`)

### 표준 구조

```typescript
// PageResponse<T>
{
  items: T[];
  page: number;          // 1-based
  size: number;
  totalElements: number;
  totalPages: number;
}

// SseEnvelope<T>
{
  type: string;
  data: T;
  timestamp: string;
}
```

## References

- [ADR-023: API Response Wrapper 표준화](./ADR-023-api-response-wrapper-standardization.md) (Deprecated → 본 ADR로 통합)
- [ADR-024: Auth Parameter Standardization](./ADR-024-auth-parameter-standardization.md)
- Spring `Page<T>` → `PageResponse<T>` 변환: `PageResponse.from(page)`

---

## 변경 이력

| 날짜 | 변경 내용 | 작성자 |
|------|----------|--------|
| 2026-02-08 | 초안 작성 및 구현 완료 | Laze |
