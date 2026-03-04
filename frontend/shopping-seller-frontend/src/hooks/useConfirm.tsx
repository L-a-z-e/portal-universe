import { useState, useCallback, useRef } from 'react';
import { ConfirmModal } from '@/components/common/ConfirmModal';

interface ConfirmOptions {
  title?: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  variant?: 'danger' | 'warning' | 'default';
}

/**
 * Promise-based confirm dialog hook.
 * Usage:
 *   const { confirm, ConfirmDialogPortal } = useConfirm()
 *   if (await confirm({ message: 'Delete?' })) { ... }
 *   // Render <ConfirmDialogPortal /> in JSX
 */
export function useConfirm() {
  const [options, setOptions] = useState<ConfirmOptions | null>(null);
  const resolveRef = useRef<((value: boolean) => void) | null>(null);

  const confirm = useCallback((opts: ConfirmOptions): Promise<boolean> => {
    return new Promise<boolean>((resolve) => {
      resolveRef.current = resolve;
      setOptions(opts);
    });
  }, []);

  const handleConfirm = useCallback(() => {
    resolveRef.current?.(true);
    resolveRef.current = null;
    setOptions(null);
  }, []);

  const handleCancel = useCallback(() => {
    resolveRef.current?.(false);
    resolveRef.current = null;
    setOptions(null);
  }, []);

  const ConfirmDialogPortal = useCallback(
    () =>
      options ? (
        <ConfirmModal
          isOpen
          title={options.title || '확인'}
          message={options.message}
          confirmText={options.confirmText}
          cancelText={options.cancelText}
          variant={options.variant}
          onConfirm={handleConfirm}
          onCancel={handleCancel}
        />
      ) : null,
    [options, handleConfirm, handleCancel],
  );

  return { confirm, ConfirmDialogPortal };
}
