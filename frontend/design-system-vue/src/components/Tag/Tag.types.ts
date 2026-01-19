/**
 * Tag Component Types
 * Used for blog tags, categories, labels, and chips
 */

export interface TagProps {
  /**
   * Visual style variant
   * @default 'default'
   */
  variant?: 'default' | 'primary' | 'success' | 'error' | 'warning' | 'info';

  /**
   * Size of the tag
   * @default 'md'
   */
  size?: 'sm' | 'md' | 'lg';

  /**
   * Shows remove button
   * @default false
   */
  removable?: boolean;

  /**
   * Makes tag clickable with cursor pointer
   * @default false
   */
  clickable?: boolean;
}

export interface TagEmits {
  (e: 'click'): void;
  (e: 'remove'): void;
}