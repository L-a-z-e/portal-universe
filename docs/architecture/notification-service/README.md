# Notification Service - Architecture 문서

이 디렉토리는 Notification Service의 아키텍처 관련 문서를 포함합니다.

## 📄 문서 목록

| 문서 ID | 제목 | 상태 | 작성일 | 설명 |
|---------|------|------|--------|------|
| notification-service-architecture-system-overview | [Notification Service 시스템 아키텍처](./system-overview.md) | current | 2026-02-06 | 시스템 전체 구조, 핵심 컴포넌트, 기술 스택, 보안 |
| notification-service-data-flow | [Notification Service 데이터 플로우](./data-flow.md) | current | 2026-02-06 | Kafka 이벤트 소비, 실시간 Push, REST API, 에러 처리 플로우 |

## 📋 문서 추가 시

새 Architecture 문서를 추가할 때는 다음 규칙을 따르세요:

1. **파일명**: `[kebab-case].md` (예: `data-flow.md`, `email-template-system.md`)
2. **필수 메타데이터**: YAML frontmatter 포함
3. **이 README 업데이트**: 문서 목록 테이블에 항목 추가

## 🔗 관련 문서

- [API 문서](../api/README.md)
- [가이드](../guides/README.md)
- [Runbook](../runbooks/README.md)
- [Architecture 템플릿](../../templates/architecture-template.md)
