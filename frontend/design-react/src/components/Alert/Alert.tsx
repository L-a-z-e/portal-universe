import { forwardRef, useState, type HTMLAttributes, type ReactNode } from 'react';
import type { AlertProps } from '@portal/design-core';
import { cn, alertBase, alertVariants } from '@portal/design-core';

export interface AlertComponentProps
  extends AlertProps,
    Omit<HTMLAttributes<HTMLDivElement>, 'title'> {
  children?: ReactNode;
  onDismiss?: () => void;
}

const icons: Record<NonNullable<AlertProps['variant']>, ReactNode> = {
  info: (
    <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth={2}
        d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
      />
    </svg>
  ),
  success: (
    <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth={2}
        d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"
      />
    </svg>
  ),
  warning: (
    <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth={2}
        d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
      />
    </svg>
  ),
  error: (
    <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth={2}
        d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z"
      />
    </svg>
  ),
};

export const Alert = forwardRef<HTMLDivElement, AlertComponentProps>(
  (
    {
      variant = 'info',
      title,
      dismissible = false,
      showIcon = true,
      bordered = false,
      className,
      children,
      onDismiss,
      ...props
    },
    ref
  ) => {
    const [dismissed, setDismissed] = useState(false);

    if (dismissed) return null;

    const handleDismiss = () => {
      setDismissed(true);
      onDismiss?.();
    };

    return (
      <div
        ref={ref}
        role="alert"
        className={cn(
          alertBase,
          alertVariants[variant].container,
          bordered && 'border',
          className
        )}
        {...props}
      >
        {showIcon && <div className={cn('shrink-0', alertVariants[variant].icon)}>{icons[variant]}</div>}
        <div className="flex-1 min-w-0">
          {title && (
            <h5 className="font-medium text-text-heading mb-1">{title}</h5>
          )}
          <div className="text-sm">{children}</div>
        </div>
        {dismissible && (
          <button
            type="button"
            onClick={handleDismiss}
            className={cn(
              'shrink-0 p-1 rounded-md hover:bg-black/10',
              'transition-colors duration-fast',
              'focus:outline-none focus:ring-2 focus:ring-brand-primary'
            )}
            aria-label="Dismiss"
          >
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
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
    );
  }
);

Alert.displayName = 'Alert';

export default Alert;
