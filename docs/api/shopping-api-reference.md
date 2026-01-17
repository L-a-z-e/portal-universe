# Shopping Service API Reference

## 목차
1. [개요](#개요)
2. [인증](#인증)
3. [공통 응답 형식](#공통-응답-형식)
4. [에러 코드](#에러-코드)
5. [Admin 상품 관리 API](#admin-상품-관리-api)
6. [일반 사용자 상품 조회 API](#일반-사용자-상품-조회-api)
7. [API 테스트 가이드](#api-테스트-가이드)

---

## 개요

**Base URL**: `http://localhost:8080/api/shopping`

**Scope**: Shopping Service의 모든 API 엔드포인트

**Protocol**: REST API (JSON)

**Auth**: JWT Bearer Token (OpenID Connect)

### API 구조
```
/api/shopping
├── /admin/products          (ADMIN only)
│   ├── POST                 - 상품 등록
│   ├── PUT /{productId}     - 상품 수정
│   ├── DELETE /{productId}  - 상품 삭제
│   └── PATCH /{productId}/stock  - 재고 수정
├── /products
│   ├── GET /{productId}     - 상품 상세 조회
│   └── GET /{productId}/with-reviews  - 상품+리뷰 조회
```

---

## 인증

### JWT Bearer Token

모든 Admin API는 **ADMIN 역할을 가진 유효한 JWT 토큰**이 필요합니다.

#### 요청 헤더
```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

#### 토큰 발급 방법

1. **Auth Service에서 OAuth2 로그인**
   ```bash
   POST http://localhost:8080/api/auth/login
   ```

2. **응답에서 access_token 추출**
   ```json
   {
     "access_token": "eyJhbGc...",
     "token_type": "Bearer",
     "expires_in": 3600
   }
   ```

3. **다음 API 요청에 사용**
   ```bash
   curl -H "Authorization: Bearer eyJhbGc..." http://localhost:8080/api/shopping/admin/products
   ```

### 권한 확인

- **ADMIN**: 상품 생성, 수정, 삭제, 재고 관리
- **USER**: 상품 조회 (공개 API)
- **GUEST**: 상품 조회 (공개 API)

---

## 공통 응답 형식

### 성공 응답

모든 성공 응답은 다음 구조를 따릅니다:

```json
{
  "success": true,
  "data": {
    // 실제 데이터
  },
  "error": null
}
```

### 에러 응답

모든 실패 응답은 다음 구조를 따릅니다:

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "S001",
    "message": "상품을 찾을 수 없습니다"
  }
}
```

### HTTP 상태 코드

| 코드 | 의미 | 예시 |
|------|------|------|
| 200 | OK | 조회, 수정, 삭제 성공 |
| 201 | Created | 상품 생성 성공 |
| 400 | Bad Request | 유효성 검증 실패 |
| 403 | Forbidden | ADMIN 권한 없음 |
| 404 | Not Found | 상품 미존재 |
| 409 | Conflict | 중복된 상품명 |
| 500 | Internal Server Error | 서버 오류 |

---

## 에러 코드

### Shopping Service 에러 코드 (S0XX)

| 코드 | HTTP | 메시지 | 설명 | 원인 |
|------|------|--------|------|------|
| S001 | 404 | Product not found | 상품을 찾을 수 없음 | 잘못된 productId |
| S002 | 409 | Product with this name already exists | 중복된 상품명 | 이미 존재하는 상품명 |
| S003 | 400 | Product is currently inactive | 비활성화된 상품 | 상품이 비활성 상태 |
| S004 | 400 | Product price must be greater than 0 | 잘못된 가격 | price <= 0 |
| S005 | 400 | Product quantity must be greater than 0 | 잘못된 수량 | stock < 0 |
| S006 | 400 | Product name is required | 상품명 누락 | name이 비어있음 |
| S007 | 400 | Product description is required | 설명 누락 | description이 비어있음 |
| S008 | 400 | Product name must be between 1 and 200 characters | 상품명 길이 위반 | 1 <= name.length <= 200 |
| S009 | 403 | Admin permission required | ADMIN 권한 없음 | JWT에 ROLE_ADMIN 없음 |
| S010 | 400 | Cannot delete product with active orders | 주문이 있는 상품 삭제 불가 | 상품에 활성 주문 존재 |

### 공통 에러 코드 (C0XX)

| 코드 | HTTP | 메시지 | 설명 |
|------|------|--------|------|
| C001 | 400 | Invalid request parameter | 잘못된 요청 파라미터 |
| C002 | 401 | Unauthorized | 인증되지 않음 |
| C003 | 503 | Service unavailable | 다른 서비스 통신 실패 |

---

## Admin 상품 관리 API

### 1. 상품 생성

새로운 상품을 등록합니다.

**Endpoint**: `POST /api/shopping/admin/products`

**권한**: ADMIN

**Request Headers**:
```http
Authorization: Bearer <ADMIN_TOKEN>
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
| name | String | O | 1-200자 | 상품명 |
| description | String | O | 최대 2000자 | 상품 설명 |
| price | Number | O | > 0 | 가격 (원) |
| stock | Integer | O | >= 0 | 재고 수량 |

**Response (201 Created)**:
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "MacBook Pro 16",
    "description": "Apple M3 Max, 36GB RAM, 1TB SSD",
    "price": 3490000.0,
    "stock": 50
  },
  "error": null
}
```

**Error Responses**:

| HTTP | 에러 코드 | 발생 조건 |
|------|----------|----------|
| 400 | S004 | price가 0 이하 |
| 400 | S005 | stock이 0 미만 |
| 400 | S006 | name이 누락됨 |
| 400 | S007 | description이 누락됨 |
| 400 | S008 | name 길이 제한 위반 |
| 403 | S009 | ADMIN 권한 없음 |
| 409 | S002 | 동일한 상품명이 이미 존재 |

**Example - cURL**:
```bash
curl -X POST http://localhost:8080/api/shopping/admin/products \
  -H "Authorization: Bearer eyJhbGc..." \
  -H "Content-Type: application/json" \
  -d '{
    "name": "MacBook Pro 16",
    "description": "Apple M3 Max, 36GB RAM, 1TB SSD",
    "price": 3490000.0,
    "stock": 50
  }'
```

**Example - JavaScript (Fetch)**:
```javascript
const response = await fetch('http://localhost:8080/api/shopping/admin/products', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    name: 'MacBook Pro 16',
    description: 'Apple M3 Max, 36GB RAM, 1TB SSD',
    price: 3490000.0,
    stock: 50
  })
});
const data = await response.json();
```

---

### 2. 상품 수정

기존 상품의 정보를 수정합니다.

**Endpoint**: `PUT /api/shopping/admin/products/{productId}`

**권한**: ADMIN

**Path Parameters**:

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| productId | Long | O | 수정할 상품의 ID |

**Request Headers**:
```http
Authorization: Bearer <ADMIN_TOKEN>
Content-Type: application/json
```

**Request Body**:
```json
{
  "name": "MacBook Pro 16 (Updated)",
  "description": "Apple M3 Max, 36GB RAM, 2TB SSD",
  "price": 3990000.0,
  "stock": 30
}
```

**Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "MacBook Pro 16 (Updated)",
    "description": "Apple M3 Max, 36GB RAM, 2TB SSD",
    "price": 3990000.0,
    "stock": 30
  },
  "error": null
}
```

**Error Responses**:

| HTTP | 에러 코드 | 발생 조건 |
|------|----------|----------|
| 400 | S004 | price가 0 이하 |
| 400 | S005 | stock이 0 미만 |
| 400 | S008 | name 길이 제한 위반 |
| 403 | S009 | ADMIN 권한 없음 |
| 404 | S001 | 상품을 찾을 수 없음 |
| 409 | S002 | 다른 상품이 동일한 이름을 사용 중 |

**Example - cURL**:
```bash
curl -X PUT http://localhost:8080/api/shopping/admin/products/1 \
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

### 3. 상품 삭제

상품을 삭제합니다.

**Endpoint**: `DELETE /api/shopping/admin/products/{productId}`

**권한**: ADMIN

**Path Parameters**:

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| productId | Long | O | 삭제할 상품의 ID |

**Request Headers**:
```http
Authorization: Bearer <ADMIN_TOKEN>
```

**Response (200 OK)**:
```json
{
  "success": true,
  "data": null,
  "error": null
}
```

**Error Responses**:

| HTTP | 에러 코드 | 발생 조건 |
|------|----------|----------|
| 400 | S010 | 주문이 있는 상품은 삭제 불가 |
| 403 | S009 | ADMIN 권한 없음 |
| 404 | S001 | 상품을 찾을 수 없음 |

**Example - cURL**:
```bash
curl -X DELETE http://localhost:8080/api/shopping/admin/products/1 \
  -H "Authorization: Bearer eyJhbGc..."
```

---

### 4. 상품 재고 수정

특정 상품의 재고 수량을 수정합니다.

**Endpoint**: `PATCH /api/shopping/admin/products/{productId}/stock`

**권한**: ADMIN

**Path Parameters**:

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| productId | Long | O | 재고를 수정할 상품의 ID |

**Request Headers**:
```http
Authorization: Bearer <ADMIN_TOKEN>
Content-Type: application/json
```

**Request Body**:
```json
{
  "stock": 100
}
```

**Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "MacBook Pro 16",
    "description": "Apple M3 Max, 36GB RAM, 1TB SSD",
    "price": 3490000.0,
    "stock": 100
  },
  "error": null
}
```

**Error Responses**:

| HTTP | 에러 코드 | 발생 조건 |
|------|----------|----------|
| 400 | S005 | stock이 0 미만 |
| 403 | S009 | ADMIN 권한 없음 |
| 404 | S001 | 상품을 찾을 수 없음 |

**Example - cURL**:
```bash
curl -X PATCH http://localhost:8080/api/shopping/admin/products/1/stock \
  -H "Authorization: Bearer eyJhbGc..." \
  -H "Content-Type: application/json" \
  -d '{ "stock": 100 }'
```

---

## 일반 사용자 상품 조회 API

### 1. 상품 상세 조회

특정 상품의 상세 정보를 조회합니다.

**Endpoint**: `GET /api/shopping/products/{productId}`

**권한**: 없음 (Public)

**Path Parameters**:

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| productId | Long | O | 조회할 상품의 ID |

**Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "MacBook Pro 16",
    "description": "Apple M3 Max, 36GB RAM, 1TB SSD",
    "price": 3490000.0,
    "stock": 50
  },
  "error": null
}
```

**Error Responses**:

| HTTP | 에러 코드 | 발생 조건 |
|------|----------|----------|
| 404 | S001 | 상품을 찾을 수 없음 |

**Example - cURL**:
```bash
curl -X GET http://localhost:8080/api/shopping/products/1
```

---

### 2. 상품 + 리뷰 조회

상품 정보와 해당 상품의 리뷰를 함께 조회합니다.

**Endpoint**: `GET /api/shopping/products/{productId}/with-reviews`

**권한**: 없음 (Public)

**Path Parameters**:

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| productId | Long | O | 조회할 상품의 ID |

**Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "product": {
      "id": 1,
      "name": "MacBook Pro 16",
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

| HTTP | 에러 코드 | 발생 조건 |
|------|----------|----------|
| 404 | S001 | 상품을 찾을 수 없음 |
| 503 | C003 | Blog Service 통신 실패 |

**Example - cURL**:
```bash
curl -X GET http://localhost:8080/api/shopping/products/1/with-reviews
```

---

## API 테스트 가이드

### Postman 사용

#### 1. 환경 변수 설정

Postman Environment 변수로 설정:
```json
{
  "base_url": "http://localhost:8080",
  "admin_token": "YOUR_JWT_TOKEN_HERE",
  "product_id": "1"
}
```

#### 2. 상품 생성 테스트

**Request**:
```
POST {{base_url}}/api/shopping/admin/products
Authorization: Bearer {{admin_token}}
Content-Type: application/json

{
  "name": "Test Product",
  "description": "This is a test product",
  "price": 100000.0,
  "stock": 50
}
```

**Tests 탭**:
```javascript
pm.test("Status code is 201", function () {
    pm.response.to.have.status(201);
});

pm.test("Response has success true", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.success).to.eql(true);
});

pm.test("Product ID is returned", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.data.id).to.be.a('number');
    pm.environment.set("product_id", jsonData.data.id);
});
```

#### 3. 상품 수정 테스트

**Request**:
```
PUT {{base_url}}/api/shopping/admin/products/{{product_id}}
Authorization: Bearer {{admin_token}}
Content-Type: application/json

{
  "name": "Updated Product",
  "description": "Updated description",
  "price": 150000.0,
  "stock": 30
}
```

### cURL 테스트 스크립트

```bash
#!/bin/bash

# 변수 설정
BASE_URL="http://localhost:8080"
ADMIN_TOKEN="YOUR_JWT_TOKEN_HERE"

# 1. 상품 생성
echo "1. Creating product..."
CREATE_RESPONSE=$(curl -s -X POST $BASE_URL/api/shopping/admin/products \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Product",
    "description": "Test description",
    "price": 100000.0,
    "stock": 50
  }')

echo "Response: $CREATE_RESPONSE"
PRODUCT_ID=$(echo $CREATE_RESPONSE | grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*')

# 2. 상품 조회
echo "\n2. Getting product..."
curl -s -X GET $BASE_URL/api/shopping/products/$PRODUCT_ID | jq .

# 3. 상품 수정
echo "\n3. Updating product..."
curl -s -X PUT $BASE_URL/api/shopping/admin/products/$PRODUCT_ID \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Updated Product",
    "description": "Updated description",
    "price": 150000.0,
    "stock": 30
  }' | jq .

# 4. 상품 삭제
echo "\n4. Deleting product..."
curl -s -X DELETE $BASE_URL/api/shopping/admin/products/$PRODUCT_ID \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq .
```

### 테스트 시나리오

#### 시나리오 1: 정상 상품 등록 및 조회

```bash
# 1. 상품 등록
curl -X POST http://localhost:8080/api/shopping/admin/products \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "iPhone 15",
    "description": "Latest iPhone model",
    "price": 1299000.0,
    "stock": 100
  }'

# Expected: 201 Created with product data

# 2. 상품 조회
curl -X GET http://localhost:8080/api/shopping/products/1

# Expected: 200 OK with product data
```

#### 시나리오 2: 유효성 검증 실패

```bash
# 가격이 음수인 상품 등록
curl -X POST http://localhost:8080/api/shopping/admin/products \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Invalid Product",
    "description": "Test",
    "price": -100.0,
    "stock": 10
  }'

# Expected: 400 Bad Request
# {
#   "success": false,
#   "data": null,
#   "error": {
#     "code": "S004",
#     "message": "Product price must be greater than 0"
#   }
# }
```

#### 시나리오 3: 권한 검증 실패

```bash
# ADMIN 권한 없이 상품 등록 시도
curl -X POST http://localhost:8080/api/shopping/admin/products \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Product",
    "description": "Test",
    "price": 100000.0,
    "stock": 10
  }'

# Expected: 403 Forbidden
# {
#   "success": false,
#   "data": null,
#   "error": {
#     "code": "S009",
#     "message": "Admin permission required"
#   }
# }
```

---

## 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.1.0 | 2026-01-17 | Documenter Agent | Admin API, 일반 사용자 API 통합 문서화 |
| 1.0.0 | 2026-01-17 | API Designer Agent | 초기 명세 작성 |
