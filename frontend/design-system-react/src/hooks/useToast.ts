import { useSyncExternalStore, useCallback } from 'react';
import type { ToastItem, StatusVariant } from '@portal/design-types';

let toastIdCounter = 0;

/**
 * Global toast state (module-level singleton)
 * All useToast() instances share the same toast list.
 */
let globalToasts: ToastItem[] = [];
const listeners = new Set<() => void>();

function getSnapshot(): ToastItem[] {
  return globalToasts;
}

function emitChange() {
  for (const listener of listeners) {
    listener();
  }
}

function subscribe(listener: () => void): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

function addToastGlobal(toast: Omit<ToastItem, 'id'>): string {
  const id = `toast-${++toastIdCounter}`;
  const newToast: ToastItem = {
    id,
    variant: 'info',
    duration: 5000,
    dismissible: true,
    ...toast,
  };
  globalToasts = [...globalToasts, newToast];
  emitChange();
  return id;
}

function removeToastGlobal(id: string) {
  globalToasts = globalToasts.filter((toast) => toast.id !== id);
  emitChange();
}

function clearToastsGlobal() {
  globalToasts = [];
  emitChange();
}

export interface UseToastReturn {
  toasts: ToastItem[];
  addToast: (toast: Omit<ToastItem, 'id'>) => string;
  removeToast: (id: string) => void;
  clearToasts: () => void;
  success: (message: string, options?: Partial<Omit<ToastItem, 'id' | 'message' | 'variant'>>) => string;
  error: (message: string, options?: Partial<Omit<ToastItem, 'id' | 'message' | 'variant'>>) => string;
  warning: (message: string, options?: Partial<Omit<ToastItem, 'id' | 'message' | 'variant'>>) => string;
  info: (message: string, options?: Partial<Omit<ToastItem, 'id' | 'message' | 'variant'>>) => string;
}

export function useToast(): UseToastReturn {
  const toasts = useSyncExternalStore(subscribe, getSnapshot, getSnapshot);

  const createToastMethod = useCallback(
    (variant: StatusVariant) =>
      (message: string, options?: Partial<Omit<ToastItem, 'id' | 'message' | 'variant'>>): string => {
        return addToastGlobal({ message, variant, ...options });
      },
    []
  );

  const success = useCallback(createToastMethod('success'), [createToastMethod]);
  const error = useCallback(createToastMethod('error'), [createToastMethod]);
  const warning = useCallback(createToastMethod('warning'), [createToastMethod]);
  const info = useCallback(createToastMethod('info'), [createToastMethod]);

  return {
    toasts,
    addToast: addToastGlobal,
    removeToast: removeToastGlobal,
    clearToasts: clearToastsGlobal,
    success,
    error,
    warning,
    info,
  };
}

export default useToast;
