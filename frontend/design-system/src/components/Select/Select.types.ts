/**
 * Select option item
 */
export interface SelectOption {
  /**
   * Option label
   */
  label: string;

  /**
   * Option value
   */
  value: string | number;

  /**
   * Disabled state for this option
   */
  disabled?: boolean;
}

/**
 * Select component props
 */
export interface SelectProps {
  /**
   * Selected value (v-model)
   */
  modelValue?: string | number | null;

  /**
   * Options list
   */
  options: SelectOption[];

  /**
   * Placeholder text
   * @default 'Select an option'
   */
  placeholder?: string;

  /**
   * Disabled state
   * @default false
   */
  disabled?: boolean;

  /**
   * Error state
   * @default false
   */
  error?: boolean;

  /**
   * Error message
   */
  errorMessage?: string;

  /**
   * Label text
   */
  label?: string;

  /**
   * Required field
   * @default false
   */
  required?: boolean;

  /**
   * Clearable
   * @default false
   */
  clearable?: boolean;

  /**
   * Searchable/filterable
   * @default false
   */
  searchable?: boolean;

  /**
   * Size variant
   * @default 'md'
   */
  size?: 'sm' | 'md' | 'lg';

  /**
   * Name attribute
   */
  name?: string;

  /**
   * ID attribute
   */
  id?: string;
}

/**
 * Select component emits
 */
export interface SelectEmits {
  (e: 'update:modelValue', value: string | number | null): void;
  (e: 'change', value: string | number | null): void;
  (e: 'open'): void;
  (e: 'close'): void;
  (e: 'search', query: string): void;
}
