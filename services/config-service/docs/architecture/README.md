# Architecture Documents

Config Service의 아키텍처 문서 목록입니다.

## 📋 문서 목록

| ID | 제목 | 상태 | 작성일 |
|----|------|------|--------|
| arch-config-system-overview | [System Overview](./system-overview.md) | current | 2026-01-18 |
| arch-config-data-flow | [Data Flow](./data-flow.md) | current | 2026-01-18 |

## 📖 문서 설명

### System Overview
Config Service의 전체 시스템 구조, 컴포넌트, 기술 스택, 모니터링 방법을 설명합니다.

**주요 내용**:
- High-Level Architecture 다이어그램
- 컴포넌트 상세 (Config Server, Git Repository, Client Services)
- 설정 우선순위
- 모니터링 및 성능 목표
- 보안 및 확장성

### Data Flow
Config Service의 설정 데이터 흐름을 상세하게 설명합니다.

**주요 내용**:
- 서비스 부팅 시 설정 로드 프로세스
- 런타임 설정 조회
- 설정 변경 및 동적 갱신 (Spring Cloud Bus)
- 설정 파일 구조 및 병합 우선순위
- 암호화된 설정 처리
- 장애 시나리오 및 대응

## 🔗 관련 문서

- [Config Service API 문서](../api/)
- [Config Service 운영 가이드](../runbooks/)
- [Config Repository (GitHub)](https://github.com/L-a-z-e/portal-universe-config-repo)

---

**최종 업데이트**: 2026-01-18
