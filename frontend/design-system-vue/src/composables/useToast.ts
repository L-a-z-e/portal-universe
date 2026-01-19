import { ref } from 'vue';
import type { ToastItem } from '../components/Toast/Toast.types';

/**
 * Global toast state
 */
const toasts = ref<ToastItem[]>([]);

/**
 * Generate unique ID
 */
const generateId = (): string => {
  return `toast-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
};

/**
 * Add a new toast
 */
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

/**
 * Remove a toast by ID
 */
const remove = (id: string): void => {
  const index = toasts.value.findIndex(t => t.id === id);
  if (index !== -1) {
    toasts.value.splice(index, 1);
  }
};

/**
 * Clear all toasts
 */
const clear = (): void => {
  toasts.value = [];
};

/**
 * Convenience method for success toast
 */
const success = (message: string, options?: Partial<Omit<ToastItem, 'id' | 'message' | 'variant'>>): string => {
  return add({ message, variant: 'success', ...options });
};

/**
 * Convenience method for error toast
 */
const error = (message: string, options?: Partial<Omit<ToastItem, 'id' | 'message' | 'variant'>>): string => {
  return add({ message, variant: 'error', ...options });
};

/**
 * Convenience method for warning toast
 */
const warning = (message: string, options?: Partial<Omit<ToastItem, 'id' | 'message' | 'variant'>>): string => {
  return add({ message, variant: 'warning', ...options });
};

/**
 * Convenience method for info toast
 */
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
