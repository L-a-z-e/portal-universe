---
id: portal-shell-guides-index
title: Portal Shell - Guides Index
type: guide
status: current
created: 2026-01-18
updated: 2026-01-18
author: Laze
tags: [portal-shell, guides, index, documentation]
---

# Portal Shell - Guides

> Portal Shell 개발자 가이드 모음

---

## 📚 가이드 목록

### 🚀 시작하기

| 문서 | 설명 | 대상 | 난이도 |
|------|------|------|--------|
| [Getting Started](./getting-started.md) | 개발 환경 설정 및 실행 가이드 | 신규 개발자 | 초급 |

**포함 내용:**
- 사전 요구사항 확인
- 의존성 설치
- 환경 변수 설정
- 로컬 실행 및 검증
- 자주 발생하는 문제 해결

---

### 🔌 Module Federation

| 문서 | 설명 | 대상 | 난이도 |
|------|------|------|--------|
| [Adding Remote Module](./adding-remote.md) | 새 Remote 모듈 추가 방법 | 중급 개발자 | 중급 |

**포함 내용:**
- Remote 애플리케이션 준비
- remoteRegistry.ts 업데이트
- vite.config.ts 설정
- 환경 변수 추가
- 테스트 및 검증

---

### 🛠️ 개발 워크플로우

| 문서 | 설명 | 대상 | 난이도 |
|------|------|------|--------|
| [Development Workflow](./development.md) | 개발 프로세스 및 베스트 프랙티스 | 모든 개발자 | 중급 |

**포함 내용:**
- 브랜치 전략
- 코드 작성 규칙
- 디버깅 방법
- 테스트 작성
- Commit & PR 프로세스
- 빌드 및 배포

---

## 🎯 학습 경로

### 신규 개발자 (1-2주)

```
1. Getting Started
   ↓
2. Development Workflow (코드 작성 규칙, 디버깅)
   ↓
3. 실습: 간단한 컴포넌트 추가
   ↓
4. 실습: API 연동
```

### 중급 개발자 (1주)

```
1. Development Workflow (고급 주제)
   ↓
2. Adding Remote Module
   ↓
3. 실습: 새 Remote 모듈 추가
   ↓
4. Architecture 문서 참조
```

---

## 📖 추가 문서

### Architecture

시스템 구조 및 설계 이해:

- [System Overview](../architecture/system-overview.md)
- [Remote Registry](../architecture/remote-registry.md)
- [Authentication Flow](../architecture/auth-flow.md)

### API

API 명세 및 사용법:

- [API Specification](../api/api-spec.md)
- [Authentication API](../api/auth-api.md)

### Troubleshooting

문제 해결 기록:

- [Troubleshooting Index](../troubleshooting/README.md)

---

## 🛟 도움이 필요하면

### 질문하기

| 채널 | 용도 | 응답 시간 |
|------|------|----------|
| Slack `#portal-shell` | 일반 질문 | 1시간 이내 |
| GitHub Issues | 버그 리포트, 기능 요청 | 1-2일 |
| 팀 미팅 | 복잡한 문제 논의 | 주 1회 |

### 자주 묻는 질문 (FAQ)

**Q1: Remote 모듈이 로드되지 않아요**

A: [Getting Started - 자주 발생하는 문제](./getting-started.md#자주-발생하는-문제) 참조

**Q2: 환경 변수 변경이 적용되지 않아요**

A: Vite는 환경 변수를 빌드 시점에 번들에 포함합니다. 개발 서버를 재시작하세요:

```bash
rm -rf node_modules/.vite
npm run dev
```

**Q3: TypeScript 타입 에러가 발생해요**

A: 다음 명령어로 타입 체크:

```bash
vue-tsc --noEmit
```

**Q4: Module Federation이 뭔가요?**

A: [Adding Remote Module](./adding-remote.md) 가이드 참조 또는 [공식 문서](https://webpack.js.org/concepts/module-federation/) 확인

---

## 📝 문서 기여

가이드 개선에 기여하고 싶으신가요?

### 기여 방법

1. 오타/오류 발견 시:
   - GitHub Issue 생성
   - 또는 직접 PR 생성

2. 새 가이드 추가 시:
   - `docs_template/guide/guides/how-to-write.md` 참조
   - 템플릿에 맞춰 작성
   - PR 생성 시 리뷰어 지정

### 문서 작성 규칙

- YAML frontmatter 필수
- 코드 블록에 언어 지정 (```typescript, ```bash)
- 실행 가능한 예시 코드 포함
- 스크린샷 첨부 (선택)
- 관련 문서 링크

---

## 🔄 업데이트 내역

| 날짜 | 변경 내용 | 작성자 |
|------|----------|--------|
| 2026-01-18 | 초기 가이드 작성 (Getting Started, Adding Remote, Development) | Claude (Documenter Agent) |

---

## 📌 빠른 링크

### 외부 문서

- [Vue 3 공식 문서](https://vuejs.org/)
- [Vite 공식 문서](https://vite.dev/)
- [Pinia 공식 문서](https://pinia.vuejs.org/)
- [Module Federation](https://webpack.js.org/concepts/module-federation/)
- [TypeScript 공식 문서](https://www.typescriptlang.org/)

### 프로젝트 문서

- [프로젝트 README](../../../../../README.md)
- [CLAUDE.md (프로젝트 개요)](../../../../../CLAUDE.md)
- [Commit Convention](../../../../../.claude/rules/commit-convention.md)
- [Frontend Patterns](../../../../../.claude/rules/frontend-patterns.md)

---

## 📞 연락처

| 역할 | 이름 | 연락처 |
|------|------|--------|
| Tech Lead | [이름] | [이메일] |
| Senior Developer | [이름] | [이메일] |
| DevOps | [이름] | [이메일] |

---

**최종 업데이트**: 2026-01-18
**문서 버전**: 1.0.0
**Portal Shell 버전**: 0.0.0 (Vue 3.5.21, Vite 7.1.7)
