# Learning

Portal Universe 개발 과정에서 습득한 학습 자료 모음입니다.

## 구조

```
learning/
├── README.md                              # 이 문서
├── admin-implementation-patterns.md       # Admin 구현 패턴
└── notes/                                 # 학습 노트
    ├── 01-domain-model.md
    ├── 02-saga-pattern.md
    ├── 03-concurrency-control.md
    ├── 04-snapshot-pattern.md
    ├── 05-react-fundamentals.md
    └── 06-shopping-frontend-implementation.md
```

## 학습 자료 목록

### 구현 패턴

| 파일명 | 주제 | 관련 서비스 |
|--------|------|-------------|
| [admin-implementation-patterns.md](./admin-implementation-patterns.md) | Admin 기능 구현 패턴 | shopping-service |

### 학습 노트

| 번호 | 파일명 | 주제 | 핵심 기술 |
|------|--------|------|-----------|
| 01 | [01-domain-model.md](./notes/01-domain-model.md) | 도메인 모델 설계 | DDD, Entity 설계 |
| 02 | [02-saga-pattern.md](./notes/02-saga-pattern.md) | Saga 패턴 | Orchestration, 분산 트랜잭션 |
| 03 | [03-concurrency-control.md](./notes/03-concurrency-control.md) | 동시성 제어 | Pessimistic Lock, Optimistic Lock |
| 04 | [04-snapshot-pattern.md](./notes/04-snapshot-pattern.md) | 스냅샷 패턴 | 가격 스냅샷, 이력 관리 |
| 05 | [05-react-fundamentals.md](./notes/05-react-fundamentals.md) | React 기초 | Hooks, Context API |
| 06 | [06-shopping-frontend-implementation.md](./notes/06-shopping-frontend-implementation.md) | Shopping Frontend 구현 | Module Federation, React Router |

## 주제별 인덱스

### Backend
- Domain Model: [01-domain-model.md](./notes/01-domain-model.md)
- Saga Pattern: [02-saga-pattern.md](./notes/02-saga-pattern.md)
- Concurrency: [03-concurrency-control.md](./notes/03-concurrency-control.md)
- Snapshot: [04-snapshot-pattern.md](./notes/04-snapshot-pattern.md)

### Frontend
- React: [05-react-fundamentals.md](./notes/05-react-fundamentals.md)
- Module Federation: [06-shopping-frontend-implementation.md](./notes/06-shopping-frontend-implementation.md)

### Admin
- Implementation Patterns: [admin-implementation-patterns.md](./admin-implementation-patterns.md)

## 관련 문서

- [ADR 목록](../adr/README.md) - 아키텍처 결정 기록
- [Architecture](../architecture/) - 아키텍처 설계 문서
- [PRD](../prd/) - 제품 요구사항 문서
