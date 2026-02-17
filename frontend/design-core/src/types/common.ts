/**
 * @portal/design-core - Common Types
 * Framework-agnostic type definitions
 */

// Size variants used across components
export type Size = 'xs' | 'sm' | 'md' | 'lg' | 'xl';

// Extended size for avatars
export type ExtendedSize = Size | '2xl';

// Button/Card variants
export type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'outline' | 'danger' | 'error';

// Badge variants
export type BadgeVariant = 'default' | 'primary' | 'success' | 'warning' | 'danger' | 'info' | 'outline' | 'brand' | 'error' | 'neutral';

// Status variants for alerts, toasts
export type StatusVariant = 'info' | 'success' | 'warning' | 'error';

// Tag variants
export type TagVariant = 'default' | 'primary' | 'success' | 'error' | 'warning' | 'info';

// Card variants
export type CardVariant = 'elevated' | 'outlined' | 'flat' | 'glass' | 'interactive' | 'glassStats';

// Padding options
export type PaddingSize = 'none' | 'sm' | 'md' | 'lg' | 'xl';

// Container max width
export type MaxWidth = 'sm' | 'md' | 'lg' | 'xl' | '2xl' | 'full';

// Orientation
export type Orientation = 'horizontal' | 'vertical';

// HTML element types for polymorphic components
export type ContainerElement = 'div' | 'section' | 'article' | 'main' | 'aside';
export type StackElement = 'div' | 'section' | 'article' | 'ul' | 'ol' | 'nav';

// Link target
export type LinkTarget = '_self' | '_blank' | '_parent' | '_top';

// Alignment options
export type Align = 'start' | 'center' | 'end' | 'stretch' | 'baseline';
export type Justify = 'start' | 'center' | 'end' | 'between' | 'around' | 'evenly';

// Gap sizes for Stack
export type GapSize = 'none' | 'xs' | 'sm' | 'md' | 'lg' | 'xl' | '2xl';

// Form field sizes (typically 3 sizes)
export type FormSize = 'sm' | 'md' | 'lg';

// Animation types
export type SkeletonAnimation = 'pulse' | 'wave' | 'none';

// Toast position
export type ToastPosition =
  | 'top-right'
  | 'top-left'
  | 'top-center'
  | 'bottom-right'
  | 'bottom-left'
  | 'bottom-center';

// Dropdown placement
export type DropdownPlacement =
  | 'bottom'
  | 'bottom-start'
  | 'bottom-end'
  | 'top'
  | 'top-start'
  | 'top-end';

// Divider styles
export type DividerVariant = 'solid' | 'dashed' | 'dotted';
export type DividerColor = 'default' | 'muted' | 'strong';

// Link variants
export type LinkVariant = 'default' | 'primary' | 'muted' | 'underline';

// Tab variants
export type TabVariant = 'default' | 'pills' | 'underline';

// Avatar shapes
export type AvatarShape = 'circle' | 'square';

// Avatar status
export type AvatarStatus = 'online' | 'offline' | 'busy' | 'away';

// Spinner colors
export type SpinnerColor = 'primary' | 'current' | 'white';

// Switch active colors
export type SwitchColor = 'primary' | 'success' | 'warning' | 'error';

// Label position
export type LabelPosition = 'left' | 'right';

// Dropdown trigger
export type DropdownTrigger = 'click' | 'hover';

// Skeleton variant
export type SkeletonVariant = 'text' | 'circular' | 'rectangular' | 'rounded';
