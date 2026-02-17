export type { TagProps } from '@portal/design-core';

export interface TagEmits {
  (e: 'click'): void;
  (e: 'remove'): void;
}
