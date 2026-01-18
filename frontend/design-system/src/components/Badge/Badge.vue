<script setup lang="ts">
import type { BadgeProps } from './Badge.types';

const props = withDefaults(defineProps<BadgeProps>(), {
  variant: 'default',
  size: 'md'
});

// Linear-inspired badge styles
const variantClasses = {
  // Default: Subtle gray
  default: 'bg-bg-muted text-text-meta border border-border-muted',
  // Primary: Brand color
  primary: 'bg-brand-primary/10 text-brand-primary border border-brand-primary/20',
  // Success: Green
  success: 'bg-status-successBg text-status-success border border-status-success/20',
  // Warning: Yellow
  warning: 'bg-status-warningBg text-status-warning border border-status-warning/20',
  // Danger: Red
  danger: 'bg-status-errorBg text-status-error border border-status-error/20',
  // Info: Blue
  info: 'bg-status-infoBg text-status-info border border-status-info/20',
  // Outline: Border only
  outline: 'bg-transparent text-text-body border border-border-default'
};

const sizeClasses = {
  xs: 'px-1.5 py-0.5 text-[10px]',
  sm: 'px-2 py-0.5 text-xs',
  md: 'px-2.5 py-1 text-xs',
  lg: 'px-3 py-1 text-sm'
};
</script>

<template>
  <span
    :class="[
      'inline-flex items-center font-medium rounded-md',
      variantClasses[variant],
      sizeClasses[size]
    ]"
  >
    <slot />
  </span>
</template>