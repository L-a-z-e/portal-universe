import type { RouteLocationRaw } from 'vue-router';

/**
 * Link component props
 */
export interface LinkProps {
  /**
   * URL for external links
   */
  href?: string;

  /**
   * Vue Router to prop
   */
  to?: RouteLocationRaw;

  /**
   * Target attribute
   * @default '_self'
   */
  target?: '_self' | '_blank' | '_parent' | '_top';

  /**
   * Visual variant
   * @default 'default'
   */
  variant?: 'default' | 'primary' | 'muted' | 'underline';

  /**
   * External link (shows icon)
   * @default false
   */
  external?: boolean;

  /**
   * Disabled state
   * @default false
   */
  disabled?: boolean;

  /**
   * Size variant
   * @default 'md'
   */
  size?: 'sm' | 'md' | 'lg';
}
