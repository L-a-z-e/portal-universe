<script setup lang="ts">
import { computed } from 'vue';
import type { SpinnerProps } from './Spinner.types';

const props = withDefaults(defineProps<SpinnerProps>(), {
  size: 'md',
  color: 'primary',
  label: 'Loading',
});

const sizeClasses = {
  xs: 'w-3 h-3 border',
  sm: 'w-4 h-4 border-2',
  md: 'w-6 h-6 border-2',
  lg: 'w-8 h-8 border-2',
  xl: 'w-12 h-12 border-3',
};

const colorClasses = {
  primary: 'border-brand-600 border-t-transparent dark:border-brand-400',
  current: 'border-current border-t-transparent',
  white: 'border-white border-t-transparent',
};

const spinnerClasses = computed(() => [
  'inline-block rounded-full animate-spin',
  sizeClasses[props.size],
  colorClasses[props.color],
]);
</script>

<template>
  <div
    role="status"
    :aria-label="label"
    :class="spinnerClasses"
  >
    <span class="sr-only">{{ label }}</span>
  </div>
</template>

<style scoped>
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border-width: 0;
}

.border-3 {
  border-width: 3px;
}
</style>
