import type { SelectProps as BaseSelectProps } from '@portal/design-core';
export type { SelectOption } from '@portal/design-core';

export type SelectProps = Omit<BaseSelectProps, 'value'> & {
  modelValue?: string | number | null;
};

export interface SelectEmits {
  (e: 'update:modelValue', value: string | number | null): void;
  (e: 'change', value: string | number | null): void;
  (e: 'open'): void;
  (e: 'close'): void;
  (e: 'search', query: string): void;
}
