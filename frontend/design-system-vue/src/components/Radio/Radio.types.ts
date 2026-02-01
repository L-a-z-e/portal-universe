import type { RadioProps as BaseRadioProps } from '@portal/design-types';
export type { RadioOption } from '@portal/design-types';

export type RadioProps = Omit<BaseRadioProps, 'value'> & {
  modelValue?: string | number;
};

export interface RadioEmits {
  (e: 'update:modelValue', value: string | number): void;
  (e: 'change', value: string | number): void;
}
