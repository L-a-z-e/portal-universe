# Config Service

Spring Cloud Config Server로 중앙 집중식 설정 관리를 제공합니다.

## 개요

모든 마이크로서비스의 설정을 Git 저장소에서 관리하고 제공합니다.

## 포트

- 서비스: `8888`

## 주요 기능

| 기능 | 설명 |
|------|------|
| 설정 중앙화 | Git 기반 설정 저장소 |
| 환경별 설정 | local, docker, k8s 프로파일 |
| 동적 갱신 | /actuator/refresh 엔드포인트 |

## 설정 구조

```
config-repo/
├── application.yml           # 공통 설정
├── auth-service.yml         # 인증 서비스
├── blog-service.yml         # 블로그 서비스
├── shopping-service.yml     # 쇼핑 서비스
├── api-gateway.yml          # API Gateway
└── {service}-{profile}.yml  # 환경별 설정
```

## 클라이언트 설정

```yaml
# 다른 서비스의 application.yml
spring:
  config:
    import: optional:configserver:http://localhost:8888
  cloud:
    config:
      fail-fast: false
```

## API 엔드포인트

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/{application}/{profile}` | 설정 조회 |
| GET | `/{application}/{profile}/{label}` | 특정 브랜치 설정 |
| POST | `/actuator/bus-refresh` | 전체 갱신 |

## 환경 변수

| 변수 | 설명 | 기본값 |
|------|------|--------|
| `SPRING_CLOUD_CONFIG_SERVER_GIT_URI` | Git 저장소 URI | - |
| `SPRING_CLOUD_CONFIG_SERVER_GIT_DEFAULT_LABEL` | 기본 브랜치 | main |

## 실행

```bash
./gradlew :services:config-service:bootRun
```

## 관련 문서

- [ARCHITECTURE.md](./ARCHITECTURE.md) - 아키텍처 상세
