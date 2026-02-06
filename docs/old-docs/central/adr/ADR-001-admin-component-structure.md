# ADR-001: Admin 컴포넌트 구조

## 상태
**Accepted**

## 날짜
2026-01-17

---

## 컨텍스트

E-commerce Admin 기능을 구현하기 위해 shopping-frontend(React)에 상품 관리 UI를 추가해야 합니다. Admin 페이지는 다음과 같은 특성을 가집니다:

- 고객용 페이지와는 별도의 인터페이스 필요
- 상품 CRUD(생성, 조회, 수정, 삭제) 기능 필수
- 데이터 테이블, 폼, 모달 등 여러 UI 컴포넌트 필요
- 권한 검증 로직 포함
- 향후 확장 가능성 (재고 관리, 주문 관리 등)

## 결정

**Admin 페이지는 3계층 컴포넌트 구조(Pages → Containers → UI Components)를 채택합니다.**

### 구조 정의

```
Admin Pages (페이지 레벨)
  ↓
Containers (비즈니스 로직, 상태 관리)
  ↓
UI Components (순수 프레젠테이션)
```

### 세부 구현 방식

#### 1. Pages Layer (페이지 레벨)
- **역할**: 라우트와 직접 연결되는 최상위 컴포넌트
- **책임**: 전체 페이지 레이아웃, 페이지 간 네비게이션
- **예시**:
  - `AdminProductListPage.tsx` - 상품 목록 페이지
  - `AdminProductFormPage.tsx` - 상품 생성/수정 페이지
  - `AdminDashboardPage.tsx` - 관리자 대시보드

#### 2. Containers Layer (컨테이너 레벨)
- **역할**: 비즈니스 로직 처리 및 상태 관리
- **책임**:
  - React Query를 통한 API 호출 및 캐싱
  - 폼 상태(React Hook Form + Zod)
  - 모달/토스트 UI 상태
  - 데이터 정렬/필터링 로직
- **특징**: UI 로직은 없고 데이터와 로직만 담당
- **예시**:
  - `ProductListContainer.tsx` - 상품 목록 데이터 조회 및 상태 관리
  - `ProductFormContainer.tsx` - 상품 폼 데이터 및 제출 로직
  - `DeleteConfirmContainer.tsx` - 삭제 확인 모달 상태

#### 3. UI Components Layer (프레젠테이션)
- **역할**: 순수 프레젠테이션 컴포넌트
- **책임**: UI 렌더링만 담당 (Props로 모든 데이터/콜백 수신)
- **특징**:
  - 재사용 가능 (다른 페이지에서도 사용 가능)
  - 테스트 용이 (Props만으로 테스트 가능)
  - 상태 비관리 (Presentational Component)
- **예시**:
  - 공용 컴포넌트: `DataTable.tsx`, `Pagination.tsx`, `SearchInput.tsx`
  - 폼 컴포넌트: `FormField.tsx`, `Input.tsx`, `Select.tsx`
  - 피드백: `ConfirmModal.tsx`, `Toast.tsx`, `LoadingSpinner.tsx`

---

## 대안 검토

| 대안 | 장점 | 단점 | 선택 이유 |
|------|------|------|----------|
| **단순 구조 (Pages만)** | 간단함, 빠른 구현 | 재사용성 낮음, 유지보수 어려움, 테스트 어려움 | ❌ |
| **3계층 (Pages → Containers → UI)** | 재사용성 높음, 테스트 용이, 관심사 분리 명확 | 초기 구현 시간 증가 | ✅ **선택** |
| **Atomic Design** | 매우 체계적, 확장성 우수 | 복잡도 높음, 학습 곡선 가팔, 오버엔지니어링 위험 | ❌ |
| **Redux/MobX** | 복잡한 상태 관리 가능 | 보일러플레이트 증가, React Query와 중복 |  ❌ |

---

## 결과

### 긍정적 영향

1. **재사용성 증대**
   - UI 컴포넌트를 다양한 페이지에서 재사용 가능
   - 예: DataTable은 상품목록, 주문목록, 재고목록 등에서 사용

2. **테스트 용이성**
   - UI 컴포넌트: Props만으로 단위 테스트 작성 가능
   - Container: 비즈니스 로직 테스트 독립적으로 수행 가능
   - Pages: E2E 테스트로 전체 플로우 검증

3. **유지보수성 개선**
   - 관심사 분리로 각 계층의 책임 명확
   - 버그 수정 시 어느 계층에서 문제인지 빠른 파악
   - 디자인 변경 시 UI 컴포넌트만 수정

4. **확장 가능성**
   - 새로운 Admin 기능 추가 시 패턴 일관성 유지
   - 재고 관리, 주문 관리 등 추가 페이지 개발 용이

### 부정적 영향

1. **초기 구현 시간 증가**
   - 파일 구조 복잡도 증가 (폴더 계층)
   - 보일러플레이트 코드 증가 가능성

2. **팀 학습 곡선**
   - 새로운 팀원이 패턴 이해 필요
   - Props drilling 주의 필요

### 완화 방안

1. 타입스크립트로 Props 인터페이스 명확히 정의
2. 스토리북으로 UI 컴포넌트 카탈로그 작성
3. 개발 가이드 문서 제공 (폴더 구조, 패턴 설명)

---

## 폴더 구조 예시

```
frontend/shopping-frontend/src/
├── components/
│   ├── admin/                 # Admin 전용 컴포넌트
│   │   ├── ProductTable.tsx
│   │   ├── ProductForm.tsx
│   │   └── ProductStats.tsx
│   │
│   ├── common/                # 공용 UI 컴포넌트
│   │   ├── DataTable.tsx
│   │   ├── Pagination.tsx
│   │   ├── SearchInput.tsx
│   │   ├── ConfirmModal.tsx
│   │   ├── Toast.tsx
│   │   └── LoadingSpinner.tsx
│   │
│   ├── form/                  # 폼 관련 컴포넌트
│   │   ├── FormField.tsx
│   │   ├── Input.tsx
│   │   ├── Select.tsx
│   │   └── NumberInput.tsx
│   │
│   └── guards/                # Route Guard 컴포넌트
│       ├── RequireAuth.tsx
│       └── RequireRole.tsx
│
├── pages/
│   ├── admin/                 # Admin 페이지
│   │   ├── AdminDashboardPage.tsx
│   │   ├── AdminProductListPage.tsx
│   │   └── AdminProductFormPage.tsx
│   └── ...
│
├── hooks/
│   ├── useAdminProducts.ts    # Container 로직 (hooks 형식)
│   ├── useErrorHandler.ts
│   └── useConfirm.ts
│
└── ...
```

---

## 구현 가이드

### 1. UI Component 작성 예

```typescript
// components/common/DataTable.tsx
interface DataTableProps<T> {
  data: T[];
  columns: TableColumn<T>[];
  actions?: TableAction<T>[];
  loading?: boolean;
  onSort?: (column: string, order: 'asc' | 'desc') => void;
}

export const DataTable: React.FC<DataTableProps> = ({
  data,
  columns,
  actions,
  loading,
  onSort
}) => {
  // 순수 렌더링 로직만
  return (
    <table>
      {/* ... */}
    </table>
  );
};
```

### 2. Container (Hook) 작성 예

```typescript
// hooks/useAdminProducts.ts
export const useAdminProducts = (filters: ProductFilters) => {
  // React Query를 통한 API 호출
  const { data, isLoading, error } = useQuery({
    queryKey: productKeys.list(filters),
    queryFn: () => productApi.getProducts(filters),
  });

  // 정렬 로직
  const handleSort = (column: string, order: 'asc' | 'desc') => {
    // ...
  };

  return { data, isLoading, error, handleSort };
};
```

### 3. Page 작성 예

```typescript
// pages/admin/AdminProductListPage.tsx
const AdminProductListPage: React.FC = () => {
  const [filters, setFilters] = useState<ProductFilters>({
    page: 0,
    size: 20,
  });

  // Container 로직
  const { data, isLoading, handleSort } = useAdminProducts(filters);

  return (
    <AdminLayout>
      <h1>Product Management</h1>

      {/* UI 컴포넌트 사용 */}
      <DataTable
        data={data?.content || []}
        columns={productColumns}
        loading={isLoading}
        onSort={handleSort}
      />

      <Pagination
        currentPage={filters.page}
        totalPages={data?.totalPages || 0}
        onPageChange={(page) => setFilters({ ...filters, page })}
      />
    </AdminLayout>
  );
};
```

---

## 참고 자료

- 참고 문서: `/Users/laze/Laze/Project/portal-universe/docs/architecture/admin-product-management.md`
- React 패턴: Container/Presentational Pattern
- 폴더 구조 상세: admin-product-management.md의 "6. 폴더 구조 제안" 참조

---

## 다음 단계

1. 폴더 구조 생성
2. 기본 UI 컴포넌트부터 구현
3. Container (Hooks) 구현
4. Pages 통합
5. 스토리북 작성

---

**문서 버전**: 1.0
**작성자**: Documenter Agent
**최종 업데이트**: 2026-01-17
