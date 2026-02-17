<script setup lang="ts">
import { computed } from 'vue';
import { spinnerSizes } from '@portal/design-core';
import type { SpinnerProps } from '@portal/design-core';

const props = withDefaults(defineProps<SpinnerProps>(), {
  size: 'md',
  color: 'primary',
  label: 'Loading',
});

// Vue uses border-based spinner (different from React SVG-based)
// Keep colorClasses local as they differ between Vue and React implementations
const colorClasses = {
  primary: 'border-brand-600 border-t-transparent dark:border-brand-400',
  current: 'border-current border-t-transparent',
  white: 'border-white border-t-transparent',
};

const spinnerClasses = computed(() => [
  'inline-block rounded-full animate-spin',
  spinnerSizes[props.size],
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
