import { forwardRef, type InputHTMLAttributes, useId } from 'react';
import type { RadioProps, RadioOption } from '@portal/design-types';
import { cn } from '../../utils/cn';

export interface RadioComponentProps
  extends Omit<RadioProps, 'value' | 'onChange'>,
    Omit<InputHTMLAttributes<HTMLInputElement>, 'size' | 'type' | 'name' | 'onChange'> {
  value?: string | number;
  onChange?: (value: string | number) => void;
}

const sizeClasses: Record<NonNullable<RadioProps['size']>, { radio: string; label: string }> = {
  sm: { radio: 'w-4 h-4', label: 'text-sm' },
  md: { radio: 'w-5 h-5', label: 'text-base' },
  lg: { radio: 'w-6 h-6', label: 'text-lg' },
};

export const Radio = forwardRef<HTMLInputElement, RadioComponentProps>(
  (
    {
      value,
      options,
      name,
      direction = 'vertical',
      disabled,
      error,
      errorMessage,
      size = 'md',
      onChange,
      className,
      ...props
    },
    ref
  ) => {
    const groupId = useId();
    const sizes = sizeClasses[size];

    const handleChange = (optionValue: string | number) => {
      if (onChange) {
        onChange(optionValue);
      }
    };

    return (
      <div
        role="radiogroup"
        aria-invalid={error}
        aria-describedby={error && errorMessage ? `${groupId}-error` : undefined}
        className={cn(
          'flex',
          direction === 'vertical' ? 'flex-col gap-2' : 'flex-row gap-4 flex-wrap',
          className
        )}
      >
        {options.map((option: RadioOption) => {
          const optionId = `${groupId}-${option.value}`;
          const isDisabled = disabled || option.disabled;
          const isChecked = value === option.value;

          return (
            <label
              key={option.value}
              htmlFor={optionId}
              className={cn(
                'inline-flex items-center gap-2 cursor-pointer',
                isDisabled && 'opacity-50 cursor-not-allowed'
              )}
            >
              <div className="relative flex items-center justify-center">
                <input
                  ref={isChecked ? ref : undefined}
                  type="radio"
                  id={optionId}
                  name={name}
                  value={option.value}
                  checked={isChecked}
                  disabled={isDisabled}
                  onChange={() => handleChange(option.value)}
                  className={cn(
                    'appearance-none rounded-full border bg-bg-card',
                    'transition-all duration-normal ease-linear-ease',
                    'focus:outline-none focus:ring-2 focus:ring-brand-primary focus:ring-offset-2',
                    'checked:border-brand-primary',
                    sizes.radio,
                    error
                      ? 'border-status-error'
                      : 'border-border-default hover:border-border-hover',
                    isDisabled && 'cursor-not-allowed'
                  )}
                  {...props}
                />
                <div
                  className={cn(
                    'absolute rounded-full bg-brand-primary',
                    'transition-transform duration-normal ease-linear-ease',
                    'scale-0',
                    isChecked && 'scale-100',
                    size === 'sm' && 'w-2 h-2',
                    size === 'md' && 'w-2.5 h-2.5',
                    size === 'lg' && 'w-3 h-3'
                  )}
                />
              </div>
              <span className={cn('text-text-body', sizes.label)}>{option.label}</span>
            </label>
          );
        })}
        {error && errorMessage && (
          <p id={`${groupId}-error`} className="text-sm text-status-error">
            {errorMessage}
          </p>
        )}
      </div>
    );
  }
);

Radio.displayName = 'Radio';

export default Radio;
