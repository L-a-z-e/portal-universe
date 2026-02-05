# Architecture 문서 작성 가이드

## 📋 개요
시스템 아키텍처를 설명하는 문서입니다.

## 📁 위치 및 명명 규칙
- 위치: `docs/architecture/`
- 파일명: `[topic].md` (kebab-case)
- 예시: `system-overview.md`, `data-flow.md`

## 📝 필수 섹션

### 1. 메타데이터
```yaml
---
id: arch-[topic]
title: [제목]
type: architecture
status: current | deprecated
created: YYYY-MM-DD
updated: YYYY-MM-DD
author: Laze
tags: [태그 배열]
related:
  - [관련문서 ID]
---
```

### 2. 개요
- 문서의 범위
- 대상 독자

### 3. 다이어그램
- Mermaid 또는 이미지
- 다이어그램 설명

### 4. 컴포넌트 설명
- 각 컴포넌트의 역할
- 기술 스택

### 5. 데이터 흐름 (해당 시)
### 6. 관련 문서 링크

## 🎨 다이어그램 작성 팁
- Mermaid 사용 권장
- 복잡한 다이어그램은 여러 개로 분리
- 색상 사용 시 일관성 유지

## ✅ 체크리스트
- [ ] 다이어그램이 포함되었는가?
- [ ] 모든 컴포넌트가 설명되었는가?
- [ ] 관련 ADR이 링크되었는가?
- [ ] README 인덱스에 추가했는가?
