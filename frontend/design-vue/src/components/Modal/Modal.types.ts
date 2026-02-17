import type { ModalProps as BaseModalProps } from '@portal/design-core';

export type ModalProps = Omit<BaseModalProps, 'open'> & {
  modelValue: boolean;
};
