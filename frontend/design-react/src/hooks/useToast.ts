import { useState, useEffect, useCallback } from 'react';
import type { ToastItem, StatusVariant } from '@portal/design-core';

let toastIdCounter = 0;

/**
 * Global toast state (module-level singleton)
 * All useToast() instances share the same toast list.
 *
 * useState+useEffect 기반으로 구현하여 Module Federation 환경에서
 * cross-React 인스턴스 문제(#321 에러)를 방지합니다.
 */
let globalToasts: ToastItem[] = [];
const listeners = new Set<() => void>();

function emitChange() {
  for (const listener of listeners) {
    listener();
  }
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
  const [toasts, setToasts] = useState<ToastItem[]>(globalToasts);

  useEffect(() => {
    const listener = () => setToasts([...globalToasts]);
    listeners.add(listener);
    return () => { listeners.delete(listener); };
  }, []);

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
