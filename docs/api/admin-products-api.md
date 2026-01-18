# Admin 상품 관리 API 명세서

## 개요

Shopping Service의 Admin 상품 관리 API 명세입니다. ADMIN 권한을 가진 사용자만 상품 등록/수정/삭제가 가능합니다.

**Base URL**: `http://localhost:8080/api/shopping/product` (API Gateway 경유)

**인증**: JWT Bearer Token (ADMIN 권한 필요)

## 에러 코드 정의

### 기존 Product 관련 에러 코드 (S0XX)

| 에러 코드 | HTTP 상태 | 메시지 | 설명 |
|-----------|-----------|--------|------|
| S001 | 404 | Product not found | 상품을 찾을 수 없음 |
| S002 | 409 | Product with this name already exists | 중복된 상품명 |
| S003 | 400 | Product is currently inactive | 비활성화된 상품 |
| S004 | 400 | Product price must be greater than 0 | 잘못된 가격 |
| S005 | 400 | Product quantity must be greater than 0 | 잘못된 수량 |

### 제안: 추가 에러 코드

Admin 기능 확장을 위해 다음 에러 코드 추가를 제안합니다:

| 에러 코드 | HTTP 상태 | 메시지 | 설명 |
|-----------|-----------|--------|------|
| S006 | 400 | Product name is required | 상품명 누락 |
| S007 | 400 | Product description is required | 상품 설명 누락 |
| S008 | 400 | Product name must be between 1 and 200 characters | 상품명 길이 제한 위반 |
| S009 | 403 | Admin permission required | ADMIN 권한 없음 |
| S010 | 400 | Cannot delete product with active orders | 주문이 있는 상품은 삭제 불가 |

## 공통 응답 형식

### 성공 응답
```json
{
  "success": true,
  "data": { ... },
  "error": null
}
```

### 에러 응답
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "S001",
    "message": "Product not found"
  }
}
```

## API 명세

### 1. 상품 등록 (ADMIN만)

새로운 상품을 등록합니다.

**Endpoint**: `POST /api/shopping/product`

**권한**: ADMIN

**Request Headers**:
```
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json
```

**Request Body**:
```json
{
  "name": "MacBook Pro 16",
  "description": "Apple M3 Max, 36GB RAM, 1TB SSD",
  "price": 3490000.0,
  "stock": 50
}
```

**Request Schema**:
| 필드 | 타입 | 필수 | 제약사항 | 설명 |
|------|------|------|----------|------|
| name | String | Y | 1-200자 | 상품명 |
| description | String | Y | 최대 2000자 | 상품 설명 |
| price | Double | Y | > 0 | 가격 (원) |
| stock | Integer | Y | >= 0 | 재고 수량 |

**Response (201 Created)**:
```json
{
  "success": true,
  "data": {
    "id": 1,
    "description": "Apple M3 Max, 36GB RAM, 1TB SSD",
    "price": 3490000.0,
    "stock": 50
  },
  "error": null
}
```

**Response Schema**:
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | 생성된 상품 ID |
| description | String | 상품 설명 |
| price | Double | 가격 |
| stock | Integer | 재고 수량 |

**Error Responses**:
| HTTP 상태 | 에러 코드 | 발생 조건 |
|-----------|-----------|----------|
| 400 | S004 | price가 0 이하 |
| 400 | S005 | stock이 0 미만 |
| 400 | S006 | name이 누락됨 (제안) |
| 400 | S007 | description이 누락됨 (제안) |
| 400 | S008 | name 길이 제한 위반 (제안) |
| 403 | S009 | ADMIN 권한 없음 (제안) |
| 409 | S002 | 동일한 상품명이 이미 존재 |

**Example**:
```bash
curl -X POST http://localhost:8080/api/shopping/product \
  -H "Authorization: Bearer eyJhbGc..." \
  -H "Content-Type: application/json" \
  -d '{
    "name": "MacBook Pro 16",
    "description": "Apple M3 Max, 36GB RAM, 1TB SSD",
    "price": 3490000.0,
    "stock": 50
  }'
```

---

### 2. 상품 상세 조회 (모든 사용자)

특정 상품의 상세 정보를 조회합니다.

**Endpoint**: `GET /api/shopping/product/{productId}`

**권한**: 인증 불필요 (Public)

**Path Parameters**:
| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| productId | Long | Y | 조회할 상품 ID |

**Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "id": 1,
    "description": "Apple M3 Max, 36GB RAM, 1TB SSD",
    "price": 3490000.0,
    "stock": 50
  },
  "error": null
}
```

**Error Responses**:
| HTTP 상태 | 에러 코드 | 발생 조건 |
|-----------|-----------|----------|
| 404 | S001 | 상품을 찾을 수 없음 |

**Example**:
```bash
curl -X GET http://localhost:8080/api/shopping/product/1
```

---

### 3. 상품 수정 (ADMIN만)

기존 상품 정보를 수정합니다. 모든 필드를 전체 교체합니다 (PUT 방식).

**Endpoint**: `PUT /api/shopping/product/{productId}`

**권한**: ADMIN

**Request Headers**:
```
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json
```

**Path Parameters**:
| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| productId | Long | Y | 수정할 상품 ID |

**Request Body**:
```json
{
  "name": "MacBook Pro 16 (Updated)",
  "description": "Apple M3 Max, 36GB RAM, 2TB SSD",
  "price": 3990000.0,
  "stock": 30
}
```

**Request Schema**:
| 필드 | 타입 | 필수 | 제약사항 | 설명 |
|------|------|------|----------|------|
| name | String | Y | 1-200자 | 상품명 |
| description | String | Y | 최대 2000자 | 상품 설명 |
| price | Double | Y | > 0 | 가격 (원) |
| stock | Integer | Y | >= 0 | 재고 수량 |

**Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "id": 1,
    "description": "Apple M3 Max, 36GB RAM, 2TB SSD",
    "price": 3990000.0,
    "stock": 30
  },
  "error": null
}
```

**Error Responses**:
| HTTP 상태 | 에러 코드 | 발생 조건 |
|-----------|-----------|----------|
| 400 | S004 | price가 0 이하 |
| 400 | S005 | stock이 0 미만 |
| 400 | S006 | name이 누락됨 (제안) |
| 400 | S007 | description이 누락됨 (제안) |
| 400 | S008 | name 길이 제한 위반 (제안) |
| 403 | S009 | ADMIN 권한 없음 (제안) |
| 404 | S001 | 상품을 찾을 수 없음 |
| 409 | S002 | 다른 상품이 동일한 이름을 사용 중 |

**Example**:
```bash
curl -X PUT http://localhost:8080/api/shopping/product/1 \
  -H "Authorization: Bearer eyJhbGc..." \
  -H "Content-Type: application/json" \
  -d '{
    "name": "MacBook Pro 16 (Updated)",
    "description": "Apple M3 Max, 36GB RAM, 2TB SSD",
    "price": 3990000.0,
    "stock": 30
  }'
```

---

### 4. 상품 삭제 (ADMIN만)

상품을 삭제합니다. 실제로는 Soft Delete 또는 Hard Delete를 구현할 수 있습니다.

**Endpoint**: `DELETE /api/shopping/product/{productId}`

**권한**: ADMIN

**Request Headers**:
```
Authorization: Bearer {JWT_TOKEN}
```

**Path Parameters**:
| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| productId | Long | Y | 삭제할 상품 ID |

**Response (200 OK)**:
```json
{
  "success": true,
  "data": null,
  "error": null
}
```

**Error Responses**:
| HTTP 상태 | 에러 코드 | 발생 조건 |
|-----------|-----------|----------|
| 400 | S010 | 주문이 있는 상품은 삭제 불가 (제안) |
| 403 | S009 | ADMIN 권한 없음 (제안) |
| 404 | S001 | 상품을 찾을 수 없음 |

**Example**:
```bash
curl -X DELETE http://localhost:8080/api/shopping/product/1 \
  -H "Authorization: Bearer eyJhbGc..."
```

---

### 5. 상품 + 리뷰 조회 (모든 사용자)

상품 정보와 해당 상품의 리뷰(블로그 게시물)를 함께 조회합니다. Blog Service와 Feign Client로 통신합니다.

**Endpoint**: `GET /api/shopping/product/{productId}/with-reviews`

**권한**: 인증 불필요 (Public)

**Path Parameters**:
| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| productId | Long | Y | 조회할 상품 ID |

**Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "product": {
      "id": 1,
      "description": "Apple M3 Max, 36GB RAM, 1TB SSD",
      "price": 3490000.0,
      "stock": 50
    },
    "reviews": [
      {
        "id": "blog-post-1",
        "title": "MacBook Pro 16 리뷰",
        "content": "정말 좋은 제품입니다...",
        "author": "user1",
        "createdAt": "2026-01-15T10:00:00Z"
      }
    ]
  },
  "error": null
}
```

**Error Responses**:
| HTTP 상태 | 에러 코드 | 발생 조건 |
|-----------|-----------|----------|
| 404 | S001 | 상품을 찾을 수 없음 |
| 503 | C003 | Blog Service 통신 실패 (Common) |

**Example**:
```bash
curl -X GET http://localhost:8080/api/shopping/product/1/with-reviews
```

---

## 현재 구현 상태 분석

### 구현된 기능
1. CRUD 기본 기능 모두 구현
2. ApiResponse 래퍼 적용
3. ProductService 인터페이스 분리
4. Feign Client 통합 (with-reviews)

### 누락된 기능

#### 1. Request Body Validation
현재 `ProductCreateRequest`와 `ProductUpdateRequest`에 Jakarta Validation 어노테이션이 없습니다.

**제안**:
```java
public record ProductCreateRequest(
    @NotBlank(message = "Product name is required")
    @Size(min = 1, max = 200, message = "Product name must be between 1 and 200 characters")
    String name,

    @NotBlank(message = "Product description is required")
    @Size(max = 2000, message = "Product description must not exceed 2000 characters")
    String description,

    @NotNull(message = "Product price is required")
    @Positive(message = "Product price must be greater than 0")
    Double price,

    @NotNull(message = "Product stock is required")
    @Min(value = 0, message = "Product stock must be non-negative")
    Integer stock
) {}
```

#### 2. Controller에 @Valid 어노테이션 추가
```java
@PostMapping
public ApiResponse<ProductResponse> createProduct(@Valid @RequestBody ProductCreateRequest request) {
    return ApiResponse.success(productService.createProduct(request));
}
```

#### 3. Spring Security 권한 검증
현재 코드에는 ADMIN 권한 체크가 없습니다.

**제안**:
```java
@PreAuthorize("hasRole('ADMIN')")
@PostMapping
public ApiResponse<ProductResponse> createProduct(@Valid @RequestBody ProductCreateRequest request) {
    return ApiResponse.success(productService.createProduct(request));
}
```

#### 4. ProductResponse에 name 필드 누락
현재 `ProductResponse`에 `name` 필드가 없습니다. 이는 버그로 보입니다.

**현재**:
```java
public record ProductResponse(Long id, String description, Double price, Integer stock) {}
```

**제안**:
```java
public record ProductResponse(Long id, String name, String description, Double price, Integer stock) {}
```

---

## 추가 필요 API 제안

Admin 기능을 확장하기 위해 다음 API 추가를 제안합니다:

### 1. Admin 전용 상품 목록 조회 (페이징)

**Endpoint**: `GET /api/shopping/product/admin/list`

**권한**: ADMIN

**Query Parameters**:
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| page | Integer | N | 0 | 페이지 번호 (0부터 시작) |
| size | Integer | N | 20 | 페이지 크기 |
| sort | String | N | id,desc | 정렬 (예: price,asc) |
| status | String | N | ALL | ACTIVE, INACTIVE, ALL |

**필요성**:
- 일반 사용자 목록 API는 활성 상품만 반환
- Admin은 비활성/삭제 상품도 관리 필요
- 재고 현황 한눈에 파악

**Response**:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "name": "MacBook Pro 16",
        "description": "Apple M3 Max...",
        "price": 3490000.0,
        "stock": 50,
        "status": "ACTIVE",
        "createdAt": "2026-01-15T10:00:00Z",
        "updatedAt": "2026-01-17T14:30:00Z"
      }
    ],
    "totalElements": 100,
    "totalPages": 5,
    "currentPage": 0,
    "size": 20
  },
  "error": null
}
```

### 2. 재고 일괄 수정 (Batch Update)

**Endpoint**: `PATCH /api/shopping/product/admin/stock-batch`

**권한**: ADMIN

**Request Body**:
```json
{
  "updates": [
    { "productId": 1, "stock": 100 },
    { "productId": 2, "stock": 50 },
    { "productId": 3, "stock": 0 }
  ]
}
```

**필요성**:
- 대량 입고 시 효율적인 재고 업데이트
- 여러 상품을 한 번에 수정 (Network 효율)

**Response**:
```json
{
  "success": true,
  "data": {
    "successCount": 3,
    "failedCount": 0,
    "failures": []
  },
  "error": null
}
```

### 3. 상품 상태 변경 (활성화/비활성화)

**Endpoint**: `PATCH /api/shopping/product/{productId}/status`

**권한**: ADMIN

**Request Body**:
```json
{
  "status": "INACTIVE"
}
```

**필요성**:
- 삭제 대신 비활성화로 관리
- 판매 중지 상품을 목록에서 숨김
- 나중에 재활성화 가능

**Response**:
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "MacBook Pro 16",
    "status": "INACTIVE"
  },
  "error": null
}
```

### 4. 상품 통계 조회

**Endpoint**: `GET /api/shopping/product/admin/statistics`

**권한**: ADMIN

**필요성**:
- Dashboard용 데이터
- 총 상품 수, 재고 부족 상품, 판매 중인 상품 등

**Response**:
```json
{
  "success": true,
  "data": {
    "totalProducts": 150,
    "activeProducts": 120,
    "inactiveProducts": 30,
    "lowStockProducts": 15,
    "outOfStockProducts": 5,
    "totalInventoryValue": 450000000.0
  },
  "error": null
}
```

---

## OpenAPI (Swagger) 명세

```yaml
openapi: 3.0.3
info:
  title: Shopping Service - Admin Product API
  description: Portal Universe E-commerce Admin 상품 관리 API
  version: 1.0.0
  contact:
    name: Portal Universe Team

servers:
  - url: http://localhost:8080
    description: API Gateway (Local)

tags:
  - name: Product Admin
    description: 상품 관리 API (ADMIN 전용)

paths:
  /api/shopping/product:
    post:
      tags:
        - Product Admin
      summary: 상품 등록
      description: 새로운 상품을 등록합니다. ADMIN 권한이 필요합니다.
      operationId: createProduct
      security:
        - bearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ProductCreateRequest'
      responses:
        '201':
          description: 상품 생성 성공
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ProductSuccessResponse'
        '400':
          description: 잘못된 요청 (유효성 검증 실패)
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
        '403':
          description: 권한 없음 (ADMIN 권한 필요)
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
        '409':
          description: 중복된 상품명
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'

  /api/shopping/product/{productId}:
    get:
      tags:
        - Product Admin
      summary: 상품 상세 조회
      description: 특정 상품의 상세 정보를 조회합니다. 인증이 필요하지 않습니다.
      operationId: getProductById
      parameters:
        - name: productId
          in: path
          required: true
          schema:
            type: integer
            format: int64
      responses:
        '200':
          description: 조회 성공
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ProductSuccessResponse'
        '404':
          description: 상품을 찾을 수 없음
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'

    put:
      tags:
        - Product Admin
      summary: 상품 수정
      description: 기존 상품 정보를 수정합니다. ADMIN 권한이 필요합니다.
      operationId: updateProduct
      security:
        - bearerAuth: []
      parameters:
        - name: productId
          in: path
          required: true
          schema:
            type: integer
            format: int64
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ProductUpdateRequest'
      responses:
        '200':
          description: 수정 성공
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ProductSuccessResponse'
        '400':
          description: 잘못된 요청
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
        '403':
          description: 권한 없음
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
        '404':
          description: 상품을 찾을 수 없음
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
        '409':
          description: 중복된 상품명
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'

    delete:
      tags:
        - Product Admin
      summary: 상품 삭제
      description: 상품을 삭제합니다. ADMIN 권한이 필요합니다.
      operationId: deleteProduct
      security:
        - bearerAuth: []
      parameters:
        - name: productId
          in: path
          required: true
          schema:
            type: integer
            format: int64
      responses:
        '200':
          description: 삭제 성공
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/EmptySuccessResponse'
        '400':
          description: 삭제 불가 (주문이 있는 상품)
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
        '403':
          description: 권한 없음
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
        '404':
          description: 상품을 찾을 수 없음
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'

  /api/shopping/product/{productId}/with-reviews:
    get:
      tags:
        - Product Admin
      summary: 상품 + 리뷰 조회
      description: 상품 정보와 리뷰를 함께 조회합니다. Blog Service와 통신합니다.
      operationId: getProductWithReviews
      parameters:
        - name: productId
          in: path
          required: true
          schema:
            type: integer
            format: int64
      responses:
        '200':
          description: 조회 성공
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ProductWithReviewsSuccessResponse'
        '404':
          description: 상품을 찾을 수 없음
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
        '503':
          description: Blog Service 통신 실패
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'

components:
  securitySchemes:
    bearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT
      description: JWT 토큰 (ADMIN 권한 필요)

  schemas:
    ProductCreateRequest:
      type: object
      required:
        - name
        - description
        - price
        - stock
      properties:
        name:
          type: string
          minLength: 1
          maxLength: 200
          description: 상품명
          example: MacBook Pro 16
        description:
          type: string
          maxLength: 2000
          description: 상품 설명
          example: Apple M3 Max, 36GB RAM, 1TB SSD
        price:
          type: number
          format: double
          minimum: 0
          exclusiveMinimum: true
          description: 가격 (원)
          example: 3490000.0
        stock:
          type: integer
          format: int32
          minimum: 0
          description: 재고 수량
          example: 50

    ProductUpdateRequest:
      type: object
      required:
        - name
        - description
        - price
        - stock
      properties:
        name:
          type: string
          minLength: 1
          maxLength: 200
          description: 상품명
          example: MacBook Pro 16 (Updated)
        description:
          type: string
          maxLength: 2000
          description: 상품 설명
          example: Apple M3 Max, 36GB RAM, 2TB SSD
        price:
          type: number
          format: double
          minimum: 0
          exclusiveMinimum: true
          description: 가격 (원)
          example: 3990000.0
        stock:
          type: integer
          format: int32
          minimum: 0
          description: 재고 수량
          example: 30

    ProductResponse:
      type: object
      properties:
        id:
          type: integer
          format: int64
          description: 상품 ID
          example: 1
        description:
          type: string
          description: 상품 설명
          example: Apple M3 Max, 36GB RAM, 1TB SSD
        price:
          type: number
          format: double
          description: 가격
          example: 3490000.0
        stock:
          type: integer
          format: int32
          description: 재고 수량
          example: 50

    ProductSuccessResponse:
      type: object
      properties:
        success:
          type: boolean
          example: true
        data:
          $ref: '#/components/schemas/ProductResponse'
        error:
          type: object
          nullable: true
          example: null

    ProductWithReviewsResponse:
      type: object
      properties:
        product:
          $ref: '#/components/schemas/ProductResponse'
        reviews:
          type: array
          items:
            type: object
            properties:
              id:
                type: string
                example: blog-post-1
              title:
                type: string
                example: MacBook Pro 16 리뷰
              content:
                type: string
                example: 정말 좋은 제품입니다...
              author:
                type: string
                example: user1
              createdAt:
                type: string
                format: date-time
                example: 2026-01-15T10:00:00Z

    ProductWithReviewsSuccessResponse:
      type: object
      properties:
        success:
          type: boolean
          example: true
        data:
          $ref: '#/components/schemas/ProductWithReviewsResponse'
        error:
          type: object
          nullable: true
          example: null

    EmptySuccessResponse:
      type: object
      properties:
        success:
          type: boolean
          example: true
        data:
          type: object
          nullable: true
          example: null
        error:
          type: object
          nullable: true
          example: null

    ErrorResponse:
      type: object
      properties:
        success:
          type: boolean
          example: false
        data:
          type: object
          nullable: true
          example: null
        error:
          type: object
          properties:
            code:
              type: string
              description: 에러 코드
              example: S001
            message:
              type: string
              description: 에러 메시지
              example: Product not found
```

---

## 구현 우선순위

### 우선순위 1 (즉시 수정 필요)
1. `ProductResponse`에 `name` 필드 추가 (버그 수정)
2. Request DTO에 `@Valid` 어노테이션 추가
3. Controller 메서드에 `@PreAuthorize("hasRole('ADMIN')")` 추가

### 우선순위 2 (단기 개선)
4. 추가 에러 코드 정의 (S006-S010)
5. Jakarta Validation 어노테이션 추가

### 우선순위 3 (중기 개선)
6. Admin 전용 상품 목록 API 구현
7. 상품 상태 관리 (ACTIVE/INACTIVE) 기능 추가

### 우선순위 4 (장기 개선)
8. 재고 일괄 수정 API
9. 상품 통계 API
10. Soft Delete 구현

---

## API 테스트 가이드

### Postman Collection 예시

**Environment Variables**:
```json
{
  "base_url": "http://localhost:8080",
  "admin_token": "eyJhbGc...",
  "product_id": "1"
}
```

**Test Scripts**:
```javascript
// POST /api/shopping/product
pm.test("Status code is 201", function () {
    pm.response.to.have.status(201);
});

pm.test("Response has success true", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.success).to.eql(true);
});

pm.test("Response has product id", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.data.id).to.be.a('number');
    pm.environment.set("product_id", jsonData.data.id);
});
```

---

## 보안 고려사항

1. **인증/인가**
   - API Gateway에서 JWT 검증
   - ADMIN 권한 체크는 서비스 레벨에서 `@PreAuthorize`로 이중 검증

2. **입력 검증**
   - Jakarta Validation으로 1차 검증
   - 비즈니스 로직에서 2차 검증 (중복 체크 등)

3. **에러 메시지**
   - 에러 메시지에 민감한 정보 포함하지 않음
   - 에러 코드로 클라이언트가 처리 가능하도록 설계

4. **Rate Limiting**
   - API Gateway에서 Admin API에 대한 Rate Limit 설정 권장

---

## 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0.0 | 2026-01-17 | API Designer Agent | 초기 명세 작성 |
