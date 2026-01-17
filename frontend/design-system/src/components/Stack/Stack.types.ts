/**
 * Stack component props
 */
export interface StackProps {
  /**
   * Stack direction
   * @default 'vertical'
   */
  direction?: 'horizontal' | 'vertical';

  /**
   * Gap between items
   * @default 'md'
   */
  gap?: 'none' | 'xs' | 'sm' | 'md' | 'lg' | 'xl' | '2xl';

  /**
   * Cross-axis alignment
   * @default 'stretch'
   */
  align?: 'start' | 'center' | 'end' | 'stretch' | 'baseline';

  /**
   * Main-axis alignment
   * @default 'start'
   */
  justify?: 'start' | 'center' | 'end' | 'between' | 'around' | 'evenly';

  /**
   * Wrap items to next line
   * @default false
   */
  wrap?: boolean;

  /**
   * HTML tag to render
   * @default 'div'
   */
  as?: 'div' | 'section' | 'article' | 'ul' | 'ol' | 'nav';
}
