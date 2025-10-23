<script setup lang="ts">
import type { CardProps } from './Card.types';

const props = withDefaults(defineProps<CardProps>(), {
  variant: 'elevated',
  hoverable: false,
  padding: 'md'
});

const variantClasses = {
  elevated: 'bg-white shadow-md hover:shadow-lg dark:bg-gray-800 dark:shadow-gray-900/50',
  outlined: 'bg-white border border-gray-200 dark:bg-gray-800 dark:border-gray-700',
  flat: 'bg-gray-50 dark:bg-gray-900'
};

const paddingClasses = {
  none: '',
  sm: 'p-4',
  md: 'p-6',
  lg: 'p-8'
};
</script>

<template>
  <div
      :class="[
      'rounded-lg transition-all duration-200',
      variantClasses[variant],
      paddingClasses[padding],
      { 'hover:scale-[1.02] cursor-pointer': hoverable }
    ]"
  >
    <slot />
  </div>
</template>