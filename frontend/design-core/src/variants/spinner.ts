import type { Size, SpinnerColor } from '../types/common';

export const spinnerSizes: Record<Size, string> = {
  xs: 'w-3 h-3',
  sm: 'w-4 h-4',
  md: 'w-6 h-6',
  lg: 'w-8 h-8',
  xl: 'w-12 h-12',
};

export const spinnerColors: Record<SpinnerColor, string> = {
  primary: 'text-brand-primary',
  current: 'text-current',
  white: 'text-white',
};
