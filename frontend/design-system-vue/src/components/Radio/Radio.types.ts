/**
 * Radio option item
 */
export interface RadioOption {
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
 * Radio component props
 */
export interface RadioProps {
  /**
   * Selected value (v-model)
   */
  modelValue?: string | number;

  /**
   * Radio options
   */
  options: RadioOption[];

  /**
   * Group name (required for radio group)
   */
  name: string;

  /**
   * Layout direction
   * @default 'vertical'
   */
  direction?: 'horizontal' | 'vertical';

  /**
   * Disabled state for all options
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
   * Size variant
   * @default 'md'
   */
  size?: 'sm' | 'md' | 'lg';
}

/**
 * Radio component emits
 */
export interface RadioEmits {
  (e: 'update:modelValue', value: string | number): void;
  (e: 'change', value: string | number): void;
}
