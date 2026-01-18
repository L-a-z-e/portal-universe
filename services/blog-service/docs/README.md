---
id: blog-service-docs
title: Blog Service Documentation Portal
type: guide
status: current
created: 2026-01-18
updated: 2026-01-18
author: Portal Universe Team
tags:
  - blog
  - mongodb
  - s3
  - spring-boot
  - microservice
related:
  - architecture/system-overview
  - api/rest-api-spec
---

# Blog Service Documentation Portal

MongoDB 기반 블로그 콘텐츠 관리 마이크로서비스입니다.

## 📋 서비스 개요

| 항목 | 내용 |
|------|------|
| **서비스명** | blog-service |
| **포트** | 8082 |
| **데이터베이스** | MongoDB |
| **스토리지** | AWS S3 |
| **인증** | OAuth2 Resource Server (JWT) |
| **API 문서** | http://localhost:8082/swagger-ui.html |

## 🎯 주요 기능

| 도메인 | 기능 | 설명 |
|--------|------|------|
| **Post** | CRUD, 검색, 통계 | 게시물 생성/수정/삭제, 키워드 검색, 고급 검색, 인기/최근 게시물 조회 |
| **Comment** | 댓글/대댓글 | 게시물에 대한 댓글 작성 및 계층 구조 지원 |
| **Series** | 시리즈 관리 | 연속된 게시물을 시리즈로 그룹화 |
| **Tag** | 태그 관리/통계 | 태그 기반 분류 및 인기 태그 통계 |
| **File** | 파일 업로드 | S3 기반 이미지/파일 업로드 |

## 🏗️ 기술 스택

| 카테고리 | 기술 | 버전 |
|----------|------|------|
| **Framework** | Spring Boot | 3.5.5 |
| **Language** | Java | 17 |
| **Database** | MongoDB | 최신 |
| **Storage** | AWS S3 | - |
| **Security** | Spring Security OAuth2 | - |
| **API Docs** | OpenAPI 3.0 (Swagger) | - |
| **Build Tool** | Gradle | - |

## 📡 API 엔드포인트 개요

### Post API

| 메서드 | 경로 | 설명 | 인증 필요 |
|--------|------|------|----------|
| POST | `/posts` | 게시물 생성 | ✅ |
| GET | `/posts` | 게시물 목록 (페이징) | ❌ |
| GET | `/posts/{id}` | 게시물 상세 | ❌ |
| GET | `/posts/{id}/view` | 조회수 증가 + 상세 | ❌ |
| PUT | `/posts/{id}` | 게시물 수정 | ✅ |
| DELETE | `/posts/{id}` | 게시물 삭제 | ✅ |
| GET | `/posts/search` | 키워드 검색 | ❌ |
| POST | `/posts/search/advanced` | 고급 검색 | ❌ |
| GET | `/posts/popular` | 인기 게시물 | ❌ |
| GET | `/posts/recent` | 최근 게시물 | ❌ |

### Comment API

| 메서드 | 경로 | 설명 | 인증 필요 |
|--------|------|------|----------|
| POST | `/comments` | 댓글 작성 | ✅ |
| GET | `/comments/post/{postId}` | 게시물 댓글 조회 | ❌ |
| PUT | `/comments/{id}` | 댓글 수정 | ✅ |
| DELETE | `/comments/{id}` | 댓글 삭제 | ✅ |

### Series API

| 메서드 | 경로 | 설명 | 인증 필요 |
|--------|------|------|----------|
| POST | `/series` | 시리즈 생성 | ✅ |
| GET | `/series` | 시리즈 목록 | ❌ |
| GET | `/series/{id}` | 시리즈 상세 | ❌ |
| PUT | `/series/{id}` | 시리즈 수정 | ✅ |

### Tag API

| 메서드 | 경로 | 설명 | 인증 필요 |
|--------|------|------|----------|
| GET | `/tags` | 태그 목록 | ❌ |
| GET | `/tags/popular` | 인기 태그 | ❌ |
| GET | `/posts/stats/tags` | 태그 통계 | ❌ |

## 🚀 실행 방법

### 로컬 실행

```bash
# Gradle을 통한 실행
./gradlew :services:blog-service:bootRun

# JAR 빌드 후 실행
./gradlew :services:blog-service:build
java -jar services/blog-service/build/libs/blog-service-*.jar
```

### Docker Compose 실행

```bash
docker-compose up -d blog-service
```

### Kubernetes 배포

```bash
kubectl apply -f k8s/blog-service/
```

## ⚙️ 환경 변수

| 변수명 | 설명 | 필수 여부 | 기본값 |
|--------|------|----------|--------|
| `MONGODB_URI` | MongoDB 연결 문자열 | ✅ | `mongodb://localhost:27017/blog` |
| `MONGODB_DATABASE` | MongoDB 데이터베이스명 | ✅ | `blog` |
| `AWS_S3_BUCKET` | S3 버킷 이름 | ✅ | - |
| `AWS_ACCESS_KEY_ID` | AWS 액세스 키 ID | ✅ | - |
| `AWS_SECRET_ACCESS_KEY` | AWS 시크릿 액세스 키 | ✅ | - |
| `AWS_REGION` | AWS 리전 | ✅ | `ap-northeast-2` |
| `SPRING_PROFILES_ACTIVE` | Spring 프로필 | ❌ | `local` |

## 📚 문서 네비게이션

### 아키텍처

| 문서 | 설명 |
|------|------|
| [System Overview](./architecture/system-overview.md) | 서비스 전체 구조 및 컴포넌트 |
| [Data Model](./architecture/data-model.md) | MongoDB 스키마 및 관계 |
| [Integration](./architecture/integration.md) | 외부 서비스 연동 방식 |

### API 명세

| 문서 | 설명 |
|------|------|
| [REST API Specification](./api/rest-api-spec.md) | 전체 API 엔드포인트 상세 명세 |
| [Error Codes](./api/error-codes.md) | 에러 코드 및 처리 방법 |
| [Authentication](./api/authentication.md) | 인증/인가 가이드 |

### 개발 가이드

| 문서 | 설명 |
|------|------|
| [Setup Guide](./guides/setup-guide.md) | 로컬 개발 환경 구성 |
| [Development Workflow](./guides/development-workflow.md) | 개발 프로세스 및 규칙 |
| [Testing Guide](./guides/testing-guide.md) | 테스트 작성 가이드 |

## 🔗 관련 서비스

| 서비스 | 연동 방식 | 용도 |
|--------|----------|------|
| **auth-service** | JWT 검증 (API Gateway 경유) | 사용자 인증/인가 |
| **api-gateway** | HTTP Gateway | 요청 라우팅 및 JWT 검증 |
| **notification-service** | Kafka (비동기) | 댓글 알림 등 이벤트 처리 |

## 📞 지원

- **Issue Tracker**: GitHub Issues
- **Documentation**: 이 문서 포털
- **Swagger UI**: http://localhost:8082/swagger-ui.html

---

**Last Updated**: 2026-01-18
**Maintained by**: Portal Universe Team
