import type { Size } from '../types/common';

export const modalSizes: Record<Exclude<Size, 'xs'>, string> = {
  sm: 'max-w-md',
  md: 'max-w-lg',
  lg: 'max-w-2xl',
  xl: 'max-w-4xl',
};
