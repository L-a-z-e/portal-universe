# ADR-005: 민감 데이터 관리 전략

**Status**: Accepted
**Date**: 2026-01-19

## Context

Portal Universe 프로젝트에서 민감한 데이터(DB 비밀번호, API 키, DockerHub credentials)가 Git 저장소에 커밋되는 보안 문제가 발생했습니다. Local, Docker, Kubernetes 등 다중 환경을 지원하면서도 개발자 경험을 해치지 않는 민감 데이터 관리 전략이 필요했습니다.

## Decision

**.env 파일 + .gitignore 방식을 채택합니다.**

템플릿 파일(`.env.local.example`, `.env.docker.example`, `secret.yaml.example`)은 Git에 커밋하고, 실제 민감 데이터가 포함된 파일은 `.gitignore`로 제외합니다.

## Rationale

- **단순성**: 대부분의 개발자가 익숙한 방식으로 학습 곡선 없음
- **비용 효율성**: HashiCorp Vault, AWS Secrets Manager 등 추가 인프라 비용 불필요
- **로컬 개발 용이**: `cp .env.local.example .env.local` 후 수정만으로 즉시 시작
- **CI/CD 자동화**: GitHub Actions에서 환경 변수 주입으로 간단히 처리
- **점진적 개선**: 향후 프로덕션 환경에서 AWS Secrets Manager 도입 가능

## Trade-offs

✅ **장점**:
- 구현이 간단하고 직관적
- 추가 인프라 비용 없음
- 로컬 개발 환경 설정 간편
- 환경별(Local, Docker, K8s) 일관된 방식

⚠️ **단점 및 완화**:
- `.gitignore` 설정 누락 시 커밋 위험 → (완화: Pre-commit hook으로 차단)
- 팀원 간 민감 데이터 공유 시 별도 채널 필요 → (완화: Slack, 1Password 활용)
- 파일 분실 시 복구 어려움 → (완화: Password Manager 백업 권장)

## Implementation

**주요 파일**:
- `.env.local.example` - 로컬 개발용 템플릿
- `.env.docker.example` - Docker Compose용 템플릿
- `k8s/base/secret.yaml.example` - Kubernetes Secret 템플릿
- `.gitignore` - 실제 파일 제외 설정 (.env.local, .env.docker, secret.yaml)
- `docker-compose.yml` - `env_file: .env.docker` 참조

**온보딩 절차**:
```bash
cp .env.local.example .env.local
# .env.local 파일에 실제 비밀번호 입력

cp .env.docker.example .env.docker
# .env.docker 파일 수정
```

## References

- [12-Factor App: Config](https://12factor.net/config)
- [OWASP: Secrets Management Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html)
- [로컬 개발 환경 설정 가이드](../guides/local-development-setup.md)

---

📂 상세: [old-docs/central/adr/ADR-005-sensitive-data-management.md](../old-docs/central/adr/ADR-005-sensitive-data-management.md)
