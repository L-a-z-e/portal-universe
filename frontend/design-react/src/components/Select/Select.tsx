import {
  forwardRef,
  useState,
  useRef,
  useEffect,
  useId,
  type SelectHTMLAttributes,
  type KeyboardEvent,
} from 'react';
import type { SelectProps, SelectOption } from '@portal/design-core';
import { cn, selectSizes } from '@portal/design-core';

export interface SelectComponentProps
  extends Omit<SelectProps, 'value' | 'onChange'>,
    Omit<SelectHTMLAttributes<HTMLButtonElement>, 'size' | 'value' | 'onChange'> {
  value?: string | number | null;
  onChange?: (value: string | number | null) => void;
}

export const Select = forwardRef<HTMLButtonElement, SelectComponentProps>(
  (
    {
      value,
      options,
      placeholder = 'Select an option',
      disabled,
      error,
      errorMessage,
      label,
      required,
      clearable,
      size = 'md',
      id: providedId,
      className,
      onChange,
      ...props
    },
    ref
  ) => {
    const [isOpen, setIsOpen] = useState(false);
    const [focusedIndex, setFocusedIndex] = useState(-1);
    const containerRef = useRef<HTMLDivElement>(null);
    const generatedId = useId();
    const id = providedId || generatedId;

    const selectedOption = options.find((opt: SelectOption) => opt.value === value);

    useEffect(() => {
      const handleClickOutside = (event: MouseEvent) => {
        if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
          setIsOpen(false);
        }
      };

      document.addEventListener('mousedown', handleClickOutside);
      return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    const handleKeyDown = (e: KeyboardEvent) => {
      if (disabled) return;

      switch (e.key) {
        case 'Enter':
        case ' ':
          e.preventDefault();
          if (isOpen && focusedIndex >= 0) {
            const option = options[focusedIndex];
            if (option && !option.disabled) {
              onChange?.(option.value);
              setIsOpen(false);
            }
          } else {
            setIsOpen(!isOpen);
          }
          break;
        case 'ArrowDown':
          e.preventDefault();
          if (!isOpen) {
            setIsOpen(true);
          } else {
            setFocusedIndex((prev) =>
              prev < options.length - 1 ? prev + 1 : prev
            );
          }
          break;
        case 'ArrowUp':
          e.preventDefault();
          if (isOpen) {
            setFocusedIndex((prev) => (prev > 0 ? prev - 1 : 0));
          }
          break;
        case 'Escape':
          setIsOpen(false);
          break;
      }
    };

    const handleSelect = (option: SelectOption) => {
      if (option.disabled) return;
      onChange?.(option.value);
      setIsOpen(false);
    };

    const handleClear = (e: React.MouseEvent) => {
      e.stopPropagation();
      onChange?.(null);
    };

    return (
      <div ref={containerRef} className={cn('relative w-full', className)}>
        {label && (
          <label
            htmlFor={id}
            className={cn(
              'block mb-1.5 text-sm font-medium text-text-body',
              disabled && 'opacity-50'
            )}
          >
            {label}
            {required && <span className="text-status-error ml-0.5">*</span>}
          </label>
        )}
        <button
          ref={ref}
          type="button"
          id={id}
          disabled={disabled}
          aria-haspopup="listbox"
          aria-expanded={isOpen}
          aria-invalid={error}
          aria-describedby={error && errorMessage ? `${id}-error` : undefined}
          onClick={() => !disabled && setIsOpen(!isOpen)}
          onKeyDown={handleKeyDown}
          className={cn(
            'w-full flex items-center justify-between rounded-md border bg-bg-card text-left',
            'transition-all duration-normal ease-linear-ease',
            'focus:outline-none focus:ring-2 focus:ring-brand-primary focus:border-transparent',
            selectSizes[size],
            error
              ? 'border-status-error focus:ring-status-error'
              : 'border-border-default hover:border-border-hover',
            disabled && 'opacity-50 cursor-not-allowed bg-bg-muted'
          )}
          {...props}
        >
          <span className={cn(!selectedOption && 'text-text-muted')}>
            {selectedOption?.label || placeholder}
          </span>
          <div className="flex items-center gap-1">
            {clearable && value != null && (
              <span
                role="button"
                tabIndex={-1}
                onClick={handleClear}
                className="p-0.5 hover:bg-bg-hover rounded text-text-muted hover:text-text-body"
              >
                <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </span>
            )}
            <svg
              className={cn(
                'w-4 h-4 text-text-muted transition-transform duration-normal',
                isOpen && 'rotate-180'
              )}
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
            </svg>
          </div>
        </button>

        {isOpen && (
          <ul
            role="listbox"
            className={cn(
              'absolute z-50 w-full mt-1 py-1 rounded-md border border-border-default',
              'bg-bg-card shadow-lg max-h-60 overflow-auto',
              'animate-fade-in'
            )}
          >
            {options.map((option: SelectOption, index: number) => (
              <li
                key={option.value}
                role="option"
                aria-selected={value === option.value}
                aria-disabled={option.disabled}
                onClick={() => handleSelect(option)}
                onMouseEnter={() => setFocusedIndex(index)}
                className={cn(
                  'px-3 py-2 cursor-pointer',
                  'transition-colors duration-fast',
                  value === option.value && 'bg-brand-primary/10 text-brand-primary',
                  focusedIndex === index && value !== option.value && 'bg-bg-hover',
                  option.disabled && 'opacity-50 cursor-not-allowed'
                )}
              >
                {option.label}
              </li>
            ))}
          </ul>
        )}

        {error && errorMessage && (
          <p id={`${id}-error`} className="mt-1.5 text-sm text-status-error">
            {errorMessage}
          </p>
        )}
      </div>
    );
  }
);

Select.displayName = 'Select';

export default Select;
