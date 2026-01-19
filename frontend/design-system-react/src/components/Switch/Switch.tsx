import { forwardRef, type InputHTMLAttributes, useId } from 'react';
import type { SwitchProps } from '@portal/design-types';
import { cn } from '../../utils/cn';

export interface SwitchComponentProps
  extends Omit<SwitchProps, 'checked'>,
    Omit<InputHTMLAttributes<HTMLInputElement>, 'size' | 'type'> {
  checked?: boolean;
}

const sizeClasses: Record<
  NonNullable<SwitchProps['size']>,
  { track: string; thumb: string; label: string; translate: string }
> = {
  sm: {
    track: 'w-8 h-4',
    thumb: 'w-3 h-3',
    label: 'text-sm',
    translate: 'translate-x-4',
  },
  md: {
    track: 'w-10 h-5',
    thumb: 'w-4 h-4',
    label: 'text-base',
    translate: 'translate-x-5',
  },
  lg: {
    track: 'w-12 h-6',
    thumb: 'w-5 h-5',
    label: 'text-lg',
    translate: 'translate-x-6',
  },
};

const colorClasses: Record<NonNullable<SwitchProps['activeColor']>, string> = {
  primary: 'peer-checked:bg-brand-primary',
  success: 'peer-checked:bg-status-success',
  warning: 'peer-checked:bg-status-warning',
  error: 'peer-checked:bg-status-error',
};

export const Switch = forwardRef<HTMLInputElement, SwitchComponentProps>(
  (
    {
      checked,
      disabled,
      label,
      labelPosition = 'right',
      size = 'md',
      activeColor = 'primary',
      id: providedId,
      className,
      ...props
    },
    ref
  ) => {
    const generatedId = useId();
    const id = providedId || generatedId;
    const sizes = sizeClasses[size];

    const labelElement = label && (
      <span className={cn('text-text-body', sizes.label)}>{label}</span>
    );

    return (
      <label
        htmlFor={id}
        className={cn(
          'inline-flex items-center gap-2 cursor-pointer',
          disabled && 'opacity-50 cursor-not-allowed',
          className
        )}
      >
        {labelPosition === 'left' && labelElement}
        <div className="relative inline-flex items-center">
          <input
            ref={ref}
            type="checkbox"
            role="switch"
            id={id}
            checked={checked}
            disabled={disabled}
            className="peer sr-only"
            {...props}
          />
          <div
            className={cn(
              'rounded-full bg-bg-muted border border-border-default',
              'transition-colors duration-normal ease-linear-ease',
              'peer-focus:ring-2 peer-focus:ring-brand-primary peer-focus:ring-offset-2',
              colorClasses[activeColor],
              sizes.track
            )}
          />
          <div
            className={cn(
              'absolute left-0.5 top-1/2 -translate-y-1/2 rounded-full bg-white shadow-sm',
              'transition-transform duration-normal ease-linear-ease',
              sizes.thumb,
              checked && sizes.translate
            )}
          />
        </div>
        {labelPosition === 'right' && labelElement}
      </label>
    );
  }
);

Switch.displayName = 'Switch';

export default Switch;
