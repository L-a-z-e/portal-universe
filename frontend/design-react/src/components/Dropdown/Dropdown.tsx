import {
  forwardRef,
  useState,
  useRef,
  useEffect,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import type { DropdownProps, DropdownItem } from '@portal/design-core';
import { cn } from '@portal/design-core';

export interface DropdownComponentProps
  extends DropdownProps,
    Omit<HTMLAttributes<HTMLDivElement>, 'onSelect'> {
  children?: ReactNode;
  onSelect?: (item: DropdownItem) => void;
  onOpen?: () => void;
  onClose?: () => void;
}

const placementClasses: Record<NonNullable<DropdownProps['placement']>, string> = {
  'bottom': 'top-full left-1/2 -translate-x-1/2 mt-1',
  'bottom-start': 'top-full left-0 mt-1',
  'bottom-end': 'top-full right-0 mt-1',
  'top': 'bottom-full left-1/2 -translate-x-1/2 mb-1',
  'top-start': 'bottom-full left-0 mb-1',
  'top-end': 'bottom-full right-0 mb-1',
};

export const Dropdown = forwardRef<HTMLDivElement, DropdownComponentProps>(
  (
    {
      items,
      trigger = 'click',
      placement = 'bottom-start',
      disabled = false,
      closeOnSelect = true,
      width = 'auto',
      children,
      onSelect,
      onOpen,
      onClose,
      className,
      ...props
    },
    ref
  ) => {
    const [isOpen, setIsOpen] = useState(false);
    const containerRef = useRef<HTMLDivElement>(null);
    const triggerRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
      const handleClickOutside = (event: MouseEvent) => {
        if (
          containerRef.current &&
          !containerRef.current.contains(event.target as Node)
        ) {
          setIsOpen(false);
          onClose?.();
        }
      };

      if (isOpen) {
        document.addEventListener('mousedown', handleClickOutside);
      }

      return () => {
        document.removeEventListener('mousedown', handleClickOutside);
      };
    }, [isOpen, onClose]);

    const handleOpen = () => {
      if (!disabled) {
        setIsOpen(true);
        onOpen?.();
      }
    };

    const handleClose = () => {
      setIsOpen(false);
      onClose?.();
    };

    const handleSelect = (item: DropdownItem) => {
      if (item.disabled || item.divider) return;
      onSelect?.(item);
      if (closeOnSelect) {
        handleClose();
      }
    };

    const handleTriggerClick = () => {
      if (trigger === 'click') {
        if (isOpen) {
          handleClose();
        } else {
          handleOpen();
        }
      }
    };

    const handleTriggerMouseEnter = () => {
      if (trigger === 'hover') {
        handleOpen();
      }
    };

    const handleMouseLeave = () => {
      if (trigger === 'hover') {
        handleClose();
      }
    };

    const widthStyle =
      width === 'trigger' && triggerRef.current
        ? { width: triggerRef.current.offsetWidth }
        : width !== 'auto'
        ? { width }
        : undefined;

    return (
      <div
        ref={containerRef}
        className={cn('relative inline-block', className)}
        onMouseLeave={handleMouseLeave}
        {...props}
      >
        <div
          ref={triggerRef}
          onClick={handleTriggerClick}
          onMouseEnter={handleTriggerMouseEnter}
        >
          {children}
        </div>

        {isOpen && (
          <div
            ref={ref}
            className={cn(
              'absolute z-50 py-1 rounded-md border border-border-default',
              'bg-bg-card shadow-lg',
              'animate-fade-in',
              placementClasses[placement]
            )}
            style={widthStyle}
          >
            {items.map((item: DropdownItem, index: number) => {
              if (item.divider) {
                return (
                  <hr
                    key={`divider-${index}`}
                    className="my-1 border-t border-border-default"
                  />
                );
              }

              return (
                <button
                  key={item.value ?? item.label}
                  type="button"
                  disabled={item.disabled}
                  onClick={() => handleSelect(item)}
                  className={cn(
                    'w-full flex items-center gap-2 px-3 py-2 text-left text-sm',
                    'transition-colors duration-fast',
                    'hover:bg-bg-hover',
                    item.disabled && 'opacity-50 cursor-not-allowed'
                  )}
                >
                  {item.icon && <span className="w-4 h-4">{item.icon}</span>}
                  {item.label}
                </button>
              );
            })}
          </div>
        )}
      </div>
    );
  }
);

Dropdown.displayName = 'Dropdown';

export default Dropdown;
