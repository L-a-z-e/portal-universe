---
id: css-variables
title: CSS Variables Reference
type: api
status: current
created: 2026-02-06
updated: 2026-02-06
author: documenter
tags: [api, css-variables, reference, design-tokens]
related:
  - themes
  - token-system
---

# CSS Variables Reference

Design Tokens에서 생성되는 모든 CSS 변수의 완전한 레퍼런스입니다.

## 개요

| 항목 | 값 |
|------|-----|
| **패키지** | `@portal/design-tokens` |
| **빌드 출력** | `dist/tokens.css` |
| **총 변수 수** | 242개 |

## Color - Base Tokens

### Brand Colors

기본 브랜드 색상입니다.

| Variable | Value | Description |
|----------|-------|-------------|
| `--color-brand-primary` | `#5e6ad2` | 기본 브랜드 색상 - Linear indigo |
| `--color-brand-primaryHover` | `#4f5bc0` | Hover 상태 |
| `--color-brand-secondary` | `#8f9df8` | 보조 브랜드 색상 |

### Neutral Scale

범용 중립 색상 스케일입니다.

| Variable | Value |
|----------|-------|
| `--color-neutral-white` | `#ffffff` |
| `--color-neutral-50` | `#f9fafb` |
| `--color-neutral-100` | `#f3f4f6` |
| `--color-neutral-200` | `#e5e7eb` |
| `--color-neutral-300` | `#d1d5db` |
| `--color-neutral-400` | `#9ca3af` |
| `--color-neutral-500` | `#6b7280` |
| `--color-neutral-600` | `#4b5563` |
| `--color-neutral-700` | `#374151` |
| `--color-neutral-800` | `#1f2937` |
| `--color-neutral-900` | `#111827` |
| `--color-neutral-black` | `#000000` |

### Linear Gray Scale

Linear.app 스타일의 다크 UI 최적화 그레이 스케일입니다.

| Variable | Value |
|----------|-------|
| `--color-linear-50` | `#f7f8f8` |
| `--color-linear-100` | `#ebeced` |
| `--color-linear-200` | `#d0d6e0` |
| `--color-linear-300` | `#b4b4b4` |
| `--color-linear-400` | `#6b6b6b` |
| `--color-linear-500` | `#5c6169` |
| `--color-linear-600` | `#3e3e44` |
| `--color-linear-700` | `#2a2a2a` |
| `--color-linear-800` | `#18191b` |
| `--color-linear-850` | `#0f1011` |
| `--color-linear-900` | `#0e0f10` |
| `--color-linear-950` | `#08090a` |

### Indigo Scale

Linear primary accent 색상입니다.

| Variable | Value |
|----------|-------|
| `--color-indigo-50` | `#e8eaf8` |
| `--color-indigo-100` | `#c2c7ef` |
| `--color-indigo-200` | `#9ba3e5` |
| `--color-indigo-300` | `#747fdb` |
| `--color-indigo-400` | `#5e6ad2` |
| `--color-indigo-500` | `#4754c9` |
| `--color-indigo-600` | `#3f4ab8` |
| `--color-indigo-700` | `#363fa3` |
| `--color-indigo-800` | `#2e348f` |
| `--color-indigo-900` | `#1e2266` |

### Green Scale

Blog 서비스 시그니처 색상입니다.

| Variable | Value |
|----------|-------|
| `--color-green-50` | `#E6FCF5` |
| `--color-green-100` | `#C3FAE8` |
| `--color-green-200` | `#96F2D7` |
| `--color-green-300` | `#63E6BE` |
| `--color-green-400` | `#38D9A9` |
| `--color-green-500` | `#20C997` |
| `--color-green-600` | `#12B886` |
| `--color-green-700` | `#0CA678` |
| `--color-green-800` | `#099268` |
| `--color-green-900` | `#087F5B` |

### Orange Scale

Shopping 서비스 시그니처 색상 (Shopify 권장)입니다.

| Variable | Value |
|----------|-------|
| `--color-orange-50` | `#FFF4E6` |
| `--color-orange-100` | `#FFE8CC` |
| `--color-orange-200` | `#FFD8A8` |
| `--color-orange-300` | `#FFC078` |
| `--color-orange-400` | `#FFA94D` |
| `--color-orange-500` | `#FF6B35` |
| `--color-orange-600` | `#FF6B35` |
| `--color-orange-700` | `#E55A2B` |
| `--color-orange-800` | `#E8590C` |
| `--color-orange-900` | `#D9480F` |

### Gray Scale

텍스트 가독성에 최적화된 그레이 스케일입니다.

| Variable | Value |
|----------|-------|
| `--color-gray-50` | `#F8F9FA` |
| `--color-gray-100` | `#F1F3F5` |
| `--color-gray-200` | `#E9ECEF` |
| `--color-gray-300` | `#DEE2E6` |
| `--color-gray-400` | `#CED4DA` |
| `--color-gray-500` | `#ADB5BD` |
| `--color-gray-600` | `#868E96` |
| `--color-gray-700` | `#495057` |
| `--color-gray-800` | `#343A40` |
| `--color-gray-900` | `#212529` |

### Red Scale

에러 및 파괴적 액션 색상입니다.

| Variable | Value |
|----------|-------|
| `--color-red-50` | `#FFE3E3` |
| `--color-red-100` | `#ffccd5` |
| `--color-red-200` | `#ffb3c1` |
| `--color-red-300` | `#ff8fa3` |
| `--color-red-400` | `#ff758f` |
| `--color-red-500` | `#FA5252` |
| `--color-red-600` | `#F03E3E` |
| `--color-red-700` | `#d62828` |
| `--color-red-800` | `#a4161a` |
| `--color-red-900` | `#C92A2A` |

### Blue Scale

정보 및 링크 색상입니다.

| Variable | Value |
|----------|-------|
| `--color-blue-50` | `#D0EBFF` |
| `--color-blue-100` | `#bbd9f8` |
| `--color-blue-200` | `#96c4f0` |
| `--color-blue-300` | `#70afe8` |
| `--color-blue-400` | `#4a9ae0` |
| `--color-blue-500` | `#339AF0` |
| `--color-blue-600` | `#228BE6` |
| `--color-blue-700` | `#1976D2` |
| `--color-blue-800` | `#145eaa` |
| `--color-blue-900` | `#1864AB` |

### Yellow Scale

경고 상태 색상입니다.

| Variable | Value |
|----------|-------|
| `--color-yellow-50` | `#FFF9DB` |
| `--color-yellow-100` | `#fff3bf` |
| `--color-yellow-200` | `#ffec99` |
| `--color-yellow-300` | `#ffe066` |
| `--color-yellow-400` | `#ffd43b` |
| `--color-yellow-500` | `#FCC419` |
| `--color-yellow-600` | `#FAB005` |
| `--color-yellow-700` | `#f59f00` |
| `--color-yellow-800` | `#e67700` |
| `--color-yellow-900` | `#d9480f` |

### Cyan Scale

Linear accent cyan 색상입니다.

| Variable | Value |
|----------|-------|
| `--color-cyan-50` | `#e6fcf5` |
| `--color-cyan-100` | `#c3fae8` |
| `--color-cyan-200` | `#96f2d7` |
| `--color-cyan-300` | `#63e6be` |
| `--color-cyan-400` | `#38d9a9` |
| `--color-cyan-500` | `#20c997` |
| `--color-cyan-600` | `#12b886` |
| `--color-cyan-700` | `#0ca678` |
| `--color-cyan-800` | `#099268` |
| `--color-cyan-900` | `#087f5b` |

## Color - Semantic Tokens

서비스와 테마에 따라 동적으로 변경되는 의미론적 색상입니다.

### Brand

| Variable | Description | Default |
|----------|-------------|---------|
| `--semantic-brand-primary` | 주요 브랜드 색상 | `#5e6ad2` |
| `--semantic-brand-primaryHover` | 브랜드 색상 hover | `#747fdb` |
| `--semantic-brand-secondary` | 보조 브랜드 색상 | `#9ba3e5` |

### Text

| Variable | Description | Default (Dark) |
|----------|-------------|----------------|
| `--semantic-text-heading` | 제목 텍스트 | `#ffffff` |
| `--semantic-text-body` | 본문 텍스트 | `#b4b4b4` |
| `--semantic-text-meta` | 메타데이터, 타임스탬프 | `#6b6b6b` |
| `--semantic-text-muted` | 비활성/약화된 텍스트 | `#6b6b6b` |
| `--semantic-text-inverse` | 밝은 배경의 텍스트 | `#08090a` |
| `--semantic-text-link` | 링크 텍스트 | `#5e6ad2` |
| `--semantic-text-linkHover` | 링크 hover | `#747fdb` |

### Background

| Variable | Description | Default (Dark) |
|----------|-------------|----------------|
| `--semantic-bg-page` | 페이지 배경 | `#08090a` |
| `--semantic-bg-card` | 카드/표면 배경 | `#0f1011` |
| `--semantic-bg-elevated` | 모달, 드롭다운 배경 | `#18191b` |
| `--semantic-bg-muted` | 약화된 배경 | `#0e0f10` |
| `--semantic-bg-hover` | Hover 상태 배경 | `#2a2a2a` |

### Border

| Variable | Description | Default (Dark) |
|----------|-------------|----------------|
| `--semantic-border-default` | 기본 테두리 | `#2a2a2a` |
| `--semantic-border-hover` | Hover 테두리 | `#3e3e44` |
| `--semantic-border-focus` | Focus 테두리 | `#5e6ad2` |
| `--semantic-border-muted` | 약화된 테두리 | `#18191b` |
| `--semantic-border-subtle` | 미세한 테두리 (glassmorphism) | `rgba(255, 255, 255, 0.1)` |

### Status

| Variable | Description | Default |
|----------|-------------|---------|
| `--semantic-status-success` | 성공 색상 | `#20C997` |
| `--semantic-status-successBg` | 성공 배경 | `#0a4034` |
| `--semantic-status-error` | 에러 색상 | `#FA5252` |
| `--semantic-status-errorBg` | 에러 배경 | `#4a1c1c` |
| `--semantic-status-warning` | 경고 색상 | `#FCC419` |
| `--semantic-status-warningBg` | 경고 배경 | `#4a3c0f` |
| `--semantic-status-info` | 정보 색상 | `#339AF0` |
| `--semantic-status-infoBg` | 정보 배경 | `#1a3a52` |

### Accent

| Variable | Value |
|----------|-------|
| `--semantic-accent-indigo` | `#5e6ad2` |
| `--semantic-accent-blue` | `#339AF0` |
| `--semantic-accent-green` | `#20C997` |
| `--semantic-accent-red` | `#FA5252` |
| `--semantic-accent-orange` | `#FF6B35` |
| `--semantic-accent-yellow` | `#FCC419` |
| `--semantic-accent-cyan` | `#20c997` |

## Typography

### Font Family

| Variable | Value |
|----------|-------|
| `--typography-fontFamily-sans` | `'Inter Variable', 'Inter', -apple-system, BlinkMacSystemFont, 'Pretendard Variable', 'Segoe UI', Roboto, 'Helvetica Neue', Arial, 'Noto Sans KR', sans-serif` |
| `--typography-fontFamily-mono` | `'JetBrains Mono', 'Fira Code', 'SF Mono', Consolas, 'Courier New', monospace` |
| `--typography-fontFamily-serif` | `'Noto Serif KR', Georgia, 'Times New Roman', serif` |

### Font Size

| Variable | Value | Pixel | Description |
|----------|-------|-------|-------------|
| `--typography-fontSize-micro` | `0.625rem` | 10px | Badges, status indicators |
| `--typography-fontSize-xs` | `0.6875rem` | 11px | Captions, small labels |
| `--typography-fontSize-sm` | `0.8125rem` | 13px | Helper text, metadata |
| `--typography-fontSize-base` | `0.875rem` | 14px | Default body text (Linear default) |
| `--typography-fontSize-lg` | `1rem` | 16px | Emphasized body |
| `--typography-fontSize-xl` | `1.125rem` | 18px | h4 / subheadings |
| `--typography-fontSize-2xl` | `1.25rem` | 20px | h3 / section headings |
| `--typography-fontSize-3xl` | `1.5rem` | 24px | h2 / major headings |
| `--typography-fontSize-4xl` | `1.875rem` | 30px | h1 / page titles |
| `--typography-fontSize-5xl` | `2.25rem` | 36px | Hero headings |
| `--typography-fontSize-6xl` | `3rem` | 48px | Large displays |
| `--typography-fontSize-7xl` | `3.75rem` | 60px | Extra large displays |
| `--typography-fontSize-8xl` | `4.5rem` | 72px | Landing page headlines |
| `--typography-fontSize-9xl` | `6rem` | 96px | Maximum display size |

### Font Weight

Linear 최적화된 가중치입니다.

| Variable | Value | Description |
|----------|-------|-------------|
| `--typography-fontWeight-light` | `300` | Light weight - reduced emphasis |
| `--typography-fontWeight-normal` | `400` | Regular weight - default body text |
| `--typography-fontWeight-medium` | `510` | Linear medium weight - subtle emphasis |
| `--typography-fontWeight-semibold` | `590` | Linear semibold - moderate emphasis |
| `--typography-fontWeight-bold` | `680` | Linear bold - strong emphasis |
| `--typography-fontWeight-extrabold` | `800` | Extra bold - maximum emphasis |

### Line Height

| Variable | Value | Description |
|----------|-------|-------------|
| `--typography-lineHeight-none` | `1` | No line height - icons, single chars |
| `--typography-lineHeight-tight` | `1.2` | Headlines and compact text |
| `--typography-lineHeight-snug` | `1.375` | Subheadings and labels |
| `--typography-lineHeight-normal` | `1.5` | Default line height for body text |
| `--typography-lineHeight-relaxed` | `1.625` | Short paragraphs and UI text |
| `--typography-lineHeight-loose` | `1.75` | Long-form content - enhanced readability |

### Letter Spacing

| Variable | Value | Description |
|----------|-------|-------------|
| `--typography-letterSpacing-tighter` | `-0.05em` | Condensed spacing - headlines |
| `--typography-letterSpacing-tight` | `-0.025em` | Slightly condensed - subtitles |
| `--typography-letterSpacing-normal` | `0` | Normal spacing - default |
| `--typography-letterSpacing-wide` | `0.025em` | Slightly expanded - labels, UI |
| `--typography-letterSpacing-wider` | `0.05em` | Expanded spacing - all caps |
| `--typography-letterSpacing-widest` | `0.1em` | Maximum spacing - decorative |

## Spacing

| Variable | Value | Pixel | Description |
|----------|-------|-------|-------------|
| `--spacing-xs` | `0.25rem` | 4px | Extra small spacing |
| `--spacing-sm` | `0.5rem` | 8px | Small spacing |
| `--spacing-md` | `1rem` | 16px | Medium spacing (default) |
| `--spacing-lg` | `1.5rem` | 24px | Large spacing |
| `--spacing-xl` | `2rem` | 32px | Extra large spacing |
| `--spacing-2xl` | `3rem` | 48px | 2x large spacing (sections) |

## Border

### Border Radius

| Variable | Value | Pixel | Description |
|----------|-------|-------|-------------|
| `--border-radius-none` | `0` | 0px | No radius |
| `--border-radius-sm` | `0.125rem` | 2px | Slightly rounded |
| `--border-radius-default` | `0.25rem` | 4px | Default border radius |
| `--border-radius-md` | `0.375rem` | 6px | Medium rounded |
| `--border-radius-lg` | `0.5rem` | 8px | Large rounded |
| `--border-radius-full` | `9999px` | - | Fully rounded (pills, circles) |

### Border Width

| Variable | Value | Description |
|----------|-------|-------------|
| `--border-width-default` | `1px` | Standard border width |
| `--border-width-2` | `2px` | Medium border width |
| `--border-width-4` | `4px` | Thick border width |

## Effects

### Shadow

| Variable | Value |
|----------|-------|
| `--effects-shadow-none` | `none` |
| `--effects-shadow-sm` | `0 1px 2px 0 rgba(0, 0, 0, 0.05)` |
| `--effects-shadow-DEFAULT` | `0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px -1px rgba(0, 0, 0, 0.1)` |
| `--effects-shadow-md` | `0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -2px rgba(0, 0, 0, 0.1)` |
| `--effects-shadow-lg` | `0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -4px rgba(0, 0, 0, 0.1)` |
| `--effects-shadow-xl` | `0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1)` |
| `--effects-shadow-2xl` | `0 25px 50px -12px rgba(0, 0, 0, 0.25)` |
| `--effects-shadow-inner` | `inset 0 2px 4px 0 rgba(0, 0, 0, 0.05)` |
| `--effects-shadow-glow` | `0 0 20px rgba(94, 106, 210, 0.3)` |
| `--effects-shadow-glowLg` | `0 0 40px rgba(94, 106, 210, 0.4)` |

### Glass Blur (Glassmorphism)

| Variable | Value |
|----------|-------|
| `--effects-glass-blur-sm` | `4px` |
| `--effects-glass-blur-DEFAULT` | `12px` |
| `--effects-glass-blur-md` | `16px` |
| `--effects-glass-blur-lg` | `24px` |
| `--effects-glass-blur-xl` | `40px` |

### Glass Opacity

| Variable | Value | Description |
|----------|-------|-------------|
| `--effects-glass-opacity-light` | `0.7` | Light mode glass opacity |
| `--effects-glass-opacity-dark` | `0.5` | Dark mode glass opacity |

### Animation Duration

Linear 최적화된 애니메이션 지속 시간입니다.

| Variable | Value | Description |
|----------|-------|-------------|
| `--effects-animation-duration-instant` | `0ms` | Instant - no animation |
| `--effects-animation-duration-fast` | `100ms` | Fast micro-interactions |
| `--effects-animation-duration-normal` | `160ms` | Standard interactions (Linear default) |
| `--effects-animation-duration-slow` | `250ms` | Complex animations |
| `--effects-animation-duration-slower` | `400ms` | Page transitions, modals |

### Animation Easing

| Variable | Value | Description |
|----------|-------|-------------|
| `--effects-animation-easing-linear` | `linear` | Linear - constant speed |
| `--effects-animation-easing-ease` | `ease` | Default ease |
| `--effects-animation-easing-easeIn` | `cubic-bezier(0.4, 0, 1, 1)` | Ease in - accelerate |
| `--effects-animation-easing-easeOut` | `cubic-bezier(0, 0, 0.2, 1)` | Ease out - decelerate |
| `--effects-animation-easing-easeInOut` | `cubic-bezier(0.4, 0, 0.2, 1)` | Ease in-out - smooth |
| `--effects-animation-easing-linearEase` | `cubic-bezier(0.25, 0.1, 0.25, 1)` | Linear app default easing |
| `--effects-animation-easing-outQuart` | `cubic-bezier(0.165, 0.84, 0.44, 1)` | Out quart - snappy decelerate |
| `--effects-animation-easing-spring` | `cubic-bezier(0.68, -0.55, 0.265, 1.55)` | Spring - bouncy effect |

### Opacity

| Variable | Value |
|----------|-------|
| `--effects-opacity-0` | `0` |
| `--effects-opacity-5` | `0.05` |
| `--effects-opacity-10` | `0.1` |
| `--effects-opacity-20` | `0.2` |
| `--effects-opacity-30` | `0.3` |
| `--effects-opacity-40` | `0.4` |
| `--effects-opacity-50` | `0.5` |
| `--effects-opacity-60` | `0.6` |
| `--effects-opacity-70` | `0.7` |
| `--effects-opacity-80` | `0.8` |
| `--effects-opacity-90` | `0.9` |
| `--effects-opacity-95` | `0.95` |
| `--effects-opacity-100` | `1` |

## 사용 예시

### CSS에서 직접 사용

```css
.button {
  background-color: var(--semantic-brand-primary);
  color: var(--semantic-text-inverse);
  border-radius: var(--border-radius-md);
  padding: var(--spacing-sm) var(--spacing-md);
  transition: background-color var(--effects-animation-duration-normal) var(--effects-animation-easing-linearEase);
}

.button:hover {
  background-color: var(--semantic-brand-primaryHover);
}
```

### 테마 전환

테마는 `data-service`와 `data-theme` 속성으로 제어됩니다. 자세한 내용은 [themes.md](./themes.md)를 참조하세요.

```html
<html data-service="portal" data-theme="dark">
  <!-- Portal 다크 모드 -->
</html>

<html data-service="blog" data-theme="light">
  <!-- Blog 라이트 모드 -->
</html>
```

## 관련 문서

- [Themes API](./themes.md) - 테마 시스템 및 서비스별 오버라이드
- [Tailwind Preset API](./tailwind-preset.md) - Tailwind CSS 프리셋 사용법
- [useTheme Hook](./use-theme.md) - Vue/React 테마 관리 훅
- [Build Process](./build-process.md) - 토큰 빌드 프로세스
