import {
  forwardRef,
  useState,
  useRef,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import type { TooltipProps } from '@portal/design-types';
import { cn } from '../../utils/cn';

export interface TooltipComponentProps
  extends TooltipProps,
    Omit<HTMLAttributes<HTMLDivElement>, 'content'> {
  children: ReactNode;
}

const placementClasses: Record<NonNullable<TooltipProps['placement']>, string> = {
  'bottom': 'top-full left-1/2 -translate-x-1/2 mt-2',
  'bottom-start': 'top-full left-0 mt-2',
  'bottom-end': 'top-full right-0 mt-2',
  'top': 'bottom-full left-1/2 -translate-x-1/2 mb-2',
  'top-start': 'bottom-full left-0 mb-2',
  'top-end': 'bottom-full right-0 mb-2',
};

export const Tooltip = forwardRef<HTMLDivElement, TooltipComponentProps>(
  (
    {
      content,
      placement = 'top',
      delay = 200,
      disabled = false,
      children,
      className,
      ...props
    },
    ref
  ) => {
    const [isVisible, setIsVisible] = useState(false);
    const timeoutRef = useRef<NodeJS.Timeout | null>(null);

    const showTooltip = () => {
      if (disabled) return;
      timeoutRef.current = setTimeout(() => {
        setIsVisible(true);
      }, delay);
    };

    const hideTooltip = () => {
      if (timeoutRef.current) {
        clearTimeout(timeoutRef.current);
      }
      setIsVisible(false);
    };

    return (
      <div
        ref={ref}
        className={cn('relative inline-block', className)}
        onMouseEnter={showTooltip}
        onMouseLeave={hideTooltip}
        onFocus={showTooltip}
        onBlur={hideTooltip}
        {...props}
      >
        {children}
        {isVisible && (
          <div
            role="tooltip"
            className={cn(
              'absolute z-50 px-2 py-1 rounded text-xs font-medium',
              'bg-bg-elevated text-text-body border border-border-default shadow-md',
              'animate-fade-in whitespace-nowrap',
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

Tooltip.displayName = 'Tooltip';

export default Tooltip;
