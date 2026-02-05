# Guide 작성 가이드

## 📋 개요
개발자 가이드를 작성하는 가이드입니다.

## 📁 위치 및 명명 규칙
- 위치: `docs/guides/`
- 파일명: `[topic].md` (kebab-case)
- 예시: `getting-started.md`, `coding-conventions.md`

## 📝 필수 섹션

### 1. 메타데이터
```yaml
---
id: guide-[topic]
title: [가이드 제목]
type: guide
status: current | deprecated
created: YYYY-MM-DD
updated: YYYY-MM-DD
author: Laze
tags: [태그 배열]
---
```

### 2. 개요
- 이 가이드의 목적
- 대상 독자
- 사전 지식

### 3. 본문
- 논리적 순서로 구성
- 코드 예시 포함
- 스크린샷 (필요시)

### 4. 다음 단계
- 연관된 다른 가이드 링크

## 🎯 가이드 유형

### 튜토리얼 (How-to)
- 특정 작업 완료 방법
- Step by Step
- 예시: "CI/CD 파이프라인 설정 방법"

### 설명 (Explanation)
- 개념 이해를 위한 문서
- 왜/어떻게 작동하는지
- 예시: "캐싱 전략 이해하기"

### 참조 (Reference)
- 기술적 세부 정보
- 설정 옵션, API 등
- 예시: "환경 변수 참조"

## ✅ 체크리스트
- [ ] 대상 독자가 명확한가?
- [ ] 사전 지식이 명시되었는가?
- [ ] 코드 예시가 실행 가능한가?
- [ ] 다음 단계가 안내되었는가?
- [ ] README 인덱스에 추가했는가?
