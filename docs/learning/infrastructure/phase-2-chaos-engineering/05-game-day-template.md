# Game Day 템플릿

팀 단위로 Chaos Engineering을 실행하는 Game Day 가이드입니다.

---

## 학습 목표

- [ ] Game Day의 목적과 진행 방법을 이해한다
- [ ] Game Day를 계획하고 실행할 수 있다
- [ ] 결과를 문서화하고 개선점을 도출할 수 있다

---

## 1. Game Day란?

### 정의

> 계획된 시간에 팀이 모여 **의도적으로 장애를 발생**시키고 **시스템 반응을 관찰**하며 **대응 능력을 향상**시키는 활동

### 목적

1. **시스템 검증**: 장애 대응 메커니즘 동작 확인
2. **팀 훈련**: 실제 장애 대응 경험 축적
3. **문제 발견**: 숨겨진 취약점 식별
4. **문서 개선**: Runbook, 알림 규칙 업데이트

---

## 2. Game Day 체크리스트

### 2주 전

- [ ] 실험 시나리오 선정
- [ ] 참가자 확정 (엔지니어, On-call, 관리자)
- [ ] 일정 공유 (달력 초대)
- [ ] 영향받는 팀에 공지

### 1주 전

- [ ] 정상 상태 가설 문서화
- [ ] 롤백 절차 검증
- [ ] 모니터링 대시보드 준비
- [ ] 예상 영향 범위 정의

### 전날

- [ ] 환경 상태 확인 (정상 동작 중)
- [ ] 알림 채널 확인 (Slack, PagerDuty)
- [ ] 참가자 역할 배정
- [ ] 비상 연락처 확인

### 당일 아침

- [ ] 시스템 정상 상태 확인
- [ ] 모든 참가자 참석 확인
- [ ] 롤백 명령어 준비
- [ ] 녹화/기록 도구 준비

---

## 3. 역할 정의

| 역할 | 책임 | 인원 |
|------|------|------|
| **Facilitator** | 진행, 시간 관리, 의사결정 | 1명 |
| **Attacker** | 장애 주입 실행 | 1명 |
| **Observer** | 모니터링, 기록 | 1-2명 |
| **Responder** | 장애 대응 (실제 On-call처럼) | 1-2명 |
| **Scribe** | 타임라인 기록, 메모 | 1명 |

---

## 4. Game Day 템플릿

### 기본 정보

```yaml
game_day:
  id: "GD-2026-01-001"
  date: "2026-01-20"
  time: "14:00-16:00 KST"
  location: "회의실 A / Zoom"

participants:
  facilitator: "김철수"
  attacker: "이영희"
  observers:
    - "박민수"
  responders:
    - "정지원"
    - "최수진"
  scribe: "한동훈"
```

### 시나리오

```yaml
scenario:
  name: "Kafka 브로커 다운"
  description: "Kafka 단일 브로커 환경에서 브로커가 다운되었을 때의 영향 확인"

  hypothesis:
    given:
      - "모든 서비스가 정상 동작 중"
      - "Kafka에 메시지가 정상 발행/소비 중"
    when:
      - "Kafka Pod가 삭제됨"
    then:
      - "Producer는 TimeoutException 발생"
      - "Consumer는 메시지 수신 중단"
      - "주문 이벤트 발행 실패"
      - "Kafka 재시작 후 정상화"

  expected_impact:
    - "Notification Service: 알림 지연"
    - "Shopping Service: 이벤트 발행 실패"
    - "사용자 영향: 알림 미수신"

  rollback_plan:
    - "kubectl apply -f k8s/infrastructure/kafka.yaml"
    - "kubectl rollout status deployment/kafka -n portal-universe"
```

### 타임라인 템플릿

```markdown
## 실행 타임라인

| 시간 | 단계 | 활동 | 담당 |
|------|------|------|------|
| 14:00 | 준비 | 참가자 집합, 역할 확인 | Facilitator |
| 14:05 | 브리핑 | 시나리오 설명, 가설 공유 | Facilitator |
| 14:10 | 확인 | 정상 상태 검증 | Observer |
| 14:15 | 실행 | 장애 주입 | Attacker |
| 14:15-14:45 | 관찰 | 시스템 반응 모니터링 | Observer |
| 14:15-14:45 | 대응 | 장애 감지 및 대응 | Responder |
| 14:45 | 복구 | 롤백 실행 (필요시) | Attacker |
| 14:50 | 검증 | 정상 상태 복귀 확인 | Observer |
| 15:00 | 회고 | 결과 토론, 교훈 도출 | All |
| 15:30 | 정리 | 문서화, 액션 아이템 정리 | Scribe |
```

---

## 5. 실행 가이드

### Phase 1: 준비 (10분)

```bash
# 1. 환경 상태 확인
kubectl get pods -n portal-universe
kubectl get events -n portal-universe --sort-by='.lastTimestamp' | tail -20

# 2. 모니터링 준비
# Grafana: http://localhost:3000 (대시보드 열기)
# Prometheus: http://localhost:9090 (Alerts 페이지)

# 3. 터미널 준비
# Terminal 1: Pod 모니터링
watch -n 2 'kubectl get pods -n portal-universe'

# Terminal 2: 이벤트 모니터링
kubectl get events -n portal-universe -w

# Terminal 3: 로그 모니터링
kubectl logs -f deployment/shopping-service -n portal-universe
```

### Phase 2: 실행 (30분)

```bash
# Attacker: 장애 주입
echo "$(date) - 장애 주입 시작"
kubectl delete pod -l app=kafka -n portal-universe

# Observer: 영향 기록
# - Grafana에서 메트릭 변화 스크린샷
# - 에러 로그 캡처
# - 알림 발생 시간 기록

# Responder: 대응 시뮬레이션
# - 알림 확인 (언제 받았는지)
# - 원인 파악 시도
# - Runbook 참조 (있는 경우)
```

### Phase 3: 복구 (10분)

```bash
# 자동 복구 대기 (Kubernetes가 Pod 재생성)
kubectl get pods -n portal-universe -l app=kafka -w

# 수동 복구 (필요시)
kubectl apply -f k8s/infrastructure/kafka.yaml

# 정상 상태 검증
./verify-steady-state.sh
```

### Phase 4: 회고 (30분)

```markdown
## 회고 질문

1. **가설 검증**
   - 예상대로 동작했는가?
   - 예상과 다른 점은 무엇인가?

2. **감지**
   - 장애를 언제 감지했는가?
   - 알림이 발생했는가?
   - 개선할 점은?

3. **대응**
   - Runbook이 있었는가?
   - 대응 절차가 명확했는가?
   - 복구 시간은 적절했는가?

4. **발견**
   - 새로 발견한 문제점은?
   - 문서화가 필요한 부분은?
   - 시스템 개선이 필요한 부분은?
```

---

## 6. 결과 보고서 템플릿

```markdown
# Game Day 결과 보고서

## 개요

| 항목 | 내용 |
|------|------|
| ID | GD-2026-01-001 |
| 일시 | 2026-01-20 14:00-16:00 |
| 시나리오 | Kafka 브로커 다운 |
| 참가자 | 김철수, 이영희, 박민수, 정지원, 최수진, 한동훈 |

## 가설 검증 결과

| 가설 | 결과 | 비고 |
|------|------|------|
| Producer TimeoutException 발생 | ✅ 확인 | 30초 후 발생 |
| Consumer 메시지 수신 중단 | ✅ 확인 | 즉시 중단 |
| Kafka 재시작 후 정상화 | ✅ 확인 | 45초 후 정상화 |

## 타임라인

| 시간 | 이벤트 |
|------|--------|
| 14:15:00 | Kafka Pod 삭제 |
| 14:15:05 | Shopping Service 로그에 연결 에러 |
| 14:15:30 | Producer TimeoutException 발생 |
| 14:15:35 | Prometheus Alert 발생 |
| 14:16:00 | 새 Kafka Pod 생성 시작 |
| 14:16:45 | Kafka Ready |
| 14:17:00 | 서비스 정상화 |

## 발견 사항

### 잘된 점
1. Kubernetes가 자동으로 Pod 재생성
2. 알림이 1분 이내에 발생
3. Circuit Breaker는 열리지 않음 (Kafka 직접 연결)

### 개선 필요
1. **Kafka 이중화 필요** - 단일 브로커는 SPOF
2. **Producer 재시도 로직 확인 필요** - 메시지 유실 가능성
3. **Consumer Lag 알림 추가 필요**

## 액션 아이템

| ID | 항목 | 담당 | 기한 |
|----|------|------|------|
| AI-001 | Kafka 3-노드 클러스터 구성 | 김철수 | 2026-02-15 |
| AI-002 | Producer 재시도 로직 검토 | 이영희 | 2026-01-31 |
| AI-003 | Consumer Lag 알림 규칙 추가 | 박민수 | 2026-01-25 |
| AI-004 | Kafka 장애 Runbook 작성 | 정지원 | 2026-01-31 |
```

---

## 7. 시나리오 라이브러리

### 난이도별 시나리오

#### ⭐ 초급

| 시나리오 | 주입 방법 | 학습 포인트 |
|---------|----------|------------|
| Pod 삭제 | `kubectl delete pod` | Kubernetes 자동 복구 |
| 서비스 Scale Down | `kubectl scale --replicas=0` | 의존성 영향 |
| Health Check 실패 | Readiness 엔드포인트 에러 | 트래픽 라우팅 |

#### ⭐⭐ 중급

| 시나리오 | 주입 방법 | 학습 포인트 |
|---------|----------|------------|
| OOM | 메모리 제한 낮춤 | 리소스 관리 |
| 네트워크 지연 | tc netem delay | Timeout 동작 |
| Kafka 브로커 다운 | Pod 삭제 | 이벤트 기반 영향 |

#### ⭐⭐⭐ 고급

| 시나리오 | 주입 방법 | 학습 포인트 |
|---------|----------|------------|
| 연쇄 장애 | 다중 서비스 지연 | Circuit Breaker |
| Redis OOM | 메모리 풀 | Rate Limiting Fallback |
| 노드 장애 | `kubectl drain` | Pod 재배치 |

---

## 8. 안전 가이드라인

### DO

- ✅ 명확한 롤백 계획 준비
- ✅ 영향 범위 제한 (Blast Radius)
- ✅ 실시간 모니터링 유지
- ✅ 비상 연락처 준비
- ✅ 결과 문서화

### DON'T

- ❌ 프로덕션에서 첫 Game Day
- ❌ 비즈니스 피크 시간대 실행
- ❌ 롤백 방법 모르는 상태로 시작
- ❌ 한 번에 여러 장애 주입
- ❌ 결과 기록 없이 종료

---

## 핵심 정리

1. **Game Day**는 계획된 장애 대응 훈련입니다
2. **역할 분담**으로 체계적으로 진행합니다
3. **가설 검증** 결과를 정량적으로 기록합니다
4. **회고**를 통해 개선점을 도출합니다
5. **액션 아이템**을 추적하여 실제 개선으로 연결합니다

---

## 다음 단계

Phase 2 완료! Phase 3로 진행합니다.

[Phase 3: 인프라 장애 시나리오](../phase-3-infrastructure-failures/kafka/01-kafka-broker-down.md)

---

## 참고 자료

- [Google DiRT (Disaster Recovery Testing)](https://cloud.google.com/blog/products/management-tools/shrinking-the-time-to-mitigate-production-incidents)
- [Netflix Chaos Engineering](https://netflix.github.io/chaosmonkey/)
