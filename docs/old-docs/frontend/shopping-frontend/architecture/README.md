# Architecture 문서

> Shopping Frontend의 아키텍처 문서 목록

---

## 📋 문서 목록

| ID | 제목 | 상태 | 최종 업데이트 |
|----|------|------|---------------|
| arch-system-overview | [System Overview](./system-overview.md) | current | 2026-01-18 |
| arch-data-flow | [Data Flow](./data-flow.md) | current | 2026-01-18 |

---

## 📌 마지막 ID

- **arch-data-flow** (2026-01-18)

---

## 📁 문서 구조

```
architecture/
├── README.md               # 이 파일
├── system-overview.md      # ✅ 시스템 전체 구조
└── data-flow.md            # ✅ 데이터 흐름, 상태 관리, API 통신
```

---

## ✅ 작성 완료

- [x] system-overview.md - React 18 기반 Module Federation Remote 구조
- [x] data-flow.md - 상품 조회, 장바구니, 주문 흐름, 테마/라우팅 동기화

---

## 🚧 작성 예정

- [ ] module-federation.md - Portal Shell 통합 상세
- [ ] state-management.md - Zustand Store 구조
- [ ] router-architecture.md - React Router v7 구조 및 네비게이션 패턴
- [ ] theme-system.md - 테마 시스템 및 디자인 토큰

---

**최종 업데이트**: 2026-01-18
