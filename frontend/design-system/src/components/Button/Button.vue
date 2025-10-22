<script setup lang="ts">
import type { ButtonProps } from './Button.types';

const props = withDefaults(defineProps<ButtonProps>(), {
  variant: 'primary',
  size: 'md',
  disabled: false
});

const variantClasses = {
  primary: 'bg-interactive-default text-text-inverse hover:bg-interactive-hover',
  secondary: 'bg-bg-secondary text-text-primary hover:bg-bg-tertiary',
  outline: 'border-2 border-border-default bg-transparent hover:border-border-hover'
};

const sizeClasses = {
  sm: 'px-sm py-xs text-sm',
  md: 'px-md py-sm text-base',
  lg: 'px-lg py-md text-lg'
};
</script>

<template>
  <button
      :class="[
      'rounded-default font-semibold transition-colors',
      variantClasses[variant],
      sizeClasses[size],
      { 'opacity-50 cursor-not-allowed': disabled }
    ]"
      :disabled="disabled"
  >
    <slot />
  </button>
</template>