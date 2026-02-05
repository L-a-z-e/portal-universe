# API 문서 작성 가이드

## 📋 개요
API 명세서를 작성하는 가이드입니다.

## 📁 위치 및 명명 규칙
- 위치: `docs/api/`
- 파일명: `[resource]-api.md`
- 예시: `product-api.md`, `order-api.md`

## 📝 필수 섹션

### 1. 메타데이터
```yaml
---
id: api-[resource]
title: [Resource] API
type: api
status: current | deprecated
created: YYYY-MM-DD
updated: YYYY-MM-DD
author: [작성자]
tags: [태그 배열]
related:
  - [관련 PRD ID]
---
```

### 2. Base URL
```
Base URL: /api/v{version}/{resource}
```

### 3. Endpoints
각 엔드포인트별로:
- HTTP Method + Path
- 설명
- Request (Parameters, Body)
- Response (Success, Error)

### 4. 에러 코드 (해당 시)

## 📐 예시 형식

```
### 리소스 조회
GET /api/v1/resources/{id}

**Path Parameters**
| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| id | Long | Y | 리소스 ID |

**Response (200)**
{code block}

**Error Responses**
| 코드 | 메시지 |
|------|--------|
| 404 | 리소스를 찾을 수 없습니다 |
```

## ✅ 체크리스트
- [ ] 모든 엔드포인트가 문서화되었는가?
- [ ] Request/Response 예시가 있는가?
- [ ] 에러 케이스가 명시되었는가?
- [ ] README 인덱스에 추가했는가?
