<script setup lang="ts">
import type { ButtonProps } from './Button.types';

const props = withDefaults(defineProps<ButtonProps>(), {
  variant: 'primary',
  size: 'md',
  disabled: false
});

const variantClasses = {
  primary: 'bg-brand-600 text-white hover:bg-brand-700 active:bg-brand-800 shadow-lg shadow-brand-500/30 border-2 border-brand-600 dark:bg-brand-700 dark:hover:bg-brand-800 dark:border-brand-700',
  secondary: 'bg-gray-100 text-gray-900 hover:bg-gray-200 active:bg-gray-300 border-2 border-gray-100 dark:bg-gray-700 dark:text-gray-100 dark:hover:bg-gray-600 dark:border-gray-700',
  outline: 'bg-transparent border-2 border-white text-white hover:bg-white hover:text-brand-600 backdrop-blur-sm dark:border-gray-300 dark:text-gray-300 dark:hover:bg-gray-800 dark:hover:text-gray-100'
};

const sizeClasses = {
  sm: 'px-4 py-2 text-sm',
  md: 'px-6 py-3 text-base',
  lg: 'px-8 py-4 text-lg font-semibold'
};
</script>

<template>
  <button
      :class="[
      'font-semibold rounded-lg transition-all duration-200',
      'focus:outline-none focus:ring-2 focus:ring-brand-500 focus:ring-offset-2',
      variantClasses[variant],
      sizeClasses[size],
      { 'opacity-50 cursor-not-allowed': disabled }
    ]"
      :disabled="disabled"
  >
    <slot />
  </button>
</template>