import { forwardRef, type HTMLAttributes } from 'react';
import type { TabsProps, TabItem } from '@portal/design-core';
import { cn, tabsSizes } from '@portal/design-core';

export interface TabsComponentProps
  extends Omit<TabsProps, 'value'>,
    Omit<HTMLAttributes<HTMLDivElement>, 'onChange'> {
  value: string;
  onChange?: (value: string) => void;
}

export const Tabs = forwardRef<HTMLDivElement, TabsComponentProps>(
  (
    {
      value,
      items,
      variant = 'default',
      size = 'md',
      fullWidth = false,
      onChange,
      className,
      ...props
    },
    ref
  ) => {
    const getTabStyles = (isActive: boolean, isDisabled: boolean) => {
      const base = cn(
        'px-4 py-2 font-medium transition-all duration-normal',
        'focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-primary',
        isDisabled && 'opacity-50 cursor-not-allowed',
        !isDisabled && 'cursor-pointer'
      );

      switch (variant) {
        case 'pills':
          return cn(
            base,
            'rounded-lg',
            isActive
              ? 'bg-brand-primary text-text-inverse'
              : 'text-text-body hover:bg-bg-hover'
          );
        case 'underline':
          return cn(
            base,
            'border-b-2 -mb-px',
            isActive
              ? 'border-brand-primary text-brand-primary'
              : 'border-transparent text-text-muted hover:text-text-body hover:border-border-default'
          );
        default:
          return cn(
            base,
            'rounded-t-lg',
            isActive
              ? 'bg-bg-card text-text-heading border border-border-default border-b-bg-card'
              : 'text-text-muted hover:text-text-body hover:bg-bg-hover'
          );
      }
    };

    return (
      <div
        ref={ref}
        role="tablist"
        className={cn(
          'flex',
          variant === 'underline' && 'border-b border-border-default',
          fullWidth && 'w-full',
          tabsSizes[size],
          className
        )}
        {...props}
      >
        {items.map((item: TabItem) => (
          <button
            key={item.value}
            type="button"
            role="tab"
            aria-selected={value === item.value}
            aria-disabled={item.disabled}
            disabled={item.disabled}
            onClick={() => !item.disabled && onChange?.(item.value)}
            className={cn(
              getTabStyles(value === item.value, !!item.disabled),
              fullWidth && 'flex-1'
            )}
          >
            {item.icon && <span className="mr-2">{item.icon}</span>}
            {item.label}
          </button>
        ))}
      </div>
    );
  }
);

Tabs.displayName = 'Tabs';

export default Tabs;
