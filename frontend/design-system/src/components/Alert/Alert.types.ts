/**
 * Alert component props
 */
export interface AlertProps {
  /**
   * Alert variant
   * @default 'info'
   */
  variant?: 'info' | 'success' | 'warning' | 'error';

  /**
   * Title text
   */
  title?: string;

  /**
   * Dismissible
   * @default false
   */
  dismissible?: boolean;

  /**
   * Show icon
   * @default true
   */
  showIcon?: boolean;

  /**
   * Bordered style
   * @default false
   */
  bordered?: boolean;
}

/**
 * Alert component emits
 */
export interface AlertEmits {
  (e: 'dismiss'): void;
}
