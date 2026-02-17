import type { ButtonVariant, Size } from '../types/common';

export const buttonBase = [
  'inline-flex items-center justify-center',
  'font-medium rounded-md',
  'transition-all duration-150 ease-out',
  'focus:outline-none focus-visible:ring-2 focus-visible:ring-[#5e6ad2] focus-visible:ring-offset-2 focus-visible:ring-offset-[#08090a]',
  'light:focus-visible:ring-offset-white',
].join(' ');

export const buttonVariants: Record<ButtonVariant, string> = {
  primary: [
    'bg-white/90 text-[#08090a]',
    'hover:bg-white',
    'active:bg-white/80 active:scale-[0.98]',
    'light:bg-brand-primary light:text-white',
    'light:hover:bg-brand-primaryHover',
    'light:active:bg-brand-primary',
    'border border-transparent',
    'shadow-sm',
  ].join(' '),

  secondary: [
    'bg-transparent text-text-body',
    'hover:bg-white/5 hover:text-text-heading',
    'active:bg-white/10 active:scale-[0.98]',
    'border border-[#2a2a2a]',
    'light:hover:bg-gray-100',
    'light:border-gray-200',
  ].join(' '),

  ghost: [
    'bg-transparent text-text-body',
    'hover:bg-white/5 hover:text-text-heading',
    'active:bg-white/10 active:scale-[0.98]',
    'border border-transparent',
    'light:hover:bg-gray-100',
  ].join(' '),

  outline: [
    'bg-transparent text-text-body',
    'hover:bg-white/5 hover:border-[#3a3a3a]',
    'active:bg-white/10 active:scale-[0.98]',
    'border border-[#2a2a2a]',
    'light:border-gray-300 light:hover:border-gray-400',
    'light:hover:bg-gray-50',
  ].join(' '),

  danger: [
    'bg-[#E03131] text-white',
    'hover:bg-[#C92A2A]',
    'active:bg-[#A51D1D] active:scale-[0.98]',
    'border border-transparent',
    'shadow-sm',
  ].join(' '),

  error: [
    'bg-[#E03131] text-white',
    'hover:bg-[#C92A2A]',
    'active:bg-[#A51D1D] active:scale-[0.98]',
    'border border-transparent',
    'shadow-sm',
  ].join(' '),
};

export const buttonSizes: Record<Exclude<Size, 'xl'>, string> = {
  xs: 'h-6 px-2 text-xs gap-1',
  sm: 'h-8 px-3 text-sm gap-1.5',
  md: 'h-9 px-4 text-sm gap-2',
  lg: 'h-11 px-5 text-base gap-2',
};
