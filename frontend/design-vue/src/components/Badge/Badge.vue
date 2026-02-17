<script setup lang="ts">
import { badgeBase, badgeVariants, badgeSizes } from '@portal/design-core';
import type { BadgeProps } from '@portal/design-core';

const props = withDefaults(defineProps<BadgeProps>(), {
  variant: 'default',
  size: 'md'
});
</script>

<template>
  <span
    :class="[
      badgeBase,
      badgeVariants[variant],
      badgeSizes[size]
    ]"
  >
    <slot />
  </span>
</template>