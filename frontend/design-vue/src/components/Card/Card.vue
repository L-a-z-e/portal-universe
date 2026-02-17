<script setup lang="ts">
import { cardBase, cardVariants, cardPadding } from '@portal/design-core';
import type { CardProps } from '@portal/design-core';

const props = withDefaults(defineProps<CardProps>(), {
  variant: 'elevated',
  hoverable: false,
  padding: 'md'
});
</script>

<template>
  <div
    :class="[
      cardBase,
      cardVariants[variant],
      cardPadding[padding],
      {
        'hover:-translate-y-0.5 hover:shadow-[0_4px_12px_rgba(0,0,0,0.5)] cursor-pointer': hoverable && variant !== 'interactive',
        'light:hover:shadow-md': hoverable && variant !== 'interactive'
      }
    ]"
  >
    <slot />
  </div>
</template>