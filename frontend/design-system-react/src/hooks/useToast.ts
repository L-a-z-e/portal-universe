import { useState, useCallback } from 'react';
import type { ToastItem, StatusVariant } from '@portal/design-types';

let toastIdCounter = 0;

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
  const [toasts, setToasts] = useState<ToastItem[]>([]);

  const addToast = useCallback((toast: Omit<ToastItem, 'id'>): string => {
    const id = `toast-${++toastIdCounter}`;
    const newToast: ToastItem = {
      id,
      variant: 'info',
      duration: 5000,
      dismissible: true,
      ...toast,
    };
    setToasts((prev) => [...prev, newToast]);
    return id;
  }, []);

  const removeToast = useCallback((id: string) => {
    setToasts((prev) => prev.filter((toast) => toast.id !== id));
  }, []);

  const clearToasts = useCallback(() => {
    setToasts([]);
  }, []);

  const createToastMethod = useCallback(
    (variant: StatusVariant) =>
      (message: string, options?: Partial<Omit<ToastItem, 'id' | 'message' | 'variant'>>): string => {
        return addToast({ message, variant, ...options });
      },
    [addToast]
  );

  const success = useCallback(createToastMethod('success'), [createToastMethod]);
  const error = useCallback(createToastMethod('error'), [createToastMethod]);
  const warning = useCallback(createToastMethod('warning'), [createToastMethod]);
  const info = useCallback(createToastMethod('info'), [createToastMethod]);

  return {
    toasts,
    addToast,
    removeToast,
    clearToasts,
    success,
    error,
    warning,
    info,
  };
}

export default useToast;
