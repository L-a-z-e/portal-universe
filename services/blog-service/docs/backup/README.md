# Blog Service

MongoDB 기반 블로그 콘텐츠 관리 서비스입니다.

## 개요

포스트, 댓글, 시리즈, 태그, 파일 업로드 기능을 제공합니다.

## 포트

- 서비스: `8082`
- Swagger: `http://localhost:8082/swagger-ui.html`

## 주요 기능

| 도메인 | 기능 |
|--------|------|
| Post | CRUD, 검색, 통계, 상태 관리 |
| Comment | 댓글/대댓글 |
| Series | 시리즈 관리 |
| Tag | 태그 관리/통계 |
| File | S3 파일 업로드 |

## 기술 스택

- **Database**: MongoDB
- **Storage**: AWS S3
- **Security**: OAuth2 Resource Server (JWT)
- **Docs**: OpenAPI 3.0 (Swagger)

## API 엔드포인트

### Post API

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/posts` | 게시물 생성 |
| GET | `/posts` | 게시물 목록 (페이징) |
| GET | `/posts/{id}` | 게시물 상세 |
| GET | `/posts/{id}/view` | 조회수 증가 + 상세 |
| PUT | `/posts/{id}` | 게시물 수정 |
| DELETE | `/posts/{id}` | 게시물 삭제 |
| GET | `/posts/search` | 키워드 검색 |
| POST | `/posts/search/advanced` | 고급 검색 |
| GET | `/posts/popular` | 인기 게시물 |
| GET | `/posts/recent` | 최근 게시물 |

### Comment API

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/comments` | 댓글 작성 |
| GET | `/comments/post/{postId}` | 게시물 댓글 조회 |
| PUT | `/comments/{id}` | 댓글 수정 |
| DELETE | `/comments/{id}` | 댓글 삭제 |

### Series API

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/series` | 시리즈 생성 |
| GET | `/series` | 시리즈 목록 |
| GET | `/series/{id}` | 시리즈 상세 |
| PUT | `/series/{id}` | 시리즈 수정 |

### Tag API

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/tags` | 태그 목록 |
| GET | `/tags/popular` | 인기 태그 |
| GET | `/posts/stats/tags` | 태그 통계 |

## 환경 변수

| 변수 | 설명 | 기본값 |
|------|------|--------|
| `MONGODB_URI` | MongoDB 연결 | localhost:27017 |
| `AWS_S3_BUCKET` | S3 버킷 | - |
| `AWS_ACCESS_KEY_ID` | AWS 액세스 키 | - |
| `AWS_SECRET_ACCESS_KEY` | AWS 시크릿 키 | - |

## 실행

```bash
./gradlew :services:blog-service:bootRun
```

## 관련 문서

- [ARCHITECTURE.md](./ARCHITECTURE.md) - 아키텍처 상세
- [API.md](./API.md) - API 명세
