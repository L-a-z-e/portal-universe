# ADR (Architecture Decision Record) 작성 가이드

## 📋 개요
ADR은 중요한 아키텍처 결정을 기록하는 문서입니다.

## 📁 위치 및 명명 규칙
- 위치: `docs/adr/`
- 파일명: `ADR-XXX-[decision-title].md`
- 예시: `ADR-001-caching-strategy.md`

## 📝 필수 섹션

### 1. 메타데이터 (YAML Frontmatter)
```yaml
---
id: ADR-XXX
title: [결정 제목]
type: adr
status: proposed | accepted | rejected | deprecated | superseded
created: YYYY-MM-DD
updated: YYYY-MM-DD
author: [작성자]
decision_date: YYYY-MM-DD
reviewers: [검토자 목록]
superseded_by: [대체 ADR ID] # deprecated/superseded 시
tags: [태그 배열]
related:
  - [관련문서 ID]
---
```

### 2. Context (배경)
- 어떤 상황에서 이 결정이 필요한지
- 기술적/비즈니스적 제약 조건

### 3. Decision Drivers (결정 요인)
- 결정에 영향을 미친 주요 요소들
- 번호 매기기 권장

### 4. Considered Options (검토한 대안)
- 각 옵션별 장단점 분석
- 최소 2개 이상의 대안 제시

### 5. Decision (최종 결정)
- 선택한 옵션
- 선택 이유

### 6. Consequences (영향)
- 긍정적 영향
- 부정적 영향 (트레이드오프)

## ✅ 체크리스트
- [ ] Context가 충분히 설명되었는가?
- [ ] 최소 2개 이상의 대안을 검토했는가?
- [ ] 각 대안의 장단점이 명확한가?
- [ ] 결정의 근거가 명시되었는가?
- [ ] README 인덱스에 추가했는가?
