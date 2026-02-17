import type { FormSize } from '../types/common';

export const inputBase = [
  'w-full rounded-md',
  'bg-transparent',
  'border border-border-default',
  'text-text-body placeholder:text-text-muted',
  'transition-colors duration-fast',
  'focus:outline-none focus:ring-2 focus:ring-brand-primary focus:border-transparent',
  'light:border-border-default light:focus:ring-brand-primary',
].join(' ');

export const inputSizes: Record<FormSize, string> = {
  sm: 'h-8 px-3 text-sm',
  md: 'h-9 px-3 text-sm',
  lg: 'h-11 px-4 text-base',
};
