import type { RadioProps as BaseRadioProps } from '@portal/design-core';
export type { RadioOption } from '@portal/design-core';

export type RadioProps = Omit<BaseRadioProps, 'value'> & {
  modelValue?: string | number;
};

export interface RadioEmits {
  (e: 'update:modelValue', value: string | number): void;
  (e: 'change', value: string | number): void;
}
