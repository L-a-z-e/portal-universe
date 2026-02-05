---
id: guide-getting-started
title: Getting Started
type: guide
status: current
created: 2026-01-18
updated: 2026-01-18
author: Laze
tags: [setup, environment, installation]
related:
  - guide-federation-integration
---

# Getting Started

> Shopping Frontend 개발 환경 설정 가이드

---

## 📋 개요

| 항목 | 내용 |
|------|------|
| **예상 소요 시간** | 10-15분 |
| **대상** | 신규 개발자, Shopping Frontend 컨트리뷰터 |
| **목표** | 로컬 개발 환경 구축 및 첫 실행 |

---

## ✅ 사전 요구사항

### 필수 소프트웨어

| 소프트웨어 | 최소 버전 | 권장 버전 | 확인 명령어 |
|-----------|---------|---------|------------|
| Node.js | 20.0.0+ | 20.11.0+ | `node --version` |
| npm | 10.0.0+ | 10.8.0+ | `npm --version` |
| Git | 2.30.0+ | 최신 | `git --version` |

### 선택 사항

| 소프트웨어 | 용도 |
|-----------|------|
| VS Code | 권장 에디터 (Extensions: Volar, ESLint, Prettier) |
| Docker Desktop | 백엔드 서비스 로컬 실행 시 필요 |

---

## 🔧 환경 설정

### Step 1: 저장소 클론

```bash
git clone https://github.com/L-a-z-e/portal-universe.git
cd portal-universe/frontend
```

### Step 2: 의존성 설치

Shopping Frontend는 npm workspaces를 사용합니다. 루트에서 한 번만 설치하면 모든 패키지가 설치됩니다.

```bash
# frontend 디렉토리에서 실행
npm install
```

**예상 소요 시간**: 2-3분

**설치되는 패키지**:
- portal-shell (Host)
- blog-frontend (Remote)
- shopping-frontend (Remote)
- @portal/design-system (공유 라이브러리)

### Step 3: 환경 변수 설정

```bash
cd shopping-frontend
cp .env.dev.example .env.dev
```

`.env.dev` 파일 내용 확인:
```bash
# API 엔드포인트
VITE_API_BASE_URL=http://localhost:8080

# 환경 구분
VITE_ENV=local

# Module Federation 설정
VITE_PORTAL_SHELL_URL=http://localhost:30000
```

---

## 🚀 개발 서버 실행

Shopping Frontend는 두 가지 모드로 실행할 수 있습니다.

### Mode 1: Standalone (독립 실행)

단독으로 실행하여 Shopping Frontend만 개발하는 모드입니다.

```bash
# frontend 디렉토리에서
npm run dev:shopping

# 또는 shopping-frontend 디렉토리에서
cd shopping-frontend
npm run dev
```

**접속 URL**: http://localhost:30002

**특징**:
- 빠른 HMR (Hot Module Replacement)
- Shopping 기능만 집중 개발
- Portal Shell 없이 독립 실행

### Mode 2: Embedded (통합 실행)

Portal Shell과 함께 전체 마이크로 프론트엔드 스택을 실행하는 모드입니다.

```bash
# frontend 디렉토리에서
npm run dev
```

이 명령어는 다음 서비스를 모두 실행합니다:
- Portal Shell (30000)
- Blog Frontend (30001)
- Shopping Frontend (30002)
- Design System (30003)

**접속 URL**:
- Portal Shell: http://localhost:30000
- Shopping 페이지: http://localhost:30000/shopping

**특징**:
- 실제 프로덕션 환경과 동일한 구조
- Module Federation 동작 확인
- 서비스 간 상호작용 테스트

---

## ✅ 실행 확인

### 1. Standalone 모드 확인

```bash
curl http://localhost:30002
```

**예상 결과**:
```html
<!doctype html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    ...
  </head>
  <body>
    <div id="app"></div>
    ...
  </body>
</html>
```

브라우저에서 http://localhost:30002 접속 시 Shopping 메인 페이지가 표시됩니다.

### 2. Embedded 모드 확인

브라우저에서 http://localhost:30000/shopping 접속 후:
- [ ] Shopping 페이지가 로드되는가?
- [ ] 좌측 네비게이션이 표시되는가?
- [ ] 상단 헤더가 Portal Shell의 것인가?

---

## 📁 프로젝트 구조

```
shopping-frontend/
├── public/                # 정적 파일
├── src/
│   ├── api/              # API 클라이언트
│   │   └── axios.config.ts
│   ├── assets/           # 이미지, 폰트 등
│   ├── components/       # 재사용 컴포넌트
│   │   ├── common/       # 공통 컴포넌트
│   │   └── product/      # 상품 관련 컴포넌트
│   ├── pages/            # 페이지 컴포�넌트
│   │   ├── HomePage.tsx
│   │   └── ProductListPage.tsx
│   ├── router/           # 라우팅 설정
│   │   └── index.tsx
│   ├── stores/           # Zustand 상태 관리
│   │   └── authStore.ts
│   ├── styles/           # 글로벌 스타일
│   │   └── globals.css
│   ├── types/            # TypeScript 타입 정의
│   │   └── product.ts
│   ├── utils/            # 유틸리티 함수
│   ├── App.tsx           # 앱 루트
│   ├── bootstrap.tsx     # Module Federation Entry
│   └── main.tsx          # 앱 진입점
├── .env.local.example    # 환경 변수 템플릿
├── .env.dev              # 로컬 환경 변수 (git ignore)
├── index.html            # HTML 템플릿
├── package.json          # 패키지 매니페스트
├── tailwind.config.js    # TailwindCSS 설정
├── tsconfig.json         # TypeScript 설정
└── vite.config.ts        # Vite 및 Module Federation 설정
```

### 주요 디렉토리 설명

| 디렉토리 | 용도 | 예시 |
|---------|------|------|
| `api/` | HTTP 클라이언트, API 호출 로직 | `axios.config.ts`, `productApi.ts` |
| `components/` | 재사용 가능한 React 컴포넌트 | `ProductCard.tsx`, `CartButton.tsx` |
| `pages/` | 라우트별 페이지 컴포넌트 | `HomePage.tsx`, `ProductDetailPage.tsx` |
| `stores/` | Zustand 기반 전역 상태 관리 | `authStore.ts`, `cartStore.ts` |
| `router/` | React Router 설정 | `index.tsx` (라우트 정의) |
| `types/` | TypeScript 타입/인터페이스 | `product.ts`, `api.types.ts` |
| `utils/` | 헬퍼 함수, 유틸리티 | `formatPrice.ts`, `validation.ts` |

---

## 🛠️ 주요 npm 스크립트

### 개발 모드

```bash
# Standalone 모드 (포트 30002)
npm run dev

# 타입 체크 + 개발 모드
npm run dev:check
```

### 빌드

```bash
# 프로덕션 빌드 (기본)
npm run build

# 개발 빌드
npm run build:dev

# Docker 환경 빌드
npm run build:docker

# Kubernetes 환경 빌드
npm run build:k8s
```

빌드 결과물은 `dist/` 디렉토리에 생성됩니다.

### 테스트

```bash
# 단위 테스트 (Vitest)
npm run test

# 테스트 커버리지
npm run test:coverage

# E2E 테스트 (Playwright)
npm run test:e2e
```

### 코드 품질

```bash
# ESLint 검사
npm run lint

# TypeScript 타입 체크
npm run type-check

# Prettier 포맷팅
npm run format
```

### 프리뷰

```bash
# 빌드된 앱 로컬 프리뷰
npm run preview
```

---

## 🔍 주요 설정 파일

### vite.config.ts

Module Federation 및 빌드 설정을 담당합니다.

**주요 설정**:
- **Federation Plugin**: `@originjs/vite-plugin-federation`
- **Exposes**: `./bootstrap` (Portal Shell이 import)
- **Remotes**: Portal Shell의 공유 서비스 참조
- **Shared Dependencies**: React, React-DOM, React Router 등

### tailwind.config.js

TailwindCSS 커스텀 테마 설정입니다.

**주요 설정**:
- Shopping 테마 색상 (`data-service="shopping"`)
- Linear-inspired 디자인 시스템 토큰
- 커스텀 유틸리티 클래스

### tsconfig.json

TypeScript 컴파일러 옵션입니다.

**주요 설정**:
- Path alias: `@/*` → `./src/*`
- Strict 모드 활성화
- JSX: `react-jsx`

---

## ⚠️ 자주 발생하는 문제

### 1. 포트 충돌

**증상**:
```
Error: listen EADDRINUSE: address already in use :::30002
```

**해결 방법**:
```bash
# 포트 사용 프로세스 확인
lsof -ti:30002

# 프로세스 종료
kill -9 $(lsof -ti:30002)

# 또는 다른 포트 사용
PORT=30012 npm run dev
```

### 2. Module not found

**증상**:
```
Cannot find module '@portal/design-system'
```

**해결 방법**:
```bash
# 루트 디렉토리에서 재설치
cd frontend
rm -rf node_modules package-lock.json
npm install
```

### 3. TypeScript 에러

**증상**:
```
Cannot find name 'React'
```

**해결 방법**:
```bash
# tsconfig.json 확인 후
npm run type-check

# 타입 정의 재설치
npm install --save-dev @types/react @types/react-dom
```

### 4. Module Federation 로드 실패

**증상**:
브라우저 콘솔에 `Failed to fetch dynamically imported module` 에러

**원인**:
- Portal Shell이 실행되지 않음
- CORS 설정 문제
- 네트워크 설정 문제

**해결 방법**:
```bash
# 1. Portal Shell 실행 확인
curl http://localhost:30000

# 2. Shopping Frontend 재시작
npm run dev

# 3. 브라우저 캐시 삭제 후 재접속
```

### 5. 환경 변수 인식 안 됨

**증상**:
`VITE_API_BASE_URL`이 undefined

**해결 방법**:
```bash
# .env.dev 파일 존재 확인
ls -la .env.dev

# VITE_ 접두사 확인
cat .env.dev | grep VITE_

# 개발 서버 재시작 (환경 변수는 빌드 시 주입됨)
npm run dev
```

---

## ➡️ 다음 단계

개발 환경 구축을 완료했다면, 다음 문서를 참고하세요.

1. **[Module Federation 통합 가이드](./federation-integration.md)**
   - Portal Shell과의 통합 방식
   - 공유 서비스 사용법
   - 듀얼 모드 구현

2. **[Architecture 문서](../architecture/README.md)**
   - 전체 아키텍처 개요
   - 기술 스택 상세
   - 설계 결정 사항

3. **[API 문서](../api/README.md)**
   - API 엔드포인트 목록
   - 요청/응답 스펙
   - 인증 방식

---

## 📞 도움이 필요하면

| 채널 | 용도 |
|------|------|
| GitHub Issues | 버그 리포트, 기능 제안 |
| Discussions | 질문, 아이디어 공유 |
| Slack #shopping-frontend | 실시간 개발 문의 |

---

**마지막 업데이트**: 2026-01-18
