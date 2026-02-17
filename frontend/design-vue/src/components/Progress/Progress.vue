<script setup lang="ts">
import { computed } from 'vue';
import type { ProgressProps } from '@portal/design-core';

const props = withDefaults(defineProps<ProgressProps>(), {
  max: 100,
  size: 'md',
  showLabel: false,
  variant: 'default',
});

const percentage = computed(() => {
  const raw = (props.value / props.max) * 100;
  return Math.min(100, Math.max(0, raw));
});

const sizeClasses: Record<string, string> = {
  sm: 'h-1',
  md: 'h-2',
  lg: 'h-3',
};

const variantClasses: Record<string, string> = {
  default: 'bg-brand-primary',
  info: 'bg-status-info',
  success: 'bg-status-success',
  warning: 'bg-status-warning',
  error: 'bg-status-error',
};
</script>

<template>
  <div class="w-full">
    <div class="flex items-center gap-2">
      <div
        role="progressbar"
        :aria-valuenow="value"
        :aria-valuemin="0"
        :aria-valuemax="max"
        :class="['w-full rounded-full bg-bg-muted overflow-hidden', sizeClasses[size]]"
      >
        <div
          :class="['h-full rounded-full transition-all duration-300', variantClasses[variant]]"
          :style="{ width: `${percentage}%` }"
        />
      </div>
      <span v-if="showLabel" class="text-sm text-text-meta whitespace-nowrap">
        {{ Math.round(percentage) }}%
      </span>
    </div>
  </div>
</template>
