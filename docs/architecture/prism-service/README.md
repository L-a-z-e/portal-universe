# Prism Service Architecture

Prism Service의 아키텍처 문서입니다.

## 문서 목록

| 파일 | 설명 | 상태 |
|------|------|------|
| [security.md](./security.md) | 보안 아키텍처 (XSS, SQLi, Audit) | Current |

## 개요

Prism Service는 NestJS 기반 AI 워크플로우 관리 플랫폼입니다.

- **기술 스택**: NestJS, TypeScript, PostgreSQL, Redis
- **주요 기능**: Board, Agent, Task, Provider 관리
- **보안**: Custom Validators (NoXss, NoSqlInjection), AuditInterceptor

---

**최종 업데이트**: 2026-02-13
