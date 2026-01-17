/**
 * Checkbox component props
 */
export interface CheckboxProps {
  /**
   * Checked state (v-model)
   */
  modelValue?: boolean;

  /**
   * Indeterminate state
   * @default false
   */
  indeterminate?: boolean;

  /**
   * Disabled state
   * @default false
   */
  disabled?: boolean;

  /**
   * Label text
   */
  label?: string;

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
   * Size variant
   * @default 'md'
   */
  size?: 'sm' | 'md' | 'lg';

  /**
   * Value for checkbox groups
   */
  value?: string | number;

  /**
   * Name attribute
   */
  name?: string;

  /**
   * ID attribute (auto-generated if not provided)
   */
  id?: string;
}

/**
 * Checkbox component emits
 */
export interface CheckboxEmits {
  (e: 'update:modelValue', value: boolean): void;
  (e: 'change', value: boolean): void;
}
