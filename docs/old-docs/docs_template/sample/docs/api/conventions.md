---
id: api-conventions
title: API 설계 규칙
type: api
status: current
created: 2026-01-18
updated: 2026-01-18
author: Laze
tags: [api, conventions, restful]
---

# 📐 API Conventions

## 기본 규칙

### URL 구조
```
/api/v{version}/{resource}
/api/v{version}/{resource}/{id}
/api/v{version}/{resource}/{id}/{sub-resource}
```

### HTTP Methods
| Method | 용도 | 예시 |
|--------|------|------|
| GET | 조회 | `GET /api/v1/products` |
| POST | 생성 | `POST /api/v1/products` |
| PUT | 전체 수정 | `PUT /api/v1/products/{id}` |
| PATCH | 부분 수정 | `PATCH /api/v1/products/{id}` |
| DELETE | 삭제 | `DELETE /api/v1/products/{id}` |

### 응답 형식
```json
{
  "success": true,
  "data": { ... },
  "error": null,
  "meta": {
    "page": 1,
    "size": 20,
    "total": 100
  }
}
```

### 에러 응답
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "PRODUCT_NOT_FOUND",
    "message": "상품을 찾을 수 없습니다"
  }
}
```

### HTTP Status Codes
| Code | 용도 |
|------|------|
| 200 | 성공 |
| 201 | 생성 성공 |
| 400 | 잘못된 요청 |
| 401 | 인증 필요 |
| 403 | 권한 없음 |
| 404 | 리소스 없음 |
| 500 | 서버 오류 |
