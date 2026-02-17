import type { TagVariant, FormSize } from '../types/common';

export const tagBase = 'inline-flex items-center gap-1 font-medium transition-all duration-200';

export const tagVariants: Record<TagVariant, string> = {
  default: 'bg-bg-muted text-text-body hover:bg-bg-hover border border-border-default',
  primary: 'bg-brand-primary/10 text-brand-primary hover:bg-brand-primary/20 border border-brand-primary/20',
  success: 'bg-status-successBg text-status-success hover:bg-status-successBg/80 border border-status-success/20',
  error: 'bg-status-errorBg text-status-error hover:bg-status-errorBg/80 border border-status-error/20',
  warning: 'bg-status-warningBg text-status-warning hover:bg-status-warningBg/80 border border-status-warning/20',
  info: 'bg-status-infoBg text-status-info hover:bg-status-infoBg/80 border border-status-info/20',
};

export const tagSizes: Record<FormSize, string> = {
  sm: 'text-xs px-2 py-1 rounded-md',
  md: 'text-sm px-3 py-1.5 rounded-lg',
  lg: 'text-base px-4 py-2 rounded-lg',
};
