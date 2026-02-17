import type { StatusVariant } from '../types/common';

export const alertBase = 'flex items-start gap-3 p-4 rounded-lg';

export const alertVariants: Record<StatusVariant, { container: string; icon: string }> = {
  info: {
    container: 'bg-status-infoBg border-status-info/30 text-text-body',
    icon: 'text-status-info',
  },
  success: {
    container: 'bg-status-successBg border-status-success/30 text-text-body',
    icon: 'text-status-success',
  },
  warning: {
    container: 'bg-status-warningBg border-status-warning/30 text-text-body',
    icon: 'text-status-warning',
  },
  error: {
    container: 'bg-status-errorBg border-status-error/30 text-text-body',
    icon: 'text-status-error',
  },
};
