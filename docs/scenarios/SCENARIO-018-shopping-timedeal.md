---
id: SCENARIO-018
title: 타임딜 탐색 및 구매
type: scenario
status: current
created: 2026-01-31
updated: 2026-01-31
author: Laze
tags:
  - time-deal
  - countdown
  - purchase
  - e2e
related:
  - PRD-002-concurrency
---

# SCENARIO-018: 타임딜 탐색 및 구매

## Overview

사용자가 **진행 중인 타임딜을 탐색**하고, **카운트다운 타이머를 확인**한 뒤, **타임딜 상품을 직접 구매**하여 내 구매 내역에서 확인하는 E2E 시나리오입니다.

---

## Actors

| Actor | 역할 | 책임 |
|-------|------|------|
| **사용자** | 타임딜 구매자 | 타임딜 탐색, 구매 |
| **Shopping Service** | 비즈니스 로직 | 타임딜 재고 관리, 구매 처리 |

---

## Preconditions

- ACTIVE 상태의 타임딜이 1개 이상 존재
- 사용자가 로그인된 상태 (구매 시)

---

## Flow

### 메인 시나리오: 타임딜 탐색 → 구매 → 내역 확인

```
1. 사용자가 /time-deals 페이지 접속
2. "진행 중" 섹션에서 활성 타임딜 목록 확인
3. "곧 시작" 섹션에서 예정된 타임딜 확인
4. 원하는 타임딜 카드 클릭 → 상세 페이지 이동
5. 카운트다운 타이머, 남은 수량(%) 바 확인
6. 수량 선택 후 "구매하기" 버튼 클릭
7. 구매 성공 메시지 확인
8. /time-deals/purchases 페이지에서 구매 내역 확인
```

### 대안 시나리오: 타임딜 종료

```
1. 카운트다운이 0에 도달
2. 타임딜 상태가 ENDED로 변경
3. 구매 버튼 비활성화
```

### 대안 시나리오: 재고 소진

```
1. 타임딜 수량이 모두 판매됨
2. "품절" 상태 표시
3. 구매 버튼 비활성화
```

---

## API Endpoints

| HTTP | Endpoint | 설명 |
|------|----------|------|
| GET | `/time-deals` | 진행 중인 타임딜 목록 |
| GET | `/time-deals/{timeDealId}` | 타임딜 상세 |
| POST | `/time-deals/purchase` | 타임딜 구매 |
| GET | `/time-deals/my/purchases` | 내 구매 내역 |

---

## E2E Test Cases

| TC | 설명 | 우선순위 |
|----|------|---------|
| TC-018-01 | 타임딜 목록 페이지 로드 | P1 |
| TC-018-02 | 활성/예정 타임딜 구분 표시 | P1 |
| TC-018-03 | 타임딜 상세 페이지 이동 | P1 |
| TC-018-04 | 카운트다운 타이머 동작 | P1 |
| TC-018-05 | 타임딜 구매 성공 | P1 |
| TC-018-06 | 구매 내역 확인 | P1 |
| TC-018-07 | 품절/종료 상태 표시 | P2 |

---

## Verification Points

- [ ] /time-deals 페이지 렌더링
- [ ] 타임딜 카드 컴포넌트 표시
- [ ] CountdownTimer 동작
- [ ] 남은 수량 바 표시
- [ ] 구매 플로우 동작
- [ ] /time-deals/purchases 데이터 로드
