<script setup lang="ts">
import { computed } from 'vue';
import type { TagProps, TagEmits } from './Tag.types';

const props = withDefaults(defineProps<TagProps>(), {
  variant: 'default',
  size: 'md',
  removable: false,
  clickable: false,
});

const emit = defineEmits<TagEmits>();

const variantClasses = {
  default: 'bg-bg-muted text-text-meta hover:bg-bg-hover border border-border-muted',
  primary: 'bg-brand-primary/10 text-brand-primary hover:bg-brand-primary/20 border border-brand-primary/20',
  success: 'bg-status-success-bg text-status-success hover:bg-status-success-bg/80 border border-status-success/20',
  error: 'bg-status-error-bg text-status-error hover:bg-status-error-bg/80 border border-status-error/20',
  warning: 'bg-status-warning-bg text-status-warning hover:bg-status-warning-bg/80 border border-status-warning/20',
  info: 'bg-status-info-bg text-status-info hover:bg-status-info-bg/80 border border-status-info/20',
};

const sizeClasses = {
  sm: 'text-xs px-2 py-1 rounded-md',
  md: 'text-sm px-3 py-1.5 rounded-lg',
  lg: 'text-base px-4 py-2 rounded-lg',
};

const tagClasses = computed(() => {
  const baseClasses = 'inline-flex items-center gap-1 font-medium transition-all duration-200';
  const clickableClasses = props.clickable ? 'cursor-pointer hover:shadow-sm' : '';

  return [
    baseClasses,
    variantClasses[props.variant],
    sizeClasses[props.size],
    clickableClasses,
  ].join(' ');
});

const iconSizeClasses = computed(() => {
  const sizes = {
    sm: 'w-3 h-3',
    md: 'w-3.5 h-3.5',
    lg: 'w-4 h-4',
  };
  return sizes[props.size];
});

const handleClick = () => {
  if (props.clickable) {
    emit('click');
  }
};

const handleRemove = (e: Event) => {
  e.stopPropagation();
  emit('remove');
};
</script>

<template>
  <span
      :class="tagClasses"
      @click="handleClick"
  >
    <slot />

    <button
        v-if="removable"
        type="button"
        class="hover:text-text-heading transition-colors focus:outline-none focus:ring-2 focus:ring-brand-primary rounded"
        @click="handleRemove"
        aria-label="Remove tag"
    >
      <svg
          :class="iconSizeClasses"
          fill="currentColor"
          viewBox="0 0 20 20"
          xmlns="http://www.w3.org/2000/svg"
      >
        <path
            fill-rule="evenodd"
            d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z"
            clip-rule="evenodd"
        />
      </svg>
    </button>
  </span>
</template>