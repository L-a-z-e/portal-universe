import type { SwitchProps as BaseSwitchProps } from '@portal/design-types';

export type SwitchProps = Omit<BaseSwitchProps, 'checked'> & {
  modelValue?: boolean;
};

export interface SwitchEmits {
  (e: 'update:modelValue', value: boolean): void;
  (e: 'change', value: boolean): void;
}
