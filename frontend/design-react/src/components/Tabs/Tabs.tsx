import { forwardRef, type HTMLAttributes } from 'react';
import type { TabsProps, TabItem } from '@portal/design-core';
import { cn, tabsBase, tabsVariants, tabsItemBase, tabsItemVariants, tabsSizes } from '@portal/design-core';

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
      variant = 'underline',
      size = 'md',
      fullWidth = false,
      onChange,
      className,
      ...props
    },
    ref
  ) => {
    const getTabStyles = (isActive: boolean, isDisabled: boolean) => {
      const variantStyles = tabsItemVariants[variant];
      return cn(
        tabsItemBase,
        'focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-primary',
        tabsSizes[size],
        isActive ? variantStyles.active : variantStyles.inactive,
        isDisabled && 'opacity-50 cursor-not-allowed',
        fullWidth && 'flex-1'
      );
    };

    return (
      <div
        ref={ref}
        role="tablist"
        className={cn(
          tabsBase,
          tabsVariants[variant],
          fullWidth && 'w-full',
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
            className={getTabStyles(value === item.value, !!item.disabled)}
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
