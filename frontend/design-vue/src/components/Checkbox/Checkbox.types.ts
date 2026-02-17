import type { CheckboxProps as BaseCheckboxProps } from '@portal/design-core';

export type CheckboxProps = Omit<BaseCheckboxProps, 'checked'> & {
  modelValue?: boolean;
};

export interface CheckboxEmits {
  (e: 'update:modelValue', value: boolean): void;
  (e: 'change', value: boolean): void;
}
