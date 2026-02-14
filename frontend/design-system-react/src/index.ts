/**
 * @portal/design-system-react
 * React component library for Portal Universe Design System
 */

// Styles
import './styles/index.css';

// Export all components
export * from './components';

// Export hooks
export * from './hooks';

// Export utilities
export { cn } from './utils/cn';
export { useLogger } from './utils/useLogger';
export { ErrorBoundary } from './components/ErrorBoundary';
export type { ErrorBoundaryProps } from './components/ErrorBoundary';

// Re-export types from @portal/design-types
export type {
  // Common types
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
  // Component Props
  ButtonProps,
  InputProps,
  TextareaProps,
  CheckboxProps,
  RadioProps,
  RadioOption,
  SwitchProps,
  SelectProps,
  SelectOption,
  FormFieldProps,
  SearchBarProps,
  AlertProps,
  ToastItem,
  ToastContainerProps,
  ToastProps,
  ModalProps,
  SpinnerProps,
  SkeletonProps,
  ProgressProps,
  CardProps,
  ContainerProps,
  StackProps,
  DividerProps,
  TabsProps,
  TabItem,
  BreadcrumbProps,
  BreadcrumbItem,
  LinkProps,
  DropdownProps,
  DropdownItem,
  PaginationProps,
  BadgeProps,
  TagProps,
  AvatarProps,
  TableProps,
  TableColumn,
  TooltipProps,
  PopoverProps,
  // Logger types
  LogLevel,
  ErrorReporter,
  LoggerOptions,
  Logger,
  // Theme types
  ServiceType,
  ThemeMode,
  ThemeConfig,
} from '@portal/design-types';
