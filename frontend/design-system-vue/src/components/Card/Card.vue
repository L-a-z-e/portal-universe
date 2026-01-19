<script setup lang="ts">
import type { CardProps } from './Card.types';

const props = withDefaults(defineProps<CardProps>(), {
  variant: 'elevated',
  hoverable: false,
  padding: 'md'
});

// Linear-inspired card styles
const variantClasses = {
  // Elevated: Default card with subtle shadow
  elevated: [
    'bg-bg-card',
    'border border-border-default',
    'shadow-sm'
  ].join(' '),
  // Outlined: Border emphasis
  outlined: [
    'bg-transparent',
    'border border-border-default'
  ].join(' '),
  // Flat: No border, subtle background
  flat: [
    'bg-bg-muted',
    'border border-transparent'
  ].join(' '),
  // Glass: Glassmorphism effect
  glass: [
    'bg-bg-card/50',
    'backdrop-blur-md',
    'border border-white/10'
  ].join(' '),
  // Interactive: For clickable cards
  interactive: [
    'bg-bg-card',
    'border border-border-default',
    'hover:border-border-hover hover:bg-bg-hover',
    'cursor-pointer'
  ].join(' ')
};

const paddingClasses = {
  none: '',
  sm: 'p-3',
  md: 'p-4',
  lg: 'p-6',
  xl: 'p-8'
};
</script>

<template>
  <div
    :class="[
      'rounded-xl',
      'transition-all duration-[160ms] ease-[cubic-bezier(0.25,0.1,0.25,1)]',
      variantClasses[variant],
      paddingClasses[padding],
      { 'hover:scale-[1.01] hover:shadow-md cursor-pointer': hoverable && variant !== 'interactive' }
    ]"
  >
    <slot />
  </div>
</template>