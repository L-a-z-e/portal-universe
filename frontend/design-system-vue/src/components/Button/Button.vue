<script setup lang="ts">
import type { ButtonProps } from './Button.types';

const props = withDefaults(defineProps<ButtonProps>(), {
  variant: 'primary',
  size: 'md',
  disabled: false,
  loading: false,
  fullWidth: false
});

// Linear-inspired button styles - Dark mode first design
const variantClasses = {
  // Primary: Bright button on dark bg (Linear style)
  // Dark mode: white/light gray button with dark text
  // Light mode: brand color button with white text (handled via theme)
  primary: [
    // Dark mode (default)
    'bg-white/90 text-[#08090a]',
    'hover:bg-white',
    'active:bg-white/80 active:scale-[0.98]',
    // Light mode override
    'light:bg-brand-primary light:text-white',
    'light:hover:bg-brand-primaryHover',
    'light:active:bg-brand-primary',
    'border border-transparent',
    'shadow-sm'
  ].join(' '),

  // Secondary: Ghost style with border
  secondary: [
    'bg-transparent text-text-body',
    'hover:bg-white/5 hover:text-text-heading',
    'active:bg-white/10 active:scale-[0.98]',
    'border border-[#2a2a2a]',
    'light:hover:bg-gray-100',
    'light:border-gray-200'
  ].join(' '),

  // Ghost: Minimal button without border
  ghost: [
    'bg-transparent text-text-body',
    'hover:bg-white/5 hover:text-text-heading',
    'active:bg-white/10 active:scale-[0.98]',
    'border border-transparent',
    'light:hover:bg-gray-100'
  ].join(' '),

  // Outline: Border emphasis
  outline: [
    'bg-transparent text-text-body',
    'hover:bg-white/5 hover:border-[#3a3a3a]',
    'active:bg-white/10 active:scale-[0.98]',
    'border border-[#2a2a2a]',
    'light:border-gray-300 light:hover:border-gray-400',
    'light:hover:bg-gray-50'
  ].join(' '),

  // Danger: Destructive action - consistent across modes
  danger: [
    'bg-[#E03131] text-white',
    'hover:bg-[#C92A2A]',
    'active:bg-[#A51D1D] active:scale-[0.98]',
    'border border-transparent',
    'shadow-sm'
  ].join(' ')
};

const sizeClasses = {
  xs: 'h-6 px-2 text-xs gap-1',
  sm: 'h-8 px-3 text-sm gap-1.5',
  md: 'h-9 px-4 text-sm gap-2',
  lg: 'h-11 px-5 text-base gap-2'
};
</script>

<template>
  <button
    :class="[
      'inline-flex items-center justify-center',
      'font-medium rounded-md',
      'transition-all duration-150 ease-out',
      'focus:outline-none focus-visible:ring-2 focus-visible:ring-[#5e6ad2] focus-visible:ring-offset-2 focus-visible:ring-offset-[#08090a]',
      'light:focus-visible:ring-offset-white',
      variantClasses[variant],
      sizeClasses[size],
      fullWidth ? 'w-full' : '',
      { 'opacity-50 cursor-not-allowed pointer-events-none': disabled || loading }
    ]"
    :disabled="disabled || loading"
  >
    <!-- Loading spinner -->
    <svg
      v-if="loading"
      class="animate-spin h-4 w-4 shrink-0"
      xmlns="http://www.w3.org/2000/svg"
      fill="none"
      viewBox="0 0 24 24"
    >
      <circle
        class="opacity-25"
        cx="12"
        cy="12"
        r="10"
        stroke="currentColor"
        stroke-width="4"
      />
      <path
        class="opacity-75"
        fill="currentColor"
        d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
      />
    </svg>
    <slot />
  </button>
</template>