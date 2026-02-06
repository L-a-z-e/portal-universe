# Admin 상품 관리 가이드

## 목차
1. [개요](#개요)
2. [Admin 접근 방법](#admin-접근-방법)
3. [상품 목록 조회](#상품-목록-조회)
4. [상품 등록](#상품-등록)
5. [상품 수정](#상품-수정)
6. [상품 삭제](#상품-삭제)
7. [재고 관리](#재고-관리)
8. [권한 및 역할](#권한-및-역할)
9. [문제 해결](#문제-해결)

---

## 개요

이 가이드는 Portal Universe 관리자(Admin)가 상품을 관리하는 방법을 설명합니다.

### Admin 페이지의 주요 기능

- 상품 등록, 수정, 삭제
- 상품 정보 및 재고 관리
- 상품 목록 조회 및 필터링
- 일괄 재고 수정

### 용어

| 용어 | 설명 |
|------|------|
| Admin | 관리자 권한을 가진 사용자 |
| 상품(Product) | 판매하는 아이템 |
| 재고(Stock) | 상품의 수량 |
| SKU | 상품 고유 식별자 |

---

## Admin 접근 방법

### 1. Admin 로그인

#### Step 1: Portal Shell 접속
```
http://localhost:30000
```

#### Step 2: 로그인
- 우측 상단의 **[Login]** 버튼 클릭
- 관리자 계정으로 로그인 (ADMIN 역할 필요)

```
Email: admin@portal-universe.com
Password: (관리자 암호)
```

#### Step 3: Admin 페이지 이동
로그인 후 상단 메뉴에서 **[Admin Dashboard]** 또는 **[Products]** 클릭

```
URL: http://localhost:30000/admin/products
```

### 2. Admin 페이지 구조

Admin 페이지는 3가지 섹션으로 구성됩니다:

```
┌────────────────────────────────────────────┐
│  Header (Portal Logo | Welcome, Admin | Logout)
├────────┬─────────────────────────────────┤
│Sidebar │  Main Content                    │
│        │  - Product List                  │
│- Dashboard
│- Products  ◄─ 현재 위치
│- Orders
│- Users
│- Settings
└────────┴─────────────────────────────────┘
```

---

## 상품 목록 조회

### 상품 목록 페이지

#### URL
```
http://localhost:30000/admin/products
```

#### 페이지 구성

**1. 헤더 영역**
```
[Products]                [+ New Product 버튼]
```
- 페이지 제목: "Products"
- "+ New Product": 새 상품 등록 버튼

**2. 데이터 테이블**

테이블에는 다음 열이 있습니다:

| 열 | 설명 | 정렬 |
|----|------|------|
| ID | 상품 고유 번호 | O |
| Name | 상품명 | O |
| Price | 가격 (원) | O |
| Category | 카테고리 | - |
| Actions | 편집/삭제 버튼 | - |

**3. 페이지네이션**

- 기본 페이지당 10개 상품 표시
- 하단에 페이지 번호 표시
- [Previous] [1] [2] [3] [Next] 형태

### 상품 검색 및 필터

현재 구현된 기능:
- 상품명으로 검색 (예: "MacBook" 입력)
- 정렬 기능 (가격순, 최신순 등)

예정된 기능:
- 카테고리 필터
- 가격 범위 필터
- 재고 상태 필터 (입고예정, 판매중, 품절)

### 상품 클릭

테이블의 상품 행을 클릭하면 **상품 수정 페이지**로 이동합니다.

---

## 상품 등록

### 1. 상품 등록 페이지 접근

#### Option A: "New Product" 버튼
```
1. 상품 목록 페이지 (http://localhost:30000/admin/products)
2. 우측 상단의 [+ New Product] 버튼 클릭
```

#### Option B: URL 직접 접근
```
http://localhost:30000/admin/products/new
```

### 2. 상품 등록 폼

#### 페이지 구성

```
┌────────────────────────────────────────┐
│ [New Product]                [Back]    │
├────────────────────────────────────────┤
│ Form Card                               │
│ ┌──────────────────────────────────────┐│
│ │ Product Name * (필수)                 ││
│ │ [입력 필드]                            ││
│ │                                       ││
│ │ Description (필수)                    ││
│ │ [텍스트 에어리어]                     ││
│ │                                       ││
│ │ Price *              Stock *          ││
│ │ [숫자]               [숫자]            ││
│ │                                       ││
│ │ Image URL                             ││
│ │ [입력 필드]                            ││
│ │                                       ││
│ │ Category                              ││
│ │ [입력 필드]                            ││
│ │                                       ││
│ │ [Cancel] [Create Product]             ││
│ └──────────────────────────────────────┘│
└────────────────────────────────────────┘
```

### 3. 필드별 입력 가이드

#### Product Name (필수)
- **제약**: 1-200자
- **예시**: "MacBook Pro 16", "iPhone 15 Pro Max"
- **팁**: 고객이 검색하기 쉬운 명칭 사용

```
입력 예시:
✓ MacBook Pro 16 inch 2024
✓ Samsung Galaxy S24 Ultra 256GB Black
✗ 상품 (너무 일반적)
✗ a (너무 짧음)
```

#### Description (필수)
- **제약**: 최대 2000자
- **예시**: 상세 스펙, 기능, 특징
- **팁**: 줄바꿈을 사용하여 가독성 높이기

```
입력 예시:
MacBook Pro 16
- Apple M3 Max 칩셋
- 36GB 통합 메모리
- 1TB SSD 스토리지
- Liquid Retina XDR 디스플레이
- 최대 22시간 배터리 수명

지원: 3년 제한 보증
```

#### Price (필수)
- **제약**: 0보다 큼 (원 단위)
- **예시**: `3490000`, `99900`
- **팁**: 소수점 가능 (예: `99.99`)

```
입력 예시:
✓ 3490000
✓ 99.99
✗ -1000 (음수 불가)
✗ 0 (0 불가)
```

#### Stock (필수)
- **제약**: 0 이상의 정수
- **예시**: `50`, `0`
- **팁**: 초기 재고가 없으면 0으로 시작 가능

```
입력 예시:
✓ 50
✓ 0
✗ -1 (음수 불가)
✗ 5.5 (소수점 불가)
```

#### Image URL (선택)
- **제약**: 유효한 URL 형식
- **예시**: `https://example.com/product.jpg`
- **팁**: HTTPS 권장

```
입력 예시:
✓ https://s3.amazonaws.com/products/macbook.jpg
✓ https://cdn.example.com/images/iphone.png
✗ /local/path/image.jpg
```

#### Category (선택)
- **제약**: 자유 입력 (지정된 카테고리 없음)
- **예시**: "Electronics", "Clothing", "Books"
- **팁**: 일관된 카테고리명 사용 권장

```
입력 예시:
✓ Electronics
✓ Computers
✓ Mobile Devices
```

### 4. 유효성 검증

폼 제출 전 자동으로 검증됩니다:

| 필드 | 검증 규칙 | 오류 메시지 |
|------|----------|-----------|
| Product Name | 1-200자 필수 | "Product name is required" 또는 "Product name must be less than 200 characters" |
| Description | 1-2000자 필수 | "Description is required" 또는 "Description must be less than 2000 characters" |
| Price | 0보다 큼 | "Price must be greater than or equal to 0" |
| Stock | 0 이상 정수 | "Stock must be greater than or equal to 0" |

### 5. 상품 등록 완료

#### 성공
```
✓ "Product created successfully!" 메시지 표시
✓ 자동으로 상품 목록 페이지로 이동
✓ 새 상품이 목록 첫 번째 페이지에 표시
```

#### 실패
```
✗ 오류 메시지 표시
✗ 폼 데이터 유지 (재수정 가능)
✗ 예시 오류:
  - "Product with this name already exists" (중복된 이름)
  - "Admin permission required" (권한 부족)
```

---

## 상품 수정

### 1. 상품 수정 페이지 접근

#### Option A: 목록에서 클릭
```
1. 상품 목록 페이지
2. 수정할 상품의 행 클릭
   또는 연필 아이콘(Edit) 버튼 클릭
```

#### Option B: URL 직접 접근
```
http://localhost:30000/admin/products/{productId}
예: http://localhost:30000/admin/products/123
```

### 2. 상품 수정 폼

#### 페이지 구성

```
┌────────────────────────────────────────┐
│ [Edit Product]                [Back]   │
├────────────────────────────────────────┤
│ (상품 등록 폼과 동일한 구조)            │
│ - 기존 데이터가 미리 입력되어 있음     │
│ - [Update Product] 버튼                │
└────────────────────────────────────────┘
```

### 3. 수정 절차

#### Step 1: 수정할 필드 변경
- 필요한 정보만 수정
- 필수 필드(Product Name, Description, Price, Stock)는 반드시 입력

#### Step 2: [Update Product] 클릭
- 유효성 검증 수행
- 동일한 이름의 다른 상품 확인

#### Step 3: 완료
```
✓ "Product updated successfully!" 메시지
✓ 상품 목록으로 자동 이동
✓ 변경사항 반영됨
```

### 4. 주의사항

#### 가격 수정
- 가격 변경 후 판매 이력이 있으면 기존 주문에는 영향 없음
- 새 주문부터 새 가격 적용

#### 상품명 수정
- 중복 확인: 다른 상품과 동일한 이름 불가
- 오류: "Product with this name already exists"

#### 재고 수정
- 음수 불가 (최소값: 0)
- 판매 중인 상품의 재고 감소 가능

---

## 상품 삭제

### 1. 삭제 방법

#### Option A: 목록에서 삭제
```
1. 상품 목록 페이지
2. 삭제할 상품의 휴지통 아이콘 클릭
3. 확인 모달 표시
```

#### Option B: 수정 페이지에서 삭제
```
(향후 구현 예정)
상품 수정 페이지 하단에 [Delete] 버튼
```

### 2. 삭제 확인 모달

```
┌─────────────────────────────────┐
│ Delete Product?                 │
├─────────────────────────────────┤
│                                 │
│ Are you sure you want to        │
│ delete "MacBook Pro 16"?        │
│                                 │
│ This action cannot be undone.   │
│                                 │
│ [Cancel]  [Delete]              │
└─────────────────────────────────┘
```

### 3. 삭제 가능 여부

#### 삭제 가능
```
✓ 재고가 있는 상품
✓ 판매되지 않은 상품
✓ 비활성 상품
```

#### 삭제 불가
```
✗ 활성 주문이 있는 상품
✗ 오류 메시지: "Cannot delete product with active orders"
✗ 해결 방법: 주문이 완료되기를 기다린 후 삭제
```

### 4. 삭제 후

```
✓ "Product deleted successfully!" 메시지
✓ 상품 목록에서 제거
✓ 복구 불가능 (신중하게 진행)
```

---

## 재고 관리

### 1. 재고 수정 (개별)

#### 방법 A: 수정 폼에서 수정
```
1. 상품 목록에서 상품 선택
2. "Edit Product" 페이지로 이동
3. "Stock" 필드 변경
4. [Update Product] 클릭
```

#### 방법 B: PATCH API 직접 사용
```bash
curl -X PATCH http://localhost:8080/api/shopping/admin/products/1/stock \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{ "stock": 100 }'
```

### 2. 재고 알림 (향후 기능)

#### 예정 기능
- 재고 부족 알림 (< 10개)
- 품절 알림 (0개)
- 입고 예정 표시

#### 현재 표시
```
Stock Badge 색상:
- 초록색 (10개 이상): 충분
- 노란색 (1-10개): 주의
- 빨간색 (0개): 품절
```

---

## 권한 및 역할

### 1. Admin 권한 확인

#### 권한이 있는 경우
```
✓ Admin 페이지 접근 가능
✓ 상품 등록/수정/삭제 가능
✓ 재고 관리 가능
```

#### 권한이 없는 경우
```
✗ Admin 페이지 접근 불가 → 403 Forbidden
✗ 상품 관리 API 호출 불가
✗ 오류 메시지: "Admin permission required"
```

### 2. 역할 기반 접근 제어 (RBAC)

| 역할 | 상품 조회 | 상품 등록 | 상품 수정 | 상품 삭제 |
|------|----------|----------|----------|----------|
| GUEST | O | X | X | X |
| USER | O | X | X | X |
| ADMIN | O | O | O | O |

### 3. 권한 요청

Admin 권한이 필요한 경우:

1. **시스템 관리자에게 문의**
   - Email: admin@portal-universe.com
   - 직책과 사유 명시

2. **권한 부여 프로세스**
   - 승인자 검토
   - 데이터베이스 업데이트
   - 로그인 다시 시도

---

## 문제 해결

### 문제 1: Admin 페이지에 접근할 수 없습니다

#### 증상
```
- /admin/products URL 접속 시 403 Forbidden 오류
- Admin Dashboard 메뉴가 보이지 않음
```

#### 원인 및 해결

| 원인 | 확인 방법 | 해결 방법 |
|------|----------|----------|
| ADMIN 권한 부족 | 로그인 사용자 확인 | Admin 권한 신청 |
| JWT 토큰 만료 | 브라우저 Console 확인 | 다시 로그인 |
| 서버 오류 | 브라우저 Network 탭 확인 | 서버 상태 확인 (Kubernetes) |

#### 디버깅

```javascript
// 브라우저 Console에서 실행
// 현재 JWT 토큰 확인
console.log(localStorage.getItem('token'));

// 토큰 디코드 (jwt.io에서 확인)
// 역할(roles) 필드에 'ROLE_ADMIN' 확인
```

### 문제 2: 상품 등록 시 "Product with this name already exists" 오류

#### 증상
```
- 새 상품 등록 시도 시 409 Conflict 오류
- 오류 메시지: "Product with this name already exists"
```

#### 해결 방법

```
1. 상품 목록에서 같은 이름 검색
2. 다른 이름으로 변경 후 등록
3. 기존 상품을 수정하려면:
   - 상품 목록에서 해당 상품 선택
   - 수정 페이지에서 변경
   - [Update Product] 클릭
```

### 문제 3: 상품을 삭제할 수 없습니다

#### 증상
```
- Delete 버튼 클릭 후 오류: "Cannot delete product with active orders"
```

#### 해결 방법

```
1. 해당 상품의 활성 주문 확인
2. 주문 완료 또는 취소 기다리기
3. 또는 상품을 비활성화 (향후 기능)
4. 시간이 지난 후 삭제 시도
```

### 문제 4: 가격 수정 후 반영이 안 됩니다

#### 증상
```
- Product 목록에서 가격이 여전히 이전 가격으로 표시
- 새로고침 후에도 안 됨
```

#### 해결 방법

```
1. 전체 페이지 새로고침 (Ctrl+F5 또는 Cmd+Shift+R)
2. 브라우저 캐시 삭제
3. 개발자 도구에서 Network 탭 확인
   - Status 200 응답 확인
   - 응답 데이터 확인
```

### 문제 5: API 요청이 시간 초과됩니다

#### 증상
```
- 상품 등록/수정/삭제 시 요청이 응답 없음
- 브라우저: "Request timeout"
```

#### 해결 방법

```
1. 네트워크 연결 확인
   - ping http://localhost:8080

2. Backend 서버 상태 확인
   - http://localhost:8080/actuator/health

3. 로그 확인
   - docker logs shopping-service

4. 서버 재시작
   - docker restart shopping-service
```

### 문제 6: 로그인 후에도 권한 오류가 발생합니다

#### 증상
```
- 로그인은 성공했으나 Admin API 호출 시 403 Forbidden
```

#### 원인
```
- JWT 토큰에 ROLE_ADMIN 없음
- 또는 토큰이 만료됨
```

#### 해결 방법

```
1. 로그아웃 후 다시 로그인
2. 토큰 새로고침 (자동)
3. 브라우저 캐시 삭제
4. Private/Incognito 모드에서 테스트
```

### 디버깅 체크리스트

Admin 기능에 문제가 있을 때 순서대로 확인:

```
□ 로그인 상태 확인
□ JWT 토큰 확인 (LocalStorage: token)
□ Admin 권한 확인 (토큰에 ROLE_ADMIN)
□ 브라우저 콘솔 오류 확인 (F12)
□ Network 탭에서 API 응답 확인
□ Backend 서버 상태 확인
  □ http://localhost:8080/actuator/health
  □ Database 연결 확인
□ 캐시 삭제 후 재시도
□ Private 창에서 테스트
```

### 로그 위치

문제 디버깅을 위한 로그 위치:

```
Backend (Java Spring Boot):
- docker logs -f shopping-service
- /var/log/application.log

Frontend (React):
- 브라우저 Developer Tools (F12)
  - Console 탭: 에러 메시지
  - Network 탭: API 요청/응답
  - Application 탭: LocalStorage (토큰)

Database:
- MySQL: mysql -u root -p
  - SELECT * FROM products;
  - SELECT * FROM users WHERE role = 'ADMIN';
```

---

## 유용한 팁과 트릭

### 팁 1: 빠른 상품 추가

여러 상품을 빠르게 등록하려면:

```
1. 첫 상품 등록 완료
2. 상품 목록 페이지에서 [+ New Product]
3. 다음 상품 정보 입력
4. 반복
```

### 팁 2: 엑셀에서 데이터 준비

대량 상품 등록 시:

```
1. 상품 정보를 Excel에 정리
   A열: 상품명
   B열: 설명
   C열: 가격
   D열: 재고

2. 한 행씩 Admin 페이지에 입력
3. (향후: CSV 일괄 업로드 기능 예정)
```

### 팁 3: URL 직접 사용

자주 사용하는 페이지는 북마크:

```
상품 목록: http://localhost:30000/admin/products
새 상품:   http://localhost:30000/admin/products/new
```

### 팁 4: 브라우저 개발자 도구

API 상태 확인:

```
1. F12 열기
2. Network 탭 클릭
3. 상품 작업 수행
4. 요청 선택하여 확인
   - 응답 상태 코드
   - 응답 본문
   - 에러 메시지
```

---

## 추가 지원

### 문서
- [API Reference](../api/shopping-api-reference.md)
- [Admin API Design](../api/admin-products-api.md)

### 연락처
- Technical: dev-team@portal-universe.com
- Product: product@portal-universe.com
- Support: support@portal-universe.com

### 피드백
- GitHub Issues: [portal-universe/issues](https://github.com/L-a-z-e/portal-universe/issues)
- 개선 제안: submit-feedback@portal-universe.com

---

## 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0.0 | 2026-01-17 | Documenter Agent | 초기 Admin 사용 가이드 작성 |
