export type { DropdownItem, DropdownProps } from '@portal/design-core';
import type { DropdownItem } from '@portal/design-core';

export interface DropdownEmits {
  (e: 'select', item: DropdownItem): void;
  (e: 'open'): void;
  (e: 'close'): void;
}
