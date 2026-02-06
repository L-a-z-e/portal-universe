# ADR-006: Config Service 및 Discovery Service 제거

**Status**: Accepted
**Date**: 2026-01-20

## Context

Spring Cloud Config Service와 Discovery Service(Eureka)를 사용하던 기존 아키텍처에서 다음 문제가 발생했습니다: 설정 변경 시 원격 Git 저장소 push가 필수적이어서 테스트가 불편하고, Config Service가 실행되어야만 다른 서비스를 시작할 수 있어 로컬 개발이 어려웠습니다. 또한 Kubernetes는 자체 DNS와 ConfigMap을 제공하므로 Config/Discovery Service가 중복된 역할을 수행했습니다.

## Decision

**Spring Cloud Config Service와 Discovery Service를 제거하고, 각 서비스에 설정 파일을 직접 포함하며 Kubernetes DNS를 사용합니다.**

## Rationale

- **테스트 용이성**: 로컬 설정 파일 수정 후 즉시 재시작으로 테스트 가능
- **독립적 실행**: Config Service 없이 각 서비스가 독립적으로 실행
- **빠른 배포**: Init Container 대기 시간 제거 (약 30-60초 단축)
- **Kubernetes 네이티브**: K8s DNS와 ConfigMap 활용으로 일관된 인프라
- **장애 격리**: Config Service Single Point of Failure 제거

## Trade-offs

✅ **장점**:
- 로컬 개발 환경 설정 간편
- 서비스 시작 속도 향상
- 아키텍처 단순화 (불필요한 네트워크 호출 제거)
- Kubernetes 환경과의 자연스러운 통합

⚠️ **단점 및 완화**:
- 설정이 여러 서비스에 분산됨 → (완화: 프로필별 파일 구조로 일관성 유지, ConfigMap으로 공통 환경 변수 관리)
- 동적 설정 새로고침 기능 상실 → (완화: Kubernetes Rolling Update로 설정 변경 적용)

## Implementation

**설정 파일 구조** (각 서비스):
```
src/main/resources/
├── application.yml              # 공통 기본 설정
├── application-local.yml        # 로컬 개발 환경
├── application-docker.yml       # Docker Compose 환경
└── application-kubernetes.yml   # Kubernetes 환경
```

**주요 변경**:
- `spring-cloud-starter-config` 의존성 제거 (5개 서비스)
- `spring-cloud-starter-netflix-eureka-client` 의존성 제거 (5개 서비스)
- `config-service.yaml`, `discovery-service.yaml` 삭제
- Init Container (`wait-for-config`) 제거
- Dockerfile 대기 로직 제거 → 직접 실행

**Kubernetes 서비스 URL 예시**:
```yaml
AUTH_SERVICE_URL: http://auth-service.portal-universe.svc.cluster.local:8081
```

## References

- [Spring Boot Profiles](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.profiles)
- [Kubernetes ConfigMaps](https://kubernetes.io/docs/concepts/configuration/configmap/)
- [12-Factor App: Config](https://12factor.net/config)

---

📂 상세: [old-docs/central/adr/ADR-006-remove-config-service.md](../old-docs/central/adr/ADR-006-remove-config-service.md)
