# PRD (Product Requirements Document) 작성 가이드

## 📋 개요
PRD는 제품 요구사항을 정의하는 문서입니다.

## 📁 위치 및 명명 규칙
- 위치: `docs/prd/`
- 파일명: `PRD-XXX-[feature-name].md`
- 예시: `PRD-001-user-authentication.md`

## 📝 필수 섹션

### 1. 메타데이터 (YAML Frontmatter)
```yaml
---
id: PRD-XXX
title: [기능명]
type: prd
status: draft | review | approved | rejected | deprecated
created: YYYY-MM-DD
updated: YYYY-MM-DD
author: Laze
stakeholders: [이해관계자 목록]
target_release: [타겟 버전]
priority: P0 | P1 | P2
tags: [태그 배열]
related:
  - [관련문서 ID]
---
```

### 2. 개요 테이블
| 항목 | 내용 |
|------|------|
| **문서 ID** | PRD-XXX |
| **상태** | 상태 아이콘 |
| **우선순위** | P0/P1/P2 |
| **타겟 릴리즈** | 버전 |

### 3. 배경 및 목적
- 왜 이 기능이 필요한지
- 어떤 문제를 해결하는지

### 4. 목표 및 비목표
- Goals: 이 PRD에서 달성할 것
- Non-Goals: 명시적으로 제외할 것

### 5. 요구사항
- 기능 요구사항 (FR)
- 비기능 요구사항 (NFR)

### 6. 데이터 모델 (해당 시)
### 7. 릴리즈 계획
### 8. 관련 문서

## ✅ 체크리스트
- [ ] ID가 순차적으로 부여되었는가?
- [ ] 모든 이해관계자가 명시되었는가?
- [ ] Goals/Non-Goals가 명확한가?
- [ ] 요구사항에 우선순위가 있는가?
- [ ] README 인덱스에 추가했는가?
