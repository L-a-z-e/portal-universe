# Portal Universe 문서

> MSA + MFA 프로젝트의 중앙 문서 저장소

**마지막 업데이트**: 2026-02-06

---

## 문서 구조

```
docs/
├── architecture/     # 시스템 및 서비스 아키텍처
├── api/             # REST API 명세서
├── adr/             # 아키텍처 결정 기록
├── guides/          # 개발/배포/운영 가이드
├── learning/        # 기술 스택별 학습 자료
├── runbooks/        # 운영 절차서
├── troubleshooting/ # 문제 해결 기록
├── diagrams/        # 다이어그램 소스
├── templates/       # 문서 템플릿
└── old-docs/        # 기존 문서 아카이브
```

---

## 서비스 문서

### 백엔드
| 서비스 | 아키텍처 | API |
|--------|----------|-----|
| **Auth** (8081) | [architecture/auth-service/](architecture/auth-service/) | [api/auth-service/](api/auth-service/) |
| **Shopping** (8083) | [architecture/shopping-service/](architecture/shopping-service/) | [api/shopping-service/](api/shopping-service/) |
| **Blog** (8082) | [architecture/blog-service/](architecture/blog-service/) | [api/blog-service/](api/blog-service/) |
| **Notification** (8084) | [architecture/notification-service/](architecture/notification-service/) | [api/notification-service/](api/notification-service/) |
| **API Gateway** (8080) | [architecture/api-gateway/](architecture/api-gateway/) | [api/api-gateway/](api/api-gateway/) |
| **Chatbot** (8086) | [architecture/chatbot-service/](architecture/chatbot-service/) | - |

### 프론트엔드
| 프로젝트 | 아키텍처 | API |
|---------|----------|-----|
| **Portal Shell** (30000, Vue) | [architecture/portal-shell/](architecture/portal-shell/) | [api/portal-shell/](api/portal-shell/) |
| **Shopping Frontend** (30002, React) | [architecture/shopping-frontend/](architecture/shopping-frontend/) | [api/shopping-frontend/](api/shopping-frontend/) |
| **Blog Frontend** (30001, Vue) | [architecture/blog-frontend/](architecture/blog-frontend/) | [api/blog-frontend/](api/blog-frontend/) |
| **Prism Frontend** (30003, React) | [architecture/prism-frontend/](architecture/prism-frontend/) | - |
| **Design System** (Vue + React) | [architecture/design-system/](architecture/design-system/) | - |

### 시스템
| 문서 | 설명 |
|------|------|
| [시스템 아키텍처](architecture/system/) | 전체 시스템 구조, 서비스 간 통신 |
| [인증 시스템](architecture/system/auth-system-design.md) | JWT + Redis 인증 |
| [ERD](architecture/database/) | 서비스별 데이터베이스 스키마 |

---

## ADR (Architecture Decision Records)

[전체 ADR 목록](adr/_INDEX.md)

---

## 학습 자료

| 카테고리 | 설명 |
|---------|------|
| [Java/Spring](learning/java-spring/) | Spring Boot, JPA, 서비스 도메인 |
| [Vue](learning/vue/) | Vue 3, Pinia, Composables |
| [React](learning/react/) | React 18, Hooks, MFE |
| [TypeScript](learning/typescript/) | TypeScript 공통 |
| [Python](learning/python/) | FastAPI |
| [Database](learning/database/) | Redis, MongoDB, PostgreSQL, ES |
| [Messaging](learning/messaging/) | Kafka |
| [Security](learning/security/) | JWT, OAuth, 보안 |
| [Infrastructure](learning/infrastructure/) | AWS, Docker, K8s |
| [Patterns](learning/patterns/) | 디자인 패턴, Clean Code, DDD |

---

## 운영

- [개발 가이드](guides/development/)
- [배포 가이드](guides/deployment/)
- [운영 가이드](guides/operations/)
- [Runbooks](runbooks/)
- [Troubleshooting](troubleshooting/)

---

## 문서 작성

### 템플릿
새 문서 작성 시 [templates/](templates/) 활용:

| 템플릿 | 용도 |
|--------|------|
| [adr-template.md](templates/adr-template.md) | 아키텍처 결정 기록 |
| [architecture-template.md](templates/architecture-template.md) | 아키텍처 문서 |
| [api-template.md](templates/api-template.md) | API 명세 |
| [guide-template.md](templates/guide-template.md) | 가이드 |
| [learning-template.md](templates/learning-template.md) | 학습 자료 |
| [runbook-template.md](templates/runbook-template.md) | 운영 절차서 |
| [troubleshooting-template.md](templates/troubleshooting-template.md) | 문제 해결 기록 |

### 문서 위치 결정
- **아키텍처 이해** → `architecture/{서비스명}/`
- **API 사용** → `api/{서비스명}/`
- **설계 이유** → `adr/`
- **실습/학습** → `learning/{기술영역}/`
- **절차 수행** → `guides/` 또는 `runbooks/`
- **문제 해결** → `troubleshooting/`

### 원칙
1. **간결성**: 핵심만 (평균 80-100줄)
2. **실용성**: 복사 가능한 명령어, 코드 스니펫
3. **맥락 보존**: 상세 내용은 `old-docs/` 또는 Git 히스토리
4. **상호참조**: 관련 문서 링크 명시
