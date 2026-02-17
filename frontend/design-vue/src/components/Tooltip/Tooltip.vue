<script setup lang="ts">
import { ref } from 'vue';
import type { TooltipProps } from '@portal/design-core';

const props = withDefaults(defineProps<TooltipProps>(), {
  placement: 'top',
  delay: 200,
  disabled: false,
});

const isVisible = ref(false);
let timeoutId: ReturnType<typeof setTimeout> | null = null;

const placementClasses: Record<string, string> = {
  'top': 'bottom-full left-1/2 -translate-x-1/2 mb-2',
  'top-start': 'bottom-full left-0 mb-2',
  'top-end': 'bottom-full right-0 mb-2',
  'bottom': 'top-full left-1/2 -translate-x-1/2 mt-2',
  'bottom-start': 'top-full left-0 mt-2',
  'bottom-end': 'top-full right-0 mt-2',
};

const show = () => {
  if (props.disabled) return;
  timeoutId = setTimeout(() => {
    isVisible.value = true;
  }, props.delay);
};

const hide = () => {
  if (timeoutId) {
    clearTimeout(timeoutId);
    timeoutId = null;
  }
  isVisible.value = false;
};
</script>

<template>
  <div
    class="relative inline-block"
    @mouseenter="show"
    @mouseleave="hide"
    @focusin="show"
    @focusout="hide"
  >
    <slot />
    <Transition
      enter-active-class="transition duration-150 ease-out"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="transition duration-100 ease-in"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div
        v-if="isVisible"
        role="tooltip"
        :class="[
          'absolute z-50 px-2.5 py-1.5 rounded-md text-xs font-medium whitespace-nowrap',
          'bg-bg-elevated text-text-heading border border-border-default shadow-lg',
          'pointer-events-none',
          placementClasses[placement],
        ]"
      >
        {{ content }}
      </div>
    </Transition>
  </div>
</template>
