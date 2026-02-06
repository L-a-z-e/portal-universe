---
id: api-shopping-search
title: Shopping Search API
type: api
status: current
version: v1
created: 2026-02-06
updated: 2026-02-06
author: Laze
tags: [api, shopping, frontend, search]
related: [api-shopping-types, api-shopping-product]
---

# Shopping Search API

> 검색, 자동완성, 인기/최근 검색어 API

---

## 개요

| 항목 | 내용 |
|------|------|
| **Base URL** | `/api/v1/shopping/search` |
| **인증** | Bearer Token (최근 검색어만 필수) |
| **엔드포인트** | `searchApi` |

---

## 검색 API

### 검색 자동완성

```typescript
suggest(keyword: string, size = 5): Promise<ApiResponse<string[]>>
```

**Endpoint**: `GET /api/v1/shopping/search/suggest?keyword=spring&size=5`

**Query Parameters**

| 파라미터 | 타입 | 설명 | 기본값 |
|----------|------|------|--------|
| `keyword` | string | 검색 키워드 (2자 이상) | - |
| `size` | number | 결과 개수 | 5 |

**Response**

```json
{
  "success": true,
  "data": [
    "스프링 부트",
    "스프링 클라우드",
    "스프링 시큐리티",
    "스프링 데이터 JPA",
    "스프링 배치"
  ]
}
```

---

### 인기 검색어

```typescript
getPopularKeywords(size = 10): Promise<ApiResponse<string[]>>
```

**Endpoint**: `GET /api/v1/shopping/search/popular?size=10`

**Response**

```json
{
  "success": true,
  "data": [
    "스프링 부트",
    "React",
    "Vue.js",
    "Node.js",
    "Python"
  ]
}
```

---

### 최근 검색어 조회

```typescript
getRecentKeywords(size = 10): Promise<ApiResponse<string[]>>
```

**Endpoint**: `GET /api/v1/shopping/search/recent?size=10`

**인증 필수**: Bearer Token

**Response**

```json
{
  "success": true,
  "data": [
    "스프링 부트",
    "React",
    "Docker"
  ]
}
```

---

### 최근 검색어 추가

```typescript
addRecentKeyword(keyword: string): Promise<ApiResponse<void>>
```

**Endpoint**: `POST /api/v1/shopping/search/recent?keyword={keyword}`

**인증 필수**: Bearer Token

---

### 최근 검색어 삭제

```typescript
deleteRecentKeyword(keyword: string): Promise<ApiResponse<void>>
```

**Endpoint**: `DELETE /api/v1/shopping/search/recent/{keyword}`

**인증 필수**: Bearer Token

---

### 최근 검색어 전체 삭제

```typescript
clearRecentKeywords(): Promise<ApiResponse<void>>
```

**Endpoint**: `DELETE /api/v1/shopping/search/recent`

**인증 필수**: Bearer Token

---

## React Hooks

### useSearchSuggest

자동완성 Hook (debounced)

```typescript
import { useSearchSuggest } from '@/hooks/useSearch'

export function SearchBar() {
  const [keyword, setKeyword] = useState('')
  const { suggestions, isLoading } = useSearchSuggest(keyword, 300)  // 300ms debounce

  return (
    <div>
      <input
        value={keyword}
        onChange={(e) => setKeyword(e.target.value)}
        placeholder="검색어를 입력하세요"
      />
      {suggestions.length > 0 && (
        <ul>
          {suggestions.map((suggestion, index) => (
            <li key={index} onClick={() => setKeyword(suggestion)}>
              {suggestion}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
```

---

### usePopularKeywords

인기 검색어 Hook

```typescript
import { usePopularKeywords } from '@/hooks/useSearch'

export function PopularKeywords() {
  const { keywords, isLoading, error } = usePopularKeywords(10)

  if (isLoading) return <div>로딩 중...</div>
  if (error) return null

  return (
    <div>
      <h3>인기 검색어</h3>
      <ul>
        {keywords.map((keyword, index) => (
          <li key={index}>{keyword}</li>
        ))}
      </ul>
    </div>
  )
}
```

---

### useRecentKeywords

최근 검색어 Hook (추가/삭제/전체삭제 포함)

```typescript
import { useRecentKeywords } from '@/hooks/useSearch'

export function RecentKeywords() {
  const { keywords, isLoading, error, addKeyword, deleteKeyword, clearAll } = useRecentKeywords(10)

  const handleSearch = async (keyword: string) => {
    await addKeyword(keyword)
    // 검색 실행
  }

  const handleDelete = async (keyword: string) => {
    await deleteKeyword(keyword)
  }

  const handleClearAll = async () => {
    if (confirm('최근 검색어를 모두 삭제하시겠습니까?')) {
      await clearAll()
    }
  }

  return (
    <div>
      <h3>최근 검색어</h3>
      {keywords.map((keyword, index) => (
        <div key={index}>
          <span onClick={() => handleSearch(keyword)}>{keyword}</span>
          <button onClick={() => handleDelete(keyword)}>✕</button>
        </div>
      ))}
      {keywords.length > 0 && (
        <button onClick={handleClearAll}>전체 삭제</button>
      )}
    </div>
  )
}
```

---

## 통합 검색 예시

```typescript
export function SearchPage() {
  const [keyword, setKeyword] = useState('')
  const [showSuggestions, setShowSuggestions] = useState(false)
  const { suggestions } = useSearchSuggest(keyword)
  const { keywords: popularKeywords } = usePopularKeywords(5)
  const { keywords: recentKeywords, addKeyword } = useRecentKeywords(5)

  const handleSearch = async (searchKeyword: string) => {
    setKeyword(searchKeyword)
    setShowSuggestions(false)

    // 최근 검색어 추가
    await addKeyword(searchKeyword)

    // 검색 API 호출
    const response = await productApi.searchProducts(searchKeyword, 0, 20)
    // ...
  }

  return (
    <div>
      <input
        value={keyword}
        onChange={(e) => {
          setKeyword(e.target.value)
          setShowSuggestions(true)
        }}
        onFocus={() => setShowSuggestions(true)}
      />

      {showSuggestions && keyword.length >= 2 && suggestions.length > 0 && (
        <div className="suggestions">
          {suggestions.map((s, i) => (
            <div key={i} onClick={() => handleSearch(s)}>{s}</div>
          ))}
        </div>
      )}

      <div className="popular-keywords">
        <h3>인기 검색어</h3>
        {popularKeywords.map((k, i) => (
          <span key={i} onClick={() => handleSearch(k)}>{k}</span>
        ))}
      </div>

      <div className="recent-keywords">
        <h3>최근 검색어</h3>
        {recentKeywords.map((k, i) => (
          <span key={i} onClick={() => handleSearch(k)}>{k}</span>
        ))}
      </div>
    </div>
  )
}
```

---

## 타입 정의

```typescript
export interface SearchSuggestion {
  keyword: string
}
```

---

## 에러 코드

| Code | HTTP Status | 설명 |
|------|-------------|------|
| `INVALID_KEYWORD` | 400 | 유효하지 않은 검색어 (2자 미만 등) |
| `UNAUTHORIZED` | 401 | 인증 필요 (최근 검색어) |

---

## 관련 문서

- [Client API](./client-api.md)
- [Product API](./product-api.md)
- [공통 타입 정의](./types.md)

---

**최종 업데이트**: 2026-02-06
