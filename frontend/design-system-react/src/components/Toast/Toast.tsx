import {
  forwardRef,
  useEffect,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { createPortal } from 'react-dom';
import type { ToastProps, ToastItem, ToastContainerProps } from '@portal/design-types';
import { cn } from '../../utils/cn';

// Toast Item Component
export interface ToastComponentProps
  extends ToastProps,
    Omit<HTMLAttributes<HTMLDivElement>, 'id'> {
  onDismiss?: (id: string) => void;
}

const variantClasses: Record<
  NonNullable<ToastItem['variant']>,
  { container: string; icon: string }
> = {
  info: {
    container: 'border-status-info/30',
    icon: 'text-status-info',
  },
  success: {
    container: 'border-status-success/30',
    icon: 'text-status-success',
  },
  warning: {
    container: 'border-status-warning/30',
    icon: 'text-status-warning',
  },
  error: {
    container: 'border-status-error/30',
    icon: 'text-status-error',
  },
};

const icons: Record<NonNullable<ToastItem['variant']>, ReactNode> = {
  info: (
    <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
    </svg>
  ),
  success: (
    <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
    </svg>
  ),
  warning: (
    <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
    </svg>
  ),
  error: (
    <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z" />
    </svg>
  ),
};

export const Toast = forwardRef<HTMLDivElement, ToastComponentProps>(
  (
    {
      id,
      variant = 'info',
      title,
      message,
      duration = 5000,
      dismissible = true,
      action,
      onDismiss,
      className,
      ...props
    },
    ref
  ) => {
    useEffect(() => {
      if (duration && duration > 0) {
        const timer = setTimeout(() => {
          onDismiss?.(id);
        }, duration);
        return () => clearTimeout(timer);
      }
    }, [id, duration, onDismiss]);

    const styles = variantClasses[variant];

    return (
      <div
        ref={ref}
        role="alert"
        className={cn(
          'flex gap-3 p-4 rounded-lg bg-bg-card border shadow-lg',
          'animate-slide-up',
          styles.container,
          className
        )}
        {...props}
      >
        <div className={cn('shrink-0', styles.icon)}>{icons[variant]}</div>
        <div className="flex-1 min-w-0">
          {title && <p className="font-medium text-text-heading">{title}</p>}
          <p className="text-sm text-text-body">{message}</p>
          {action && (
            <button
              type="button"
              onClick={action.onClick}
              className="mt-2 text-sm font-medium text-brand-primary hover:text-brand-primaryHover"
            >
              {action.label}
            </button>
          )}
        </div>
        {dismissible && (
          <button
            type="button"
            onClick={() => onDismiss?.(id)}
            className="shrink-0 p-1 rounded-md hover:bg-bg-hover transition-colors"
            aria-label="Dismiss"
          >
            <svg className="w-4 h-4 text-text-muted" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        )}
      </div>
    );
  }
);

Toast.displayName = 'Toast';

// Toast Container Component
export interface ToastContainerComponentProps extends ToastContainerProps {
  toasts: ToastItem[];
  onDismiss: (id: string) => void;
}

const positionClasses: Record<NonNullable<ToastContainerProps['position']>, string> = {
  'top-right': 'top-4 right-4',
  'top-left': 'top-4 left-4',
  'top-center': 'top-4 left-1/2 -translate-x-1/2',
  'bottom-right': 'bottom-4 right-4',
  'bottom-left': 'bottom-4 left-4',
  'bottom-center': 'bottom-4 left-1/2 -translate-x-1/2',
};

export const ToastContainer = forwardRef<HTMLDivElement, ToastContainerComponentProps>(
  ({ position = 'top-right', maxToasts = 5, toasts, onDismiss }, ref) => {
    const displayedToasts = toasts.slice(0, maxToasts);

    if (displayedToasts.length === 0) return null;

    return createPortal(
      <div
        ref={ref}
        className={cn(
          'fixed z-50 flex flex-col gap-2 w-full max-w-sm',
          positionClasses[position]
        )}
      >
        {displayedToasts.map((toast) => (
          <Toast key={toast.id} {...toast} onDismiss={onDismiss} />
        ))}
      </div>,
      document.body
    );
  }
);

ToastContainer.displayName = 'ToastContainer';

export default Toast;
