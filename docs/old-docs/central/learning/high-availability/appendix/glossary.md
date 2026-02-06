# 용어 사전

고가용성 관련 주요 용어를 정리합니다.

---

## A

### Active-Active
모든 인스턴스가 동시에 트래픽을 처리하는 이중화 패턴.

### Active-Passive
Primary가 트래픽을 처리하고 Secondary는 대기하는 이중화 패턴.

### Availability (가용성)
시스템이 정상적으로 서비스를 제공하는 시간의 비율.

---

## B

### Blast Radius
장애 발생 시 영향받는 범위.

### Bulkhead
장애 격리를 위해 리소스를 분리하는 패턴.

---

## C

### Cascading Failure (연쇄 장애)
하나의 장애가 다른 컴포넌트로 전파되는 현상.

### Chaos Engineering
시스템의 회복력을 검증하기 위해 의도적으로 장애를 주입하는 방법론.

### Circuit Breaker
장애 서비스로의 요청을 차단하여 연쇄 장애를 방지하는 패턴.

---

## D

### Disaster Recovery (DR, 재해 복구)
재해 발생 시 시스템을 복구하는 프로세스.

### Drain
노드에서 Pod를 안전하게 제거하는 Kubernetes 작업.

---

## F

### Failover
장애 발생 시 백업 시스템으로 전환하는 것.

### Fail-Open
장애 시 모든 요청을 허용 (예: Rate Limiter)

### Fail-Closed
장애 시 모든 요청을 거부.

---

## G

### Game Day
팀이 모여 의도적으로 장애를 발생시키고 대응을 연습하는 활동.

### Graceful Shutdown
진행 중인 작업을 완료한 후 종료하는 것.

---

## H

### HPA (Horizontal Pod Autoscaler)
메트릭 기반으로 Pod 수를 자동 조절하는 Kubernetes 리소스.

---

## I

### Incident
서비스 중단 또는 품질 저하를 일으키는 이벤트.

### ISR (In-Sync Replicas)
Kafka에서 Leader와 동기화된 Replica 집합.

---

## M

### MTBF (Mean Time Between Failures)
장애 사이 평균 시간.

### MTTR (Mean Time To Recovery)
복구 평균 시간.

### MTTD (Mean Time To Detect)
장애 감지 평균 시간.

### MTTA (Mean Time To Acknowledge)
장애 인지 평균 시간.

---

## P

### PDB (Pod Disruption Budget)
자발적 중단 시 최소 가용 Pod를 보장하는 Kubernetes 리소스.

### Post-Mortem
인시던트 후 원인 분석 및 개선 사항을 도출하는 문서.

---

## R

### RTO (Recovery Time Objective)
복구 목표 시간.

### RPO (Recovery Point Objective)
데이터 손실 허용 범위 (시간).

### Runbook
장애 대응 절차를 문서화한 가이드.

---

## S

### SLA (Service Level Agreement)
고객과의 서비스 수준 계약.

### SLO (Service Level Objective)
서비스 수준 목표값.

### SLI (Service Level Indicator)
서비스 수준을 측정하는 지표.

### SPOF (Single Point of Failure)
단일 장애점. 장애 시 전체 시스템에 영향을 주는 컴포넌트.

### Steady State (정상 상태)
시스템이 비즈니스 목표를 달성하고 있음을 나타내는 측정 가능한 상태.

---

## T

### Thundering Herd
동시에 많은 요청이 몰리는 현상.

---

## 9s (나인스)

| 표현 | 가용성 | 연간 다운타임 |
|------|--------|--------------|
| Two 9s | 99% | 3.65일 |
| Three 9s | 99.9% | 8.77시간 |
| Four 9s | 99.99% | 52.6분 |
| Five 9s | 99.999% | 5.26분 |
