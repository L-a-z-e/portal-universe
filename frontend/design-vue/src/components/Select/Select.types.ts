import type { SelectProps as BaseSelectProps } from '@portal/design-core';
export type { SelectOption } from '@portal/design-core';

export type SelectProps = Omit<BaseSelectProps, 'value'> & {
  modelValue?: string | number | null | (string | number)[];
};

export interface SelectEmits {
  (e: 'update:modelValue', value: string | number | null | (string | number)[]): void;
  (e: 'change', value: string | number | null | (string | number)[]): void;
  (e: 'open'): void;
  (e: 'close'): void;
  (e: 'search', query: string): void;
}
