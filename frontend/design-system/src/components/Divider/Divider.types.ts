/**
 * Divider component props
 */
export interface DividerProps {
  /**
   * Orientation
   * @default 'horizontal'
   */
  orientation?: 'horizontal' | 'vertical';

  /**
   * Visual style
   * @default 'solid'
   */
  variant?: 'solid' | 'dashed' | 'dotted';

  /**
   * Color intensity
   * @default 'default'
   */
  color?: 'default' | 'muted' | 'strong';

  /**
   * Label text in center
   */
  label?: string;

  /**
   * Spacing around divider
   * @default 'md'
   */
  spacing?: 'none' | 'sm' | 'md' | 'lg';
}
