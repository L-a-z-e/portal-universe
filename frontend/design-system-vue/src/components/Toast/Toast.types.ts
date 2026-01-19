/**
 * Toast item
 */
export interface ToastItem {
  /**
   * Unique ID
   */
  id: string;

  /**
   * Toast variant
   * @default 'info'
   */
  variant?: 'info' | 'success' | 'warning' | 'error';

  /**
   * Title text
   */
  title?: string;

  /**
   * Message text
   */
  message: string;

  /**
   * Duration in ms (0 = persistent)
   * @default 5000
   */
  duration?: number;

  /**
   * Dismissible
   * @default true
   */
  dismissible?: boolean;

  /**
   * Action button
   */
  action?: {
    label: string;
    onClick: () => void;
  };
}

/**
 * Toast position
 */
export type ToastPosition =
  | 'top-right'
  | 'top-left'
  | 'top-center'
  | 'bottom-right'
  | 'bottom-left'
  | 'bottom-center';

/**
 * Toast container props
 */
export interface ToastContainerProps {
  /**
   * Position on screen
   * @default 'top-right'
   */
  position?: ToastPosition;

  /**
   * Maximum number of visible toasts
   * @default 5
   */
  maxToasts?: number;
}

/**
 * Toast component props (internal)
 */
export interface ToastProps extends ToastItem {}

/**
 * Toast component emits
 */
export interface ToastEmits {
  (e: 'dismiss', id: string): void;
}
