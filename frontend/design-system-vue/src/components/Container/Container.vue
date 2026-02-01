<script setup lang="ts">
import { computed } from 'vue';
import type { ContainerProps } from '@portal/design-types';

const props = withDefaults(defineProps<ContainerProps>(), {
  maxWidth: 'lg',
  centered: true,
  padding: 'md',
  as: 'div',
});

const maxWidthClasses: Record<NonNullable<ContainerProps['maxWidth']>, string> = {
  sm: 'max-w-screen-sm',
  md: 'max-w-screen-md',
  lg: 'max-w-screen-lg',
  xl: 'max-w-screen-xl',
  '2xl': 'max-w-screen-2xl',
  full: 'max-w-full',
};

const paddingClasses: Record<NonNullable<ContainerProps['padding']>, string> = {
  none: 'px-0',
  sm: 'px-4',
  md: 'px-6 sm:px-8',
  lg: 'px-8 sm:px-12 lg:px-16',
};

const containerClasses = computed(() => [
  'w-full',
  maxWidthClasses[props.maxWidth],
  paddingClasses[props.padding],
  props.centered ? 'mx-auto' : '',
]);
</script>

<template>
  <component :is="as" :class="containerClasses">
    <slot />
  </component>
</template>
