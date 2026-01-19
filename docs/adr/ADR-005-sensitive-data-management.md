---
id: ADR-005
title: 민감 데이터 관리 전략
type: adr
status: accepted
created: 2026-01-19
updated: 2026-01-19
author: Documenter Agent
decision_date: 2026-01-19
reviewers: []
tags: [security, devops, configuration]
related:
  - ADR-001
  - ADR-002
  - ADR-003
---

# ADR-005: 민감 데이터 관리 전략

## 📋 메타데이터

| 항목 | 내용 |
|------|------|
| **상태** | ✅ Accepted |
| **결정일** | 2026-01-19 |
| **검토자** | Documenter Agent |

---

## 📌 Context (배경)

### 문제 상황

Portal Universe 프로젝트에서 민감한 데이터(DB 비밀번호, API 키, DockerHub credentials 등)가 Git 저장소에 커밋되는 보안 문제가 발생했습니다.

**보안 위험**:
- 데이터베이스 접근 정보 노출
- API 키 및 서드파티 서비스 credentials 유출
- CI/CD 파이프라인 접근 토큰 노출
- 공개 저장소에서 민감 정보 접근 가능

**영향받는 파일**:
- `.env` - 로컬 개발 환경 변수
- `.env.docker` - Docker Compose 환경 변수
- `k8s/base/secret.yaml` - Kubernetes Secret 리소스

### 기술적 제약

1. **다중 환경 지원**: Local, Docker, Kubernetes 환경별로 다른 설정 필요
2. **개발자 경험**: 초기 설정이 복잡하면 온보딩이 어려움
3. **자동화 요구**: CI/CD 파이프라인에서 자동으로 민감 데이터 주입
4. **비용 고려**: 외부 Secret 관리 도구 도입 시 추가 비용 발생

---

## 🎯 Decision Drivers (결정 요인)

1. **보안**: Git에 민감 데이터가 절대 커밋되지 않아야 함
2. **개발자 경험**: 초기 설정이 간단하고 명확해야 함
3. **환경 일관성**: Local, Docker, K8s 환경에서 동일한 방식 적용
4. **비용 효율성**: 추가 인프라 비용 최소화
5. **자동화 지원**: CI/CD 파이프라인과 통합 가능
6. **유지보수성**: 민감 데이터 업데이트가 쉬워야 함

---

## 🔄 Considered Options (검토한 대안)

### Option 1: .env 파일 + .gitignore (선택됨)

**구조**:
```
.env                   # 실제 민감 데이터 (gitignored)
.env.example           # 템플릿 (Git에 커밋)
.env.docker            # Docker 환경 실제 데이터 (gitignored)
.env.docker.example    # Docker 템플릿 (Git에 커밋)
```

**적용 방법**:
```bash
# 로컬 개발
cp .env.example .env
# .env 파일 수정 (실제 비밀번호 입력)

# Docker 환경
cp .env.docker.example .env.docker
# .env.docker 파일 수정

# Kubernetes 환경
cp k8s/base/secret.yaml.example k8s/base/secret.yaml
# secret.yaml 파일 수정
kubectl apply -f k8s/base/secret.yaml
```

**장점**:
- ✅ 구현이 간단하고 직관적
- ✅ 추가 인프라 불필요 (비용 없음)
- ✅ 대부분의 개발자가 익숙한 방식
- ✅ CI/CD에서 환경 변수 주입으로 자동화 가능
- ✅ 로컬 개발 시 즉시 적용 가능

**단점**:
- ⚠️ .gitignore 설정 누락 시 커밋 위험
- ⚠️ 팀원 간 민감 데이터 공유 시 별도 채널 필요 (Slack, 1Password)
- ⚠️ 파일 분실 시 복구 어려움

**위험 완화**:
- Git pre-commit hook으로 .env 파일 커밋 차단
- 템플릿 파일에 명확한 주석 작성
- 온보딩 문서에 명확히 가이드

---

### Option 2: HashiCorp Vault

**구조**:
```
Vault Server → Secret 저장 및 관리
Application → Vault API로 Secret 조회
```

**적용 방법**:
```java
// Spring Boot
@Value("${spring.cloud.vault.token}")
private String vaultToken;

VaultTemplate vaultTemplate = new VaultTemplate(
    new VaultEndpoint("https://vault.example.com", 8200),
    new TokenAuthentication(vaultToken)
);
```

**장점**:
- ✅ 중앙 집중식 Secret 관리
- ✅ 암호화 및 접근 제어 강력
- ✅ Secret Rotation 자동화
- ✅ Audit Logging 지원

**단점**:
- ❌ 별도 인프라 운영 필요 (Vault Server)
- ❌ 학습 곡선 높음
- ❌ 로컬 개발 시 Vault 접근 필요 (복잡도 증가)
- ❌ 초기 설정 시간 소요

**평가**: 🔴 **과도한 복잡성** - 현재 프로젝트 규모에 비해 오버엔지니어링

---

### Option 3: AWS Secrets Manager / Kubernetes External Secrets

**구조**:
```
AWS Secrets Manager → Secret 저장
K8s External Secrets Operator → AWS에서 Secret 동기화
K8s Secret → Pod 환경 변수 주입
```

**적용 방법**:
```yaml
# ExternalSecret CRD
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: portal-universe-secret
spec:
  secretStoreRef:
    name: aws-secrets-manager
  target:
    name: portal-universe-secret
  data:
    - secretKey: DB_PASSWORD
      remoteRef:
        key: /portal-universe/prod/db_password
```

**장점**:
- ✅ 클라우드 네이티브 솔루션
- ✅ AWS IAM 통합 보안
- ✅ Secret Rotation 지원
- ✅ 프로덕션 환경에 적합

**단점**:
- ❌ AWS 종속성 발생
- ❌ 비용 발생 (AWS Secrets Manager: $0.40/secret/month)
- ❌ 로컬/Docker 환경에서 사용 불가
- ❌ External Secrets Operator 설치 필요

**평가**: 🟡 **프로덕션 환경 고려 시 향후 도입 검토**

---

### Option 4: Git-crypt (암호화된 Git 저장)

**구조**:
```
.env 파일을 GPG로 암호화하여 Git 커밋
팀원들은 GPG 키로 복호화
```

**적용 방법**:
```bash
# Git-crypt 설정
git-crypt init
git-crypt add-gpg-user USER_ID

# .gitattributes
.env filter=git-crypt diff=git-crypt
.env.docker filter=git-crypt diff=git-crypt
```

**장점**:
- ✅ 민감 데이터를 Git에 안전하게 저장 가능
- ✅ 팀원 간 공유 간편

**단점**:
- ❌ GPG 키 관리 필요
- ❌ CI/CD에서 GPG 키 설정 복잡
- ❌ 팀원 추가/제거 시 재암호화 필요
- ❌ Git-crypt 도구 의존성

**평가**: 🔴 **관리 복잡도 높음** - 작은 팀에게는 부담

---

## 🔍 Option 비교표

| 항목 | .env + .gitignore | Vault | AWS Secrets | Git-crypt |
|------|-------------------|-------|-------------|-----------|
| **보안** | 🟡 중간 | 🟢 강력 | 🟢 강력 | 🟢 강력 |
| **구현 난이도** | 🟢 쉬움 | 🔴 어려움 | 🟡 보통 | 🟡 보통 |
| **비용** | 🟢 무료 | 🟡 인프라 비용 | 🔴 사용량 과금 | 🟢 무료 |
| **로컬 개발** | 🟢 간편 | 🔴 복잡 | 🔴 불가 | 🟢 간편 |
| **CI/CD 통합** | 🟢 쉬움 | 🟡 보통 | 🟢 쉬움 | 🟡 보통 |
| **유지보수** | 🟢 쉬움 | 🔴 높음 | 🟡 보통 | 🟡 보통 |
| **팀 공유** | 🟡 별도 채널 | 🟢 중앙 관리 | 🟢 중앙 관리 | 🟢 Git 저장 |

---

## ✅ Decision (최종 결정)

**Option 1: .env 파일 + .gitignore 방식을 채택합니다.**

### 이유

1. **현재 프로젝트 규모에 적합**: 소규모 팀, 학습 목적 프로젝트
2. **개발자 경험 우선**: 로컬 개발 환경 설정이 간단
3. **비용 효율성**: 추가 인프라 비용 없음
4. **점진적 개선 가능**: 향후 프로덕션 환경에서 AWS Secrets Manager 도입 가능

### 구현 내역

#### 1. .gitignore 설정

```gitignore
# Environment variables (보안 정보 포함)
.env
.env.local
.env.*.local
.env.docker

# Kubernetes Secrets (민감 정보 포함)
k8s/base/secret.yaml

# Local Development Data (Docker Compose)
localstack_data/
logs/
test-results/

# Monitoring Data (Prometheus, Grafana)
monitoring/grafana-data/
monitoring/prometheus-data/
```

#### 2. 템플릿 파일 제공

**`.env.example`** (로컬 개발용):
```bash
# ===================================================================
# Environment Template - Local Development
# ===================================================================
# Copy this file to .env and fill in the values
# cp .env.example .env
#
# WARNING: .env should NEVER be committed to Git!
# ===================================================================

# --- Database Credentials ---
MYSQL_ROOT_PASSWORD=your-secure-root-password
MYSQL_PASSWORD=your-secure-password
MONGO_PASSWORD=your-secure-password

# --- Grafana Admin ---
GF_SECURITY_ADMIN_USER=admin
GF_SECURITY_ADMIN_PASSWORD=your-secure-grafana-password
```

**`.env.docker.example`** (Docker Compose용):
```bash
# ===================================================================
# Environment Template - Docker Compose
# ===================================================================
# Copy this file to .env.docker and fill in the values
# cp .env.docker.example .env.docker
#
# WARNING: .env.docker should NEVER be committed to Git!
# ===================================================================

# --- MySQL Configuration ---
MYSQL_ROOT_PASSWORD=your-secure-root-password
MYSQL_USER=portal_user
MYSQL_PASSWORD=your-secure-password

# --- MongoDB Configuration ---
MONGO_INITDB_ROOT_USERNAME=admin
MONGO_INITDB_ROOT_PASSWORD=your-secure-password

# --- Grafana Configuration ---
GF_SECURITY_ADMIN_USER=admin
GF_SECURITY_ADMIN_PASSWORD=your-secure-grafana-password
```

**`k8s/base/secret.yaml.example`** (Kubernetes용):
```yaml
# ===================================================================
# Secret Template - Kubernetes
# ===================================================================
# Copy this file to secret.yaml and fill in the values
# cp secret.yaml.example secret.yaml
#
# WARNING: secret.yaml should NEVER be committed to Git!
# ===================================================================
apiVersion: v1
kind: Secret
metadata:
  name: portal-universe-secret
  namespace: portal-universe
type: Opaque

stringData:
  # --- Database Credentials ---
  MYSQL_ROOT_PASSWORD: "your-secure-root-password"
  MYSQL_PASSWORD: "your-secure-password"
  MONGO_PASSWORD: "your-secure-password"

  # --- CI/CD Docker Hub Credentials ---
  # Get from: https://hub.docker.com/settings/security
  DOCKERHUB_USERNAME: "your-dockerhub-username"
  DOCKERHUB_TOKEN: "your-dockerhub-access-token"
```

#### 3. Docker Compose에서 환경 변수 사용

```yaml
services:
  mysql-db:
    image: mysql:8.0
    env_file:
      - .env.docker  # 실제 환경 변수 파일
    environment:
      - MYSQL_DATABASE=auth_db, shopping_db
      - MYSQL_ROOT_HOST=%

  auth-service:
    build:
      context: .
      dockerfile: ./services/auth-service/Dockerfile
    env_file:
      - .env.docker
    environment:
      - SPRING_PROFILES_ACTIVE=docker
```

#### 4. Kubernetes Secret 적용

```bash
# Secret 생성
kubectl apply -f k8s/base/secret.yaml

# Secret 확인
kubectl get secret portal-universe-secret -n portal-universe

# Deployment에서 Secret 사용
env:
  - name: MYSQL_PASSWORD
    valueFrom:
      secretKeyRef:
        name: portal-universe-secret
        key: MYSQL_PASSWORD
```

---

## 📊 Consequences (영향)

### 긍정적 영향

1. **보안 개선**
   - ✅ Git 저장소에 민감 데이터 커밋 방지
   - ✅ .gitignore로 자동 제외
   - ✅ 템플릿 파일에 명확한 경고 메시지

2. **개발자 경험 향상**
   - ✅ 로컬 환경 설정 간단 (`cp .env.example .env`)
   - ✅ 템플릿 파일로 필요한 변수 명확히 제시
   - ✅ 환경별(Local, Docker, K8s) 일관된 방식

3. **CI/CD 자동화**
   - ✅ GitHub Actions에서 환경 변수로 주입 가능
   - ✅ Kubernetes Secret으로 배포 자동화

4. **비용 절감**
   - ✅ 추가 인프라 비용 없음
   - ✅ 외부 Secret 관리 도구 불필요

### 부정적 영향 (트레이드오프)

1. **관리 부담**
   - ⚠️ 팀원 온보딩 시 민감 데이터를 별도로 공유해야 함 (Slack, 1Password)
   - ⚠️ 민감 데이터 업데이트 시 팀원들에게 개별 전달 필요

2. **실수 가능성**
   - ⚠️ .gitignore 설정 누락 시 커밋 위험
   - ⚠️ 템플릿 파일 대신 실제 파일을 수정할 가능성

3. **복구 어려움**
   - ⚠️ 로컬 .env 파일 분실 시 재설정 필요
   - ⚠️ 백업 없이 Secret 삭제 시 복구 불가

### 위험 완화 방안

1. **Pre-commit Hook 추가**
```bash
# .git/hooks/pre-commit
#!/bin/bash
if git diff --cached --name-only | grep -E '\.env$|\.env\.docker$|secret\.yaml$'; then
  echo "❌ ERROR: Attempting to commit sensitive files!"
  echo "   Files like .env, .env.docker, secret.yaml should NOT be committed."
  exit 1
fi
```

2. **온보딩 문서 강화**
   - `docs/guides/local-development-setup.md`에 명확한 가이드 작성
   - 템플릿 파일 복사 및 수정 절차 명시

3. **Secret 백업 권장**
   - 개인 Password Manager (1Password, LastPass) 사용 권장
   - 팀 공유 Secret은 보안 채널(Slack Private Message, 암호화된 문서)을 통해 전달

---

## 🔗 다음 단계

### 단기 (현재 프로젝트)

1. ✅ .gitignore 설정 완료
2. ✅ 템플릿 파일 생성 완료
3. ✅ docker-compose.yml 환경 변수 적용 완료
4. ⬜ Pre-commit hook 추가
5. ⬜ 온보딩 문서 작성 (`docs/guides/local-development-setup.md`)

### 중기 (향후 3-6개월)

1. ⬜ CI/CD 파이프라인에서 환경 변수 주입 자동화
2. ⬜ GitHub Actions Secrets 활용
3. ⬜ Secret Rotation 정책 수립

### 장기 (프로덕션 환경)

1. ⬜ AWS Secrets Manager 도입 검토
2. ⬜ Kubernetes External Secrets Operator 적용
3. ⬜ Secret Rotation 자동화

---

## 📚 참고 자료

- [12-Factor App: Config](https://12factor.net/config)
- [OWASP: Secrets Management Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html)
- [Kubernetes: Secrets](https://kubernetes.io/docs/concepts/configuration/secret/)
- [Docker: Environment variables in Compose](https://docs.docker.com/compose/environment-variables/)

---

## 🔗 관련 문서

- [ADR-001: Admin 컴포넌트 구조](./ADR-001-admin-component-structure.md)
- [ADR-002: Admin API 엔드포인트 설계](./ADR-002-api-endpoint-design.md)
- [ADR-003: Admin 권한 검증 전략](./ADR-003-authorization-strategy.md)
- [로컬 개발 환경 설정 가이드](../guides/local-development-setup.md) (작성 예정)

---

**최종 업데이트**: 2026-01-19
**작성자**: Documenter Agent
**검토자**: -
