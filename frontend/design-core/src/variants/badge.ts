import type { BadgeVariant, Size } from '../types/common';

export const badgeBase = 'inline-flex items-center font-medium rounded-full';

export const badgeVariants: Record<BadgeVariant, string> = {
  default: 'bg-bg-muted text-text-body border border-border-default',
  primary: 'bg-brand-primary/10 text-brand-primary border border-brand-primary/20',
  success: 'bg-status-successBg text-status-success border border-status-success/20',
  warning: 'bg-status-warningBg text-status-warning border border-status-warning/20',
  danger: 'bg-status-errorBg text-status-error border border-status-error/20',
  info: 'bg-status-infoBg text-status-info border border-status-info/20',
  outline: 'bg-transparent text-text-body border border-border-default',
  brand: 'bg-brand-primary/10 text-brand-primary border border-brand-primary/20',
  error: 'bg-status-errorBg text-status-error border border-status-error/20',
  neutral: 'bg-bg-muted text-text-body border border-border-default',
};

export const badgeSizes: Record<Exclude<Size, 'xl'>, string> = {
  xs: 'px-1.5 py-0.5 text-[10px]',
  sm: 'px-2 py-0.5 text-xs',
  md: 'px-2.5 py-1 text-xs',
  lg: 'px-3 py-1.5 text-sm',
};
