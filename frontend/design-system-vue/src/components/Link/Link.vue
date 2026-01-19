<script setup lang="ts">
import { computed } from 'vue';
import type { LinkProps } from './Link.types';

const props = withDefaults(defineProps<LinkProps>(), {
  target: '_self',
  variant: 'default',
  external: false,
  disabled: false,
  size: 'md',
});

const isExternal = computed(() => props.external || props.target === '_blank');
const isRouterLink = computed(() => !!props.to && !props.href);

const variantClasses = {
  default: 'text-text-link hover:text-text-linkHover',
  primary: 'text-brand-600 hover:text-brand-700 dark:text-brand-400 dark:hover:text-brand-300',
  muted: 'text-text-muted hover:text-text-body',
  underline: 'text-text-link hover:text-text-linkHover underline underline-offset-2',
};

const sizeClasses = {
  sm: 'text-sm',
  md: 'text-base',
  lg: 'text-lg',
};

const linkClasses = computed(() => [
  'inline-flex items-center gap-1 transition-colors duration-200',
  'focus:outline-none focus:ring-2 focus:ring-brand-500/20 focus:ring-offset-2 rounded',
  variantClasses[props.variant],
  sizeClasses[props.size],
  props.disabled && 'opacity-50 cursor-not-allowed pointer-events-none',
]);

const rel = computed(() => {
  if (isExternal.value) {
    return 'noopener noreferrer';
  }
  return undefined;
});
</script>

<template>
  <component
    :is="isRouterLink ? 'router-link' : 'a'"
    :to="isRouterLink ? to : undefined"
    :href="!isRouterLink ? href : undefined"
    :target="target"
    :rel="rel"
    :class="linkClasses"
    :aria-disabled="disabled"
    :tabindex="disabled ? -1 : undefined"
  >
    <slot />
    <svg
      v-if="isExternal"
      class="w-4 h-4 flex-shrink-0"
      fill="none"
      viewBox="0 0 24 24"
      stroke="currentColor"
    >
      <path
        stroke-linecap="round"
        stroke-linejoin="round"
        stroke-width="2"
        d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14"
      />
    </svg>
  </component>
</template>
