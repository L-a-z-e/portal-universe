# 환경 변수 설정 가이드

## 사전 요구사항

- Git
- 텍스트 에디터 (vim, nano, VSCode 등)
- Base64 인코딩 도구 (Kubernetes Secret 사용 시)

## 빠른 시작

```bash
# 1. 로컬 개발용 환경 변수 설정
cp .env.local.example .env.local
vi .env.local  # 또는 원하는 에디터로 편집

# 2. Docker Compose용 환경 변수 설정
cp .env.docker.example .env.docker
vi .env.docker

# 3. Kubernetes용 Secret 설정
cp k8s/base/secret.yaml.example k8s/base/secret.yaml
vi k8s/base/secret.yaml
```

> **⚠️ 중요**: 생성한 `.env.local`, `.env.docker`, `secret.yaml` 파일은 Git에 커밋되지 않습니다. 이미 `.gitignore`에 등록되어 있습니다.

## 환경별 설정

### 1. 로컬 개발 (IDE/로컬 실행)

**파일**: `.env.local`

**용도**: IDE에서 직접 서비스를 실행하거나 로컬 테스트 시 사용

```bash
# .env.local.example을 복사하여 생성
cp .env.local.example .env.local
```

**Spring Boot 프로필**: `local` (기본값)

### 2. Docker Compose 환경

**파일**: `.env.docker`

**용도**: Docker Compose로 전체 스택을 실행할 때 사용

```bash
# .env.docker.example을 복사하여 생성
cp .env.docker.example .env.docker
```

**Spring Boot 프로필**: `docker`

**참조 서비스** (`docker-compose.yml`에서 `env_file: .env.docker` 사용):
- `mysql-db`
- `mongodb`
- `auth-service`
- `grafana`

### 3. Kubernetes 환경

**파일**: `k8s/base/secret.yaml`

**용도**: Kubernetes 클러스터에 배포할 때 Secret으로 사용

```bash
# 템플릿을 복사하여 생성
cp k8s/base/secret.yaml.example k8s/base/secret.yaml

# Secret 적용
kubectl apply -f k8s/base/secret.yaml
```

**Spring Boot 프로필**: `k8s`

**Secret 이름**: `portal-universe-secret` (네임스페이스: `portal-universe`)

## 환경 변수 목록

### 데이터베이스 인증 정보

| 변수명 | 설명 | 필수 | 기본값 | 사용 환경 |
|--------|------|------|--------|-----------|
| `MYSQL_ROOT_PASSWORD` | MySQL root 계정 비밀번호 | ✅ | - | Docker, K8s |
| `MYSQL_PASSWORD` | MySQL 일반 사용자 비밀번호 | ✅ | - | Docker, K8s |
| `MONGO_PASSWORD` | MongoDB 비밀번호 | ✅ | - | Docker, K8s |

**사용 서비스**:
- MySQL: `auth-service`, `shopping-service`, `notification-service`
- MongoDB: `blog-service`

### OAuth2 인증 정보

| 변수명 | 설명 | 필수 | 기본값 | 사용 환경 |
|--------|------|------|--------|-----------|
| `GOOGLE_CLIENT_ID` | Google OAuth2 클라이언트 ID | ❌ | - | 로컬, Docker, K8s |
| `GOOGLE_CLIENT_SECRET` | Google OAuth2 클라이언트 Secret | ❌ | - | 로컬, Docker, K8s |

**사용 서비스**: `auth-service`

**발급 방법**:
1. [Google Cloud Console](https://console.cloud.google.com/) 접속
2. 프로젝트 생성 또는 선택
3. API 및 서비스 > OAuth 동의 화면 설정
4. 사용자 인증 정보 > OAuth 2.0 클라이언트 ID 생성
5. 승인된 리디렉션 URI 추가:
   - 로컬: `http://localhost:8081/login/oauth2/code/google`
   - Docker: `https://localhost:30000/auth-service/login/oauth2/code/google`
   - K8s: `https://your-domain.com/auth-service/login/oauth2/code/google`

> **참고**: OAuth2 설정은 선택 사항입니다. 설정하지 않으면 기본 username/password 인증만 사용됩니다.

### Grafana 인증 정보

| 변수명 | 설명 | 필수 | 기본값 | 사용 환경 |
|--------|------|--------|--------|-----------|
| `GF_SECURITY_ADMIN_PASSWORD` | Grafana 관리자 비밀번호 | ✅ | `password` | Docker, K8s |

**기본 계정**: `admin` / `password` (기본값)

**접속 URL**:
- Docker: http://localhost:3000
- K8s: `http://<node-ip>:32000`

### CI/CD 인증 정보

| 변수명 | 설명 | 필수 | 기본값 | 사용 환경 |
|--------|------|------|--------|-----------|
| `DOCKERHUB_USERNAME` | Docker Hub 사용자명 | ✅ | - | K8s (CI/CD) |
| `DOCKERHUB_TOKEN` | Docker Hub Access Token | ✅ | - | K8s (CI/CD) |

**사용 목적**: GitHub Actions에서 Docker 이미지 빌드 및 푸시

**발급 방법**:
1. [Docker Hub](https://hub.docker.com/) 로그인
2. Account Settings > Security > New Access Token
3. 토큰 생성 후 안전한 곳에 저장 (한 번만 표시됨)

## 보안 주의사항

### ✅ DO

1. **템플릿 파일 사용**
   ```bash
   # 항상 .example 파일을 복사해서 사용
   cp .env.local.example .env.local
   ```

2. **민감 정보 별도 관리**
   - 프로덕션 환경 변수는 팀 내 보안 도구 사용 (1Password, AWS Secrets Manager 등)
   - Slack DM 또는 암호화된 채널로 공유

3. **강력한 비밀번호 사용**
   ```bash
   # 예: 무작위 비밀번호 생성
   openssl rand -base64 32
   ```

4. **커밋 전 확인**
   ```bash
   # .gitignore가 제대로 작동하는지 확인
   git status
   # .env, .env.docker, secret.yaml이 나타나면 안 됨
   ```

### ❌ DON'T

1. **절대 커밋하지 마세요**
   - `.env.local`
   - `.env.docker`
   - `k8s/base/secret.yaml`
   - 민감 정보가 포함된 모든 파일

2. **공개 채널에 공유하지 마세요**
   - GitHub Issues
   - 공개 Slack 채널
   - 스크린샷 (민감 정보 포함 시)

3. **기본값 그대로 사용하지 마세요**
   - 프로덕션 환경에서는 강력한 비밀번호로 변경 필수

### Pre-commit Hook 설정 (권장)

실수로 민감 정보를 커밋하는 것을 방지:

```bash
# .git/hooks/pre-commit 파일 생성
cat > .git/hooks/pre-commit << 'EOF'
#!/bin/sh
if git diff --cached --name-only | grep -E "^\.env$|^\.env\.docker$|^k8s/base/secret\.yaml$"; then
    echo "❌ Error: Attempting to commit sensitive files!"
    echo "Files blocked: .env, .env.docker, k8s/base/secret.yaml"
    exit 1
fi
EOF

# 실행 권한 부여
chmod +x .git/hooks/pre-commit
```

## 설정 예시

### .env.local (로컬 개발)

```bash
# OAuth2 (선택 사항)
GOOGLE_CLIENT_ID=123456789-abcdefg.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=GOCSPX-xxxxxxxxxxxxx
```

### .env.docker (Docker Compose)

```bash
# Database Credentials
MYSQL_ROOT_PASSWORD=secureRootPass123!
MYSQL_PASSWORD=secureUserPass123!
MONGO_INITDB_ROOT_USERNAME=admin
MONGO_INITDB_ROOT_PASSWORD=secureMongoPass123!

# Grafana
GF_SECURITY_ADMIN_PASSWORD=admin123!

# OAuth2 (선택 사항)
GOOGLE_CLIENT_ID=123456789-abcdefg.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=GOCSPX-xxxxxxxxxxxxx
```

### k8s/base/secret.yaml (Kubernetes)

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: portal-universe-secret
  namespace: portal-universe
type: Opaque

stringData:
  # Database Credentials
  MYSQL_ROOT_PASSWORD: "secureRootPass123!"
  MYSQL_PASSWORD: "secureUserPass123!"
  MONGO_PASSWORD: "secureMongoPass123!"

  # CI/CD Docker Hub Credentials
  DOCKERHUB_USERNAME: "your-dockerhub-username"
  DOCKERHUB_TOKEN: "dckr_pat_xxxxxxxxxxxxx"
```

> **참고**: Kubernetes Secret의 `stringData` 필드는 자동으로 Base64 인코딩됩니다.

## 트러블슈팅

### 1. 환경 변수가 인식되지 않음

**증상**:
```
Error: Could not resolve placeholder 'MYSQL_PASSWORD'
```

**해결 방법**:

```bash
# 1. 파일 존재 확인
ls -la .env.docker

# 2. docker-compose.yml에서 env_file 설정 확인
grep -A 2 "env_file:" docker-compose.yml

# 3. 파일 형식 확인 (Windows 줄바꿈 문제)
file .env.docker
# 출력: .env.docker: ASCII text

# 4. 컨테이너 내부에서 환경 변수 확인
docker exec auth-service env | grep MYSQL_PASSWORD
```

### 2. 권한 거부 (Permission denied)

**증상**:
```
bash: .env: Permission denied
```

**해결 방법**:

```bash
# 읽기 권한 부여
chmod 600 .env.local
chmod 600 .env.docker
chmod 600 k8s/base/secret.yaml

# 소유권 확인
ls -l .env.local
```

### 3. 템플릿 파일을 찾을 수 없음

**증상**:
```
cp: .env.example: No such file or directory
```

**해결 방법**:

```bash
# 1. 현재 위치 확인
pwd
# 프로젝트 루트 디렉토리에 있어야 함: /path/to/portal-universe

# 2. 프로젝트 루트로 이동
cd /Users/laze/Laze/Project/portal-universe

# 3. 템플릿 파일 존재 확인
ls -la *.example
ls -la k8s/base/*.example
```

### 4. Kubernetes Secret이 적용되지 않음

**증상**:
```
Error: secret "portal-universe-secret" not found
```

**해결 방법**:

```bash
# 1. Secret 적용 확인
kubectl get secret portal-universe-secret -n portal-universe

# 2. Secret이 없으면 생성
kubectl apply -f k8s/base/secret.yaml

# 3. Secret 내용 확인 (디버깅 시에만)
kubectl get secret portal-universe-secret -n portal-universe -o yaml

# 4. Pod 재시작 (Secret 변경 후)
kubectl rollout restart deployment -n portal-universe
```

### 5. OAuth2 인증 실패

**증상**:
```
[invalid_client] Invalid client or Invalid client credentials
```

**해결 방법**:

```bash
# 1. 환경 변수 확인
echo $GOOGLE_CLIENT_ID
echo $GOOGLE_CLIENT_SECRET

# 2. Google Cloud Console에서 리디렉션 URI 확인
# - 정확한 프로토콜 (http/https)
# - 정확한 포트 번호
# - 정확한 경로 (/login/oauth2/code/google)

# 3. 컨테이너 로그 확인
docker-compose logs -f auth-service | grep -i oauth
```

### 6. Docker Compose에서 환경 변수가 보이지 않음

**증상**:
서비스는 실행되지만 환경 변수가 적용되지 않음

**해결 방법**:

```bash
# 1. .env.docker 파일 형식 검증
cat -A .env.docker
# 각 줄 끝에 ^M (Windows 줄바꿈)이 있으면 안 됨

# 2. 줄바꿈 문자 변환 (필요시)
dos2unix .env.docker

# 3. Docker Compose 설정 검증
docker-compose config | grep -A 10 "auth-service:"

# 4. 컨테이너 재생성 (--force-recreate)
docker-compose up -d --force-recreate auth-service
```

## 환경별 확인 명령어

### 로컬 개발

```bash
# Spring Boot 애플리케이션 실행 시 환경 변수 확인
./gradlew bootRun --args='--spring.profiles.active=local'

# 환경 변수 로딩 확인 (로그)
grep "GOOGLE_CLIENT_ID" logs/application.log
```

### Docker Compose

```bash
# 환경 변수가 제대로 로드되었는지 확인
docker-compose config

# 실행 중인 컨테이너의 환경 변수 확인
docker exec auth-service env

# 특정 환경 변수만 확인
docker exec auth-service env | grep MYSQL
```

### Kubernetes

```bash
# Secret 확인
kubectl get secret portal-universe-secret -n portal-universe

# Secret 내용 디코딩 (민감 정보 주의)
kubectl get secret portal-universe-secret -n portal-universe -o jsonpath='{.data.MYSQL_PASSWORD}' | base64 -d

# Pod에서 환경 변수 확인
kubectl exec -it <pod-name> -n portal-universe -- env | grep MYSQL
```

## 참고

- [Docker Compose 배포 가이드](./docker-compose.md)
- [Kubernetes 배포 가이드](./kubernetes.md)
- [프로젝트 README](../../README.md)
- [Spring Boot Externalized Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
- [Kubernetes Secrets](https://kubernetes.io/docs/concepts/configuration/secret/)
