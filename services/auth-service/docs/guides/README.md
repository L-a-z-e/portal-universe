# Auth Service Guides

이 디렉토리는 Auth Service 개발자 가이드 문서를 포함합니다.

## 📋 가이드 목록

| ID | 제목 | 설명 | 상태 | 작성일 |
|----|------|------|------|--------|
| guide-getting-started | [Getting Started](./getting-started.md) | 로컬 개발 환경 설정 및 실행 가이드 | current | 2026-01-18 |

## 🎯 가이드 유형

### Setup & Configuration
- 개발 환경 설정
- 배포 설정
- 환경 변수 관리

### Integration
- 다른 서비스와의 연동 방법
- 외부 API 연동
- OAuth2 클라이언트 통합

### Development
- 코딩 규칙
- 테스트 작성
- 디버깅 팁

### Operations
- 모니터링 설정
- 로그 분석
- 성능 튜닝

## 📝 가이드 작성 규칙

새로운 가이드를 작성할 때는 다음 규칙을 따라주세요:

1. **파일명**: `kebab-case.md` 형식 사용
2. **메타데이터**: YAML frontmatter 포함 필수
3. **ID 규칙**: `guide-[주제명]` 형식
4. **README 업데이트**: 이 파일에 새 가이드 추가

### 필수 메타데이터
```yaml
---
id: guide-[주제명]
title: [가이드 제목]
type: guide
status: draft | current | deprecated
created: YYYY-MM-DD
updated: YYYY-MM-DD
author: [작성자명]
tags: [태그 배열]
related: [관련문서 ID 배열]
---
```

## 🔗 관련 문서

- [Architecture Documentation](../architecture/README.md)
- [API Documentation](../api/README.md)
- [Troubleshooting Guide](../troubleshooting/README.md)
