# 📜 문서화 규칙

## 1. 핵심 원칙

### 일관성 (Consistency)
- 모든 문서는 정해진 템플릿과 규칙을 따름
- 용어, 형식, 스타일의 통일

### 완전성 (Completeness)
- 필수 섹션 누락 없이 작성
- 메타데이터 완전히 기재

### 최신성 (Currency)
- 코드 변경 시 문서 동기화
- `updated` 날짜 갱신

### 접근성 (Accessibility)
- README 인덱스 유지
- 명확한 링크 구조

## 2. 디렉토리 규칙

```
docs/
├── prd/                     # 제품 요구사항
├── adr/                     # 아키텍처 결정
├── architecture/            # 시스템 구조
├── api/                     # API 명세
├── diagrams/                # 다이어그램
│   ├── source/
│   └── exported/
├── testing/                 # 테스트 문서
│   ├── test-plan/
│   ├── test-cases/
│   └── coverage/
├── troubleshooting/         # 문제 해결
│   ├── templates/
│   └── YYYY/MM/
├── runbooks/                # 운영 절차
├── guides/                  # 개발 가이드
└── learning/                # 학습 자료
```

## 3. 명명 규칙

| 유형 | 패턴 | 예시 |
|------|------|------|
| PRD | `PRD-XXX-[feature].md` | `PRD-001-user-auth.md` |
| ADR | `ADR-XXX-[decision].md` | `ADR-001-caching.md` |
| Test Plan | `TP-XXX-YY-[feature].md` | `TP-001-01-login.md` |
| Test Case | `TC-XXX-YY-ZZ-[scenario].md` | `TC-001-01-01-success.md` |
| Troubleshooting | `TS-YYYYMMDD-XXX-[title].md` | `TS-20260118-001-redis.md` |
| 일반 | `[kebab-case].md` | `system-overview.md` |

### ID 규칙
- 순차 번호 사용 (001, 002, ...)
- 삭제된 ID 재사용 금지
- 각 디렉토리 README에 마지막 ID 기록

## 4. 메타데이터 규칙

모든 문서에 필수:
```yaml
---
id: [고유 ID]
title: [문서 제목]
type: [문서 유형]
status: [상태]
created: YYYY-MM-DD
updated: YYYY-MM-DD
author: [작성자]
tags: [태그 배열]
related:
  - [관련문서 ID]
---
```

### 상태 값
- `draft`: 작성 중
- `review`: 검토 중
- `approved`: 승인됨 (PRD, ADR)
- `current`: 현재 유효 (기타)
- `deprecated`: 폐기됨

## 5. 링크 규칙

### 상대 경로 사용
```markdown
[ADR-001](../adr/ADR-001-caching-strategy.md)
```

### 양방향 링크
- A에서 B 링크 시, B의 related에도 A 추가

## 6. 업데이트 규칙

### 문서 수정 시
1. `updated` 날짜 갱신
2. 변경 내용이 크면 관련 문서 확인
3. 필요시 README 인덱스 업데이트

### Deprecation
1. `status: deprecated` 변경
2. 대체 문서 링크 추가
3. README에서 표시 변경

## 7. 품질 체크리스트

- [ ] 올바른 디렉토리에 있는가?
- [ ] 명명 규칙을 따르는가?
- [ ] 필수 메타데이터가 있는가?
- [ ] 템플릿 구조를 따르는가?
- [ ] README 인덱스에 있는가?
- [ ] 관련 문서가 링크되었는가?
- [ ] 깨진 링크가 없는가?
