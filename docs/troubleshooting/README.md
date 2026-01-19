# Troubleshooting

Portal Universe 문제 해결 기록 모음입니다.

## 구조

```
troubleshooting/
├── README.md           # 이 문서
├── templates/          # 템플릿
│   └── template.md
└── 2026/               # 연도별 폴더
    └── 01/             # 월별 폴더
        └── TS-*.md     # 개별 문서
```

## 문서 목록

### 2026년 1월

| ID | 제목 | 심각도 | 상태 | 영향 서비스 |
|----|------|--------|------|-------------|
| [TS-20260117-001](./2026/01/TS-20260117-001-react-module-federation.md) | React Error #31 Module Federation 호환성 | 🟠 High | 해결됨 | shopping-frontend |

## 심각도 기준

| 레벨 | 아이콘 | 기준 |
|------|--------|------|
| Critical | 🔴 | 서비스 전체 중단 |
| High | 🟠 | 주요 기능 장애 |
| Medium | 🟡 | 일부 기능 영향 |
| Low | 🟢 | 경미한 이슈 |

## 작성 가이드

새 문서 작성 시:

1. `templates/template.md` 복사
2. 연/월 디렉토리에 저장
3. 파일명: `TS-YYYYMMDD-XXX-[title].md`
4. 이 README에 항목 추가

[Troubleshooting 작성 가이드](../../docs_template/guide/troubleshooting/how-to-write.md) 참조

## 빠른 검색

### 서비스별

- **shopping-frontend**: [TS-20260117-001](./2026/01/TS-20260117-001-react-module-federation.md)

### 태그별

- **Module Federation**: [TS-20260117-001](./2026/01/TS-20260117-001-react-module-federation.md)
- **React**: [TS-20260117-001](./2026/01/TS-20260117-001-react-module-federation.md)
