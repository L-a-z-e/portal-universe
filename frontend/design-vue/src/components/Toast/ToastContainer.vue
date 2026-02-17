<script setup lang="ts">
import { computed } from 'vue';
import Toast from './Toast.vue';
import { useToast } from '../../composables/useToast';
import type { ToastContainerProps } from './Toast.types';

const props = withDefaults(defineProps<ToastContainerProps>(), {
  position: 'top-right',
  maxToasts: 5,
});

const { toasts, remove } = useToast();

const visibleToasts = computed(() => {
  const list = toasts.value.slice(0, props.maxToasts);
  // Reverse for bottom positions so newest appears at bottom
  if (props.position.startsWith('bottom')) {
    return [...list].reverse();
  }
  return list;
});

const positionClasses = {
  'top-right': 'top-4 right-4',
  'top-left': 'top-4 left-4',
  'top-center': 'top-4 left-1/2 -translate-x-1/2',
  'bottom-right': 'bottom-4 right-4',
  'bottom-left': 'bottom-4 left-4',
  'bottom-center': 'bottom-4 left-1/2 -translate-x-1/2',
};

const containerClasses = computed(() => [
  'fixed z-50 flex flex-col gap-2 w-full max-w-sm pointer-events-none',
  positionClasses[props.position],
]);

const handleDismiss = (id: string) => {
  remove(id);
};
</script>

<template>
  <Teleport to="body">
    <div :class="containerClasses">
      <TransitionGroup
        tag="div"
        class="flex flex-col gap-2"
        enter-active-class="transition duration-200 ease-out"
        enter-from-class="transform scale-95 opacity-0"
        enter-to-class="transform scale-100 opacity-100"
        leave-active-class="transition duration-150 ease-in"
        leave-from-class="transform scale-100 opacity-100"
        leave-to-class="transform scale-95 opacity-0"
        move-class="transition-all duration-200"
      >
        <div
          v-for="toast in visibleToasts"
          :key="toast.id"
          class="pointer-events-auto"
        >
          <Toast
            v-bind="toast"
            @dismiss="handleDismiss"
          />
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>
