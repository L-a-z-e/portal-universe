---
id: guide-getting-started
title: 개발 환경 설정 가이드
type: guide
status: current
created: 2026-01-18
updated: 2026-01-18
author: Laze
tags: [setup, environment, onboarding]
---

# 🚀 Getting Started

## 📋 개요
Shopping Service 개발 환경을 설정하는 가이드입니다.

## 💻 필수 요구사항

| 도구 | 버전 | 비고 |
|------|------|------|
| JDK | 17+ | OpenJDK 권장 |
| Gradle | 8.0+ | Wrapper 사용 |
| Docker | 24.0+ | Desktop 권장 |
| IDE | IntelliJ IDEA | Ultimate 권장 |

## 🔧 설치 가이드

### 1. 저장소 클론
```bash
git clone https://github.com/company/shopping-service.git
cd shopping-service
```

### 2. 인프라 실행
```bash
docker-compose up -d mysql redis kafka
```

### 3. 애플리케이션 실행
```bash
./gradlew bootRun
```

### 4. 동작 확인
```bash
curl http://localhost:8080/health
```

## 📚 다음 단계
- [System Overview](../architecture/system-overview.md)
- [API Conventions](../api/conventions.md)
