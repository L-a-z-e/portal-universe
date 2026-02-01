import type { ModalProps as BaseModalProps } from '@portal/design-types';

export type ModalProps = Omit<BaseModalProps, 'open'> & {
  modelValue: boolean;
};
