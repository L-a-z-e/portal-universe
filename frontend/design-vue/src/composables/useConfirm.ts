import { ref, h, render, type VNode } from 'vue';
import Modal from '../components/Modal/Modal.vue';
import Button from '../components/Button/Button.vue';

interface ConfirmOptions {
  title?: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  variant?: 'default' | 'danger';
}

/**
 * Programmatic confirm dialog using DS Modal.
 * Usage: const { confirm } = useConfirm()
 *        if (await confirm({ message: '삭제하시겠습니까?' })) { ... }
 */
export function useConfirm() {
  async function confirm(options: ConfirmOptions): Promise<boolean> {
    return new Promise<boolean>((resolve) => {
      const container = document.createElement('div');
      document.body.appendChild(container);

      const isOpen = ref(true);

      function cleanup() {
        isOpen.value = false;
        // Wait for transition
        setTimeout(() => {
          render(null, container);
          container.remove();
        }, 200);
      }

      function handleConfirm() {
        cleanup();
        resolve(true);
      }

      function handleCancel() {
        cleanup();
        resolve(false);
      }

      const vnode = h(
        Modal,
        {
          modelValue: isOpen.value,
          'onUpdate:modelValue': (val: boolean) => {
            if (!val) handleCancel();
          },
          title: options.title || '확인',
          size: 'sm',
          showClose: false,
          closeOnBackdrop: false,
        },
        {
          default: () => [
            h('p', { class: 'text-sm text-text-body mb-6' }, options.message),
            h('div', { class: 'flex justify-end gap-3 pt-4 border-t border-border-default' }, [
              h(
                Button,
                { variant: 'ghost', onClick: handleCancel },
                () => options.cancelText || '취소',
              ),
              h(
                Button,
                {
                  variant: options.variant === 'danger' ? 'danger' : 'primary',
                  onClick: handleConfirm,
                },
                () => options.confirmText || '확인',
              ),
            ]),
          ],
        },
      );

      render(vnode, container);
    });
  }

  return { confirm };
}

export type { ConfirmOptions };
