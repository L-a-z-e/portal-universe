/**
 * Spinner component props
 */
export interface SpinnerProps {
  /**
   * Size
   * @default 'md'
   */
  size?: 'xs' | 'sm' | 'md' | 'lg' | 'xl';

  /**
   * Color
   * @default 'primary'
   */
  color?: 'primary' | 'current' | 'white';

  /**
   * Label for accessibility
   * @default 'Loading'
   */
  label?: string;
}
