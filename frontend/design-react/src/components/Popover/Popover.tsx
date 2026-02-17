import {
  forwardRef,
  useState,
  useRef,
  useEffect,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import type { PopoverProps } from '@portal/design-core';
import { cn } from '@portal/design-core';

export interface PopoverComponentProps
  extends Omit<PopoverProps, 'open'>,
    Omit<HTMLAttributes<HTMLDivElement>, 'content'> {
  open?: boolean;
  onOpenChange?: (open: boolean) => void;
  children: ReactNode;
  content: ReactNode;
}

const placementClasses: Record<NonNullable<PopoverProps['placement']>, string> = {
  'bottom': 'top-full left-1/2 -translate-x-1/2 mt-2',
  'bottom-start': 'top-full left-0 mt-2',
  'bottom-end': 'top-full right-0 mt-2',
  'top': 'bottom-full left-1/2 -translate-x-1/2 mb-2',
  'top-start': 'bottom-full left-0 mb-2',
  'top-end': 'bottom-full right-0 mb-2',
};

export const Popover = forwardRef<HTMLDivElement, PopoverComponentProps>(
  (
    {
      open: controlledOpen,
      onOpenChange,
      placement = 'bottom',
      trigger = 'click',
      closeOnClickOutside = true,
      children,
      content,
      className,
      ...props
    },
    ref
  ) => {
    const [internalOpen, setInternalOpen] = useState(false);
    const containerRef = useRef<HTMLDivElement>(null);

    const isControlled = controlledOpen !== undefined;
    const isOpen = isControlled ? controlledOpen : internalOpen;

    const setOpen = (value: boolean) => {
      if (!isControlled) {
        setInternalOpen(value);
      }
      onOpenChange?.(value);
    };

    useEffect(() => {
      if (!closeOnClickOutside || !isOpen) return;

      const handleClickOutside = (event: MouseEvent) => {
        if (
          containerRef.current &&
          !containerRef.current.contains(event.target as Node)
        ) {
          setOpen(false);
        }
      };

      document.addEventListener('mousedown', handleClickOutside);
      return () => {
        document.removeEventListener('mousedown', handleClickOutside);
      };
    }, [isOpen, closeOnClickOutside]);

    const handleTriggerClick = () => {
      if (trigger === 'click') {
        setOpen(!isOpen);
      }
    };

    const handleTriggerMouseEnter = () => {
      if (trigger === 'hover') {
        setOpen(true);
      }
    };

    const handleMouseLeave = () => {
      if (trigger === 'hover') {
        setOpen(false);
      }
    };

    return (
      <div
        ref={containerRef}
        className={cn('relative inline-block', className)}
        onMouseLeave={handleMouseLeave}
        {...props}
      >
        <div
          onClick={handleTriggerClick}
          onMouseEnter={handleTriggerMouseEnter}
        >
          {children}
        </div>

        {isOpen && (
          <div
            ref={ref}
            className={cn(
              'absolute z-50 p-4 rounded-lg border border-border-default',
              'bg-bg-card shadow-lg',
              'animate-scale-in',
              placementClasses[placement]
            )}
          >
            {content}
          </div>
        )}
      </div>
    );
  }
);

Popover.displayName = 'Popover';

export default Popover;
