# Shopping Frontend 모듈

Portal Universe 프로젝트의 이커머스 마이크로 프론트엔드(Remote) 애플리케이션입니다.

## 개요

**Shopping Frontend**는 Portal Shell(Host)에 의해 Module Federation으로 로드되는 React 기반 리모트 애플리케이션입니다. 제품 검색/상세 조회, 장바구니, 결제, 주문 관리 등 이커머스 핵심 기능을 제공합니다.

### 핵심 특징

- **Module Federation**: Host(Portal Shell)에서 런타임에 로드됨
- **Dual Mode**: Standalone(개발) 및 Embedded(프로덕션) 모드 지원
- **Props 기반 통신**: Portal Shell과 Props를 통해 테마, 인증, 로케일 동기화
- **독립적 라우팅**: Memory Router(Embedded) 또는 Browser Router(Standalone) 사용
- **디자인 토큰 연동**: Portal Shell의 디자인 시스템과 공유

## 현재 상태 (Bootstrap Phase)

### 구현된 기능

- ✅ 프로젝트 구조 및 빌드 설정
- ✅ Module Federation 설정 (remoteEntry.js)
- ✅ Dual Mode 라우팅 시스템
- ✅ Portal Shell 통신 파이프라인
- ✅ 인증 상태 관리 (Zustand)
- ✅ 테마 동기화 (light/dark mode)
- ✅ 관리자 페이지 프레임워크
- ✅ API 클라이언트 기본 설정

### 미구현 기능

- 🔲 상품 목록 페이지 UI
- 🔲 상품 상세 페이지 UI
- 🔲 장바구니 기능 및 UI
- 🔲 결제 플로우 및 UI
- 🔲 주문 관리 기능 및 UI
- 🔲 관리자 패널 기능 및 UI
- 🔲 API 연동 (axios Interceptor)
- 🔲 테스트 커버리지
- 🔲 E2E 테스트

## 빠른 시작

### 환경 설정

```bash
# 루트 디렉토리에서
cd frontend && npm install

# 또는 특정 모듈만
cd frontend/shopping-frontend && npm install
```

### 개발 모드 실행

**Standalone 모드** (포트 30002에서 독립 실행):

```bash
cd frontend/shopping-frontend
npm run dev
```

**Portal Shell과 함께 실행** (권장):

```bash
cd frontend && npm run dev
```

### 빌드

```bash
# 개발 환경 빌드
npm run build:dev

# Docker 환경 빌드
npm run build:docker

# Kubernetes 환경 빌드
npm run build:k8s

# 프로덕션 빌드 (타입 체크 + 최적화)
npm run build
```

## 프로젝트 구조

```
shopping-frontend/
├── docs/                   # 문서
│   ├── README.md          # 모듈 개요
│   ├── ARCHITECTURE.md    # 아키텍처
│   ├── ROADMAP.md         # 로드맵
│   └── FEDERATION.md      # Module Federation
├── src/
│   ├── bootstrap.tsx      # MF entry point
│   ├── App.tsx            # 루트 컴포넌트
│   ├── router/            # React Router
│   ├── stores/            # Zustand stores
│   ├── hooks/             # 커스텀 훅
│   ├── api/               # API 클라이언트
│   ├── types/             # TypeScript 타입
│   ├── components/        # UI 컴포넌트
│   ├── pages/             # 페이지 컴포넌트
│   └── styles/            # 전역 스타일
├── vite.config.ts         # Vite 설정
└── package.json
```

## 주요 기술 스택

| 항목 | 기술 | 버전 |
|------|------|------|
| 프레임워크 | React | 18.2.0 |
| 빌드 도구 | Vite | 7.1.12 |
| 라우팅 | React Router | 7.1.5 |
| 상태 관리 | Zustand | 5.0.3 |
| 폼 처리 | React Hook Form | 7.71.1 |
| API 클라이언트 | Axios | 1.12.2 |
| 스타일링 | Tailwind CSS + SCSS | 3.4.15 |
| 모듈 연동 | Module Federation | 1.4.1 |
| 타입 스크립트 | TypeScript | 5.9.3 |

## 다음 단계

자세한 정보는 다음 문서를 참고하세요:

- [ARCHITECTURE.md](./ARCHITECTURE.md) - 시스템 아키텍처
- [ROADMAP.md](./ROADMAP.md) - 구현 로드맵
- [FEDERATION.md](./FEDERATION.md) - Module Federation 설정
