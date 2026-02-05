---
id: guide-getting-started
title: Blog Frontend Getting Started
type: guide
status: current
created: 2026-01-18
updated: 2026-01-18
author: Claude
tags: [guide, setup, development, vue, vite, module-federation]
---

# Getting Started

> Blog Frontend 개발 환경 설정 가이드

---

## 📋 개요

| 항목 | 내용 |
|------|------|
| **예상 소요 시간** | 15-20분 |
| **대상** | 프론트엔드 개발자, 신규 팀원 |
| **난이도** | 초급 |

이 가이드는 Blog Frontend 개발 환경을 설정하고 로컬에서 실행하는 방법을 안내합니다.

---

## ✅ 사전 요구사항

### 필수 소프트웨어

| 소프트웨어 | 버전 | 확인 명령어 |
|-----------|------|------------|
| Node.js | 18.x 이상 | `node --version` |
| npm | 9.x 이상 | `npm --version` |
| Git | 최신 버전 | `git --version` |

### 필수 지식

- Vue 3 Composition API 기본 이해
- TypeScript 기초
- npm workspaces 개념

---

## 🔧 환경 설정

### Step 1: 저장소 클론

```bash
# 전체 프로젝트 클론 (권장)
git clone https://github.com/L-a-z-e/portal-universe.git
cd portal-universe/frontend
```

또는 Blog Frontend만 독립적으로 개발하는 경우:

```bash
cd portal-universe/frontend/blog-frontend
```

---

### Step 2: 의존성 설치

Blog Frontend는 npm workspaces 구조로 관리됩니다. **반드시 `frontend/` 루트에서 설치**해야 합니다.

```bash
# frontend/ 디렉토리에서 실행
cd frontend
npm install
```

이 명령은 다음 모듈들을 모두 설치합니다:
- `@portal/blog-frontend`
- `@portal/portal-shell`
- `@portal/design-system`
- `@portal/shopping-frontend`

**예상 결과**:
```
added 1234 packages in 30s
```

---

### Step 3: 환경 변수 설정

Blog Frontend는 3가지 프로필을 지원합니다:

| 프로필 | 파일 | 용도 |
|--------|------|------|
| dev | `.env.dev` | 로컬 개발 (기본값) |
| docker | `.env.docker` | Docker Compose 환경 |
| k8s | `.env.k8s` | Kubernetes 환경 |

#### 필수 환경 변수

```bash
# .env.dev 파일 내용 (예시)
VITE_PORTAL_SHELL_REMOTE_URL=http://localhost:30000/assets/remoteEntry.js
VITE_SHOPPING_REMOTE_URL=http://localhost:30002/assets/remoteEntry.js
```

**설명**:
- `VITE_PORTAL_SHELL_REMOTE_URL`: Portal Shell의 Module Federation 진입점
- `VITE_SHOPPING_REMOTE_URL`: Shopping Frontend의 Module Federation 진입점

> ⚠️ **주의**: `.env.*` 파일은 이미 설정되어 있으므로 수정이 필요 없습니다. 커스터마이징이 필요한 경우에만 수정하세요.

---

## 🚀 실행 방법

Blog Frontend는 **2가지 실행 모드**를 지원합니다:

### Mode 1: Standalone 모드 (독립 실행)

Blog Frontend만 단독으로 실행합니다. Module Federation 없이 일반 Vue 앱처럼 동작합니다.

```bash
cd frontend/blog-frontend
npm run dev
```

**예상 출력**:
```
🔧 [Vite Config] Building for mode: dev
🔧 [Vite Config] Portal Remote URL: http://localhost:30000/assets/remoteEntry.js

  ➜  Local:   http://localhost:30001/
  ➜  Network: use --host to expose
```

**브라우저 접속**:
```
http://localhost:30001
```

---

### Mode 2: Portal Shell 통합 모드 (Embedded)

Portal Shell에서 Blog Frontend를 Remote 모듈로 로드하는 방식입니다.

#### Step 1: Blog Frontend 빌드 & 미리보기

```bash
cd frontend/blog-frontend
npm run dev
```

이 명령은 내부적으로 다음을 실행합니다:
```bash
vite build --watch --mode dev
vite preview --port 30001 --strictPort --mode dev
```

- `vite build --watch`: 파일 변경 시 자동 리빌드
- `vite preview`: 빌드된 파일을 서빙 (Hot Module Replacement 없음)

#### Step 2: Portal Shell 실행

```bash
cd frontend/portal-shell
npm run dev
```

#### Step 3: 브라우저 접속

```
http://localhost:30000
```

Portal Shell에서 `/blog` 경로로 이동하면 Blog Frontend가 로드됩니다.

---

## ✅ 실행 확인

### Standalone 모드 확인

1. 브라우저에서 `http://localhost:30001` 접속
2. 콘솔에서 다음 로그 확인:

```
🎯 [Blog] Detected mode: STANDALONE
📦 [Blog] Starting in STANDALONE mode
✅ [Blog] Mounted successfully
   URL: http://localhost:30001/
   Route: /
```

3. 화면에 블로그 메인 페이지가 표시되어야 합니다.

---

### Portal Shell 통합 모드 확인

1. 브라우저에서 `http://localhost:30000` 접속
2. `/blog` 경로로 이동
3. 콘솔에서 다음 로그 확인:

```
🎯 [Blog] Detected mode: EMBEDDED
⏳ [Blog] Waiting for Portal Shell to mount...
🚀 [Blog] Mounting app in EMBEDDED mode
📍 Mount target: DIV blog-container
✅ [Blog] App mounted successfully
```

4. Portal Shell의 네비게이션이 유지되면서 Blog 콘텐츠가 표시되어야 합니다.

---

## 📦 주요 기술 스택

| 기술 | 버전 | 용도 |
|------|------|------|
| Vue | 3.5.21 | UI 프레임워크 |
| Vite | 7.1.7 | 빌드 도구 |
| TypeScript | 5.9.3 | 타입 시스템 |
| Pinia | 3.0.3 | 상태 관리 |
| Vue Router | 4.5.1 | 라우팅 |
| @originjs/vite-plugin-federation | 1.4.1 | Module Federation |
| oidc-client-ts | 3.3.0 | 인증 (OAuth2) |
| Tailwind CSS | 3.4.15 | 스타일링 |
| Toast UI Editor | 3.2.2 | Markdown 에디터 |

---

## ⚠️ 자주 발생하는 문제

### 문제 1: `Cannot find module '@portal/design-system'`

**원인**: npm workspaces 의존성이 설치되지 않음

**해결 방법**:
```bash
cd frontend
npm install
```

---

### 문제 2: `Port 30001 is already in use`

**원인**: 이전 프로세스가 아직 실행 중이거나 포트가 점유됨

**해결 방법**:
```bash
# macOS/Linux
lsof -ti:30001 | xargs kill -9

# Windows
netstat -ano | findstr :30001
taskkill /PID <PID> /F
```

---

### 문제 3: `Failed to fetch dynamically imported module`

**원인**: Module Federation Remote URL이 잘못 설정되었거나 Portal Shell이 실행 중이지 않음

**해결 방법**:
1. `.env.dev` 파일의 `VITE_PORTAL_SHELL_REMOTE_URL` 확인
2. Portal Shell이 `http://localhost:30000`에서 실행 중인지 확인
3. 브라우저 캐시 삭제 후 새로고침

---

### 문제 4: CSS가 적용되지 않음

**원인**: Tailwind CSS 설정 문제 또는 design-system 빌드 누락

**해결 방법**:
```bash
# design-system 빌드
cd frontend/design-system
npm run build

# blog-frontend 재실행
cd ../blog-frontend
npm run dev
```

---

### 문제 5: Hot Module Replacement (HMR)가 작동하지 않음

**원인**: `npm run dev`는 `vite preview` 모드를 사용하므로 HMR이 지원되지 않음

**해결 방법**:

개발 중에는 **Standalone 모드**를 사용하는 것을 권장합니다:

```bash
# package.json의 dev 스크립트를 임시로 변경
# "dev": "vite --port 30001"

# 또는 직접 vite 명령 실행
npx vite --port 30001
```

통합 테스트가 필요한 경우에만 Portal Shell과 함께 실행하세요.

---

## 🔄 개발 워크플로우 권장사항

### 로컬 개발 시 (권장)

```bash
# Standalone 모드로 실행 (HMR 지원)
cd frontend/blog-frontend
npx vite --port 30001
```

- ✅ HMR(Hot Module Replacement) 지원
- ✅ 빠른 피드백
- ✅ 독립적인 개발 가능

---

### 통합 테스트 시

```bash
# Terminal 1: Blog Frontend 빌드 & 서빙
cd frontend/blog-frontend
npm run dev

# Terminal 2: Portal Shell 실행
cd frontend/portal-shell
npm run dev
```

- ✅ Module Federation 동작 확인
- ✅ 라우팅 통합 테스트
- ✅ 프로덕션 환경과 유사한 구조

---

## 🛠️ 빌드 명령어

### 개발 빌드

```bash
npm run build:dev
```

**출력**: `dist/` 디렉토리에 빌드된 파일 생성

---

### Docker 빌드

```bash
npm run build:docker
```

`.env.docker` 환경 변수를 사용하여 빌드합니다.

---

### Kubernetes 빌드

```bash
npm run build:k8s
```

`.env.k8s` 환경 변수를 사용하여 빌드합니다.

---

## 📁 프로젝트 구조

```
blog-frontend/
├── src/
│   ├── main.ts              # Standalone 진입점
│   ├── bootstrap.ts         # Embedded 진입점 (Module Federation)
│   ├── App.vue              # 루트 컴포넌트
│   ├── router/              # Vue Router 설정
│   ├── stores/              # Pinia 스토어
│   ├── views/               # 페이지 컴포넌트
│   ├── components/          # 재사용 가능한 컴포넌트
│   └── style.css            # 전역 스타일
├── public/                  # 정적 파일
├── dist/                    # 빌드 결과물
├── vite.config.ts           # Vite 설정
├── tsconfig.json            # TypeScript 설정
├── tailwind.config.js       # Tailwind CSS 설정
├── package.json             # 의존성 및 스크립트
├── .env.dev                 # 개발 환경 변수
├── .env.docker              # Docker 환경 변수
└── .env.k8s                 # Kubernetes 환경 변수
```

---

## ➡️ 다음 단계

환경 설정이 완료되었다면 다음 문서를 참고하세요:

1. **개발 프로세스**: `development-workflow.md` (예정)
2. **API 연동**: `../api/` 디렉토리 참고
3. **아키텍처**: `../architecture/` 디렉토리 참고
4. **컴포넌트 가이드**: Design System 문서 참고

---

## 📞 도움이 필요하면

| 채널 | 용도 |
|------|------|
| GitHub Issues | 버그 리포트, 기능 제안 |
| Slack #frontend | 일반적인 질문 |
| 문서 | `docs/` 디렉토리 참고 |

---

**최종 업데이트**: 2026-01-18
