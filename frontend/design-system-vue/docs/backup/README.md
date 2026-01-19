# Design System - Portal Universe

## 개요

Portal Universe Design System은 마이크로 프론트엔드 아키텍처 환경에서 일관된 UI/UX를 제공하기 위한 Vue 3 컴포넌트 라이브러리입니다. Blog, Shopping 등 다양한 서비스 모듈이 공유할 수 있는 통일된 디자인 언어를 정의하고 구현합니다.

### 핵심 특징

- **Vue 3 Composition API**: `<script setup>` 문법으로 간결한 컴포넌트 구현
- **Tailwind CSS 기반**: 유틸리티 우선 CSS 프레임워크로 효율적인 스타일링
- **3-계층 토큰 시스템**: Base → Semantic → Component 계층으로 체계화된 디자인 토큰
- **서비스별 테마**: `data-service` 속성으로 Blog/Shopping 테마 동적 전환
- **명암 모드 지원**: Light/Dark 테마 자동 감지 및 수동 전환
- **Storybook 통합**: 모든 컴포넌트의 상호작용형 문서 및 테스트
- **타입 안전성**: TypeScript 완전 지원으로 개발자 경험 향상

## 프로젝트 구조

```
frontend/design-system/
├── src/
│   ├── components/          # Vue 3 컴포넌트 (21개)
│   ├── composables/         # Vue 3 컴포저블 (useTheme, useToast)
│   ├── styles/             # 글로벌 스타일
│   │   └── themes/         # 서비스별 테마
│   ├── tokens/             # 디자인 토큰 정의 (JSON)
│   │   ├── base/           # Base 토큰
│   │   ├── semantic/       # Semantic 토큰
│   │   └── themes/         # Service 테마 토큰
│   ├── types/              # TypeScript 타입 정의
│   └── index.ts            # 라이브러리 진입점
├── .storybook/             # Storybook 설정
├── dist/                   # 빌드 결과물
├── tailwind.preset.js      # Tailwind CSS 프리셋
├── docs/                   # 설명서
└── package.json
```

## 빠른 시작

### 로컬 개발

```bash
cd frontend/design-system
npm install
npm run dev                 # http://localhost:30003
npm run storybook          # http://localhost:6006
npm run build              # 빌드
npm test                   # 테스트
```

### 다른 모듈에서 사용

```typescript
import { Button, Input, Modal } from '@portal/design-system'
import '@portal/design-system/style.css'
```

## 주요 문서

| 문서 | 설명 |
|------|------|
| [ARCHITECTURE.md](./ARCHITECTURE.md) | 아키텍처, 토큰 흐름, 빌드 파이프라인 |
| [COMPONENTS.md](./COMPONENTS.md) | 21개 컴포넌트 카탈로그 및 사용 예시 |
| [TOKENS.md](./TOKENS.md) | 3-계층 토큰 시스템 상세 설명 |
| [THEMING.md](./THEMING.md) | 테마 시스템, Blog/Shopping 테마 |
| [USAGE.md](./USAGE.md) | 실제 사용 가이드 및 베스트 프랙티스 |

## 기술 스택

- Vue 3.5.21 / TypeScript 5.9
- Tailwind CSS 3.4 / Vite 7.1
- Storybook 9.1 / Vitest 4.0

## 컴포넌트 분류

**입력 (8)**: Button, Input, Textarea, Select, Checkbox, Radio, Switch, SearchBar  
**피드백 (7)**: Modal, Toast, Badge, Tag, Alert, Spinner, Skeleton  
**레이아웃 (6)**: Card, Container, Stack, Divider, FormField, Breadcrumb  
**기타 (4)**: Avatar, Link, Tabs, Dropdown

---

**다음**: [ARCHITECTURE.md](./ARCHITECTURE.md)를 읽어 아키텍처를 이해하세요.
