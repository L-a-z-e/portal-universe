# Testing

Portal Universe 테스트 문서 모음입니다.

## 구조

```
testing/
├── test-strategy.md    # 전체 테스트 전략
├── test-plan/          # 테스트 계획서
├── test-cases/         # 테스트 케이스
└── coverage/           # 커버리지 리포트
```

## 테스트 전략

[test-strategy.md](./test-strategy.md) - Unit, Integration, E2E 테스트 전략

## 테스트 계획서

| ID | 파일명 | 대상 | 상태 |
|----|--------|------|------|
| TP-001-01 | [TP-001-01-shopping-api.md](./test-plan/TP-001-01-shopping-api.md) | Shopping API | Draft |
| TP-002-01 | [TP-002-01-auth-flow.md](./test-plan/TP-002-01-auth-flow.md) | Auth Flow | Draft |

## 테스트 케이스

| ID | 파일명 | 관련 계획 | 상태 |
|----|--------|-----------|------|
| - | - | - | - |

## 커버리지

[coverage/](./coverage/) - 커버리지 리포트 및 가이드

## 작성 가이드

- [Testing 문서 작성 가이드](../../docs_template/guide/testing/how-to-write.md) 참조

### 명명 규칙

- **테스트 계획서**: `TP-XXX-YY-[feature].md`
  - XXX: PRD 번호
  - YY: 계획서 순번
- **테스트 케이스**: `TC-XXX-YY-ZZ-[scenario].md`
  - ZZ: 케이스 순번

## 테스트 실행

### Backend (JUnit)

```bash
cd services/shopping-service
./gradlew test
```

### Frontend (Vitest)

```bash
cd frontend/shopping-frontend
npm run test
```
