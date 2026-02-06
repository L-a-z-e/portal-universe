---
id: SCENARIO-020
title: 대기열 진입 및 이벤트 참여
type: scenario
status: current
created: 2026-01-31
updated: 2026-01-31
author: Laze
tags:
  - queue
  - waiting
  - event
  - websocket
  - e2e
related:
  - PRD-002-concurrency
---

# SCENARIO-020: 대기열 진입 및 이벤트 참여

## Overview

사용자가 **인기 이벤트 대기열에 진입**하여 **실시간 대기 순번을 확인**하고, **입장 허용 시 이벤트 페이지로 자동 이동**하는 E2E 시나리오입니다.

---

## Actors

| Actor | 역할 | 책임 |
|-------|------|------|
| **사용자** | 이벤트 참여자 | 대기열 진입, 대기, 참여 |
| **Shopping Service** | 대기열 관리 | 순번 관리, 입장 허용 |

---

## Preconditions

- 대기열이 활성화된 이벤트가 존재
- 사용자가 해당 이벤트 URL로 접근

---

## Flow

### 메인 시나리오: 대기열 진입 → 대기 → 입장

```
1. 사용자가 이벤트 접근 시 /queue/:eventType/:eventId 로 리다이렉트
2. 대기열 진입 API 자동 호출
3. 대기 화면 표시:
   - 현재 대기 순번
   - 예상 대기 시간
   - 전체 대기 인원
4. WebSocket을 통한 실시간 순번 업데이트
5. 입장 허용 시 (ENTERED 상태)
6. returnUrl 파라미터로 자동 리다이렉트
```

### 대안 시나리오: 대기열 이탈

```
1. 대기 중 "나가기" 버튼 클릭
2. 대기열 이탈 API 호출
3. LEFT 상태 메시지 표시
4. 메인 페이지로 이동 안내
```

### 대안 시나리오: 대기 세션 만료

```
1. 일정 시간 초과 시 EXPIRED 상태
2. "대기 시간이 초과되었습니다" 메시지
3. 다시 시도 버튼 제공
```

---

## API Endpoints

| HTTP | Endpoint | 설명 |
|------|----------|------|
| POST | `/queue/{eventType}/{eventId}/enter` | 대기열 진입 |
| GET | `/queue/{eventType}/{eventId}/status` | 대기열 상태 조회 |
| GET | `/queue/token/{entryToken}` | 토큰으로 상태 조회 |
| DELETE | `/queue/{eventType}/{eventId}/leave` | 대기열 이탈 |

---

## E2E Test Cases

| TC | 설명 | 우선순위 |
|----|------|---------|
| TC-020-01 | 대기열 페이지 로드 및 진입 | P1 |
| TC-020-02 | 대기 순번/시간 표시 | P1 |
| TC-020-03 | 대기열 이탈 | P2 |
| TC-020-04 | 세션 만료 처리 | P2 |
| TC-020-05 | 입장 허용 시 리다이렉트 | P2 |

---

## Verification Points

- [ ] QueueWaitingPage 렌더링
- [ ] QueueStatus 컴포넌트 데이터 표시
- [ ] WebSocket 연결 및 실시간 업데이트
- [ ] 상태별 UI 변경 (WAITING/ENTERED/EXPIRED/LEFT)
- [ ] 이탈 버튼 동작
