# ADR-028: Shopping Service SSE 실시간 엔드포인트 인증 방식

**Status**: Proposed
**Date**: 2026-02-07
**Author**: Laze

## Context

`QueueStreamController`의 SSE 구독 엔드포인트 `GET /queue/{eventType}/{eventId}/subscribe/{entryToken}`은 entryToken만으로 접근할 수 있습니다.

현재 `SecurityConfig`에서 `/queue/**`는 `hasAnyRole("USER", "ADMIN")`으로 보호되어 있지만, SSE 연결 특성상 브라우저의 `EventSource` API는 커스텀 헤더(Authorization)를 지원하지 않습니다. 실제 운영에서는 API Gateway의 JWT 검증을 거치므로 인증된 사용자만 접근하지만, 다음 문제가 있습니다:

1. **entryToken 소유권 미검증**: 인증된 사용자 A가 사용자 B의 entryToken으로 대기열 상태를 구독할 수 있습니다. entryToken은 UUID이므로 추측이 어렵지만, 로그 노출이나 URL 공유로 유출될 수 있습니다.
2. **SSE 연결 인증 갱신 불가**: SSE는 long-lived connection이므로 JWT 만료 후에도 연결이 유지됩니다. 5분 타임아웃 내에서 인증이 무효화되어도 스트림이 계속됩니다.

## Decision

**entryToken + userId 소유권 검증을 SSE 구독 시점에 추가합니다.**

SSE 엔드포인트에서 Gateway가 전달한 `X-User-Id` 헤더와 entryToken의 소유자(DB 조회)를 비교하여 불일치 시 403을 반환합니다. JWT 만료에 대해서는 SSE의 5분 타임아웃으로 수용합니다.

## Alternatives

| 대안 | 장점 | 단점 |
|------|------|------|
| ① entryToken + userId 소유권 검증 (채택) | 구현 단순, 기존 인프라 활용 | JWT 만료 후 최대 5분 연결 유지 |
| ② JWT query parameter | 표준적 SSE 인증 방식 | 토큰이 URL/로그에 노출, 갱신 불가 |
| ③ 일회성 ticket 발급 | 보안 강력 (단기 유효, 1회용) | ticket 발급 엔드포인트 추가 필요, 복잡도 증가 |

## Rationale

- **위험 대비 복잡도**: 대기열 상태 조회는 민감한 정보(결제, 개인정보)가 아니라 순번/예상 시간 정도입니다. 공격 표면이 제한적이므로 소유권 검증만으로 충분합니다.
- **EventSource 제약**: 브라우저 `EventSource`는 헤더 커스터마이즈를 지원하지 않으므로, JWT를 query param으로 전달해야 합니다. 이는 서버 로그와 브라우저 히스토리에 토큰이 남는 보안 리스크를 수반합니다.
- **기존 아키텍처 활용**: API Gateway가 `X-User-Id`를 이미 전달하므로, 컨트롤러에서 한 줄 비교만 추가하면 됩니다.
- **타임아웃 활용**: SSE emitter의 5분 타임아웃이 사실상 세션 만료 역할을 합니다. 대기열 상태 조회는 수분 내 종료되는 short-lived 용도입니다.

## Trade-offs

✅ **장점**:
- 최소 코드 변경으로 소유권 검증 추가
- API Gateway의 기존 인증 인프라 활용
- URL에 JWT 노출 없음

⚠️ **단점 및 완화**:
- JWT 만료 후 최대 5분간 연결 유지 → (완화: 대기열 상태는 비민감 정보, 5분 타임아웃으로 자동 종료)
- DB 조회 1회 추가 (entryToken → userId 확인) → (완화: SSE 연결 시 1회만, 이후 상태 조회는 기존 로직)

## Implementation

### QueueStreamController 수정
```java
@GetMapping("/{eventType}/{eventId}/subscribe/{entryToken}")
public SseEmitter subscribe(
        @PathVariable String eventType,
        @PathVariable Long eventId,
        @PathVariable String entryToken,
        @RequestHeader("X-User-Id") String userId  // 추가
) {
    // 소유권 검증
    queueService.validateTokenOwnership(entryToken, userId);
    // ... 기존 로직
}
```

### QueueService 수정
- `validateTokenOwnership(entryToken, userId)`: `queueEntryRepository.findByEntryToken(entryToken)`으로 조회 후 `entry.getUserId().equals(userId)` 검증

### 코드 참조
- `QueueStreamController.java:39-89` (현재 SSE 구독)
- `SecurityConfig.java:110` (`/queue/**` 보안 규칙)

## References

- [ADR-020: Redis Sorted Set 대기열 시스템](./ADR-020-shopping-queue-system.md)
- [OWASP: Server-Sent Events Security](https://cheatsheetseries.owasp.org/cheatsheets/HTML5_Security_Cheat_Sheet.html)

---

## 변경 이력

| 날짜 | 변경 내용 | 작성자 |
|------|----------|--------|
| 2026-02-07 | 초안 작성 | Laze |
