import type { SearchBarProps as BaseSearchBarProps } from '@portal/design-types';

export type SearchBarProps = Omit<BaseSearchBarProps, 'value'> & {
  modelValue: string;
};
