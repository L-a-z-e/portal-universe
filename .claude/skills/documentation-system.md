# documentation-system - 프로젝트 문서화 시스템

`docs/` 디렉토리 문서 작업 시 참조되는 규칙입니다.

## 문서 구조

```
docs/
├── architecture/        # 시스템 및 서비스 아키텍처
├── api/                 # REST API 명세서
├── adr/                 # 아키텍처 결정 기록
├── guides/              # 개발/배포/운영 가이드
├── learning/            # 학습 노트 및 트레이드오프 분석
├── troubleshooting/     # 문제 해결 기록 (YYYY/MM/ 하위)
├── runbooks/            # 운영 절차서
├── diagrams/            # 다이어그램 소스
├── templates/           # 문서 템플릿 (7종)
└── old-docs/            # 기존 문서 아카이브
```

## 템플릿 매핑

| 문서 유형 | 템플릿 파일 | 위치 패턴 |
|-----------|------------|----------|
| Architecture | `docs/templates/architecture-template.md` | `docs/architecture/{service}/` |
| API | `docs/templates/api-template.md` | `docs/api/{service}/` |
| ADR | `docs/templates/adr-template.md` | `docs/adr/` |
| Guide | `docs/templates/guide-template.md` | `docs/guides/{category}/` |
| Learning | `docs/templates/learning-template.md` | `docs/learning/{topic}/` |
| Troubleshooting | `docs/templates/troubleshooting-template.md` | `docs/troubleshooting/YYYY/MM/` |
| Runbook | `docs/templates/runbook-template.md` | `docs/runbooks/` |

## 명명 규칙

| 유형 | 패턴 | 예시 |
|------|------|------|
| ADR | `ADR-XXX-[decision].md` | `ADR-001-caching-strategy.md` |
| Learning | `[topic].md` | `pessimistic-vs-optimistic-lock.md` |
| Troubleshooting | `TS-YYYYMMDD-XXX-[title].md` | `TS-20260118-001-redis-timeout.md` |
| Runbook | `[kebab-case].md` | `deploy-production.md` |
| 일반 | `[kebab-case].md` | `system-overview.md` |

## 필수 메타데이터

```yaml
---
id: [고유 ID]
title: [문서 제목]
type: architecture | api | adr | guide | learning | troubleshooting | runbook
status: draft | review | approved | current | deprecated
created: YYYY-MM-DD
updated: YYYY-MM-DD
author: [작성자]
---
```

## README 계단식 업데이트

문서 추가/수정/삭제 시 관련 README를 계층적으로 업데이트합니다.

### 계층 정의

| 레벨 | 예시 | 역할 |
|------|------|------|
| L0 | `docs/README.md` | 전체 문서 포털 (최상위 인덱스) |
| L1 | `docs/architecture/README.md` | 카테고리 인덱스 |
| L2 | `docs/architecture/auth-service/README.md` | 서비스/하위 카테고리 인덱스 |

### 업데이트 규칙

**문서 추가 시:**
1. L2 README에 새 문서 항목 추가
2. L1 README에 문서 수 갱신 (해당되는 경우)
3. L0 README에 반영 필요 시 업데이트 (최근 문서 섹션 등)

**문서 수정 시:**
1. L2 README의 업데이트 날짜 갱신
2. L1/L0은 제목이나 구조 변경이 있을 때만 업데이트

**문서 삭제/폐기 시:**
1. L2 README에서 항목 제거 또는 deprecated 표시
2. L1 README에 문서 수 갱신
3. L0 README에서 해당 링크 제거

### 예시: Troubleshooting 문서 추가

```
1. docs/troubleshooting/2026/02/TS-20260206-001-xxx.md 생성
2. docs/troubleshooting/README.md (L1) → 문서 목록 테이블에 항목 추가
3. docs/README.md (L0) → "최근 이슈" 섹션에 반영
```

## 카테고리별 핵심 가이드

### Architecture
- 서비스별 `system-overview.md` 필수
- 시스템 구조, 기술 스택, 주요 컴포넌트 설명

### API
- 서비스별 디렉토리 하위에 작성
- 엔드포인트, Request/Response, 에러 코드 포함

### ADR
- 순차 번호 부여 (ADR-001, ADR-002, ...)
- Context → Decision → Consequences 구조

### Troubleshooting
- 연/월 디렉토리 구조 (`YYYY/MM/`)
- 증상 → 원인 → 해결 → 예방 구조

### Runbook
- 모든 명령어는 복사 가능해야 함
- 각 Step마다 예상 결과 기재, 롤백 정보 필수

## 문서 작업 체크리스트

1. `docs/templates/[type]-template.md` 확인
2. 명명 규칙에 따른 파일명 작성
3. 필수 메타데이터 포함
4. README 계단식 업데이트 (L2 → L1 → L0)
5. 관련 문서 상호 링크
