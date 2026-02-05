# 종합 테스트 시나리오

학습한 내용을 종합적으로 테스트합니다.

---

## 학습 목표

- [ ] 고가용성 개선 사항을 종합적으로 검증할 수 있다
- [ ] 99.9% 가용성 목표 달성을 확인할 수 있다
- [ ] 장애 대응 능력을 실습할 수 있다

---

## 1. 테스트 시나리오 개요

### 목표

- **가용성**: 99.9% (월 43분 이하 다운타임)
- **복구 시간**: MTTR < 5분
- **장애 격리**: 단일 서비스 장애가 전체에 영향 없음

### 전제 조건

- [ ] Phase 4 개선 사항 적용 완료
- [ ] replicas >= 2 (모든 서비스)
- [ ] HPA, PDB 설정 완료
- [ ] 모니터링 대시보드 준비

---

## 2. 테스트 1: Rolling Update 무중단

### 목표

배포 중 에러율 0%

### 절차

```bash
# 1. 부하 생성
k6 run -d 120s services/load-tests/k6/scenarios/a-shopping-flow.js &

# 2. Rolling Update 실행
kubectl set image deployment/api-gateway \
  api-gateway=portal-universe-api-gateway:latest \
  -n portal-universe

# 3. 에러율 관찰
# Grafana에서 HTTP 5xx 확인 - 0이어야 함
```

### 성공 기준

- 에러율 = 0%
- 응답 시간 급증 없음

---

## 3. 테스트 2: 서비스 장애 격리

### 목표

Auth Service 다운 시 다른 기능 정상

### 절차

```bash
# 1. Auth Service 중단
kubectl scale deployment auth-service --replicas=0 -n portal-universe

# 2. 비인증 API 정상 확인
curl http://localhost:8080/api/v1/shopping/products
# 200 OK 반환해야 함

# 3. Circuit Breaker 상태 확인
curl http://localhost:8080/actuator/health | jq '.components.circuitBreakers'
# authCircuitBreaker: OPEN

# 4. 복구
kubectl scale deployment auth-service --replicas=2 -n portal-universe
```

### 성공 기준

- 비인증 API 정상 동작
- Circuit Breaker 열림
- 전체 시스템 붕괴 없음

---

## 4. 테스트 3: 부하 스파이크 대응

### 목표

트래픽 급증 시 HPA 동작

### 절차

```bash
# 1. 현재 replicas 확인
kubectl get hpa -n portal-universe

# 2. 높은 부하 생성
k6 run -u 100 -d 300s services/load-tests/k6/scenarios/c-coupon-spike.js

# 3. HPA 스케일 아웃 관찰
watch -n 5 'kubectl get hpa -n portal-universe'

# 4. 부하 중단 후 스케일 다운 관찰
```

### 성공 기준

- CPU 70% 초과 시 스케일 아웃
- 에러율 < 1%
- 부하 감소 후 스케일 다운

---

## 5. 테스트 4: 노드 유지보수

### 목표

노드 Drain 시 PDB 준수

### 절차

```bash
# 1. PDB 확인
kubectl get pdb -n portal-universe

# 2. 노드 Drain 시도
kubectl drain <node-name> --ignore-daemonsets --delete-emptydir-data

# 3. Pod 재배치 관찰
kubectl get pods -n portal-universe -o wide -w

# 4. 서비스 정상 확인
curl http://localhost:8080/api/health
```

### 성공 기준

- PDB minAvailable 유지
- 서비스 중단 없음

---

## 6. 테스트 5: 인프라 장애 복구

### 목표

Kafka 다운 후 자동 복구

### 절차

```bash
# 1. Kafka Pod 삭제
kubectl delete pod -l app=kafka -n portal-universe

# 2. 복구 시간 측정
time (
  while ! kubectl exec -it kafka-0 -n portal-universe -- \
    kafka-broker-api-versions.sh --bootstrap-server localhost:9092 2>/dev/null; do
    sleep 1
  done
)

# 3. Producer/Consumer 정상화 확인
kubectl logs deployment/shopping-service -n portal-universe --tail=10
```

### 성공 기준

- 복구 시간 < 2분
- 메시지 유실 최소화 (재시도로 복구)

---

## 7. 최종 체크리스트

### 가용성

- [ ] 모든 서비스 replicas >= 2
- [ ] PDB 설정 완료
- [ ] HPA 동작 확인
- [ ] Circuit Breaker 동작 확인

### 복구

- [ ] Pod 자동 재시작 확인
- [ ] Rolling Update 무중단 확인
- [ ] Failover 동작 확인

### 운영

- [ ] Runbook 작성 완료
- [ ] 알림 규칙 설정 완료
- [ ] 대시보드 구성 완료

---

## Phase 5 완료!

축하합니다! 고가용성 학습 로드맵을 완료했습니다.

### 다음 단계

1. 실제 프로덕션에 개선 사항 적용
2. 정기적인 Game Day 실시
3. 장애 발생 시 Post-Mortem 작성
4. 지속적인 개선

### 부록 참조

- [용어 사전](../appendix/glossary.md)
- [명령어 치트시트](../appendix/commands-cheatsheet.md)
- [PromQL 쿼리 모음](../appendix/monitoring-queries.md)
