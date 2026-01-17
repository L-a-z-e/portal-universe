/**
 * Skeleton component props
 */
export interface SkeletonProps {
  /**
   * Shape variant
   * @default 'text'
   */
  variant?: 'text' | 'circular' | 'rectangular' | 'rounded';

  /**
   * Width (CSS value)
   */
  width?: string;

  /**
   * Height (CSS value)
   */
  height?: string;

  /**
   * Animation type
   * @default 'pulse'
   */
  animation?: 'pulse' | 'wave' | 'none';

  /**
   * Number of lines (for text variant)
   * @default 1
   */
  lines?: number;
}
