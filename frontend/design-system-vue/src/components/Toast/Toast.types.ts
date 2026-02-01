export type { ToastItem, ToastPosition, ToastContainerProps, ToastProps } from '@portal/design-types';

export interface ToastEmits {
  (e: 'dismiss', id: string): void;
}
