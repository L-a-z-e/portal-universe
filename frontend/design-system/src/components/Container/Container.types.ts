/**
 * Container component props
 */
export interface ContainerProps {
  /**
   * Maximum width variant
   * @default 'lg'
   */
  maxWidth?: 'sm' | 'md' | 'lg' | 'xl' | '2xl' | 'full';

  /**
   * Center container horizontally
   * @default true
   */
  centered?: boolean;

  /**
   * Horizontal padding
   * @default 'md'
   */
  padding?: 'none' | 'sm' | 'md' | 'lg';

  /**
   * HTML tag to render
   * @default 'div'
   */
  as?: 'div' | 'section' | 'article' | 'main' | 'aside';
}
