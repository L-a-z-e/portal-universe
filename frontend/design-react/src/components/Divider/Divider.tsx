import { forwardRef, type HTMLAttributes } from 'react';
import type { DividerProps } from '@portal/design-core';
import { cn, dividerVariants, dividerColors, dividerSpacing } from '@portal/design-core';

export interface DividerComponentProps
  extends DividerProps,
    Omit<HTMLAttributes<HTMLHRElement>, 'color'> {}

export const Divider = forwardRef<HTMLHRElement, DividerComponentProps>(
  (
    {
      orientation = 'horizontal',
      variant = 'solid',
      color = 'default',
      label,
      spacing = 'md',
      className,
      ...props
    },
    ref
  ) => {
    if (label) {
      return (
        <div
          className={cn(
            'flex items-center',
            orientation === 'vertical' ? 'flex-col h-full' : 'w-full',
            dividerSpacing[spacing],
            className
          )}
        >
          <hr
            ref={ref}
            className={cn(
              'flex-1',
              orientation === 'vertical' ? 'border-l h-full w-0' : 'border-t w-full h-0',
              dividerVariants[variant],
              dividerColors[color]
            )}
            {...props}
          />
          <span className="px-3 text-sm text-text-muted">{label}</span>
          <hr
            className={cn(
              'flex-1',
              orientation === 'vertical' ? 'border-l h-full w-0' : 'border-t w-full h-0',
              dividerVariants[variant],
              dividerColors[color]
            )}
          />
        </div>
      );
    }

    return (
      <hr
        ref={ref}
        className={cn(
          orientation === 'vertical'
            ? 'border-l h-full w-0 mx-4'
            : 'border-t w-full h-0',
          dividerVariants[variant],
          dividerColors[color],
          dividerSpacing[spacing],
          className
        )}
        {...props}
      />
    );
  }
);

Divider.displayName = 'Divider';

export default Divider;
