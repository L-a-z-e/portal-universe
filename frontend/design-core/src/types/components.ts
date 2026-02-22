/**
 * @portal/design-core - Component Props
 * Framework-agnostic component type definitions
 */

import type {
  Size,
  ExtendedSize,
  ButtonVariant,
  BadgeVariant,
  StatusVariant,
  TagVariant,
  CardVariant,
  PaddingSize,
  MaxWidth,
  Orientation,
  ContainerElement,
  StackElement,
  LinkTarget,
  Align,
  Justify,
  GapSize,
  FormSize,
  SkeletonAnimation,
  ToastPosition,
  DropdownPlacement,
  DividerVariant,
  DividerColor,
  LinkVariant,
  TabVariant,
  AvatarShape,
  AvatarStatus,
  SpinnerColor,
  SwitchColor,
  LabelPosition,
  DropdownTrigger,
  SkeletonVariant,
} from './common';

// ============================================
// Form Components
// ============================================

export interface ButtonProps {
  variant?: ButtonVariant;
  size?: Exclude<Size, 'xl'>;
  disabled?: boolean;
  loading?: boolean;
  fullWidth?: boolean;
  type?: 'button' | 'submit' | 'reset';
}

export interface InputProps {
  type?: 'text' | 'email' | 'password' | 'number' | 'tel' | 'url' | 'date' | 'time' | 'datetime-local' | 'search';
  value?: string | number;
  placeholder?: string;
  disabled?: boolean;
  error?: boolean;
  errorMessage?: string;
  label?: string;
  required?: boolean;
  size?: FormSize;
  name?: string;
  id?: string;
}

export interface TextareaProps extends Omit<InputProps, 'type'> {
  rows?: number;
  resize?: 'none' | 'vertical' | 'horizontal' | 'both';
}

export interface CheckboxProps {
  checked?: boolean;
  indeterminate?: boolean;
  disabled?: boolean;
  label?: string;
  error?: boolean;
  errorMessage?: string;
  size?: FormSize;
  value?: string | number;
  name?: string;
  id?: string;
}

export interface RadioOption {
  label: string;
  value: string | number;
  disabled?: boolean;
}

export interface RadioProps {
  value?: string | number;
  options: RadioOption[];
  name: string;
  direction?: Orientation;
  disabled?: boolean;
  error?: boolean;
  errorMessage?: string;
  size?: FormSize;
}

export interface SwitchProps {
  checked?: boolean;
  disabled?: boolean;
  label?: string;
  labelPosition?: LabelPosition;
  size?: FormSize;
  activeColor?: SwitchColor;
  name?: string;
  id?: string;
}

export interface SelectOption {
  label: string;
  value: string | number;
  disabled?: boolean;
}

export interface SelectProps {
  value?: string | number | null | (string | number)[];
  options: SelectOption[];
  placeholder?: string;
  disabled?: boolean;
  error?: boolean;
  errorMessage?: string;
  label?: string;
  required?: boolean;
  clearable?: boolean;
  searchable?: boolean;
  multiple?: boolean;
  size?: FormSize;
  name?: string;
  id?: string;
}

export interface FormFieldProps {
  label?: string;
  required?: boolean;
  error?: boolean;
  errorMessage?: string;
  helperText?: string;
  id?: string;
  disabled?: boolean;
  size?: FormSize;
}

export interface SearchBarProps {
  value: string;
  placeholder?: string;
  loading?: boolean;
  disabled?: boolean;
  autofocus?: boolean;
}

// ============================================
// Feedback Components
// ============================================

export interface AlertProps {
  variant?: StatusVariant;
  title?: string;
  dismissible?: boolean;
  showIcon?: boolean;
  bordered?: boolean;
}

export interface ToastItem {
  id: string;
  variant?: StatusVariant;
  title?: string;
  message: string;
  duration?: number;
  dismissible?: boolean;
  action?: {
    label: string;
    onClick: () => void;
  };
}

export interface ToastContainerProps {
  position?: ToastPosition;
  maxToasts?: number;
}

export interface ToastProps extends ToastItem {}

export interface ModalProps {
  open: boolean;
  title?: string;
  size?: Exclude<Size, 'xs'>;
  showClose?: boolean;
  closeOnBackdrop?: boolean;
  closeOnEscape?: boolean;
}

export interface SpinnerProps {
  size?: Size;
  color?: SpinnerColor;
  label?: string;
}

export interface SkeletonProps {
  variant?: SkeletonVariant;
  width?: string;
  height?: string;
  animation?: SkeletonAnimation;
  lines?: number;
}

export interface ProgressProps {
  value: number;
  max?: number;
  size?: FormSize;
  showLabel?: boolean;
  variant?: StatusVariant | 'default';
}

// ============================================
// Layout Components
// ============================================

export interface CardProps {
  variant?: CardVariant;
  hoverable?: boolean;
  padding?: PaddingSize;
}

export interface ContainerProps {
  maxWidth?: MaxWidth;
  centered?: boolean;
  padding?: Exclude<PaddingSize, 'xl'>;
  as?: ContainerElement;
}

export interface StackProps {
  direction?: Orientation;
  gap?: GapSize;
  align?: Align;
  justify?: Justify;
  wrap?: boolean;
  as?: StackElement;
}

export interface DividerProps {
  orientation?: Orientation;
  variant?: DividerVariant;
  color?: DividerColor;
  label?: string;
  spacing?: Exclude<PaddingSize, 'xl'>;
}

// ============================================
// Navigation Components
// ============================================

export interface BreadcrumbItem {
  label: string;
  href?: string;
  icon?: string;
}

export interface BreadcrumbProps {
  items: BreadcrumbItem[];
  separator?: string;
  maxItems?: number;
  size?: FormSize;
}

export interface TabItem {
  label: string;
  value: string;
  disabled?: boolean;
  icon?: string;
}

export interface TabsProps {
  value: string;
  items: TabItem[];
  variant?: TabVariant;
  size?: FormSize;
  fullWidth?: boolean;
}

export interface LinkProps {
  href?: string;
  target?: LinkTarget;
  variant?: LinkVariant;
  external?: boolean;
  disabled?: boolean;
  size?: FormSize;
}

export interface DropdownItem {
  label: string;
  value?: string | number;
  icon?: string;
  disabled?: boolean;
  divider?: boolean;
}

export interface DropdownProps {
  items: DropdownItem[];
  trigger?: DropdownTrigger;
  placement?: DropdownPlacement;
  disabled?: boolean;
  closeOnSelect?: boolean;
  width?: 'auto' | 'trigger' | string;
}

export interface PaginationProps {
  page: number;
  totalPages: number;
  siblingCount?: number;
  showFirstLast?: boolean;
  size?: FormSize;
}

// ============================================
// Data Display Components
// ============================================

export interface BadgeProps {
  variant?: BadgeVariant;
  size?: Exclude<Size, 'xl'>;
}

export interface TagProps {
  variant?: TagVariant;
  size?: FormSize;
  removable?: boolean;
  clickable?: boolean;
}

export interface AvatarProps {
  src?: string;
  alt?: string;
  name?: string;
  size?: ExtendedSize;
  status?: AvatarStatus;
  shape?: AvatarShape;
}

export interface TooltipProps {
  content: string;
  placement?: DropdownPlacement;
  delay?: number;
  disabled?: boolean;
}

export interface TableColumn<T = unknown> {
  key: string;
  label: string;
  sortable?: boolean;
  width?: string;
  align?: 'left' | 'center' | 'right';
  render?: (value: unknown, row: T) => unknown;
}

export interface TableProps<T = unknown> {
  columns: TableColumn<T>[];
  data: T[];
  loading?: boolean;
  emptyText?: string;
  striped?: boolean;
  hoverable?: boolean;
  onRowClick?: (row: T, index: number) => void;
}

// ============================================
// Overlay Components
// ============================================

export interface PopoverProps {
  open: boolean;
  placement?: DropdownPlacement;
  trigger?: DropdownTrigger;
  closeOnClickOutside?: boolean;
}
