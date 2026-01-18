# Config Service - Guides

> Config Service 개발자 가이드 모음

---

## 📚 가이드 목록

### Getting Started

| ID | 문서명 | 설명 | 상태 |
|----|--------|------|------|
| guide-config-getting-started | [Getting Started](./getting-started.md) | Config Service 개발 환경 설정 및 실행 가이드 | ✅ Current |

### Configuration

| ID | 문서명 | 설명 | 상태 |
|----|--------|------|------|
| guide-config-client-configuration | [Client Configuration Guide](./client-configuration.md) | 다른 서비스에서 Config Service 연결 방법 | ✅ Current |

---

## 🎯 학습 경로

### 신규 개발자

```
1. Getting Started
   ↓
2. Client Configuration Guide
   ↓
3. Architecture Document (../architecture/)
```

### 운영 담당자

```
1. Getting Started
   ↓
2. Runbook (../runbooks/)
```

---

## 📋 가이드 작성 현황

- [x] Getting Started - Config Service 시작 가이드
- [x] Client Configuration - 클라이언트 설정 가이드
- [ ] Spring Cloud Bus Integration - 자동 설정 갱신 (예정)
- [ ] Vault Integration - 민감 정보 관리 (예정)
- [ ] Multi-Repository Configuration - 여러 저장소 관리 (예정)

---

## 🔗 관련 문서

### Architecture
- [Config Service Architecture](../architecture/config-service-architecture.md)

### API
- [Config Service API](../api/config-service-api.md)

### Operations
- [Config Service Operations Runbook](../runbooks/config-service-operations.md)

### Troubleshooting
- [Config Service Troubleshooting](../../troubleshooting/)

---

## 📝 가이드 작성 규칙

가이드 문서를 추가할 때는 다음 규칙을 따릅니다:

1. **파일명**: `kebab-case.md` 형식 사용
2. **ID**: `guide-config-[topic]` 형식
3. **메타데이터**: 필수 YAML front matter 포함
4. **내용**: 초보자 관점에서 단계별로 설명
5. **코드**: 복사-붙여넣기 가능한 명령어 제공
6. **인덱스**: 이 README.md에 새 가이드 추가

### 메타데이터 예시

```yaml
---
id: guide-config-[topic]
title: [Guide Title]
type: guide
status: current
created: YYYY-MM-DD
updated: YYYY-MM-DD
author: [Author Name]
tags: [tag1, tag2]
related:
  - related-doc-id
---
```

---

## 🤝 기여 방법

새로운 가이드를 추가하거나 기존 가이드를 개선하려면:

1. `/docs_template/guide/guides/how-to-write.md` 템플릿 참고
2. 가이드 작성
3. 이 README.md 인덱스에 추가
4. PR 생성

---

**최종 업데이트**: 2026-01-18
