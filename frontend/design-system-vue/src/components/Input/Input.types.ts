import type { InputProps as BaseInputProps, TextareaProps as BaseTextareaProps } from '@portal/design-types';

export type InputProps = Omit<BaseInputProps, 'value'> & {
  modelValue?: string | number;
};

export type TextareaProps = Omit<BaseTextareaProps, 'value'> & {
  modelValue?: string | number;
};
