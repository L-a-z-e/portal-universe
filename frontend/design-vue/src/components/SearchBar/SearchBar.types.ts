import type { SearchBarProps as BaseSearchBarProps } from '@portal/design-core';

export type SearchBarProps = Omit<BaseSearchBarProps, 'value'> & {
  modelValue: string;
};
