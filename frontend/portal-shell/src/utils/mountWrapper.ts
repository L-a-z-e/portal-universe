import {defineComponent, h, onBeforeUnmount, onMounted, ref} from "vue";

export function mountWrapper(mountFn: (el: HTMLElement) => (() => void) | void) {
  return defineComponent({
    name: 'RemoteWrapper',
    setup() {
      const container = ref<HTMLElement | null>(null);
      let cleanup: (() => void) | void;

      onMounted(() => {
        if (container.value) {
          const ret = mountFn(container.value);
          if (typeof ret === 'function') {
            cleanup = ret;
          }
        }
      });

      onBeforeUnmount(() => {
        if (typeof cleanup === 'function') {
          cleanup();
        }
      });

      return () => h('div', { ref: container });
    },
  });
}