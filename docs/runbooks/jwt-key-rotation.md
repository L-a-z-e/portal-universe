# JWT Secret Key 교체 가이드

Portal Universe 프로젝트의 JWT Secret Key 교체 절차를 설명합니다.

## 목차

- [개요](#개요)
- [키 교체 전략](#키-교체-전략)
- [환경별 키 관리](#환경별-키-관리)
- [키 교체 절차](#키-교체-절차)
- [트러블슈팅](#트러블슈팅)

---

## 개요

### 왜 키 교체가 필요한가?

JWT Secret Key는 토큰의 서명과 검증에 사용되는 민감한 정보입니다. 정기적인 키 교체는 다음과 같은 보안 이점을 제공합니다:

- **보안 강화**: 키 노출 위험 최소화
- **규정 준수**: 보안 정책 및 컴플라이언스 요구사항 충족
- **피해 최소화**: 키 유출 시 영향 범위 제한

### 다중 키 지원 구조

Portal Universe는 **kid(Key ID)** 기반 다중 키 관리를 지원합니다:

```yaml
jwt:
  current-key-id: key-2026-01  # 현재 토큰 생성에 사용할 키
  keys:
    key-2026-01:  # 현재 키
      secret-key: ${JWT_SECRET_KEY_2024_01}
      activated-at: 2026-01-01T00:00:00
    key-2025-12:  # 이전 키 (검증용)
      secret-key: ${JWT_SECRET_KEY_2023_12}
      activated-at: 2023-12-01T00:00:00
      expires-at: 2024-02-01T00:00:00  # 2개월 후 만료
```

**작동 방식:**

1. **토큰 생성**: `current-key-id`에 해당하는 키로 서명하며, JWT 헤더에 `kid` 포함
2. **토큰 검증**: JWT 헤더의 `kid`를 확인하여 적절한 키로 검증
3. **키 만료**: `expires-at`이 지나면 검증 실패 (새로운 토큰 생성 불가)

---

## 키 교체 전략

### 교체 주기

| 환경 | 권장 주기 | 비고 |
|------|----------|------|
| Production | 3~6개월 | 보안 정책에 따라 조정 |
| Staging | 6개월 | Production과 동일한 절차로 테스트 |
| Development | 1년 | 자주 교체할 필요 없음 |

### 이전 키 유지 기간

**최소 유지 기간**: Refresh Token 만료 기간 + 버퍼

```
Refresh Token 만료: 7일
버퍼: 7~14일
총 유지 기간: 14~21일 권장
```

**예시:**

- 2024년 1월 1일: 새 키 활성화
- 2024년 1월 22일: 이전 키 만료 설정
- 2024년 2월 1일: 이전 키 제거

---

## 환경별 키 관리

### Local 환경

**파일**: `services/auth-service/src/main/resources/application-local.yml`

```yaml
# application-local.yml에 직접 설정 (개발용)
jwt:
  current-key-id: key-default
  keys:
    key-default:
      secret-key: "your-local-development-secret-key-min-32-chars"
      activated-at: 2026-01-01T00:00:00
```

**변경 방법**: 파일 직접 수정 후 애플리케이션 재시작

---

### Docker 환경

**파일**: `.env.docker`

```bash
# .env.docker 파일 생성/수정
JWT_CURRENT_KEY_ID=key-2026-01
JWT_SECRET_KEY_2024_01=새로운256비트시크릿키
JWT_SECRET_KEY_2023_12=이전256비트시크릿키  # 검증용
```

**변경 절차:**

```bash
# 1. 새 키 생성
openssl rand -base64 32

# 2. .env.docker 업데이트
vim .env.docker

# 3. 컨테이너 재시작
docker-compose down
docker-compose up -d
```

---

### Kubernetes 환경

**파일**: `k8s/base/jwt-secrets.yaml`

⚠️ **주의**: 이 파일은 `.gitignore`에 포함되어 있으며, Git에 커밋하지 마세요!

**변경 절차:**

```bash
# 1. 새 키 생성
NEW_KEY=$(openssl rand -base64 32)
echo "새 키: $NEW_KEY"

# 2. jwt-secrets.yaml 업데이트
vim k8s/base/jwt-secrets.yaml

# 예시:
apiVersion: v1
kind: Secret
metadata:
  name: jwt-secrets
  namespace: default
type: Opaque
stringData:
  JWT_CURRENT_KEY_ID: "key-2026-02"  # 새 키 ID로 변경
  JWT_SECRET_KEY_2024_02: "새로운키"
  JWT_SECRET_KEY_2024_01: "이전키"  # 검증용 유지

# 3. Secret 업데이트
kubectl apply -f k8s/base/jwt-secrets.yaml

# 4. ConfigMap 업데이트 (만료 시간 설정)
kubectl edit configmap jwt-rotation-config

# 5. 롤링 배포
kubectl rollout restart deployment/auth-service -n portal-universe
kubectl rollout restart deployment/api-gateway -n portal-universe

# 6. 배포 상태 확인
kubectl rollout status deployment/auth-service -n portal-universe
kubectl rollout status deployment/api-gateway -n portal-universe
```

---

## 키 교체 절차

### 단계별 가이드

#### 1. 준비 단계

**새 키 생성:**

```bash
# 256-bit (32 bytes) 키 생성
openssl rand -base64 32

# 출력 예시:
# YxZ1mK3pN7qR8sT4vW5xY6zA1bC2dE3fG4hI5jK6lM7=
```

**키 ID 규칙:**

```
key-YYYY-MM 형식 권장
예: key-2026-01, key-2026-02
```

#### 2. 설정 업데이트

**Auth Service & API Gateway 설정:**

```yaml
jwt:
  current-key-id: key-2026-02  # 새 키 ID
  keys:
    key-2026-02:  # 새 키 추가
      secret-key: ${JWT_SECRET_KEY_2024_02}
      activated-at: 2024-02-01T00:00:00
    key-2026-01:  # 이전 키 유지
      secret-key: ${JWT_SECRET_KEY_2024_01}
      activated-at: 2026-01-01T00:00:00
      expires-at: 2024-02-22T00:00:00  # 3주 후 만료
```

#### 3. 배포 순서

**중요**: 반드시 이 순서를 따르세요!

```
1. API Gateway 배포
   ↓
2. Auth Service 배포
```

**이유:**

- Gateway가 먼저 새 키를 인식해야 Auth Service의 새 토큰을 검증 가능
- 반대로 배포하면 새로운 토큰 검증 실패

**Kubernetes 배포:**

```bash
# 1. API Gateway 배포
kubectl rollout restart deployment/api-gateway -n portal-universe
kubectl rollout status deployment/api-gateway -n portal-universe

# 2. Auth Service 배포
kubectl rollout restart deployment/auth-service -n portal-universe
kubectl rollout status deployment/auth-service -n portal-universe
```

**Docker 배포:**

```bash
# 1. API Gateway 재시작
docker-compose restart api-gateway

# 2. 헬스 체크 확인
curl http://localhost:8080/actuator/health

# 3. Auth Service 재시작
docker-compose restart auth-service

# 4. 헬스 체크 확인
curl http://localhost:8081/actuator/health
```

#### 4. 검증

**새 토큰 발급 테스트:**

```bash
# 1. 로그인 요청
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password"
  }'

# 2. JWT 토큰 헤더 확인
# 응답에서 access_token을 복사하여 jwt.io에서 디코딩
# 헤더에 "kid": "key-2026-02" 확인
```

**기존 토큰 검증 테스트:**

```bash
# 이전 키로 발급된 토큰도 여전히 유효한지 확인
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer OLD_ACCESS_TOKEN"

# 200 OK 응답 확인
```

**로그 확인:**

```bash
# Kubernetes
kubectl logs deployment/auth-service -n portal-universe | grep "key ID"

# 출력 예시:
# Generating access token with key ID: key-2026-02
# Validating access token with key ID: key-2026-01  # 이전 토큰도 검증됨
```

#### 5. 이전 키 만료 설정

**3주 후 (Refresh Token 만료 + 버퍼):**

```yaml
jwt:
  keys:
    key-2026-02:
      secret-key: ${JWT_SECRET_KEY_2024_02}
      activated-at: 2024-02-01T00:00:00
    key-2026-01:
      secret-key: ${JWT_SECRET_KEY_2024_01}
      activated-at: 2026-01-01T00:00:00
      expires-at: 2024-02-22T00:00:00  # 만료 시간 설정
```

#### 6. 이전 키 제거

**만료 시간이 지난 후:**

```yaml
jwt:
  current-key-id: key-2026-02
  keys:
    key-2026-02:
      secret-key: ${JWT_SECRET_KEY_2024_02}
      activated-at: 2024-02-01T00:00:00
    # key-2026-01은 제거
```

**Kubernetes Secret 정리:**

```bash
# jwt-secrets.yaml에서 이전 키 제거
kubectl apply -f k8s/base/jwt-secrets.yaml

# ConfigMap 정리
kubectl edit configmap jwt-rotation-config
```

---

## 트러블슈팅

### 문제 1: "JWT key not found for ID: key-2026-02"

**원인**: Gateway/Auth Service가 새 키를 인식하지 못함

**해결 방법:**

```bash
# 1. Secret/ConfigMap 확인
kubectl get secret jwt-secrets -o yaml
kubectl describe configmap jwt-rotation-config

# 2. 환경 변수 확인
kubectl exec deployment/auth-service -n portal-universe -- env | grep JWT

# 3. 설정 파일 확인
kubectl exec deployment/auth-service -n portal-universe -- \
  cat /workspace/BOOT-INF/classes/application-kubernetes.yml
```

### 문제 2: "JWT key is expired: key-2025-12"

**원인**: 만료된 키로 발급된 토큰 사용

**해결 방법:**

```bash
# 사용자에게 재로그인 요청 (새 토큰 발급)
# 또는 expires-at 시간을 일시적으로 연장
```

### 문제 3: 배포 후 모든 토큰이 무효화됨

**원인**: 배포 순서가 잘못되었거나, 이전 키가 설정에서 누락됨

**해결 방법:**

```bash
# 1. 이전 설정으로 롤백
kubectl rollout undo deployment/auth-service -n portal-universe
kubectl rollout undo deployment/api-gateway -n portal-universe

# 2. 설정 수정 후 올바른 순서로 재배포
```

### 문제 4: Gateway와 Auth Service의 키가 불일치

**증상**: 401 Unauthorized 에러

**해결 방법:**

```bash
# 1. 두 서비스의 환경 변수 비교
kubectl exec deployment/api-gateway -n portal-universe -- env | grep JWT
kubectl exec deployment/auth-service -n portal-universe -- env | grep JWT

# 2. 불일치 시 Secret 재적용
kubectl apply -f k8s/base/jwt-secrets.yaml
kubectl rollout restart deployment/api-gateway -n portal-universe
kubectl rollout restart deployment/auth-service -n portal-universe
```

---

## 체크리스트

### 키 교체 전

- [ ] 새 키 생성 (256-bit 이상)
- [ ] 키 ID 결정 (key-YYYY-MM 형식)
- [ ] 이전 키 만료 시간 계산 (Refresh Token 만료 + 14일)
- [ ] 백업 생성 (현재 Secret/ConfigMap)

### 키 교체 중

- [ ] Secret/환경 변수 업데이트
- [ ] API Gateway 먼저 배포
- [ ] Auth Service 배포
- [ ] 헬스 체크 확인
- [ ] 새 토큰 발급 테스트
- [ ] 기존 토큰 검증 테스트
- [ ] 로그 확인

### 키 교체 후

- [ ] 모니터링 (Grafana 대시보드)
- [ ] 에러 로그 확인
- [ ] 사용자 피드백 모니터링
- [ ] 3주 후 이전 키 만료 설정
- [ ] 만료 후 이전 키 제거

---

## 참고 자료

- [JWT Best Practices](https://tools.ietf.org/html/rfc8725)
- [JwtProperties.java](/services/auth-service/src/main/java/com/portal/universe/authservice/config/JwtProperties.java)
- [TokenService.java](/services/auth-service/src/main/java/com/portal/universe/authservice/service/TokenService.java)
- [Kubernetes Secret 관리](/k8s/base/jwt-secrets.yaml.example)

---

## 문의

JWT 키 교체 관련 문의사항은 DevOps 팀으로 연락주세요.
