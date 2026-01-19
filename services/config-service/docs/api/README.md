# Config Service API 문서

> Config Service의 모든 API 명세서 목록

---

## 📑 API 문서 목록

| ID | 문서명 | 버전 | 상태 | 최종 수정일 |
|----|--------|------|------|-------------|
| api-config | [Config Service API](./config-api.md) | v1 | current | 2026-01-18 |

---

## 📋 API 개요

### Base URL
```
http://localhost:8888
```

### 인증
Config Service는 기본적으로 인증이 필요하지 않습니다 (내부 서비스 전용).

프로덕션 환경에서는 Spring Security를 통한 Basic Auth 설정을 권장합니다.

---

## 🔍 주요 API 카테고리

### 1. 설정 조회
- JSON 형식 조회
- YAML 파일 다운로드
- Properties 파일 다운로드
- 특정 브랜치/태그 조회

### 2. 암호화/복호화
- 평문 암호화 (`/encrypt`)
- 암호문 복호화 (`/decrypt`)

### 3. 모니터링
- Health Check (`/actuator/health`)
- Server Info (`/actuator/info`)
- 설정 갱신 (`/actuator/bus-refresh`)

---

## 📖 API 문서 상세

### [Config Service API](./config-api.md)
Spring Cloud Config Server의 모든 엔드포인트에 대한 상세 명세서입니다.

**포함 내용**:
- 설정 조회 API (JSON, YAML, Properties)
- Git 브랜치/태그 기반 설정 조회
- 암호화/복호화 API
- Actuator 엔드포인트
- Config Client 통합 가이드
- 보안 설정 방법

---

## 🚀 빠른 시작

### 1. Config Server 실행

```bash
# Gradle 빌드 및 실행
cd services/config-service
./gradlew bootRun

# 또는 Docker Compose
docker-compose up config-service
```

### 2. 설정 조회 테스트

```bash
# auth-service의 local 프로파일 설정 조회
curl http://localhost:8888/auth-service/local

# YAML 형식으로 다운로드
curl http://localhost:8888/auth-service-local.yml
```

### 3. 암호화 테스트

```bash
# 비밀번호 암호화
curl -X POST http://localhost:8888/encrypt \
  -H "Content-Type: text/plain" \
  -d "mysecretpassword"
```

---

## 🔗 관련 문서

- [Config Service Architecture](../architecture/config-architecture.md)
- [Config Repository](https://github.com/L-a-z-e/portal-universe-config-repo)
- [Spring Cloud Config 공식 문서](https://docs.spring.io/spring-cloud-config/docs/current/reference/html/)

---

## 📝 문서 작성 가이드

새로운 API 문서를 추가하려면 다음 가이드를 참고하세요:
- [API 문서 작성 가이드](/docs_template/guide/api/how-to-write.md)

---

**최종 업데이트**: 2026-01-18
