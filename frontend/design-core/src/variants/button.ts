import type { ButtonVariant, Size } from '../types/common';

export const buttonBase = [
  'inline-flex items-center justify-center',
  'font-medium rounded-md',
  'transition-all duration-150 ease-out',
  'focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-primary focus-visible:ring-offset-2 focus-visible:ring-offset-bg-page',
  'light:focus-visible:ring-offset-white',
].join(' ');

export const buttonVariants: Record<ButtonVariant, string> = {
  primary: [
    'bg-white/90 text-text-inverse',
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
    'border border-border-default',
    'light:hover:bg-bg-hover',
    'light:border-border-default',
  ].join(' '),

  ghost: [
    'bg-transparent text-text-body',
    'hover:bg-white/5 hover:text-text-heading',
    'active:bg-white/10 active:scale-[0.98]',
    'border border-transparent',
    'light:hover:bg-bg-hover',
  ].join(' '),

  outline: [
    'bg-transparent text-text-body',
    'hover:bg-white/5 hover:border-border-hover',
    'active:bg-white/10 active:scale-[0.98]',
    'border border-border-default',
    'light:border-border-default light:hover:border-border-hover',
    'light:hover:bg-bg-hover',
  ].join(' '),

  danger: [
    'bg-status-error text-white',
    'hover:bg-red-700',
    'active:bg-red-800 active:scale-[0.98]',
    'border border-transparent',
    'shadow-sm',
  ].join(' '),

  error: [
    'bg-status-error text-white',
    'hover:bg-red-700',
    'active:bg-red-800 active:scale-[0.98]',
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
