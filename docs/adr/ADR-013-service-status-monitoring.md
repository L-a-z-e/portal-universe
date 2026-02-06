# ADR-013: 서비스 상태 모니터링 전략

**Status**: Accepted
**Date**: 2026-01-21

## Context
Portal Universe는 여러 마이크로서비스로 구성되어 있으며, 개발자와 관리자가 각 서비스의 UP/DOWN 상태를 실시간으로 확인할 수 있는 모니터링 기능이 필요합니다. Spring Boot Actuator `/actuator/health`를 활용하되, JWT 인증 작업과 충돌을 회피하고 프론트엔드 단독으로 구현 가능해야 합니다.

## Decision
**Client-Side Polling 방식을 채택**하여 Portal Shell에서 주기적으로(10-30초) 각 서비스의 `/actuator/health`를 호출하고 상태를 표시합니다.

## Rationale
- 구현 매우 간단 (프론트엔드만 수정, 2-3시간 내 완료)
- 백엔드 변경 최소화 (Actuator 설정만)
- 추가 인프라 불필요
- 서버 부하 낮음 (10-30초 간격)
- Kubernetes 환경 호환

## Trade-offs
✅ **장점**:
- 빠른 MVP 출시 가능
- 낮은 유지보수 비용
- 디버깅 용이 (브라우저 DevTools)
- Polling 간격 유연하게 조정 가능

⚠️ **단점 및 완화**:
- 실시간성 제한 (최대 10-30초 지연) → (완화: 수동 새로고침 버튼 제공)
- 브라우저 탭마다 중복 요청 → (완화: `document.visibilitychange`로 비활성 탭 Polling 중단)
- 히스토리 기능 없음 → (완화: Phase 2에서 Monitoring Service 추가 예정)

## Implementation
- `useHealthCheck.ts`: Polling 로직 및 Service 목록 관리
- `ServiceStatus.vue`: 상태 표시 UI 컴포넌트
- `application.yml` (각 서비스): Actuator Health endpoint 설정
- `SagaState` 테이블: 향후 히스토리 저장용 (Phase 2)

### 대안 비교
| 대안 | 구현 복잡도 | 실시간성 | 인프라 비용 | 평가 |
|------|-------------|----------|------------|------|
| **Polling** | 매우 낮음 | 10-30초 | 없음 | ✅ 채택 |
| WebSocket | 높음 | 즉시 | 중간 | ❌ 과도한 복잡도 |
| SSE | 중간 | 즉시 | 중간 | 🟡 향후 검토 |
| Prometheus | 중간 | 우수 | 높음 | 🟡 프로덕션 |

## References
- [SCENARIO-005 서비스 상태 모니터링](../scenarios/SCENARIO-005-service-status.md)
- [Spring Boot Actuator Docs](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)

---

📂 상세: [old-docs/central/adr/ADR-013-service-status-monitoring.md](../old-docs/central/adr/ADR-013-service-status-monitoring.md)
