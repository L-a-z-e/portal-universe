import { forwardRef, type HTMLAttributes } from 'react';
import type { DividerProps } from '@portal/design-types';
import { cn } from '../../utils/cn';

export interface DividerComponentProps
  extends DividerProps,
    Omit<HTMLAttributes<HTMLHRElement>, 'color'> {}

const variantClasses: Record<NonNullable<DividerProps['variant']>, string> = {
  solid: 'border-solid',
  dashed: 'border-dashed',
  dotted: 'border-dotted',
};

const colorClasses: Record<NonNullable<DividerProps['color']>, string> = {
  default: 'border-border-default',
  muted: 'border-border-muted',
  strong: 'border-border-hover',
};

const spacingClasses: Record<NonNullable<DividerProps['spacing']>, string> = {
  none: 'my-0',
  sm: 'my-2',
  md: 'my-4',
  lg: 'my-6',
};

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
            spacingClasses[spacing],
            className
          )}
        >
          <hr
            ref={ref}
            className={cn(
              'flex-1',
              orientation === 'vertical' ? 'border-l h-full w-0' : 'border-t w-full h-0',
              variantClasses[variant],
              colorClasses[color]
            )}
            {...props}
          />
          <span className="px-3 text-sm text-text-muted">{label}</span>
          <hr
            className={cn(
              'flex-1',
              orientation === 'vertical' ? 'border-l h-full w-0' : 'border-t w-full h-0',
              variantClasses[variant],
              colorClasses[color]
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
          variantClasses[variant],
          colorClasses[color],
          spacingClasses[spacing],
          className
        )}
        {...props}
      />
    );
  }
);

Divider.displayName = 'Divider';

export default Divider;
