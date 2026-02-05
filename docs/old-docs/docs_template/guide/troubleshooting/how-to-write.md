# Troubleshooting 문서 작성 가이드

## 📋 개요
문제 해결 기록을 작성하는 가이드입니다.

## 📁 위치 및 명명 규칙
- 위치: `docs/troubleshooting/YYYY/MM/`
- 파일명: `TS-YYYYMMDD-XXX-[title].md`
- 예시: `TS-20260118-001-redis-connection-timeout.md`

## 📝 필수 섹션

### 1. 메타데이터
```yaml
---
id: TS-YYYYMMDD-XXX
title: [제목]
type: troubleshooting
status: investigating | resolved
created: YYYY-MM-DD
updated: YYYY-MM-DD
author: [작성자]
severity: critical | high | medium | low
resolved: true | false
affected_services: [영향받은 서비스 목록]
tags: [태그 배열]
---
```

### 2. 요약 테이블
| 항목 | 내용 |
|------|------|
| **심각도** | 아이콘 + 레벨 |
| **발생일** | YYYY-MM-DD HH:MM |
| **해결일** | YYYY-MM-DD HH:MM |
| **영향 서비스** | 서비스 목록 |

### 3. 증상 (Symptoms)
- 어떤 현상이 발생했는가?
- 에러 메시지
- 모니터링 지표

### 4. 원인 분석 (Root Cause)
- 근본 원인
- 분석 과정

### 5. 해결 방법 (Solution)
- 즉시 조치 (명령어 포함)
- 영구 조치

### 6. 재발 방지 (Prevention)
- 알람 설정
- 프로세스 개선

### 7. 학습 포인트

## 🏷️ 심각도 기준
| 레벨 | 아이콘 | 기준 |
|------|--------|------|
| Critical | 🔴 | 서비스 전체 중단 |
| High | 🟠 | 주요 기능 장애 |
| Medium | 🟡 | 일부 기능 영향 |
| Low | 🟢 | 경미한 이슈 |

## ✅ 체크리스트
- [ ] 연/월 디렉토리가 올바른가?
- [ ] 증상이 구체적으로 기록되었는가?
- [ ] 해결 명령어가 복사 가능한가?
- [ ] 재발 방지책이 있는가?
- [ ] README 인덱스에 추가했는가?
