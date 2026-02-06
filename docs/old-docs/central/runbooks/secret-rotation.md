---
id: runbook-secret-rotation
title: Secret 로테이션 절차
type: runbook
status: current
created: 2026-01-19
updated: 2026-01-19
author: Laze
tags: [secret, rotation, security, kubernetes, database, jwt]
---

# Secret 로테이션 Runbook

## 개요

| 항목 | 내용 |
|------|------|
| **예상 소요 시간** | DB: 15분, JWT: 20분 |
| **필요 권한** | K8s cluster admin, DB admin |
| **영향 범위** | 전체 서비스 (Rolling Update) |
| **다운타임** | 없음 (Rolling Update 시) |

## 사전 조건

- [ ] Kubernetes 클러스터 접근 권한 확인
- [ ] 현재 Secret 백업 완료
- [ ] 모든 서비스 정상 상태 확인
- [ ] 롤백 계획 수립 완료
- [ ] 작업 시간대 확인 (가능하면 트래픽이 적은 시간)

## 현재 Secret 백업

**작업 전 반드시 현재 Secret을 백업하세요.**

```bash
# 현재 Secret 전체 백업
kubectl get secret portal-universe-secret -n portal-universe -o yaml > secret-backup-$(date +%Y%m%d-%H%M%S).yaml

# 특정 키만 백업
kubectl get secret portal-universe-secret -n portal-universe -o jsonpath='{.data.DB_PASSWORD}' | base64 -d > db-password-backup.txt
kubectl get secret portal-universe-secret -n portal-universe -o jsonpath='{.data.JWT_SECRET}' | base64 -d > jwt-secret-backup.txt
```

**예상 결과**: 백업 파일 생성 확인

---

## DB 비밀번호 로테이션

### Step 1: 새 비밀번호 생성

```bash
# 강력한 랜덤 비밀번호 생성 (32자)
NEW_DB_PASSWORD=$(openssl rand -base64 32 | tr -d "=+/" | cut -c1-32)
echo "New password generated: $NEW_DB_PASSWORD"

# 환경 변수로 저장
export NEW_DB_PASSWORD
```

**예상 결과**: 32자 길이의 랜덤 문자열 생성

### Step 2: 데이터베이스에 새 비밀번호 추가 (Dual Password)

```bash
# PostgreSQL의 경우
kubectl exec -it deployment/postgres -n portal-universe -- psql -U postgres -c "ALTER USER portal_user WITH PASSWORD '$NEW_DB_PASSWORD';"

# MySQL의 경우
# kubectl exec -it deployment/mysql -n portal-universe -- mysql -u root -p -e "ALTER USER 'portal_user'@'%' IDENTIFIED BY '$NEW_DB_PASSWORD';"
```

**예상 결과**: `ALTER ROLE` 또는 `Query OK` 메시지

### Step 3: Kubernetes Secret 업데이트

```bash
# Base64 인코딩
NEW_DB_PASSWORD_B64=$(echo -n "$NEW_DB_PASSWORD" | base64)

# Secret 패치
kubectl patch secret portal-universe-secret -n portal-universe \
  --type merge \
  -p "{\"data\":{\"DB_PASSWORD\":\"$NEW_DB_PASSWORD_B64\"}}"
```

**예상 결과**: `secret/portal-universe-secret patched`

### Step 4: 서비스별 Rolling Restart

```bash
# DB를 사용하는 서비스만 재시작
for svc in auth-service blog-service shopping-service; do
  echo "Restarting $svc..."
  kubectl rollout restart deployment/$svc -n portal-universe
  kubectl rollout status deployment/$svc -n portal-universe --timeout=5m
done
```

**예상 결과**: 각 서비스가 순차적으로 재시작되며 `successfully rolled out` 메시지

### Step 5: 연결 테스트

```bash
# Pod 로그에서 DB 연결 확인
kubectl logs -n portal-universe deployment/auth-service --tail=50 | grep -i "database\|connection"

# 헬스 체크
kubectl exec -n portal-universe deployment/auth-service -- curl -s http://localhost:8080/actuator/health | jq .
```

**예상 결과**:
- 로그에 에러 없음
- 헬스 체크 `{"status": "UP"}`

### Step 6: 이전 비밀번호 검증 (선택사항)

```bash
# 이전 비밀번호로 접속 시도 (성공해야 함)
OLD_DB_PASSWORD=$(cat db-password-backup.txt)
kubectl exec -it deployment/postgres -n portal-universe -- psql -U portal_user -c "\conninfo"
```

**예상 결과**: 아직 이전 비밀번호도 작동 (DB가 알아서 처리)

---

## JWT 키 로테이션

### Step 1: 새 JWT Secret Key 생성

```bash
# 256-bit 키 생성 (HS256용)
NEW_JWT_SECRET=$(openssl rand -base64 32)
echo "New JWT secret generated: $NEW_JWT_SECRET"

# 환경 변수로 저장
export NEW_JWT_SECRET
```

**예상 결과**: Base64 인코딩된 32바이트 키

### Step 2: Kubernetes Secret 업데이트 (이전 키 유지)

```bash
# 현재 키를 OLD_JWT_SECRET으로 백업
CURRENT_JWT_SECRET=$(kubectl get secret portal-universe-secret -n portal-universe -o jsonpath='{.data.JWT_SECRET}' | base64 -d)

# 새 키와 이전 키 모두 저장
NEW_JWT_SECRET_B64=$(echo -n "$NEW_JWT_SECRET" | base64)
OLD_JWT_SECRET_B64=$(echo -n "$CURRENT_JWT_SECRET" | base64)

kubectl patch secret portal-universe-secret -n portal-universe \
  --type merge \
  -p "{\"data\":{\"JWT_SECRET\":\"$NEW_JWT_SECRET_B64\",\"JWT_SECRET_OLD\":\"$OLD_JWT_SECRET_B64\"}}"
```

**예상 결과**: `secret/portal-universe-secret patched`

### Step 3: Auth Service 재시작

```bash
# Auth Service만 먼저 재시작
kubectl rollout restart deployment/auth-service -n portal-universe
kubectl rollout status deployment/auth-service -n portal-universe --timeout=5m
```

**예상 결과**: `deployment "auth-service" successfully rolled out`

### Step 4: 토큰 검증 테스트

```bash
# 새 토큰 발급
NEW_TOKEN=$(kubectl exec -n portal-universe deployment/auth-service -- \
  curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"testpass"}' | jq -r .data.token)

echo "New token: $NEW_TOKEN"

# 새 토큰으로 인증 테스트
kubectl exec -n portal-universe deployment/auth-service -- \
  curl -s http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer $NEW_TOKEN" | jq .
```

**예상 결과**: 정상적인 사용자 정보 반환

### Step 5: Grace Period 대기 (중요!)

**이전 토큰이 만료될 때까지 대기하세요.**

```bash
# JWT 만료 시간 확인 (보통 1시간 또는 24시간)
# 예: 1시간이면 60분 대기

echo "Waiting for grace period (60 minutes)..."
# 실제로는 기다려야 하지만, 테스트 환경에서는 건너뛸 수 있음
```

### Step 6: 이전 키 제거

```bash
# 이전 JWT_SECRET_OLD 제거
kubectl patch secret portal-universe-secret -n portal-universe \
  --type json \
  -p '[{"op": "remove", "path": "/data/JWT_SECRET_OLD"}]'
```

**예상 결과**: `secret/portal-universe-secret patched`

### Step 7: 전체 서비스 재시작 (선택사항)

```bash
# Gateway와 다른 서비스도 재시작하여 새 키 사용
for svc in api-gateway blog-service shopping-service notification-service; do
  echo "Restarting $svc..."
  kubectl rollout restart deployment/$svc -n portal-universe
done
```

---

## 전체 확인 체크리스트

로테이션 완료 후 다음을 확인하세요:

- [ ] 모든 Pod이 Running 상태
- [ ] DB 연결 정상 (로그 확인)
- [ ] JWT 발급/검증 정상
- [ ] 기존 사용자 세션 유지 (Grace Period 동안)
- [ ] 헬스 체크 모두 UP
- [ ] 에러 로그 없음

```bash
# 전체 서비스 상태 확인
kubectl get pods -n portal-universe

# 헬스 체크
for svc in auth-service blog-service shopping-service api-gateway; do
  echo "Checking $svc..."
  kubectl exec -n portal-universe deployment/$svc -- \
    curl -s http://localhost:8080/actuator/health | jq '.status'
done
```

---

## 롤백 절차

### DB 비밀번호 롤백

```bash
# 백업 파일에서 이전 비밀번호 복원
OLD_DB_PASSWORD=$(cat db-password-backup.txt)
OLD_DB_PASSWORD_B64=$(echo -n "$OLD_DB_PASSWORD" | base64)

# Secret 복원
kubectl patch secret portal-universe-secret -n portal-universe \
  --type merge \
  -p "{\"data\":{\"DB_PASSWORD\":\"$OLD_DB_PASSWORD_B64\"}}"

# 서비스 재시작
kubectl rollout restart deployment/auth-service -n portal-universe
kubectl rollout restart deployment/blog-service -n portal-universe
kubectl rollout restart deployment/shopping-service -n portal-universe
```

### JWT 키 롤백

```bash
# 백업 파일에서 이전 JWT Secret 복원
OLD_JWT_SECRET=$(cat jwt-secret-backup.txt)
OLD_JWT_SECRET_B64=$(echo -n "$OLD_JWT_SECRET" | base64)

# Secret 복원
kubectl patch secret portal-universe-secret -n portal-universe \
  --type merge \
  -p "{\"data\":{\"JWT_SECRET\":\"$OLD_JWT_SECRET_B64\"}}"

# Auth Service 재시작
kubectl rollout restart deployment/auth-service -n portal-universe
```

### 완전 복원 (비상 시)

```bash
# 전체 Secret 백업 파일에서 복원
kubectl apply -f secret-backup-YYYYMMDD-HHMMSS.yaml

# 모든 서비스 재시작
kubectl rollout restart deployment -n portal-universe --all
```

---

## 문제 발생 시

### 증상별 대응

| 증상 | 원인 | 대응 |
|------|------|------|
| Pod CrashLoopBackOff | DB 연결 실패 | DB 비밀번호 확인, 롤백 |
| 401 Unauthorized | JWT 검증 실패 | JWT Secret 확인, 롤백 |
| 로그인 불가 | Auth Service 장애 | 로그 확인, 롤백 |
| 기존 세션 끊김 | 이전 키 너무 빨리 제거 | 이전 키 복원 |

### 로그 확인

```bash
# Auth Service 로그
kubectl logs -n portal-universe deployment/auth-service --tail=100 -f

# DB 연결 에러 검색
kubectl logs -n portal-universe deployment/auth-service --tail=500 | grep -i "connection\|password\|authentication"
```

### 긴급 연락

1. 즉시 롤백 실행
2. DevOps Lead에게 보고
3. [incident-response.md](./incident-response.md) 참고

---

## 에스컬레이션

| 단계 | 담당자 | 연락처 |
|------|--------|--------|
| 1차 | DevOps Engineer | TBD |
| 2차 | DevOps Lead | TBD |
| 3차 | Security Team | TBD |

---

## 참고 자료

- [Kubernetes Secrets 관리](../guides/kubernetes-secrets.md)
- [Rollback 절차](./rollback-procedure.md)
- [Incident Response](./incident-response.md)

---

## 변경 이력

| 날짜 | 변경 내용 | 작성자 |
|------|-----------|--------|
| 2026-01-19 | 초안 작성 | DevOps Team |
