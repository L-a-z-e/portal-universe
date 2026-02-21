import type { InputProps as BaseInputProps, TextareaProps as BaseTextareaProps } from '@portal/design-core';

export type InputProps = Omit<BaseInputProps, 'value'> & {
  modelValue?: string | number | null;
};

export type TextareaProps = Omit<BaseTextareaProps, 'value'> & {
  modelValue?: string | number;
};
