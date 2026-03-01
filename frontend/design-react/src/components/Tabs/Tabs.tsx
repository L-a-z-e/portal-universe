import { forwardRef, useRef, useCallback, type HTMLAttributes, type KeyboardEvent } from 'react';
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
    const tablistRef = useRef<HTMLDivElement | null>(null);

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

    const handleKeyDown = useCallback(
      (e: KeyboardEvent, currentIndex: number) => {
        const enabledTabs = items.filter((t) => !t.disabled);
        const currentEnabledIndex = enabledTabs.findIndex(
          (t) => t.value === items[currentIndex].value
        );

        let newIndex = currentEnabledIndex;

        switch (e.key) {
          case 'ArrowLeft':
            e.preventDefault();
            newIndex = currentEnabledIndex > 0 ? currentEnabledIndex - 1 : enabledTabs.length - 1;
            break;
          case 'ArrowRight':
            e.preventDefault();
            newIndex = currentEnabledIndex < enabledTabs.length - 1 ? currentEnabledIndex + 1 : 0;
            break;
          case 'Home':
            e.preventDefault();
            newIndex = 0;
            break;
          case 'End':
            e.preventDefault();
            newIndex = enabledTabs.length - 1;
            break;
          default:
            return;
        }

        const newTab = enabledTabs[newIndex];
        if (newTab) {
          onChange?.(newTab.value);
          const tabElements = tablistRef.current?.querySelectorAll<HTMLElement>('[role="tab"]');
          const newTabIndex = items.findIndex((t) => t.value === newTab.value);
          tabElements?.[newTabIndex]?.focus();
        }
      },
      [items, onChange]
    );

    return (
      <div
        ref={(node) => {
          tablistRef.current = node;
          if (typeof ref === 'function') ref(node);
          else if (ref) ref.current = node;
        }}
        role="tablist"
        className={cn(
          tabsBase,
          tabsVariants[variant],
          fullWidth && 'w-full',
          className
        )}
        {...props}
      >
        {items.map((item: TabItem, index: number) => (
          <button
            key={item.value}
            type="button"
            role="tab"
            aria-selected={value === item.value}
            aria-disabled={item.disabled}
            tabIndex={value === item.value ? 0 : -1}
            disabled={item.disabled}
            onClick={() => !item.disabled && onChange?.(item.value)}
            onKeyDown={(e) => handleKeyDown(e, index)}
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
