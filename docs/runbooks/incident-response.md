---
id: runbook-incident-response
title: 장애 대응 매뉴얼
type: runbook
status: current
created: 2026-01-19
updated: 2026-01-19
author: DevOps Team
tags: [incident, response, production]
---

# 장애 대응 Runbook

## 개요

| 항목 | 내용 |
|------|------|
| **필요 권한** | K8s cluster admin, Monitoring access |
| **영향 범위** | 상황에 따라 상이 |

## 심각도 분류

| 레벨 | 정의 | 대응 시간 | 에스컬레이션 |
|------|------|----------|-------------|
| P1 (Critical) | 서비스 전체 중단 | 즉시 | 즉시 전체 팀 |
| P2 (High) | 핵심 기능 장애 | 15분 내 | 30분 내 리더 |
| P3 (Medium) | 일부 기능 영향 | 1시간 내 | 필요시 |
| P4 (Low) | 경미한 이슈 | 업무 시간 내 | 불필요 |

## 장애 인지

### 모니터링 확인

```bash
# Pod 상태 확인
kubectl get pods -n portal-universe

# 최근 이벤트 확인
kubectl get events -n portal-universe --sort-by='.lastTimestamp' | tail -20

# 서비스 로그 확인
kubectl logs -n portal-universe -l app=<service-name> --tail=100
```

### 주요 메트릭 확인

- **Response Time**: 평균 응답 시간 급증
- **Error Rate**: 5xx 에러율 증가
- **CPU/Memory**: 리소스 사용량 급증
- **Pod Status**: CrashLoopBackOff, OOMKilled

## 장애 유형별 대응

### 1. Pod CrashLoopBackOff

**증상**: Pod이 반복적으로 재시작됨

```bash
# 상세 상태 확인
kubectl describe pod <pod-name> -n portal-universe

# 이전 컨테이너 로그 확인
kubectl logs <pod-name> -n portal-universe --previous

# 리소스 확인
kubectl top pod <pod-name> -n portal-universe
```

**대응**:
1. 로그에서 원인 파악
2. OOM이면 리소스 limit 조정
3. 애플리케이션 에러면 롤백 검토

### 2. 서비스 응답 없음

**증상**: API 호출 timeout 또는 502/503 에러

```bash
# 서비스 엔드포인트 확인
kubectl get endpoints <service-name> -n portal-universe

# Pod 상태 확인
kubectl get pods -n portal-universe -l app=<service-name> -o wide

# 네트워크 정책 확인
kubectl get networkpolicies -n portal-universe
```

**대응**:
1. Pod이 Running 상태인지 확인
2. Service Endpoint가 연결되어 있는지 확인
3. 필요시 Pod 재시작

### 3. 데이터베이스 연결 장애

**증상**: DB 연결 에러, Connection Pool 고갈

```bash
# DB 연결 상태 확인 (MySQL)
kubectl exec -it <mysql-pod> -n portal-universe -- \
  mysql -u root -p -e "SHOW PROCESSLIST"

# 애플리케이션 로그에서 DB 에러 확인
kubectl logs -n portal-universe -l app=shopping-service | grep -i "connection"
```

**대응**:
1. DB 서버 상태 확인
2. Connection Pool 설정 검토
3. 쿼리 성능 이슈 확인

### 4. Kafka 연결 장애

**증상**: 메시지 발행/소비 실패

```bash
# Kafka Pod 상태 확인
kubectl get pods -n portal-universe -l app=kafka

# Consumer Group 상태 확인
kubectl exec -it kafka-0 -n portal-universe -- \
  kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --all-groups
```

**대응**:
1. Kafka broker 상태 확인
2. Topic 존재 여부 확인
3. Consumer lag 확인

### 5. Redis 장애

**증상**: 캐시 조회 실패, 세션 유실

```bash
# Redis 상태 확인
kubectl exec -it redis-0 -n portal-universe -- redis-cli ping

# 메모리 사용량 확인
kubectl exec -it redis-0 -n portal-universe -- redis-cli info memory
```

**대응**:
1. Redis 서버 상태 확인
2. 메모리 사용량 확인
3. 필요시 캐시 플러시

## 공통 긴급 조치

### Pod 강제 재시작

```bash
kubectl rollout restart deployment/<deployment-name> -n portal-universe
```

### 특정 Pod 삭제 (재생성)

```bash
kubectl delete pod <pod-name> -n portal-universe
```

### 트래픽 차단 (비상 시)

```bash
kubectl scale deployment/<deployment-name> -n portal-universe --replicas=0
```

## 롤백

문제가 최근 배포로 인한 것이라면:

```bash
kubectl rollout undo deployment/<deployment-name> -n portal-universe
```

상세 절차: [rollback-procedure.md](./rollback-procedure.md)

## 사후 조치

### 1. 장애 리포트 작성

`docs/troubleshooting/YYYY/MM/TS-YYYYMMDD-XXX-[title].md` 작성

### 2. RCA (Root Cause Analysis)

- 근본 원인 분석
- 재발 방지책 수립

### 3. 개선 사항 반영

- 모니터링/알람 추가
- 프로세스 개선

## 에스컬레이션

| 역할 | 담당자 | 연락처 | 호출 조건 |
|------|--------|--------|----------|
| DevOps Lead | TBD | - | P1, P2 |
| Backend Lead | TBD | - | Backend 관련 |
| Frontend Lead | TBD | - | Frontend 관련 |
| DBA | TBD | - | DB 관련 |
