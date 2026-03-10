import { ref } from 'vue';
import type { ToastItem } from '../components/Toast/Toast.types';

const toasts = ref<ToastItem[]>([]);

const generateId = (): string => {
  return `toast-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
};

const add = (toast: Omit<ToastItem, 'id'>): string => {
  const id = generateId();
  const newToast: ToastItem = {
    id,
    variant: 'info',
    duration: 5000,
    dismissible: true,
    ...toast,
  };

  toasts.value.push(newToast);
  return id;
};

const remove = (id: string): void => {
  const index = toasts.value.findIndex(t => t.id === id);
  if (index !== -1) {
    toasts.value.splice(index, 1);
  }
};

const clear = (): void => {
  toasts.value = [];
};

const success = (message: string, options?: Partial<Omit<ToastItem, 'id' | 'message' | 'variant'>>): string => {
  return add({ message, variant: 'success', ...options });
};

const error = (message: string, options?: Partial<Omit<ToastItem, 'id' | 'message' | 'variant'>>): string => {
  return add({ message, variant: 'error', ...options });
};

const warning = (message: string, options?: Partial<Omit<ToastItem, 'id' | 'message' | 'variant'>>): string => {
  return add({ message, variant: 'warning', ...options });
};

const info = (message: string, options?: Partial<Omit<ToastItem, 'id' | 'message' | 'variant'>>): string => {
  return add({ message, variant: 'info', ...options });
};

/**
 * Toast composable
 *
 * @example
 * ```vue
 * <script setup>
 * import { useToast } from '@portal/design-system';
 *
 * const { success, error } = useToast();
 *
 * const handleSave = async () => {
 *   try {
 *     await save();
 *     success('Changes saved successfully!');
 *   } catch (e) {
 *     error('Failed to save changes');
 *   }
 * };
 * </script>
 * ```
 */
export function useToast() {
  return {
    toasts,
    add,
    remove,
    clear,
    success,
    error,
    warning,
    info,
  };
}

export type UseToast = ReturnType<typeof useToast>;
