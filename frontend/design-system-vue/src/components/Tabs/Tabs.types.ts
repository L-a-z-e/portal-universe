import type { TabsProps as BaseTabsProps } from '@portal/design-types';
export type { TabItem } from '@portal/design-types';

export type TabsProps = Omit<BaseTabsProps, 'value'> & {
  modelValue: string;
};

export interface TabsEmits {
  (e: 'update:modelValue', value: string): void;
  (e: 'change', value: string): void;
}
