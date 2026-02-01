export type { TagProps } from '@portal/design-types';

export interface TagEmits {
  (e: 'click'): void;
  (e: 'remove'): void;
}
