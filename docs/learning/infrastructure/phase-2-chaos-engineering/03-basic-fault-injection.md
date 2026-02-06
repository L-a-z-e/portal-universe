# 기본 장애 주입

kubectl과 Docker를 사용한 기본 장애 주입 기법을 실습합니다.

---

## 학습 목표

- [ ] kubectl을 사용해 Pod를 삭제하고 복구를 관찰할 수 있다
- [ ] 리소스 제한을 통해 OOM을 시뮬레이션할 수 있다
- [ ] 네트워크 지연을 주입하고 영향을 확인할 수 있다

---

## 1. 실습 환경 준비

### 사전 확인

```bash
# 네임스페이스 확인
kubectl get ns portal-universe

# Pod 상태 확인
kubectl get pods -n portal-universe

# 모든 Pod가 Running 상태인지 확인
kubectl get pods -n portal-universe -o jsonpath='{range .items[*]}{.metadata.name}{"\t"}{.status.phase}{"\n"}{end}'
```

### 모니터링 준비

```bash
# 터미널 1: Pod 상태 실시간 모니터링
watch -n 2 'kubectl get pods -n portal-universe'

# 터미널 2: 이벤트 모니터링
kubectl get events -n portal-universe -w

# 터미널 3: Grafana 대시보드 (브라우저)
# http://localhost:3000
```

---

## 2. 실험 1: Pod 삭제

### 개요

가장 기본적인 장애 주입으로, Pod를 삭제하고 Kubernetes가 자동으로 재생성하는지 확인합니다.

### 정상 상태 가설

```yaml
hypothesis:
  - "API Gateway Pod가 삭제되면"
  - "Kubernetes가 30초 이내에 새 Pod를 생성할 것이다"
  - "서비스 중단 시간은 1분 이내일 것이다"
```

### 실험 절차

**Step 1: 현재 상태 기록**

```bash
# Pod 이름 확인
kubectl get pods -n portal-universe -l app=api-gateway -o name
# 예: pod/api-gateway-abc123

# 현재 시간 기록
date

# Health Check
curl -s http://localhost:8080/actuator/health | jq
```

**Step 2: 장애 주입**

```bash
# Pod 삭제
kubectl delete pod -n portal-universe -l app=api-gateway

# 삭제 시간 기록
echo "삭제 시간: $(date)"
```

**Step 3: 관찰**

```bash
# Pod 재생성 모니터링 (터미널 1에서)
# STATUS가 Terminating → ContainerCreating → Running 변화 관찰

# Health Check 반복
while true; do
  status=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health)
  echo "$(date): HTTP $status"
  sleep 2
done
```

**Step 4: 결과 기록**

```yaml
# 실험 결과 기록
results:
  pod_deleted_at: "2026-01-15T14:23:00Z"
  new_pod_created_at: "2026-01-15T14:23:05Z"
  service_healthy_at: "2026-01-15T14:23:45Z"
  total_downtime: "45초"
  hypothesis_validated: true
```

### 예상 동작

```
시간 ──────────────────────────────────────────────────>
      │        │              │                    │
   Pod 삭제  새 Pod 생성   Container 시작       Ready
      │        │              │                    │
      0초      5초           30초                 45초
                              │
                        (startupProbe 대기)
```

---

## 3. 실험 2: 강제 종료 (Grace Period 0)

### 개요

graceful shutdown 없이 즉시 종료하여 "예기치 않은 프로세스 종료"를 시뮬레이션합니다.

### 정상 상태 가설

```yaml
hypothesis:
  - "진행 중인 요청이 있는 상태에서 강제 종료하면"
  - "해당 요청은 실패할 것이다"
  - "새 Pod가 시작되면 정상화될 것이다"
```

### 실험 절차

**Step 1: 부하 생성**

```bash
# 지속적인 요청 (백그라운드)
while true; do
  curl -s http://localhost:8080/api/health > /dev/null
  sleep 0.1
done &
LOAD_PID=$!
```

**Step 2: 강제 종료**

```bash
# Grace Period 0으로 즉시 종료
kubectl delete pod -n portal-universe -l app=api-gateway --grace-period=0 --force
```

**Step 3: 에러 관찰**

```bash
# 부하 프로세스에서 에러 발생 관찰
# Connection refused, 502, 503 등

# 부하 중지
kill $LOAD_PID
```

### 결과 비교

| 항목 | 일반 삭제 | 강제 종료 |
|------|----------|----------|
| 진행 중 요청 | 완료됨 | 실패함 |
| 종료 시간 | ~30초 | 즉시 |
| 에러 발생 | 거의 없음 | 있음 |

---

## 4. 실험 3: 리소스 제한 (OOM 시뮬레이션)

### 개요

메모리 제한을 낮추어 Out of Memory 상황을 시뮬레이션합니다.

### 정상 상태 가설

```yaml
hypothesis:
  - "메모리 제한을 64Mi로 낮추면"
  - "Pod가 OOMKilled 상태가 될 것이다"
  - "Kubernetes가 자동으로 재시작할 것이다"
```

### 실험 절차

**Step 1: 현재 리소스 확인**

```bash
# 현재 메모리 제한 확인
kubectl get deployment redis -n portal-universe -o jsonpath='{.spec.template.spec.containers[0].resources}'

# 현재 사용량 확인
kubectl top pod -n portal-universe -l app=redis
```

**Step 2: 메모리 제한 적용**

```bash
# 메모리 제한을 64Mi로 낮춤
kubectl patch deployment redis -n portal-universe -p \
  '{"spec":{"template":{"spec":{"containers":[{"name":"redis","resources":{"limits":{"memory":"64Mi"}}}]}}}}'
```

**Step 3: OOM 관찰**

```bash
# Pod 상태 모니터링
kubectl get pods -n portal-universe -l app=redis -w

# 이벤트 확인
kubectl get events -n portal-universe --field-selector involvedObject.name=redis -w

# OOMKilled 확인
kubectl describe pod -n portal-universe -l app=redis | grep -A5 "Last State"
```

**Step 4: 복원**

```bash
# 원래 설정으로 복원
kubectl patch deployment redis -n portal-universe -p \
  '{"spec":{"template":{"spec":{"containers":[{"name":"redis","resources":{"limits":{"memory":"256Mi"}}}]}}}}'
```

### 예상 결과

```
NAME       READY   STATUS      RESTARTS   AGE
redis-xxx  0/1     OOMKilled   1          30s
redis-xxx  0/1     CrashLoopBackOff   2    45s
redis-xxx  1/1     Running     3          60s
```

---

## 5. 실험 4: 네트워크 지연 주입

### 개요

`tc` (traffic control) 명령을 사용하여 네트워크 지연을 추가합니다.

### 사전 조건

```bash
# Pod에 tc 명령이 있는지 확인 (대부분 없음)
kubectl exec -it <pod-name> -n portal-universe -- which tc

# 없다면 Docker 레벨에서 수행하거나
# 테스트용 네트워크 지연 이미지 사용
```

### 대안: 애플리케이션 레벨 지연

테스트 엔드포인트를 추가하여 지연을 시뮬레이션합니다.

```bash
# API Gateway에 테스트 엔드포인트가 있다면
curl http://localhost:8080/api/test/delay?ms=5000
```

### 프로그래밍 방식 (Spring Boot)

```java
// TestController.java (개발 환경 전용)
@RestController
@RequestMapping("/api/test")
@Profile("local")  // 로컬에서만 활성화
public class TestController {

    @GetMapping("/delay")
    public ResponseEntity<String> delay(
            @RequestParam(defaultValue = "1000") int ms) throws InterruptedException {
        Thread.sleep(ms);
        return ResponseEntity.ok("Delayed " + ms + "ms");
    }

    @GetMapping("/error")
    public ResponseEntity<String> error() {
        throw new RuntimeException("Test error");
    }
}
```

---

## 6. 실험 5: Deployment Scale Down

### 개요

replicas를 0으로 설정하여 서비스를 완전히 중단시킵니다.

### 정상 상태 가설

```yaml
hypothesis:
  - "auth-service replicas를 0으로 설정하면"
  - "인증 관련 API가 모두 실패할 것이다"
  - "Circuit Breaker가 열릴 것이다"
```

### 실험 절차

**Step 1: Scale Down**

```bash
# 현재 replicas 확인
kubectl get deployment auth-service -n portal-universe

# replicas를 0으로
kubectl scale deployment auth-service -n portal-universe --replicas=0
```

**Step 2: 영향 확인**

```bash
# 인증 API 호출 시도
curl -v http://localhost:8080/api/v1/auth/health

# Circuit Breaker 상태 확인
curl http://localhost:8080/actuator/health | jq '.components.circuitBreakers'
```

**Step 3: 복원**

```bash
# replicas 복원
kubectl scale deployment auth-service -n portal-universe --replicas=1

# 복구 확인
kubectl get pods -n portal-universe -l app=auth-service -w
```

---

## 7. 실험 결과 기록 템플릿

### 실험 보고서

```yaml
experiment:
  id: "EXP-001"
  name: "API Gateway Pod 삭제"
  date: "2026-01-15"
  executor: "your-name"

hypothesis:
  description: "Pod 삭제 후 45초 이내 복구"
  expected_behavior: "Kubernetes가 자동으로 새 Pod 생성"

environment:
  cluster: "kind-portal-universe"
  namespace: "portal-universe"
  affected_components:
    - "api-gateway"

execution:
  start_time: "14:23:00"
  end_time: "14:24:30"
  commands_executed:
    - "kubectl delete pod -n portal-universe -l app=api-gateway"

observations:
  - time: "14:23:00"
    event: "Pod 삭제 명령 실행"
  - time: "14:23:05"
    event: "새 Pod 생성 시작"
  - time: "14:23:35"
    event: "Container 시작"
  - time: "14:23:45"
    event: "Readiness Probe 통과"

results:
  hypothesis_validated: true
  actual_downtime: "45초"
  unexpected_findings: []

recommendations:
  - "replicas를 2 이상으로 설정하여 다운타임 제거"
  - "PDB 설정으로 최소 가용 Pod 보장"
```

---

## 8. 안전 체크리스트

### 실험 전

- [ ] 모든 Pod가 Running 상태
- [ ] 모니터링 대시보드 준비됨
- [ ] 롤백 명령어 준비됨
- [ ] 팀원에게 공유됨 (필요시)

### 실험 중

- [ ] 예상대로 동작하는지 확인
- [ ] 예상치 못한 영향 모니터링
- [ ] 비상 정지 준비

### 실험 후

- [ ] 정상 상태 복귀 확인
- [ ] 결과 기록
- [ ] 발견 사항 공유

---

## 핵심 정리

1. **Pod 삭제**는 가장 기본적인 장애 주입 방법입니다
2. **강제 종료**는 graceful shutdown 실패를 시뮬레이션합니다
3. **리소스 제한**으로 OOM을 시뮬레이션할 수 있습니다
4. **Scale Down**으로 서비스 완전 중단을 테스트합니다
5. **모든 실험은 결과를 기록**해야 합니다

---

## 다음 단계

[04-steady-state-hypothesis.md](./04-steady-state-hypothesis.md) - 정상 상태 가설을 체계적으로 수립합니다.

---

## 참고 자료

- [Kubernetes - Pod Lifecycle](https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle/)
- [Linux Traffic Control (tc)](https://man7.org/linux/man-pages/man8/tc.8.html)
