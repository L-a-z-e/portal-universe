export type { DropdownItem, DropdownProps } from '@portal/design-types';
import type { DropdownItem } from '@portal/design-types';

export interface DropdownEmits {
  (e: 'select', item: DropdownItem): void;
  (e: 'open'): void;
  (e: 'close'): void;
}
