import type { Size } from '../types/common';

export const modalBase = [
  'bg-bg-elevated',
  'border border-border-default',
  'rounded-xl shadow-xl',
].join(' ');

export const modalOverlay = [
  'fixed inset-0',
  'bg-bg-overlay',
  'backdrop-blur-sm',
].join(' ');

export const modalSizes: Record<Exclude<Size, 'xs'>, string> = {
  sm: 'max-w-md',
  md: 'max-w-lg',
  lg: 'max-w-2xl',
  xl: 'max-w-4xl',
};
