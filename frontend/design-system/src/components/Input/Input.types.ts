export interface InputProps {
  /**
   * Input type
   */
  type?: 'text' | 'email' | 'password' | 'number' | 'tel' | 'url';

  /**
   * 모델 값 (v-model용)
   */
  modelValue?: string | number;

  /**
   * Placeholder
   */
  placeholder?: string;

  /**
   * Disabled 상태
   */
  disabled?: boolean;

  /**
   * Error 상태
   */
  error?: boolean;

  /**
   * Error 메시지
   */
  errorMessage?: string;

  /**
   * Label
   */
  label?: string;

  /**
   * Required
   */
  required?: boolean;
}

export interface TextareaProps extends Omit<InputProps, 'type'> {
  /**
   * Rows
   */
  rows?: number;
}