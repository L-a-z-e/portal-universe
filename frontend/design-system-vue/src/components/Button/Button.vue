<script setup lang="ts">
import type { ButtonProps } from './Button.types';

const props = withDefaults(defineProps<ButtonProps>(), {
  variant: 'primary',
  size: 'md',
  disabled: false
});

// Linear-inspired button styles
const variantClasses = {
  // Primary: Light button on dark bg (Linear style)
  primary: [
    'bg-[#e6e6e6] text-[#08090a]',
    'hover:bg-white',
    'active:bg-[#d4d4d4]',
    'border border-transparent'
  ].join(' '),
  // Secondary: Subtle transparent button
  secondary: [
    'bg-transparent text-text-meta',
    'hover:bg-bg-hover hover:text-text-body',
    'active:bg-bg-muted',
    'border border-border-default'
  ].join(' '),
  // Ghost: Minimal button
  ghost: [
    'bg-transparent text-text-meta',
    'hover:bg-bg-hover hover:text-text-body',
    'active:bg-bg-muted',
    'border border-transparent'
  ].join(' '),
  // Outline: Border only
  outline: [
    'bg-transparent text-text-body',
    'hover:bg-bg-hover',
    'active:bg-bg-muted',
    'border border-border-default hover:border-border-hover'
  ].join(' '),
  // Danger: Destructive action
  danger: [
    'bg-status-error text-white',
    'hover:bg-red-600',
    'active:bg-red-700',
    'border border-transparent'
  ].join(' ')
};

const sizeClasses = {
  xs: 'px-2 py-1 text-xs',
  sm: 'px-3 py-1.5 text-sm',
  md: 'px-4 py-2 text-sm',
  lg: 'px-5 py-2.5 text-base'
};
</script>

<template>
  <button
    :class="[
      'inline-flex items-center justify-center gap-2',
      'font-medium rounded-md',
      'transition-all duration-[160ms] ease-[cubic-bezier(0.25,0.1,0.25,1)]',
      'focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-primary focus-visible:ring-offset-2 focus-visible:ring-offset-bg-page',
      variantClasses[variant],
      sizeClasses[size],
      { 'opacity-50 cursor-not-allowed pointer-events-none': disabled }
    ]"
    :disabled="disabled"
  >
    <slot />
  </button>
</template>