# 📚 Portal Universe 문서

> MSA + MFA 프로젝트의 중앙 문서 저장소

**마지막 업데이트**: YYYY-MM-DD

---

## 🗂️ 문서 구조

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
└── old-docs/        # 기존 문서 아카이브
```

---

## 📋 빠른 시작

### 개발자 온보딩
1. [개발 환경 설정](guides/development/setup.md)
2. [코드 컨벤션](guides/development/code-conventions.md)
3. [Git 워크플로우](guides/development/git-workflow.md)

### 서비스 이해하기
1. [전체 시스템 아키텍처](architecture/system/README.md)
2. [Identity Model](architecture/system/identity-model.md)
3. [서비스 간 통신](architecture/system/service-communication.md)

### 운영 작업
1. [배포 가이드](guides/deployment/README.md)
2. [모니터링](guides/operations/monitoring.md)
3. [Runbooks](runbooks/README.md)

---

## 🏗️ 아키텍처

### 시스템 레벨
| 문서 | 설명 |
|------|------|
| [시스템 아키텍처](architecture/system/README.md) | 전체 시스템 구조 |
| [Identity Model](architecture/system/identity-model.md) | 사용자 식별 체계 아키텍처 |
| [보안 설계](architecture/system/security-architecture.md) | 시스템 보안 아키텍처 |

### 백엔드 서비스
| 서비스 | 설명 | 아키텍처 | API |
|--------|------|----------|-----|
| **Auth** | 인증/인가 | [📐](architecture/auth-service/README.md) | [📡](api/auth-service/README.md) |
| **Shopping** | 주문/결제 | [📐](architecture/shopping-service/README.md) | [📡](api/shopping-service/README.md) |
| **Blog** | 블로그 | [📐](architecture/blog-service/README.md) | [📡](api/blog-service/README.md) |
| **Notification** | 알림 | [📐](architecture/notification-service/README.md) | [📡](api/notification-service/README.md) |
| **API Gateway** | 라우팅 | [📐](architecture/api-gateway/README.md) | [📡](api/api-gateway/README.md) |
| **Chatbot** | AI 챗봇 | [📐](architecture/chatbot-service/README.md) | [📡](api/chatbot-service/README.md) |

### 프론트엔드
| 프로젝트 | 설명 | 아키텍처 | API |
|---------|------|----------|-----|
| **Portal Shell** | MFE 호스트 (Vue) | [📐](architecture/portal-shell/README.md) | [📡](api/portal-shell/README.md) |
| **Shopping Frontend** | 쇼핑몰 (Vue) | [📐](architecture/shopping-frontend/README.md) | [📡](api/shopping-frontend/README.md) |
| **Blog Frontend** | 블로그 (React) | [📐](architecture/blog-frontend/README.md) | [📡](api/blog-frontend/README.md) |
| **Prism Frontend** | Prism (React) | [📐](architecture/prism-frontend/README.md) | - |
| **Design System** | Vue + React | [📐](architecture/design-system/README.md) | [📡](api/design-system/README.md) |

---

## 📝 ADR (Architecture Decision Records)

최근 아키텍처 결정:

| ID | 제목 | 날짜 | 상태 |
|----|------|------|------|
| [ADR-015](adr/ADR-015.md) | [제목] | 2026-XX-XX | Accepted |
| [ADR-014](adr/ADR-014.md) | [제목] | 2026-XX-XX | Accepted |
| [ADR-013](adr/ADR-013.md) | [제목] | 2026-XX-XX | Accepted |

➡️ [전체 ADR 목록](adr/_INDEX.md)

---

## 🎓 학습 자료

### 기술 스택별

| 카테고리 | 주요 자료 |
|---------|----------|
| **Java/Spring** | [Saga 패턴](learning/java-spring/saga-orchestration.md), [JPA 최적화](learning/java-spring/jpa-optimization.md) |
| **Vue** | [Composition API](learning/vue/composition-api.md), [Pinia](learning/vue/pinia-state.md) |
| **React** | [Hooks 심화](learning/react/advanced-hooks.md), [MFE](learning/react/module-federation.md) |
| **TypeScript** | [고급 타입](learning/typescript/advanced-types.md) |
| **Python/FastAPI** | [Async 패턴](learning/python/async-patterns.md) |
| **Database** | [Redis 활용](learning/database/redis-patterns.md), [MongoDB](learning/database/mongodb-design.md) |
| **Infrastructure** | [K8s 운영](learning/infrastructure/k8s-operations.md), [AWS](learning/infrastructure/aws-services.md) |
| **Security** | [JWT 심화](learning/security/jwt-deep-dive.md), [OAuth 2.0](learning/security/oauth2-flow.md) |

➡️ [전체 학습 자료](learning/README.md)

---

## 🔧 운영

### 가이드
- [개발 가이드](guides/development/README.md)
- [배포 가이드](guides/deployment/README.md)
- [운영 가이드](guides/operations/README.md)

### Runbooks
- [일일 점검](runbooks/daily-health-check.md)
- [배포 절차](runbooks/deployment-procedure.md)
- [장애 대응](runbooks/incident-response.md)

### Troubleshooting
최근 이슈:
- [TS-20260205-001](troubleshooting/2026/02/TS-20260205-001.md): [제목]
- [TS-20260204-002](troubleshooting/2026/02/TS-20260204-002.md): [제목]

➡️ [전체 Troubleshooting](troubleshooting/README.md)

---

## 🔍 문서 찾기

### 카테고리별
- **아키텍처 이해**: [architecture/](architecture/)
- **API 사용**: [api/](api/)
- **설계 이유**: [adr/](adr/)
- **실습/학습**: [learning/](learning/)
- **절차 수행**: [guides/](guides/), [runbooks/](runbooks/)
- **문제 해결**: [troubleshooting/](troubleshooting/)

### 검색 팁
```bash
# 전체 문서에서 키워드 검색
grep -r "JWT" docs/

# ADR에서만 검색
grep -r "Redis" docs/adr/

# 최근 수정된 문서 찾기
find docs/ -name "*.md" -mtime -7
```

---

## 📐 다이어그램

주요 다이어그램:
- [전체 시스템 아키텍처](diagrams/system-architecture.mmd)
- [인증 플로우](diagrams/auth-flow.mmd)
- [Saga 패턴](diagrams/saga-pattern.mmd)
- [ERD](architecture/database/erd.md)

---

## 🔄 PDCA

활성 개선 사이클:
- [Feature: OAuth2 통합](pdca/01-plan/features/oauth2-integration.md)
- [Feature: 성능 최적화](pdca/01-plan/features/performance-optimization.md)

➡️ [PDCA 프로세스](pdca/README.md)

---

## 📜 문서 작성 가이드

### 템플릿
새 문서 작성 시 템플릿 활용:
- [ADR 템플릿](templates/adr-template.md)
- [Architecture 템플릿](templates/architecture-template.md)
- [API 템플릿](templates/api-template.md)
- [Learning 템플릿](templates/learning-template.md)
- [Guide 템플릿](templates/guide-template.md)
- [Runbook 템플릿](templates/runbook-template.md)
- [Troubleshooting 템플릿](templates/troubleshooting-template.md)

### 문서 작성 원칙
1. **간결성**: 핵심만 담기 (평균 80-100줄)
2. **실용성**: 복사 가능한 명령어, 코드 스니펫
3. **맥락 보존**: 상세 내용은 `old-docs/` 또는 Git 히스토리
4. **상호참조**: 관련 문서 링크 명시

---

## 🤝 기여 방법

### 문서 업데이트
1. 문서 수정
2. 관련 인덱스 업데이트 (README, _INDEX.md)
3. Git commit에 변경 이유 명시
4. PR 생성

### 새 문서 작성
1. 적절한 템플릿 선택
2. 카테고리별 위치 확인
3. 작성 후 인덱스 추가
4. 리뷰 요청

---

## 📞 문의

- 이슈: [GitHub Issues](링크)
- 담당자: [이름] (@slack-username)

---

**라이선스**: MIT
**마지막 업데이트**: 2026-02-05
