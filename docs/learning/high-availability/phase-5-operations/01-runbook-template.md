# 장애 대응 Runbook 템플릿

일관된 장애 대응을 위한 Runbook 작성 가이드입니다.

---

## 학습 목표

- [ ] Runbook의 목적과 구조를 이해한다
- [ ] Portal Universe용 Runbook을 작성할 수 있다
- [ ] 장애 발생 시 Runbook을 활용할 수 있다

---

## 1. Runbook이란?

### 정의

> 특정 상황에서 수행해야 할 절차를 문서화한 가이드

### 목적

- **일관된 대응**: 누가 대응해도 같은 품질
- **빠른 복구**: 조사 시간 단축
- **지식 공유**: 개인 의존성 제거

---

## 2. Runbook 템플릿

```markdown
# Runbook: [장애 유형]

## 1. 개요

| 항목 | 내용 |
|------|------|
| **문서 ID** | RB-XXX |
| **심각도** | P1 / P2 / P3 / P4 |
| **영향 범위** | [영향받는 서비스/기능] |
| **예상 복구 시간** | X분 |
| **마지막 업데이트** | YYYY-MM-DD |
| **작성자** | [이름] |

## 2. 증상 식별

다음 중 하나 이상 해당:
- [ ] 알림: [AlertName]
- [ ] 로그: [에러 패턴]
- [ ] 메트릭: [지표 이상]
- [ ] 사용자 신고: [증상]

## 3. 영향 범위 확인

```bash
# 영향 확인 명령어
kubectl get pods -n portal-universe -l app=affected-service
```

## 4. 즉시 대응 (5분 이내)

### 4.1 상황 확인

```bash
# 명령어 1
kubectl describe pod ...

# 명령어 2
kubectl logs ...
```

### 4.2 즉시 완화

```bash
# 서비스 재시작 (필요시)
kubectl rollout restart deployment/xxx -n portal-universe
```

## 5. 원인 분석 (15분 이내)

### 5.1 로그 확인

```bash
kubectl logs -l app=xxx -n portal-universe --tail=100
```

### 5.2 메트릭 확인

- Grafana 대시보드: [URL]
- 확인할 패널: [지표명]

### 5.3 일반적인 원인

| 원인 | 확인 방법 | 해결 |
|------|----------|------|
| 리소스 부족 | `kubectl top pod` | 스케일링 |
| 설정 오류 | ConfigMap 확인 | 수정 후 재배포 |
| 의존성 장애 | 의존 서비스 상태 | 의존성 복구 |

## 6. 복구 절차

### 6.1 표준 복구

```bash
# 단계별 명령어
```

### 6.2 롤백 (필요시)

```bash
kubectl rollout undo deployment/xxx -n portal-universe
```

## 7. 확인 및 마무리

- [ ] 서비스 정상 동작 확인
- [ ] 메트릭 정상화 확인
- [ ] 알림 해제 확인
- [ ] 인시던트 기록 작성
- [ ] Post-mortem 필요 여부 판단

## 8. 에스컬레이션

| 조건 | 대상 | 연락처 |
|------|------|--------|
| 15분 이상 미복구 | 팀 리드 | ... |
| 데이터 손실 | DBA | ... |
| 고객 영향 | PM | ... |

## 9. 관련 문서

- [관련 Runbook 1](link)
- [아키텍처 문서](link)
- [모니터링 대시보드](link)
```

---

## 3. Portal Universe Runbook 예시

### API Gateway 다운

```markdown
# Runbook: API Gateway 다운

## 1. 개요

| 항목 | 내용 |
|------|------|
| **문서 ID** | RB-001 |
| **심각도** | P1 (Critical) |
| **영향 범위** | 전체 서비스 접근 불가 |
| **예상 복구 시간** | 5-10분 |

## 2. 증상 식별

- [ ] 알림: ServiceDown (api-gateway)
- [ ] 로그: 접속 불가
- [ ] 메트릭: up{job="api-gateway"} = 0

## 3. 즉시 대응

```bash
# Pod 상태 확인
kubectl get pods -n portal-universe -l app=api-gateway

# 이벤트 확인
kubectl describe pod -l app=api-gateway -n portal-universe | grep -A10 Events

# 재시작
kubectl rollout restart deployment/api-gateway -n portal-universe
```

## 4. 확인

```bash
# Health Check
curl http://localhost:8080/actuator/health
```
```

---

## 4. Runbook 관리

### 저장 위치

```
docs/runbooks/
├── infrastructure/
│   ├── RB-001-api-gateway-down.md
│   ├── RB-002-kafka-down.md
│   └── RB-003-redis-oom.md
└── application/
    ├── RB-101-auth-failure.md
    └── RB-102-payment-error.md
```

### 검토 주기

- 분기별 검토
- 장애 발생 후 업데이트
- 아키텍처 변경 시 업데이트

---

## 다음 단계

[02-alerting-setup.md](./02-alerting-setup.md) - AlertManager 설정
