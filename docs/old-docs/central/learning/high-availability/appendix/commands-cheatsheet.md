# 명령어 치트시트

고가용성 관련 자주 사용하는 명령어 모음입니다.

---

## kubectl 기본

```bash
# Pod 상태 확인
kubectl get pods -n portal-universe
kubectl get pods -n portal-universe -o wide  # 노드 정보 포함
kubectl get pods -n portal-universe -w       # 실시간 감시

# Pod 상세 정보
kubectl describe pod <pod-name> -n portal-universe

# 로그 확인
kubectl logs <pod-name> -n portal-universe
kubectl logs <pod-name> -n portal-universe --tail=100
kubectl logs <pod-name> -n portal-universe -f  # 실시간
kubectl logs <pod-name> -n portal-universe --previous  # 이전 컨테이너

# 이벤트 확인
kubectl get events -n portal-universe --sort-by='.lastTimestamp'
kubectl get events -n portal-universe -w
```

---

## 장애 주입

```bash
# Pod 삭제
kubectl delete pod <pod-name> -n portal-universe
kubectl delete pod -l app=api-gateway -n portal-universe

# 강제 종료
kubectl delete pod <pod-name> -n portal-universe --grace-period=0 --force

# Deployment 스케일링
kubectl scale deployment <name> --replicas=0 -n portal-universe
kubectl scale deployment <name> --replicas=2 -n portal-universe

# 리소스 제한 변경 (OOM 시뮬레이션)
kubectl patch deployment redis -n portal-universe -p \
  '{"spec":{"template":{"spec":{"containers":[{"name":"redis","resources":{"limits":{"memory":"64Mi"}}}]}}}}'
```

---

## 복구

```bash
# Deployment 재시작
kubectl rollout restart deployment/<name> -n portal-universe

# Rollback
kubectl rollout undo deployment/<name> -n portal-universe
kubectl rollout undo deployment/<name> -n portal-universe --to-revision=2

# 배포 상태 확인
kubectl rollout status deployment/<name> -n portal-universe
kubectl rollout history deployment/<name> -n portal-universe
```

---

## 리소스 모니터링

```bash
# 리소스 사용량
kubectl top pods -n portal-universe
kubectl top nodes

# HPA 상태
kubectl get hpa -n portal-universe
kubectl describe hpa <name> -n portal-universe

# PDB 상태
kubectl get pdb -n portal-universe
```

---

## Kafka

```bash
# Pod 접속
kubectl exec -it kafka-0 -n portal-universe -- /bin/bash

# 토픽 목록
kafka-topics.sh --list --bootstrap-server localhost:9092

# 토픽 상세
kafka-topics.sh --describe --topic <topic> --bootstrap-server localhost:9092

# Consumer Group 확인
kafka-consumer-groups.sh --list --bootstrap-server localhost:9092
kafka-consumer-groups.sh --describe --group <group> --bootstrap-server localhost:9092

# 메시지 생산
kafka-console-producer.sh --topic <topic> --bootstrap-server localhost:9092

# 메시지 소비
kafka-console-consumer.sh --topic <topic> --from-beginning --bootstrap-server localhost:9092
```

---

## Redis

```bash
# Pod 접속
kubectl exec -it redis-0 -n portal-universe -- redis-cli

# 정보 확인
INFO
INFO memory
INFO replication
INFO clients

# Slow Log
SLOWLOG GET 10

# 설정 확인/변경
CONFIG GET maxmemory
CONFIG SET maxmemory-policy allkeys-lru
```

---

## MySQL

```bash
# Pod 접속
kubectl exec -it mysql-0 -n portal-universe -- mysql -u root -p

# 상태 확인
SHOW STATUS LIKE 'Threads_connected';
SHOW PROCESSLIST;
SHOW ENGINE INNODB STATUS;

# 슬로우 쿼리
SHOW VARIABLES LIKE 'slow_query_log';
SELECT * FROM mysql.slow_log ORDER BY query_time DESC LIMIT 10;
```

---

## Prometheus/Grafana

```bash
# 포트 포워딩
kubectl port-forward svc/prometheus 9090:9090 -n portal-universe &
kubectl port-forward svc/grafana 3000:3000 -n portal-universe &

# Prometheus API
curl -s "localhost:9090/api/v1/query?query=up" | jq
curl -s "localhost:9090/api/v1/alerts" | jq
```

---

## 부하 테스트 (k6)

```bash
# 기본 실행
k6 run script.js

# 옵션
k6 run -u 10 -d 30s script.js  # 10 VU, 30초
k6 run --out influxdb=http://localhost:8086/k6 script.js
```

---

## 유틸리티

```bash
# 실시간 모니터링
watch -n 2 'kubectl get pods -n portal-universe'

# JSON 파싱
kubectl get pods -o json | jq '.items[].metadata.name'

# 레이블 기반 선택
kubectl get pods -l app=api-gateway -n portal-universe

# 시간 측정
time kubectl rollout restart deployment/api-gateway -n portal-universe
```
