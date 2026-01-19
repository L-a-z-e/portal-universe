# Runbook 작성 가이드

## 📋 개요
운영 절차서를 작성하는 가이드입니다.

## 📁 위치 및 명명 규칙
- 위치: `docs/runbooks/`
- 파일명: `[action]-[target].md` (kebab-case)
- 예시: `deploy-production.md`, `rollback.md`

## 📝 필수 섹션

### 1. 메타데이터
```yaml
---
id: runbook-[action]-[target]
title: [절차 제목]
type: runbook
status: current | deprecated
created: YYYY-MM-DD
updated: YYYY-MM-DD
author: [작성자]
tags: [태그 배열]
---
```

### 2. 개요 테이블
| 항목 | 내용 |
|------|------|
| **예상 소요 시간** | XX분 |
| **필요 권한** | 권한 목록 |
| **영향 범위** | 서비스/환경 |

### 3. 사전 조건
- [ ] 체크리스트 형태

### 4. 절차 (Step by Step)
각 Step마다:
- Step 번호와 제목
- 실행할 명령어 (코드 블록)
- **예상 결과** (중요!)

### 5. 문제 발생 시
- 롤백 절차 링크
- 또는 직접 대응 방법

### 6. 에스컬레이션
- 담당자 연락처

## 📐 Step 작성 형식

```markdown
### Step N: [단계명]
```bash
# 설명
명령어
```
**예상 결과**: [정상적인 출력 또는 상태]
```

## ⚠️ 주의사항
- 모든 명령어는 복사해서 바로 실행 가능해야 함
- 환경 변수는 명시적으로 표기
- 예상 결과 필수 기재

## ✅ 체크리스트
- [ ] 모든 Step에 예상 결과가 있는가?
- [ ] 명령어가 복사 가능한가?
- [ ] 롤백/에스컬레이션이 명시되었는가?
- [ ] README 인덱스에 추가했는가?
