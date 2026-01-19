<script setup lang="ts">
import { computed } from 'vue';
import type { StackProps } from './Stack.types';

const props = withDefaults(defineProps<StackProps>(), {
  direction: 'vertical',
  gap: 'md',
  align: 'stretch',
  justify: 'start',
  wrap: false,
  as: 'div',
});

const gapClasses: Record<NonNullable<StackProps['gap']>, string> = {
  none: 'gap-0',
  xs: 'gap-1',
  sm: 'gap-2',
  md: 'gap-4',
  lg: 'gap-6',
  xl: 'gap-8',
  '2xl': 'gap-12',
};

const alignClasses: Record<NonNullable<StackProps['align']>, string> = {
  start: 'items-start',
  center: 'items-center',
  end: 'items-end',
  stretch: 'items-stretch',
  baseline: 'items-baseline',
};

const justifyClasses: Record<NonNullable<StackProps['justify']>, string> = {
  start: 'justify-start',
  center: 'justify-center',
  end: 'justify-end',
  between: 'justify-between',
  around: 'justify-around',
  evenly: 'justify-evenly',
};

const stackClasses = computed(() => [
  'flex',
  props.direction === 'horizontal' ? 'flex-row' : 'flex-col',
  gapClasses[props.gap],
  alignClasses[props.align],
  justifyClasses[props.justify],
  props.wrap ? 'flex-wrap' : 'flex-nowrap',
]);
</script>

<template>
  <component :is="as" :class="stackClasses">
    <slot />
  </component>
</template>
