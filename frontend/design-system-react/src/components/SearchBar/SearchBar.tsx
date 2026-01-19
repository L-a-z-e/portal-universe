import { forwardRef, type InputHTMLAttributes, useId } from 'react';
import type { SearchBarProps } from '@portal/design-types';
import { cn } from '../../utils/cn';
import { Spinner } from '../Spinner';

export interface SearchBarComponentProps
  extends Omit<SearchBarProps, 'value'>,
    Omit<InputHTMLAttributes<HTMLInputElement>, 'type' | 'value'> {
  value: string;
  onValueChange?: (value: string) => void;
}

export const SearchBar = forwardRef<HTMLInputElement, SearchBarComponentProps>(
  (
    {
      value,
      placeholder = 'Search...',
      loading = false,
      disabled = false,
      autoFocus,
      onValueChange,
      onChange,
      className,
      ...props
    },
    ref
  ) => {
    const id = useId();

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
      onValueChange?.(e.target.value);
      onChange?.(e);
    };

    return (
      <div className={cn('relative w-full', className)}>
        <div className="absolute inset-y-0 left-0 flex items-center pl-3 pointer-events-none">
          <svg
            className="w-5 h-5 text-text-muted"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
            />
          </svg>
        </div>
        <input
          ref={ref}
          type="search"
          id={id}
          value={value}
          placeholder={placeholder}
          disabled={disabled}
          autoFocus={autoFocus}
          onChange={handleChange}
          className={cn(
            'w-full h-10 pl-10 pr-10 rounded-lg border bg-bg-card text-text-body',
            'placeholder:text-text-muted',
            'transition-all duration-normal ease-linear-ease',
            'focus:outline-none focus:ring-2 focus:ring-brand-primary focus:border-transparent',
            'border-border-default hover:border-border-hover',
            disabled && 'opacity-50 cursor-not-allowed bg-bg-muted'
          )}
          {...props}
        />
        {loading && (
          <div className="absolute inset-y-0 right-0 flex items-center pr-3">
            <Spinner size="sm" color="current" />
          </div>
        )}
      </div>
    );
  }
);

SearchBar.displayName = 'SearchBar';

export default SearchBar;
