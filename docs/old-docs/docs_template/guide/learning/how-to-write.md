# Learning 자료 작성 가이드

## 📋 개요
학습 자료 문서를 작성하는 가이드입니다.

## 📁 위치 및 명명 규칙
- 위치: `docs/learning/`
- 파일명: `[topic].md` (kebab-case)
- 예시: `spring-boot-basics.md`, `kafka-introduction.md`

## 📝 필수 섹션

### 1. 메타데이터
```yaml
---
id: learning-[topic]
title: [학습 자료 제목]
type: learning
status: current | deprecated
created: YYYY-MM-DD
updated: YYYY-MM-DD
author: Laze
tags: [태그 배열]
difficulty: beginner | intermediate | advanced
estimated_time: [예상 학습 시간]
---
```

### 2. 개요
- 학습 목표
- 사전 지식
- 예상 소요 시간

### 3. 학습 내용
- 외부 자료 링크 (공식 문서, 튜토리얼)
- 사내 자료 링크
- 추천 순서

### 4. 실습 과제 (선택)
- 학습 내용 적용 과제
- 확인 질문

## 📊 난이도 기준

| 레벨 | 아이콘 | 대상 |
|------|--------|------|
| Beginner | ⭐ | 해당 기술 처음 접함 |
| Intermediate | ⭐⭐⭐ | 기본 사용 경험 있음 |
| Advanced | ⭐⭐⭐⭐⭐ | 심화 학습 필요 |

## 📐 자료 링크 형식

```markdown
| 자료 | 난이도 | 설명 |
|------|--------|------|
| [제목](URL) | ⭐⭐ | 간단한 설명 |
```

## ✅ 체크리스트
- [ ] 난이도가 명시되었는가?
- [ ] 링크가 유효한가?
- [ ] 학습 경로가 논리적인가?
- [ ] README 인덱스에 추가했는가?
