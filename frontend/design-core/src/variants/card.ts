import type { CardVariant, PaddingSize } from '../types/common';

export const cardBase = 'rounded-xl transition-all duration-150 ease-out';

export const cardVariants: Record<CardVariant, string> = {
  elevated: [
    'bg-[#0f1011]',
    'border border-[#2a2a2a]',
    'shadow-[0_1px_2px_rgba(0,0,0,0.3)]',
    'light:bg-white light:border-gray-200 light:shadow-sm',
  ].join(' '),

  outlined: [
    'bg-transparent',
    'border border-[#2a2a2a]',
    'light:border-gray-200',
  ].join(' '),

  flat: [
    'bg-[#18191b]',
    'border border-transparent',
    'light:bg-gray-50',
  ].join(' '),

  glass: [
    'bg-[#0f1011]/80',
    'backdrop-blur-md',
    'border border-white/10',
    'light:bg-white/80 light:border-gray-200/50',
  ].join(' '),

  interactive: [
    'bg-[#0f1011]',
    'border border-[#2a2a2a]',
    'hover:border-[#3a3a3a] hover:bg-[#18191b]',
    'hover:-translate-y-0.5 hover:shadow-[0_8px_24px_rgba(0,0,0,0.4)]',
    'cursor-pointer',
    'light:bg-white light:border-gray-200',
    'light:hover:border-gray-300 light:hover:bg-gray-50',
    'light:hover:shadow-lg',
  ].join(' '),
};

export const cardPadding: Record<PaddingSize, string> = {
  none: 'p-0',
  sm: 'p-3',
  md: 'p-4',
  lg: 'p-6',
  xl: 'p-8',
};
