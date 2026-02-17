export type { ToastItem, ToastPosition, ToastContainerProps, ToastProps } from '@portal/design-core';

export interface ToastEmits {
  (e: 'dismiss', id: string): void;
}
