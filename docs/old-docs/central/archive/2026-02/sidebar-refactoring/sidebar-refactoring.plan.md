# Plan: Sidebar Refactoring

> Feature: `sidebar-refactoring`
> Created: 2026-02-03
> Status: Planning

## 1. 현재 문제점 분석

### 1.1 정렬 불일치

| 요소 | 현재 | 문제점 |
|------|------|--------|
| Navigation 버튼 | `justify-start gap-3` | 기준 ✅ |
| Notification Bell | 별도 div, `justify-center/start` 조건부 | 일관성 없음 |
| 프로필 버튼 | `justify-start gap-2` | gap 불일치 |
| Status/Settings | `justify-start gap-3` | OK |

### 1.2 알림 드롭다운 위치 문제

```
현재 상태:
┌─────────────────────────────┬────────────────────────────┐
│ Sidebar (w-64 = 256px)      │  (화면 밖으로 잘림!)        │
│                             │  NotificationDropdown      │
│  [🔔 알림]                   │  (w-80 = 320px, right-0)  │
│                             │                            │
└─────────────────────────────┴────────────────────────────┘
```

**원인**: `absolute right-0`으로 설정되어 Bell 버튼 기준 오른쪽으로 확장되는데, Sidebar 외부로 나감

### 1.3 프로필 영역 구조 문제

현재 Bottom Section 순서:
1. Notification Bell (별도 div)
2. Profile Button
3. Status
4. Settings
5. Logout
6. Collapse Toggle

**사용자 요청**: 프로필(아이디) 우측에 알림 아이콘

## 2. 목표

### 2.1 UI/UX 개선

1. **일관된 정렬**: 모든 버튼 `gap-3` 통일
2. **프로필 + 알림 통합**: 한 행에 프로필 + 알림 아이콘 배치
3. **알림 드롭다운 위치 수정**: Sidebar 안쪽 또는 오른쪽 화면에서 열리도록

### 2.2 예상 레이아웃

```
┌─────────────────────────────────────┐
│ ┌─┐ Portal Universe                 │ ← Logo
│ └─┘                                 │
├─────────────────────────────────────┤
│ 🏠 Home                             │ ← Navigation
│ 📝 Blog                             │
│ 🛒 Shopping                         │
│ 🤖 Prism                            │
│                                     │
│         (spacer)                    │
│                                     │
├─────────────────────────────────────┤
│ ┌──┐ Username        🔔            │ ← Profile + Bell (NEW)
│ └──┘ ADMIN                          │
│ 📊 Status                           │
│ ⚙️ Settings                         │
│ 🚪 Logout                           │
│ ◀️ Collapse                         │
└─────────────────────────────────────┘
```

### 2.3 알림 드롭다운 방향

```
옵션 A: 우측으로 확장 (화면 중앙)
┌─────────────────────────────────────────────────────────┐
│ Sidebar              │ NotificationDropdown             │
│ ┌──┐ Username    🔔  │ ┌────────────────────────────┐   │
│ └──┘                 │ │ 알림 목록...              │   │
│                      │ └────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘

옵션 B: 위쪽으로 확장 (Sidebar 내부)
┌─────────────────────────────────────┐
│                                     │
│   ┌────────────────────────────┐    │
│   │ 알림 목록...              │    │
│   └────────────────────────────┘    │
│ ┌──┐ Username        🔔            │
│ └──┘                                │
└─────────────────────────────────────┘
```

**권장**: 옵션 A (우측 확장) - 공간 활용 최적

## 3. 구현 범위

### 3.1 수정 대상 파일

| 파일 | 변경 내용 |
|------|----------|
| `Sidebar.vue` | Profile + Bell 통합, 정렬 통일 |
| `NotificationDropdown.vue` | 위치 조정 (`left-full` 또는 portal) |
| `NotificationBell.vue` | prop으로 dropdown 방향 제어 (선택) |

### 3.2 구현 순서

1. Profile + NotificationBell 같은 행 배치
2. NotificationDropdown 위치 수정
3. gap 일관성 적용
4. Collapsed 상태 처리

## 4. 비기능 요구사항

- **반응형**: 모바일에서도 알림 드롭다운 정상 표시
- **접근성**: 키보드 네비게이션, aria-label 유지
- **애니메이션**: 드롭다운 열림/닫힘 transition 유지

## 5. 테스트 계획

| 테스트 항목 | 검증 방법 |
|-------------|----------|
| 정렬 일관성 | 시각적 검증 (Playwright 스크린샷) |
| 알림 드롭다운 위치 | 클릭 후 화면 내 표시 확인 |
| Collapsed 상태 | 접힌 상태에서 Bell 아이콘만 표시 |
| 모바일 반응형 | 768px 이하에서 정상 동작 |

## 6. 다음 단계

```bash
/pdca design sidebar-refactoring
```
