# Shopping Frontend 문서 포털

> Portal Universe - Shopping Frontend 기술 문서 통합 인덱스

---

## 📋 개요

Shopping Frontend는 React 18 + TypeScript + Vite 기반의 마이크로 프론트엔드 모듈입니다. Module Federation을 통해 Portal Shell에 통합되며, 전자상거래 기능(상품 관리, 장바구니, 주문/결제)을 제공합니다.

> ⚠️ **현재 개발 상태**: 기본 구조와 부트스트랩 함수는 구현되었으나, 일부 기능은 아직 개발 중입니다.

---

## 🚀 빠른 시작

| 단계 | 문서 | 설명 |
|------|------|------|
| 1️⃣ | [Getting Started](./guides/getting-started.md) | 개발 환경 설정 및 실행 |
| 2️⃣ | [Architecture](./architecture/) | 시스템 구조 이해 |
| 3️⃣ | [API 문서](./api/) | Backend API 연동 |
| 4️⃣ | [Module Federation 통합](./guides/federation-integration.md) | Portal Shell 통합 가이드 |

---

## 📁 문서 구조

```
docs/
├── README.md                  # 📍 현재 문서 (메인 포털)
├── guides/                    # 개발자 가이드
│   ├── README.md
│   ├── getting-started.md     🔜 예정
│   └── federation-integration.md ✅ 완료
├── architecture/              # 아키텍처 문서
│   ├── README.md              ✅ 완료
│   └── system-overview.md     ✅ 완료
├── api/                       # API 명세서
│   └── README.md              🔜 예정
└── backup/                    # 백업 (구 문서)
```

---

## 📚 문서 유형별 인덱스

### 🗺️ [Guides - 개발자 가이드](./guides/)

개발 환경 설정, 온보딩, 개발 프로세스 안내

| 문서 | 상태 | 설명 |
|------|------|------|
| [Module Federation 통합](./guides/federation-integration.md) | ✅ Current | Portal Shell과의 통합 방법 |
| Getting Started | 🔜 예정 | 개발 환경 설정 및 실행 |
| Component Development | 🔜 예정 | React 컴포넌트 개발 가이드 |
| State Management | 🔜 예정 | Zustand 스토어 사용법 |
| Testing Guide | 🔜 예정 | 테스트 작성 가이드 |

---

### 🏗️ [Architecture - 아키텍처](./architecture/)

시스템 설계, 기술 스택, 디렉토리 구조

| 문서 | 상태 | 설명 |
|------|------|------|
| [System Overview](./architecture/system-overview.md) | ✅ Current | React 18 Module Federation Remote 구조 |
| Module Federation | 🔜 예정 | MFA 구조 설명 |
| State Management | 🔜 예정 | Zustand 스토어 구조 |
| Routing Architecture | 🔜 예정 | React Router 구조 |

---

### 📡 [API - API 명세서](./api/)

Backend Shopping Service API 엔드포인트, 요청/응답 스펙

| 문서 | 상태 | 설명 |
|------|------|------|
| Product API | 🔜 예정 | 상품 CRUD API |
| Cart API | 🔜 예정 | 장바구니 API |
| Order API | 🔜 예정 | 주문 API |
| Payment API | 🔜 예정 | 결제 API |
| Auth API | 🔜 예정 | 인증/인가 API |

---

## 🔧 기술 스택

| 카테고리 | 기술 | 버전 |
|----------|------|------|
| **Framework** | React | 18.2.0 |
| **Build Tool** | Vite | 7.1.12 |
| **Language** | TypeScript | 5.9.3 |
| **State** | Zustand | 5.0.3 |
| **Router** | React Router | 7.1.5 |
| **MFA** | @originjs/vite-plugin-federation | 1.4.1 |
| **HTTP** | Axios | 1.12.2 |
| **Styling** | Tailwind CSS | 3.4.15 |
| **CSS Preprocessor** | Sass | 1.69.0 |

---

## 🗂️ 디렉토리 구조

```
shopping-frontend/
├── src/
│   ├── api/                   # API 클라이언트
│   │   ├── client.ts          # Axios 인스턴스
│   │   └── endpoints.ts       # API 엔드포인트 정의
│   ├── components/            # 재사용 컴포넌트
│   │   ├── ProductCard.tsx    # 상품 카드
│   │   └── CartItem.tsx       # 장바구니 아이템
│   ├── pages/                 # 페이지 컴포넌트
│   │   ├── ProductList.tsx    # 상품 목록
│   │   ├── ProductDetail.tsx  # 상품 상세
│   │   ├── Cart.tsx           # 장바구니
│   │   ├── Checkout.tsx       # 주문/결제
│   │   ├── OrderHistory.tsx   # 주문 내역
│   │   └── OrderDetail.tsx    # 주문 상세
│   ├── router/                # 라우팅 설정
│   │   └── index.tsx
│   ├── stores/                # Zustand 스토어
│   │   ├── authStore.ts       # 인증 상태
│   │   └── cartStore.ts       # 장바구니 상태
│   ├── styles/                # SCSS 스타일
│   │   └── global.scss
│   ├── types/                 # TypeScript 타입 정의
│   │   └── index.ts
│   ├── App.tsx                # 메인 앱 컴포넌트
│   ├── bootstrap.tsx          # Module Federation 마운트 함수
│   └── main.tsx               # 엔트리포인트
├── public/                    # 정적 자산
├── docs/                      # 📍 현재 위치
├── vite.config.ts             # Vite + Federation 설정
├── tsconfig.json              # TypeScript 설정
├── tailwind.config.js         # Tailwind 설정
└── package.json
```

---

## 🎯 주요 기능

### 구현 완료
- ✅ Module Federation 부트스트랩 함수
- ✅ 기본 라우팅 구조
- ✅ API 클라이언트 설정
- ✅ Zustand 스토어 (authStore, cartStore)

### 개발 중
- 🚧 상품 목록/상세 페이지
- 🚧 장바구니 기능
- 🚧 주문/결제 프로세스
- 🚧 주문 내역 조회

### 향후 계획
- 📋 Design System 통합 (React 컴포넌트)
- 📋 E2E 테스트
- 📋 성능 최적화
- 📋 접근성(A11y) 개선

---

## 🔄 Module Federation 통합

### Exposed Modules
```typescript
// vite.config.ts
federation({
  name: 'shopping-frontend',
  filename: 'remoteEntry.js',
  exposes: {
    './bootstrap': './src/bootstrap.tsx',
  },
})
```

### 통합 모드
- **Embedded Mode**: Portal Shell에서 동적 로드
- **Standalone Mode**: 독립 실행 (개발 전용)

자세한 내용은 [Module Federation 통합 가이드](./guides/federation-integration.md)를 참고하세요.

---

## 🚦 서비스 URL (로컬 개발)

| 환경 | URL | 설명 |
|------|-----|------|
| **Development** | http://localhost:30002 | Standalone 개발 모드 |
| **Portal Shell** | http://localhost:30000/shopping | Portal에 통합된 상태 |
| **Backend API** | http://localhost:8080/api/v1/shopping | Shopping Service API |

---

## 📊 문서 통계

| 유형 | 완료 | 작성 중 | 예정 | 총계 |
|------|------|---------|------|------|
| Guides | 1 | 0 | 4 | 5 |
| Architecture | 1 | 0 | 3 | 4 |
| API | 0 | 0 | 5 | 5 |
| **합계** | **2** | **0** | **12** | **14** |

---

## 🔗 관련 리소스

### 프로젝트 전체
- [Portal Universe 메인 문서](../../../README.md)
- [CLAUDE.md](../../../CLAUDE.md) - Agent 가이드

### 다른 프론트엔드 모듈
- [Portal Shell 문서](../../portal-shell/docs/)
- [Blog Frontend 문서](../../blog-frontend/docs/)
- [Design System 문서](../../design-system/docs/)

### 백엔드 서비스
- [Shopping Service 문서](../../../services/shopping-service/docs/)
- [Auth Service 문서](../../../services/auth-service/docs/)
- [API Gateway 문서](../../../services/api-gateway/docs/)

---

## 📝 문서 작성 규칙

Shopping Frontend 문서 작성 시 다음 규칙을 준수하세요:

1. **메타데이터 필수**: YAML frontmatter 포함
2. **명명 규칙**: kebab-case (예: `getting-started.md`)
3. **인덱스 업데이트**: 새 문서 추가 시 해당 디렉토리의 README 업데이트
4. **템플릿 참조**: `/docs_template/guide/` 참고

자세한 내용은 [문서화 시스템 가이드](../../../.claude/skills/documentation-system.md)를 참고하세요.

---

## 🚀 개발 시작하기

```bash
# 1. 프로젝트 루트에서 의존성 설치
cd frontend
npm install

# 2. Shopping Frontend 개발 모드 실행
npm run dev:shopping

# 3. 브라우저에서 확인
# http://localhost:30002
```

자세한 개발 가이드는 [Getting Started](./guides/getting-started.md)를 참고하세요. (🔜 예정)

---

## 📞 문의

| 채널 | 용도 |
|------|------|
| GitHub Issues | 버그 리포트, 기능 제안 |
| Slack #frontend | 일반적인 질문 |
| PR 리뷰 | 코드 리뷰 요청 |

---

**최종 업데이트**: 2026-01-18
