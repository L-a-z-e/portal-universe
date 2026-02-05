---
id: STATUS
title: Blog Service 구현 현황
type: architecture
status: current
created: 2026-01-26
updated: 2026-01-26
author: Laze
tags: [blog-service, status, implementation, phase-1]
---

# Blog Service 구현 현황

## 개요

Blog Service는 Portal Universe의 블로그 도메인을 담당하는 마이크로서비스입니다.
MongoDB 기반 문서 저장소를 사용하며, AWS S3와 연동된 파일 관리 기능을 제공합니다.

**Phase 1 (Core CRUD)** 완료 상태이며, 총 53개의 API 엔드포인트를 제공합니다.

## 기술 스택

| 영역 | 기술 | 비고 |
|------|------|------|
| Language | Java 17 | |
| Framework | Spring Boot 3.5.5 | |
| Database | MongoDB | 문서 저장 및 전문 검색 |
| Storage | AWS S3 | 파일 업로드 (LocalStack 로컬) |
| Security | Spring Security OAuth2 | JWT 기반 인증 |
| Messaging | Kafka | Producer만 구현 |
| API Docs | Swagger/OpenAPI 3.0 | |
| Monitoring | Prometheus, Zipkin | 메트릭 및 분산 추적 |

## 도메인별 구현 상태

| 도메인 | CRUD | 검색 | 통계 | 비고 |
|--------|------|------|------|------|
| Post | ✅ | ✅ (단순+고급) | ✅ (카테고리, 태그, 작성자, 블로그) | 핵심 도메인 |
| Comment | ✅ | - | - | 대댓글 지원 (계층 구조) |
| Like | ✅ (토글) | - | - | 중복 방지 (복합 인덱스) |
| Series | ✅ | - | - | 포스트 순서 관리 |
| Tag | ✅ | ✅ (자동완성) | ✅ (인기태그) | 역정규화 (postCount) |
| File | ✅ (업로드/삭제) | - | - | S3 연동 (LocalStack 로컬) |

## API 엔드포인트 현황

### 전체 집계

| 도메인 | 엔드포인트 수 |
|--------|---------------|
| Post | 25개 |
| Comment | 4개 |
| Like | 3개 |
| Series | 10개 |
| Tag | 9개 |
| File | 2개 |
| **총계** | **53개** |

### 주요 엔드포인트

#### Post API (25개)
- CRUD: Create, Read, Update, Delete
- 검색: 단순 검색, 고급 검색, 전문 검색 (MongoDB Text Index)
- 통계: 카테고리별, 태그별, 작성자별, 블로그별
- 특수: 트렌딩, 피드, 상태 관리, 네비게이션 (이전/다음)

#### Comment API (4개)
- CRUD: Create, Read, Update, Delete
- 계층 구조: 대댓글 지원 (`parentId`)

#### Like API (3개)
- 토글: 좋아요 추가/취소
- 조회: 포스트별 좋아요 목록, 사용자별 좋아요 여부

#### Series API (10개)
- CRUD: Create, Read, Update, Delete
- 관리: 포스트 추가/제거, 순서 변경
- 조회: 작성자별 시리즈 목록

#### Tag API (9개)
- CRUD: Create, Read, Update, Delete
- 검색: 자동완성 (prefix 검색)
- 통계: 인기 태그 (postCount 기준)

#### File API (2개)
- 업로드: S3 업로드 (MultipartFile)
- 삭제: S3 삭제 (ADMIN 권한 필수)

## 주요 기능

### 1. 게시물 관리
- **상태 관리**: DRAFT, PUBLISHED, ARCHIVED
- **전문 검색**: MongoDB Text Index (가중치: title 2.0, content 1.0)
- **트렌딩 알고리즘**: `score = viewCount×1 + likeCount×3 + commentCount×5` (시간 감쇠)
- **피드 기능**: 팔로잉 사용자 게시물
- **네비게이션**: 이전/다음 포스트 (scope: all/author/category/series)

### 2. 댓글 시스템
- **계층 구조**: 대댓글 지원 (`parentId`)
- **정렬**: 최신순 (`createdAt DESC`)

### 3. 좋아요 시스템
- **토글 방식**: 같은 사용자/포스트에 중복 방지
- **복합 인덱스**: `{userId, postId}` (UNIQUE)

### 4. 시리즈 관리
- **포스트 순서**: `order` 필드로 관리
- **순서 변경**: 드래그 앤 드롭 지원 (API)

### 5. 파일 관리
- **업로드**: S3 연동 (LocalStack 로컬 개발)
- **권한**: ADMIN만 삭제 가능

## 인증/보안

### 인증 방식
- **GET 요청**: 공개 (인증 불필요)
- **POST/PUT/DELETE**: 인증 필수 (`@AuthenticationPrincipal`)
- **파일 삭제**: ADMIN 권한 필수 (`@PreAuthorize("hasRole('ADMIN')")`)

### GatewayAuthenticationFilter
- API Gateway에서 `X-User-Id` 헤더 전달
- JWT 토큰 검증은 Gateway에서 처리
- Blog Service는 헤더 기반 인증만 수행

## Kafka

### 현재 상태
- **Producer**: 설정 완료 (`KafkaProducerConfig`)
  - Serializer: JsonSerializer
  - acks: all
  - retries: 3
- **Consumer**: 미구현

### 향후 계획
- `notification-service` 연동 예정
- 이벤트 발행: 게시물 생성, 댓글 추가, 좋아요 등

## MongoDB 인덱스

총 7개의 인덱스 설정:

| 번호 | 인덱스 필드 | 용도 |
|------|-------------|------|
| 1 | `{title: "text", content: "text"}` | 전문 검색 (가중치: 2.0, 1.0) |
| 2 | `{status: 1, publishedAt: -1}` | 발행된 포스트 목록 |
| 3 | `{authorId: 1, createdAt: -1}` | 작성자별 포스트 |
| 4 | `{category: 1, status: 1, publishedAt: -1}` | 카테고리별 포스트 |
| 5 | `{tags: 1}` | 태그 검색 |
| 6 | `{status: 1, viewCount: -1, publishedAt: -1}` | 트렌딩 포스트 |
| 7 | `{productId: 1}` | 상품별 포스트 |

## 에러 코드 범위

| 도메인 | 코드 범위 | 예시 |
|--------|-----------|------|
| Post | B001-B004 | B001: POST_NOT_FOUND |
| Like | B020-B022 | B020: LIKE_NOT_FOUND |
| Comment | B030-B032 | B030: COMMENT_NOT_FOUND |
| Series | B040-B045 | B040: SERIES_NOT_FOUND |
| Tag | B050-B051 | B050: TAG_NOT_FOUND |
| File | B060-B065 | B060: FILE_UPLOAD_FAILED |

> 전체 목록은 `docs/architecture/error-codes.md` 참조

## 문서 현황 (Phase 1 대조 결과)

### API 문서 (`docs/api/`)
- **커버리지**: 89% (47/53 엔드포인트)
- **누락**:
  - Like API 전체 (3개)
  - Post API 일부 (트렌딩, 피드, 네비게이션)

### 아키텍처 문서 (`docs/architecture/`)
- **에러 코드 테이블**: 완전 불일치 (수정 필요)
- **도메인 모델**: 대부분 일치
  - 문서에 `seriesId` 필드 기재되었으나 실제 코드에 없음
  - Series 필드명 불일치 (문서: `title` → 코드: `name`)

### 인덱스 문서 (`docs/architecture/mongodb-indexes.md`)
- **누락**: 3개
- **불일치**: 2개

## 알려진 이슈/TODO

### Phase 1 완료 후 수정 필요
1. **Kafka Consumer 미구현**
   - notification-service 연동 예정
   - 이벤트: 게시물 생성, 댓글 추가, 좋아요 등

2. **API 문서 불일치**
   - Like API 전체 누락 (3개)
   - Post API 일부 누락 (3개)

3. **에러 코드 문서 완전 불일치**
   - `docs/architecture/error-codes.md` 전체 재작성 필요
   - 실제 코드와 동기화

4. **인덱스 문서 불일치**
   - `docs/architecture/mongodb-indexes.md` 업데이트 필요
   - 3개 누락, 2개 불일치

5. **File API가 ApiResponse wrapper 미사용**
   - 일관성을 위해 수정 고려
   - 현재: `ResponseEntity<Map<String, String>>`
   - 권장: `ResponseEntity<ApiResponse<FileResponse>>`

## Profile 설정

| Profile | MongoDB | Kafka | LocalStack | 용도 |
|---------|---------|-------|------------|------|
| local | localhost:27017 | localhost:9092 | localhost:4566 | 로컬 개발 |
| docker | mongodb:27017 | kafka:9092 | localstack:4566 | Docker Compose |
| kubernetes | blog-mongodb:27017 | kafka:9092 | localstack:4566 | K8s |

## 포트

| 환경 | 포트 |
|------|------|
| Application | 8082 |
| Management | 9082 |

## 다음 단계

### Phase 2 (Advanced Features)
- 이미지 최적화 (썸네일, WebP 변환)
- 실시간 알림 (SSE, WebSocket)
- 고급 통계 (일별/월별 조회수, 인기 포스트)
- 추천 시스템 (협업 필터링)

### Phase 3 (Enterprise Features)
- 다국어 지원 (i18n)
- 버전 관리 (포스트 히스토리)
- 예약 발행 (Scheduled Publishing)
- A/B 테스트 (제목, 썸네일)

---

**최종 업데이트**: 2026-01-26
**문서 버전**: 1.0.0
