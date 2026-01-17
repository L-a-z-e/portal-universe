/**
 * FormField component props
 */
export interface FormFieldProps {
  /**
   * Label text
   */
  label?: string;

  /**
   * Required indicator
   * @default false
   */
  required?: boolean;

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
   * Helper text (shown below the field)
   */
  helperText?: string;

  /**
   * Unique ID for accessibility
   */
  id?: string;

  /**
   * Disabled state (visual indicator)
   * @default false
   */
  disabled?: boolean;

  /**
   * Size variant
   * @default 'md'
   */
  size?: 'sm' | 'md' | 'lg';
}
