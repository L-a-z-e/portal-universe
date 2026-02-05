# Graceful Shutdown 구현

서비스 종료 시 진행 중인 요청을 안전하게 처리합니다.

---

## 학습 목표

- [ ] Graceful Shutdown의 필요성을 이해한다
- [ ] Spring Boot에서 Graceful Shutdown을 설정할 수 있다
- [ ] Kubernetes와 연동하여 무중단 배포를 구현할 수 있다

---

## 1. Graceful Shutdown이란?

### 문제 상황

```
SIGTERM 수신 → 즉시 종료 → 진행 중인 요청 실패
```

### 해결 방법

```
SIGTERM 수신 → 새 요청 거부 → 진행 중인 요청 완료 → 종료
```

---

## 2. Spring Boot 설정

### application.yml

```yaml
server:
  shutdown: graceful  # graceful 또는 immediate (기본)

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s  # 대기 시간
```

### 효과

```
1. SIGTERM 수신
2. 새 요청 거부 (503 반환)
3. 진행 중인 요청 완료 대기 (최대 30초)
4. 프로세스 종료
```

---

## 3. Kubernetes 연동

### preStop Hook

```yaml
spec:
  template:
    spec:
      containers:
        - name: api-gateway
          lifecycle:
            preStop:
              exec:
                command:
                  - /bin/sh
                  - -c
                  - sleep 5  # LB에서 제외될 시간 확보
          terminationGracePeriodSeconds: 60
```

### 종료 순서

```
1. Pod Terminating 상태
2. Service 엔드포인트에서 제외 (Readiness 실패)
3. preStop Hook 실행 (sleep 5)
4. SIGTERM 전송
5. Graceful Shutdown (최대 30초)
6. 미종료 시 SIGKILL
```

---

## 4. 설정 조합

```yaml
# Deployment
spec:
  template:
    spec:
      terminationGracePeriodSeconds: 60  # K8s 전체 대기
      containers:
        - lifecycle:
            preStop:
              exec:
                command: ["/bin/sh", "-c", "sleep 5"]

# application.yml
server:
  shutdown: graceful
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s

# 타임라인:
# 0s:  Terminating, 엔드포인트 제외 시작
# 0-5s: preStop (sleep 5)
# 5s:  SIGTERM 수신
# 5-35s: Graceful Shutdown (30s)
# 60s: SIGKILL (미종료 시)
```

---

## 5. 검증

```bash
# Rolling Update 중 에러 확인
kubectl rollout restart deployment api-gateway -n portal-universe

# 동시에 부하 테스트
k6 run -d 60s services/load-tests/k6/scenarios/a-shopping-flow.js

# 에러율 확인 (0%여야 함)
```

---

## 다음 단계

[08-multi-zone-deployment.md](./08-multi-zone-deployment.md) - Multi-AZ 배포
