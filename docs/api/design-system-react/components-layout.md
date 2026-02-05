---
id: components-layout-react
title: Layout Components
type: api
status: current
created: 2026-02-06
updated: 2026-02-06
author: Laze
tags: [api, react, layout, card, container, stack, divider]
related:
  - components-button-react
  - components-feedback-react
---

# Layout Components

레이아웃 관련 컴포넌트의 API 레퍼런스입니다.

## Card

카드 컨테이너 컴포넌트입니다. Linear 스타일의 다크모드 우선 디자인입니다.

### Import

```tsx
import { Card } from '@portal/design-system-react';
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `variant` | `'elevated' \| 'outlined' \| 'flat' \| 'glass' \| 'interactive'` | `'elevated'` | 카드 스타일 |
| `hoverable` | `boolean` | - | 호버 효과 (interactive 제외) |
| `padding` | `'none' \| 'sm' \| 'md' \| 'lg' \| 'xl'` | `'md'` | 내부 패딩 |
| `children` | `ReactNode` | - | 카드 내용 |
| `className` | `string` | - | 추가 CSS 클래스 |

모든 표준 `<div>` HTML 속성도 지원합니다.

### Variants

```tsx
<div className="space-y-4">
  {/* 기본: 미세한 그림자 */}
  <Card variant="elevated">Elevated card</Card>

  {/* 테두리만 강조 */}
  <Card variant="outlined">Outlined card</Card>

  {/* 배경만, 테두리 없음 */}
  <Card variant="flat">Flat card</Card>

  {/* 글래스모피즘 효과 */}
  <Card variant="glass">Glass card</Card>

  {/* 클릭 가능한 카드 (호버 시 이동 + 그림자) */}
  <Card variant="interactive" onClick={handleClick}>
    Interactive card
  </Card>
</div>
```

### Padding

```tsx
<Card padding="none">No padding</Card>
<Card padding="sm">Small padding (12px)</Card>
<Card padding="md">Medium padding (16px)</Card>
<Card padding="lg">Large padding (24px)</Card>
<Card padding="xl">Extra large padding (32px)</Card>
```

### Hoverable

```tsx
{/* elevated + hoverable: 호버 시 살짝 올라가며 그림자 강화 */}
<Card hoverable>
  <h3>Hoverable Card</h3>
  <p>Hover to see effect</p>
</Card>

{/* interactive variant는 자체 호버 효과 포함, hoverable 불필요 */}
<Card variant="interactive">
  <h3>Interactive Card</h3>
  <p>Built-in hover effect</p>
</Card>
```

### 카드 레이아웃 예시

```tsx
function ProductCard({ product }: { product: Product }) {
  return (
    <Card variant="interactive" onClick={() => navigate(`/product/${product.id}`)}>
      <img src={product.image} alt={product.name} className="w-full rounded-lg" />
      <div className="mt-4">
        <h3 className="font-semibold">{product.name}</h3>
        <p className="text-text-muted mt-1">{product.description}</p>
        <p className="text-lg font-bold mt-2">{product.price}원</p>
      </div>
    </Card>
  );
}
```

---

## Container

콘텐츠 너비를 제한하는 래퍼 컴포넌트입니다. Polymorphic 컴포넌트로 렌더링 요소를 변경할 수 있습니다.

### Import

```tsx
import { Container } from '@portal/design-system-react';
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `maxWidth` | `'sm' \| 'md' \| 'lg' \| 'xl' \| '2xl' \| 'full'` | `'lg'` | 최대 너비 |
| `centered` | `boolean` | `true` | 가운데 정렬 (mx-auto) |
| `padding` | `'none' \| 'sm' \| 'md' \| 'lg'` | `'md'` | 좌우 패딩 |
| `as` | `'div' \| 'section' \| 'article' \| 'main' \| 'aside'` | `'div'` | 렌더링 HTML 요소 |
| `children` | `ReactNode` | - | 내용 |
| `className` | `string` | - | 추가 CSS 클래스 |

모든 표준 HTML 속성도 지원합니다.

### 기본 사용법

```tsx
<Container>
  <h1>Page Title</h1>
  <p>Content within max-width constraint</p>
</Container>
```

### Max Width

```tsx
{/* 640px */}
<Container maxWidth="sm">Narrow content</Container>

{/* 768px */}
<Container maxWidth="md">Medium content</Container>

{/* 1024px (기본) */}
<Container maxWidth="lg">Standard content</Container>

{/* 1280px */}
<Container maxWidth="xl">Wide content</Container>

{/* 1536px */}
<Container maxWidth="2xl">Extra wide content</Container>

{/* 제한 없음 */}
<Container maxWidth="full">Full width content</Container>
```

### Semantic HTML

```tsx
function PageLayout() {
  return (
    <>
      <Container as="main" maxWidth="xl">
        <Container as="article" maxWidth="md">
          <h1>Blog Post Title</h1>
          <p>Article content...</p>
        </Container>
      </Container>

      <Container as="aside" maxWidth="sm" padding="lg">
        <h2>Sidebar</h2>
      </Container>
    </>
  );
}
```

---

## Stack

Flexbox 기반 레이아웃 컴포넌트입니다. 수직/수평 방향으로 자식 요소를 배치합니다.

### Import

```tsx
import { Stack } from '@portal/design-system-react';
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `direction` | `'vertical' \| 'horizontal'` | `'vertical'` | 배치 방향 |
| `gap` | `'none' \| 'xs' \| 'sm' \| 'md' \| 'lg' \| 'xl' \| '2xl'` | `'md'` | 간격 |
| `align` | `'start' \| 'center' \| 'end' \| 'stretch' \| 'baseline'` | `'stretch'` | 교차축 정렬 |
| `justify` | `'start' \| 'center' \| 'end' \| 'between' \| 'around' \| 'evenly'` | `'start'` | 주축 정렬 |
| `wrap` | `boolean` | `false` | 줄바꿈 허용 |
| `as` | `'div' \| 'section' \| 'article' \| 'ul' \| 'ol' \| 'nav'` | `'div'` | 렌더링 HTML 요소 |
| `children` | `ReactNode` | - | 내용 |
| `className` | `string` | - | 추가 CSS 클래스 |

모든 표준 HTML 속성도 지원합니다.

### Gap Scale

| Gap | Tailwind | 실제 크기 |
|-----|----------|----------|
| `none` | `gap-0` | 0 |
| `xs` | `gap-1` | 4px |
| `sm` | `gap-2` | 8px |
| `md` | `gap-4` | 16px |
| `lg` | `gap-6` | 24px |
| `xl` | `gap-8` | 32px |
| `2xl` | `gap-12` | 48px |

### 기본 사용법

```tsx
{/* 수직 스택 (기본) */}
<Stack gap="md">
  <Card>Item 1</Card>
  <Card>Item 2</Card>
  <Card>Item 3</Card>
</Stack>

{/* 수평 스택 */}
<Stack direction="horizontal" gap="sm" align="center">
  <Avatar name="John" />
  <span>John Doe</span>
  <Badge variant="success">Active</Badge>
</Stack>
```

### Justify & Align

```tsx
{/* 양끝 정렬 */}
<Stack direction="horizontal" justify="between" align="center">
  <h1>Title</h1>
  <Button>Action</Button>
</Stack>

{/* 가운데 정렬 */}
<Stack align="center" justify="center" className="h-screen">
  <Spinner size="lg" />
  <p>Loading...</p>
</Stack>
```

### 줄바꿈

```tsx
<Stack direction="horizontal" gap="sm" wrap>
  {tags.map((tag) => (
    <Tag key={tag.id}>{tag.name}</Tag>
  ))}
</Stack>
```

### Semantic HTML

```tsx
{/* 네비게이션 메뉴 */}
<Stack as="nav" direction="horizontal" gap="md">
  <Link href="/">Home</Link>
  <Link href="/about">About</Link>
  <Link href="/contact">Contact</Link>
</Stack>

{/* 리스트 */}
<Stack as="ul" gap="sm">
  <li>Item 1</li>
  <li>Item 2</li>
</Stack>
```

---

## Divider

구분선 컴포넌트입니다. 수평/수직 방향 모두 지원하며, 레이블을 포함할 수 있습니다.

### Import

```tsx
import { Divider } from '@portal/design-system-react';
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `orientation` | `'horizontal' \| 'vertical'` | `'horizontal'` | 방향 |
| `variant` | `'solid' \| 'dashed' \| 'dotted'` | `'solid'` | 선 스타일 |
| `color` | `'default' \| 'muted' \| 'strong'` | `'default'` | 선 색상 |
| `label` | `string` | - | 중간 레이블 텍스트 |
| `spacing` | `'none' \| 'sm' \| 'md' \| 'lg'` | `'md'` | 상하 여백 |
| `className` | `string` | - | 추가 CSS 클래스 |

모든 표준 `<hr>` HTML 속성도 지원합니다.

### 기본 사용법

```tsx
<div>
  <p>Section 1</p>
  <Divider />
  <p>Section 2</p>
</div>
```

### Variants

```tsx
<Divider variant="solid" />
<Divider variant="dashed" />
<Divider variant="dotted" />
```

### With Label

```tsx
<Divider label="OR" />
<Divider label="Section Title" variant="dashed" />
```

### Vertical Divider

```tsx
<div className="flex items-center h-6 gap-4">
  <span>Left</span>
  <Divider orientation="vertical" />
  <span>Right</span>
</div>
```

### Colors

```tsx
<Divider color="default" />  {/* 기본 테두리 색상 */}
<Divider color="muted" />    {/* 연한 색상 */}
<Divider color="strong" />   {/* 강한 색상 */}
```

### Spacing

```tsx
<Divider spacing="none" />  {/* 여백 없음 */}
<Divider spacing="sm" />    {/* 8px */}
<Divider spacing="md" />    {/* 16px (기본) */}
<Divider spacing="lg" />    {/* 24px */}
```
