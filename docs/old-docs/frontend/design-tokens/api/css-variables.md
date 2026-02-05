---
id: css-variables
title: CSS Variables Reference
type: api
status: current
created: 2026-01-19
updated: 2026-01-19
author: Laze
tags: [api, css-variables, reference]
related:
  - token-system
  - getting-started
---

# CSS Variables Reference

Design Tokens에서 생성되는 모든 CSS 변수의 레퍼런스입니다.

## Color - Base Tokens

### Brand Colors

| Variable | Value | Description |
|----------|-------|-------------|
| `--color-brand-primary` | `#5e6ad2` | 기본 브랜드 색상 |
| `--color-brand-primaryHover` | `#6e7ae2` | Hover 상태 |
| `--color-brand-secondary` | `#8f9df8` | 보조 브랜드 색상 |

### Indigo Scale

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

### Linear Gray Scale

Linear.app 스타일의 다크 UI 최적화 그레이 스케일입니다.

| Variable | Value |
|----------|-------|
| `--color-linear-50` | `#f7f8f8` |
| `--color-linear-100` | `#ebeced` |
| `--color-linear-200` | `#d0d6e0` |
| `--color-linear-300` | `#8a8f98` |
| `--color-linear-400` | `#6c717a` |
| `--color-linear-500` | `#5c6169` |
| `--color-linear-600` | `#3e3e44` |
| `--color-linear-700` | `#26282b` |
| `--color-linear-800` | `#1b1c1e` |
| `--color-linear-850` | `#141516` |
| `--color-linear-900` | `#0e0f10` |
| `--color-linear-950` | `#08090a` |

### Green Scale (Blog)

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

### Orange Scale (Shopping)

| Variable | Value |
|----------|-------|
| `--color-orange-50` | `#FFF4E6` |
| `--color-orange-100` | `#FFE8CC` |
| `--color-orange-200` | `#FFD8A8` |
| `--color-orange-300` | `#FFC078` |
| `--color-orange-400` | `#FFA94D` |
| `--color-orange-500` | `#FF922B` |
| `--color-orange-600` | `#FD7E14` |
| `--color-orange-700` | `#F76707` |
| `--color-orange-800` | `#E8590C` |
| `--color-orange-900` | `#D9480F` |

### Status Colors

| Variable | Value | Description |
|----------|-------|-------------|
| `--color-red-500` | `#FA5252` | Error |
| `--color-green-500` | `#20C997` | Success |
| `--color-yellow-500` | `#FCC419` | Warning |
| `--color-blue-500` | `#339AF0` | Info |

## Color - Semantic Tokens

### Brand

| Variable | Description |
|----------|-------------|
| `--semantic-brand-primary` | 주요 브랜드 색상 |
| `--semantic-brand-primaryHover` | 브랜드 색상 hover |
| `--semantic-brand-secondary` | 보조 브랜드 색상 |

### Text

| Variable | Description |
|----------|-------------|
| `--semantic-text-heading` | 제목 텍스트 |
| `--semantic-text-body` | 본문 텍스트 |
| `--semantic-text-meta` | 메타데이터, 타임스탬프 |
| `--semantic-text-muted` | 비활성/약화된 텍스트 |
| `--semantic-text-inverse` | 밝은 배경의 텍스트 |
| `--semantic-text-link` | 링크 텍스트 |
| `--semantic-text-linkHover` | 링크 hover |

### Background

| Variable | Description |
|----------|-------------|
| `--semantic-bg-page` | 페이지 배경 |
| `--semantic-bg-card` | 카드/표면 배경 |
| `--semantic-bg-elevated` | 모달, 드롭다운 배경 |
| `--semantic-bg-muted` | 약화된 배경 |
| `--semantic-bg-hover` | Hover 상태 배경 |

### Border

| Variable | Description |
|----------|-------------|
| `--semantic-border-default` | 기본 테두리 |
| `--semantic-border-hover` | Hover 테두리 |
| `--semantic-border-focus` | Focus 테두리 |
| `--semantic-border-muted` | 약화된 테두리 |
| `--semantic-border-subtle` | 미세한 테두리 (glassmorphism) |

### Status

| Variable | Description |
|----------|-------------|
| `--semantic-status-success` | 성공 색상 |
| `--semantic-status-successBg` | 성공 배경 |
| `--semantic-status-error` | 에러 색상 |
| `--semantic-status-errorBg` | 에러 배경 |
| `--semantic-status-warning` | 경고 색상 |
| `--semantic-status-warningBg` | 경고 배경 |
| `--semantic-status-info` | 정보 색상 |
| `--semantic-status-infoBg` | 정보 배경 |

### Accent

| Variable | Description |
|----------|-------------|
| `--semantic-accent-indigo` | Indigo 강조 |
| `--semantic-accent-blue` | Blue 강조 |
| `--semantic-accent-green` | Green 강조 |
| `--semantic-accent-red` | Red 강조 |
| `--semantic-accent-orange` | Orange 강조 |
| `--semantic-accent-yellow` | Yellow 강조 |
| `--semantic-accent-cyan` | Cyan 강조 |

## Typography

### Font Family

| Variable | Value |
|----------|-------|
| `--typography-fontFamily-sans` | `'Inter Variable', 'Inter', -apple-system, ...` |
| `--typography-fontFamily-serif` | `'Noto Serif KR', Georgia, ...` |
| `--typography-fontFamily-mono` | `'JetBrains Mono', 'Fira Code', ...` |

### Font Size

| Variable | Value |
|----------|-------|
| `--typography-fontSize-micro` | `0.625rem` |
| `--typography-fontSize-xs` | `0.6875rem` |
| `--typography-fontSize-sm` | `0.8125rem` |
| `--typography-fontSize-base` | `0.875rem` |
| `--typography-fontSize-lg` | `1rem` |
| `--typography-fontSize-xl` | `1.125rem` |
| `--typography-fontSize-2xl` | `1.25rem` |
| `--typography-fontSize-3xl` | `1.5rem` |
| `--typography-fontSize-4xl` | `1.875rem` |
| `--typography-fontSize-5xl` | `2.25rem` |
| `--typography-fontSize-6xl` | `3rem` |
| `--typography-fontSize-7xl` | `3.75rem` |
| `--typography-fontSize-8xl` | `4.5rem` |
| `--typography-fontSize-9xl` | `6rem` |

### Font Weight

| Variable | Value |
|----------|-------|
| `--typography-fontWeight-light` | `300` |
| `--typography-fontWeight-normal` | `400` |
| `--typography-fontWeight-medium` | `510` |
| `--typography-fontWeight-semibold` | `590` |
| `--typography-fontWeight-bold` | `680` |
| `--typography-fontWeight-extrabold` | `800` |

### Line Height

| Variable | Value |
|----------|-------|
| `--typography-lineHeight-none` | `1` |
| `--typography-lineHeight-tight` | `1.2` |
| `--typography-lineHeight-snug` | `1.375` |
| `--typography-lineHeight-normal` | `1.5` |
| `--typography-lineHeight-relaxed` | `1.625` |
| `--typography-lineHeight-loose` | `1.75` |

## Spacing

| Variable | Value |
|----------|-------|
| `--spacing-xs` | `0.25rem` |
| `--spacing-sm` | `0.5rem` |
| `--spacing-md` | `1rem` |
| `--spacing-lg` | `1.5rem` |
| `--spacing-xl` | `2rem` |
| `--spacing-2xl` | `3rem` |

## Border

### Border Radius

| Variable | Value |
|----------|-------|
| `--border-radius-none` | `0` |
| `--border-radius-sm` | `0.125rem` |
| `--border-radius-default` | `0.25rem` |
| `--border-radius-md` | `0.375rem` |
| `--border-radius-lg` | `0.5rem` |
| `--border-radius-full` | `9999px` |

### Border Width

| Variable | Value |
|----------|-------|
| `--border-width-default` | `1px` |
| `--border-width-2` | `2px` |
| `--border-width-4` | `4px` |

## Effects

### Shadow

| Variable | Value |
|----------|-------|
| `--effects-shadow-none` | `none` |
| `--effects-shadow-sm` | `0 1px 2px 0 rgba(0, 0, 0, 0.05)` |
| `--effects-shadow-DEFAULT` | `0 1px 3px 0 rgba(0, 0, 0, 0.1), ...` |
| `--effects-shadow-md` | `0 4px 6px -1px rgba(0, 0, 0, 0.1), ...` |
| `--effects-shadow-lg` | `0 10px 15px -3px rgba(0, 0, 0, 0.1), ...` |
| `--effects-shadow-xl` | `0 20px 25px -5px rgba(0, 0, 0, 0.1), ...` |
| `--effects-shadow-2xl` | `0 25px 50px -12px rgba(0, 0, 0, 0.25)` |
| `--effects-shadow-inner` | `inset 0 2px 4px 0 rgba(0, 0, 0, 0.05)` |
| `--effects-shadow-glow` | `0 0 20px rgba(94, 106, 210, 0.3)` |
| `--effects-shadow-glowLg` | `0 0 40px rgba(94, 106, 210, 0.4)` |

### Glass Blur

| Variable | Value |
|----------|-------|
| `--effects-glass-blur-sm` | `4px` |
| `--effects-glass-blur-DEFAULT` | `12px` |
| `--effects-glass-blur-md` | `16px` |
| `--effects-glass-blur-lg` | `24px` |
| `--effects-glass-blur-xl` | `40px` |

### Animation Duration

| Variable | Value |
|----------|-------|
| `--effects-animation-duration-instant` | `0ms` |
| `--effects-animation-duration-fast` | `100ms` |
| `--effects-animation-duration-normal` | `160ms` |
| `--effects-animation-duration-slow` | `250ms` |
| `--effects-animation-duration-slower` | `400ms` |

### Animation Easing

| Variable | Value |
|----------|-------|
| `--effects-animation-easing-linear` | `linear` |
| `--effects-animation-easing-ease` | `ease` |
| `--effects-animation-easing-easeIn` | `cubic-bezier(0.4, 0, 1, 1)` |
| `--effects-animation-easing-easeOut` | `cubic-bezier(0, 0, 0.2, 1)` |
| `--effects-animation-easing-easeInOut` | `cubic-bezier(0.4, 0, 0.2, 1)` |
| `--effects-animation-easing-spring` | `cubic-bezier(0.68, -0.55, 0.265, 1.55)` |

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
