import {
  forwardRef,
  useEffect,
  useCallback,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { createPortal } from 'react-dom';
import type { ModalProps } from '@portal/design-core';
import { cn, modalSizes } from '@portal/design-core';

export interface ModalComponentProps
  extends Omit<ModalProps, 'open'>,
    Omit<HTMLAttributes<HTMLDivElement>, 'title'> {
  open: boolean;
  onClose: () => void;
  children?: ReactNode;
}

export const Modal = forwardRef<HTMLDivElement, ModalComponentProps>(
  (
    {
      open,
      onClose,
      title,
      size = 'md',
      showClose = true,
      closeOnBackdrop = true,
      closeOnEscape = true,
      className,
      children,
      ...props
    },
    ref
  ) => {
    const handleEscape = useCallback(
      (e: KeyboardEvent) => {
        if (closeOnEscape && e.key === 'Escape') {
          onClose();
        }
      },
      [closeOnEscape, onClose]
    );

    useEffect(() => {
      if (open) {
        document.addEventListener('keydown', handleEscape);
        document.body.style.overflow = 'hidden';
      }

      return () => {
        document.removeEventListener('keydown', handleEscape);
        document.body.style.overflow = '';
      };
    }, [open, handleEscape]);

    if (!open) return null;

    const modalContent = (
      <div
        className="fixed inset-0 z-50 flex items-center justify-center p-4"
        role="dialog"
        aria-modal="true"
        aria-labelledby={title ? 'modal-title' : undefined}
      >
        {/* Backdrop */}
        <div
          className="absolute inset-0 bg-black/60 backdrop-blur-sm animate-fade-in"
          onClick={closeOnBackdrop ? onClose : undefined}
          aria-hidden="true"
        />

        {/* Modal - Linear dark mode first */}
        <div
          ref={ref}
          className={cn(
            'relative w-full rounded-xl',
            // Dark mode (default)
            'bg-[#18191b]',
            'border border-[#2a2a2a]',
            'shadow-[0_16px_48px_rgba(0,0,0,0.6)]',
            // Light mode
            'light:bg-white light:border-gray-200 light:shadow-2xl',
            'animate-scale-in',
            modalSizes[size],
            className
          )}
          {...props}
        >
          {/* Header */}
          {(title || showClose) && (
            <div className="flex items-center justify-between px-5 py-4 border-b border-[#2a2a2a] light:border-gray-200">
              {title && (
                <h2 id="modal-title" className="text-lg font-semibold text-white light:text-gray-900">
                  {title}
                </h2>
              )}
              {showClose && (
                <button
                  type="button"
                  onClick={onClose}
                  className={cn(
                    'p-1.5 rounded-md',
                    'text-[#6b6b6b] hover:text-[#b4b4b4] hover:bg-white/5',
                    'light:text-gray-400 light:hover:text-gray-600 light:hover:bg-gray-100',
                    'transition-colors duration-100',
                    'focus:outline-none focus:ring-2 focus:ring-[#5e6ad2]',
                    !title && 'ml-auto'
                  )}
                  aria-label="Close"
                >
                  <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M6 18L18 6M6 6l12 12"
                    />
                  </svg>
                </button>
              )}
            </div>
          )}

          {/* Content */}
          <div className="px-5 py-5 text-[#b4b4b4] light:text-gray-600">{children}</div>
        </div>
      </div>
    );

    return createPortal(modalContent, document.body);
  }
);

Modal.displayName = 'Modal';

export default Modal;
