# Troubleshooting

Portal Universe 문제 해결 기록 모음입니다.

## 구조

```
troubleshooting/
├── README.md           # 이 문서
├── templates/          # 템플릿
│   └── template.md
└── 2026/               # 연도별 폴더
    └── 01/             # 월별 폴더
        └── TS-*.md     # 개별 문서
```

## 문서 목록

### 2026년 1월

| ID | 제목 | 심각도 | 상태 | 영향 서비스 |
|----|------|--------|------|-------------|
| [TS-20260129-005](./2026/01/TS-20260129-005-react-error-321-module-federation.md) | React Error #321 Module Federation 듀얼 인스턴스 | 🟠 High | 해결됨 | prism-frontend, shopping-frontend |
| [TS-20260128-004](./2026/01/TS-20260128-004-like-api-url-mismatch.md) | 좋아요 기능 API 경로 불일치 오류 | 🟡 Medium | 해결됨 | blog-frontend, blog-service |
| [TS-20260121-003](./2026/01/TS-20260121-003-k8s-deployment-issues.md) | Kubernetes 배포 중 발생한 복합 인프라 이슈 | 🟠 High | 진행 중 | notification-service, auth-service, all-services |
| [TS-20260119-002](./2026/01/TS-20260119-002-design-system-import-error.md) | Design System CSS Import 오류 | 🟠 High | 해결됨 | portal-shell, blog-frontend, shopping-frontend |
| [TS-20260117-001](./2026/01/TS-20260117-001-react-module-federation.md) | React Error #31 Module Federation 호환성 | 🟠 High | 해결됨 | shopping-frontend |

## 심각도 기준

| 레벨 | 아이콘 | 기준 |
|------|--------|------|
| Critical | 🔴 | 서비스 전체 중단 |
| High | 🟠 | 주요 기능 장애 |
| Medium | 🟡 | 일부 기능 영향 |
| Low | 🟢 | 경미한 이슈 |

## 작성 가이드

새 문서 작성 시:

1. `templates/template.md` 복사
2. 연/월 디렉토리에 저장
3. 파일명: `TS-YYYYMMDD-XXX-[title].md`
4. 이 README에 항목 추가

[Troubleshooting 작성 가이드](../../docs_template/guide/troubleshooting/how-to-write.md) 참조

## 빠른 검색

### 서비스별

- **auth-service**: [TS-20260121-003](./2026/01/TS-20260121-003-k8s-deployment-issues.md)
- **blog-service**: [TS-20260128-004](./2026/01/TS-20260128-004-like-api-url-mismatch.md)
- **notification-service**: [TS-20260121-003](./2026/01/TS-20260121-003-k8s-deployment-issues.md)
- **portal-shell**: [TS-20260119-002](./2026/01/TS-20260119-002-design-system-import-error.md)
- **blog-frontend**: [TS-20260128-004](./2026/01/TS-20260128-004-like-api-url-mismatch.md), [TS-20260119-002](./2026/01/TS-20260119-002-design-system-import-error.md)
- **prism-frontend**: [TS-20260129-005](./2026/01/TS-20260129-005-react-error-321-module-federation.md)
- **shopping-frontend**: [TS-20260129-005](./2026/01/TS-20260129-005-react-error-321-module-federation.md), [TS-20260119-002](./2026/01/TS-20260119-002-design-system-import-error.md), [TS-20260117-001](./2026/01/TS-20260117-001-react-module-federation.md)

### 태그별

- **API**: [TS-20260128-004](./2026/01/TS-20260128-004-like-api-url-mismatch.md)
- **API Gateway**: [TS-20260128-004](./2026/01/TS-20260128-004-like-api-url-mismatch.md)
- **Route Mismatch**: [TS-20260128-004](./2026/01/TS-20260128-004-like-api-url-mismatch.md)
- **Kubernetes**: [TS-20260121-003](./2026/01/TS-20260121-003-k8s-deployment-issues.md)
- **Redis**: [TS-20260121-003](./2026/01/TS-20260121-003-k8s-deployment-issues.md)
- **Kind**: [TS-20260121-003](./2026/01/TS-20260121-003-k8s-deployment-issues.md)
- **Docker**: [TS-20260121-003](./2026/01/TS-20260121-003-k8s-deployment-issues.md)
- **Image Pull**: [TS-20260121-003](./2026/01/TS-20260121-003-k8s-deployment-issues.md)
- **OAuth2**: [TS-20260121-003](./2026/01/TS-20260121-003-k8s-deployment-issues.md)
- **Spring Security**: [TS-20260121-003](./2026/01/TS-20260121-003-k8s-deployment-issues.md)
- **Vite**: [TS-20260129-005](./2026/01/TS-20260129-005-react-error-321-module-federation.md), [TS-20260119-002](./2026/01/TS-20260119-002-design-system-import-error.md)
- **Design System**: [TS-20260119-002](./2026/01/TS-20260119-002-design-system-import-error.md)
- **CSS**: [TS-20260119-002](./2026/01/TS-20260119-002-design-system-import-error.md)
- **Module Federation**: [TS-20260129-005](./2026/01/TS-20260129-005-react-error-321-module-federation.md), [TS-20260119-002](./2026/01/TS-20260119-002-design-system-import-error.md), [TS-20260117-001](./2026/01/TS-20260117-001-react-module-federation.md)
- **React**: [TS-20260129-005](./2026/01/TS-20260129-005-react-error-321-module-federation.md), [TS-20260117-001](./2026/01/TS-20260117-001-react-module-federation.md)
- **React-DOM**: [TS-20260129-005](./2026/01/TS-20260129-005-react-error-321-module-federation.md)
- **Shared Dependencies**: [TS-20260129-005](./2026/01/TS-20260129-005-react-error-321-module-federation.md)
- **Vue**: [TS-20260128-004](./2026/01/TS-20260128-004-like-api-url-mismatch.md)
