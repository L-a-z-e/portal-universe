# 📚 Documentation Template

> 프로젝트 문서화를 위한 표준 템플릿과 가이드

## 📁 구조

```
docs_template/
├── sample/docs/     # 샘플 문서 (복사해서 사용)
├── guide/           # 문서 작성 가이드
└── setting/         # AI Agent 설정
```

## 🚀 시작하기

### 1. 새 프로젝트에 문서 구조 추가
```bash
cp -r docs_template/sample/docs your-project/docs
```

### 2. 작성 가이드 참고
- [PRD 작성 가이드](./guide/prd/how-to-write.md)
- [ADR 작성 가이드](./guide/adr/how-to-write.md)
- [Architecture 작성 가이드](./guide/architecture/how-to-write.md)
- [API 작성 가이드](./guide/api/how-to-write.md)
- [Testing 작성 가이드](./guide/testing/how-to-write.md)
- [Troubleshooting 작성 가이드](./guide/troubleshooting/how-to-write.md)
- [Runbook 작성 가이드](./guide/runbooks/how-to-write.md)
- [Guide 작성 가이드](./guide/guides/how-to-write.md)

### 3. AI Agent 설정
- [Documentation Rules](./setting/rules/documentation-rules.md)
- [Agent Prompt](./setting/prompts/documentation-agent-prompt.md)

## 📋 문서 유형

| 유형 | 목적 | 위치 |
|------|------|------|
| PRD | 제품 요구사항 | `docs/prd/` |
| ADR | 아키텍처 결정 | `docs/adr/` |
| Architecture | 시스템 구조 | `docs/architecture/` |
| API | API 명세 | `docs/api/` |
| Diagrams | 다이어그램 | `docs/diagrams/` |
| Testing | 테스트 문서 | `docs/testing/` |
| Troubleshooting | 장애 기록 | `docs/troubleshooting/` |
| Runbooks | 운영 절차 | `docs/runbooks/` |
| Guides | 개발 가이드 | `docs/guides/` |
| Learning | 학습 자료 | `docs/learning/` |

## 🏷️ 명명 규칙

| 유형 | 패턴 | 예시 |
|------|------|------|
| PRD | `PRD-XXX-[feature].md` | `PRD-001-user-auth.md` |
| ADR | `ADR-XXX-[decision].md` | `ADR-001-caching.md` |
| Test Plan | `TP-XXX-YY-[feature].md` | `TP-001-01-login.md` |
| Troubleshooting | `TS-YYYYMMDD-XXX-[title].md` | `TS-20260118-001-redis.md` |

---
**Version**: 1.0.0  
**Last Updated**: 2026-01-18
