# Testing 문서 작성 가이드

## 📋 개요
테스트 계획서와 테스트 케이스를 작성하는 가이드입니다.

## 📁 구조
```
testing/
├── test-plan/     # 테스트 계획서
├── test-cases/    # 테스트 케이스
└── coverage/      # 커버리지 리포트
```

## 📝 명명 규칙

### 테스트 계획서
- 파일명: `TP-XXX-YY-[feature].md`
- XXX: PRD 번호
- YY: 계획서 순번
- 예시: `TP-001-01-product-crud.md`

### 테스트 케이스
- 파일명: `TC-XXX-YY-ZZ-[scenario].md`
- ZZ: 케이스 순번
- 예시: `TC-001-01-01-create-product.md`

## 📄 테스트 계획서 구조

```yaml
---
id: TP-XXX-YY
title: [테스트 계획 제목]
type: test-plan
status: draft | approved | completed
related:
  - [PRD-XXX]
---
```

### 필수 섹션
1. 개요 (관련 PRD, 범위, 환경)
2. 테스트 목표
3. 테스트 케이스 요약 (테이블)
4. 합격 기준

## 📄 테스트 케이스 구조

### 필수 섹션
1. 테스트 정보 (ID, 우선순위)
2. 사전 조건
3. 테스트 단계 (Step by Step)
4. 예상 결과
5. 실제 결과 (실행 후)

## ✅ 체크리스트
- [ ] PRD와 연결되어 있는가?
- [ ] 테스트 케이스에 우선순위가 있는가?
- [ ] 합격 기준이 명확한가?
- [ ] README 인덱스에 추가했는가?
