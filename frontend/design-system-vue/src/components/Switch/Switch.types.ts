/**
 * Switch component props
 */
export interface SwitchProps {
  /**
   * Toggled state (v-model)
   */
  modelValue?: boolean;

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
   * Label position
   * @default 'right'
   */
  labelPosition?: 'left' | 'right';

  /**
   * Size variant
   * @default 'md'
   */
  size?: 'sm' | 'md' | 'lg';

  /**
   * Active color
   * @default 'primary'
   */
  activeColor?: 'primary' | 'success' | 'warning' | 'error';

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
 * Switch component emits
 */
export interface SwitchEmits {
  (e: 'update:modelValue', value: boolean): void;
  (e: 'change', value: boolean): void;
}
