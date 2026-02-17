<script setup lang="ts">
import { computed } from 'vue';
import { dividerVariants, dividerColors, dividerSpacing, dividerVerticalSpacing } from '@portal/design-core';
import type { DividerProps } from '@portal/design-core';

const props = withDefaults(defineProps<DividerProps>(), {
  orientation: 'horizontal',
  variant: 'solid',
  color: 'default',
  spacing: 'md',
});

const isHorizontal = computed(() => props.orientation === 'horizontal');
const hasLabel = computed(() => props.label || false);

const dividerClasses = computed(() => {
  if (isHorizontal.value) {
    if (hasLabel.value) {
      return [
        'flex items-center',
        dividerSpacing[props.spacing],
      ];
    }
    return [
      'border-t',
      dividerVariants[props.variant],
      dividerColors[props.color],
      dividerSpacing[props.spacing],
    ];
  }
  return [
    'border-l self-stretch',
    dividerVariants[props.variant],
    dividerColors[props.color],
    dividerVerticalSpacing[props.spacing],
  ];
});

const lineClasses = computed(() => [
  'flex-1 border-t',
  dividerVariants[props.variant],
  dividerColors[props.color],
]);
</script>

<template>
  <div
    :class="dividerClasses"
    :role="label ? 'separator' : undefined"
    :aria-orientation="orientation"
  >
    <template v-if="isHorizontal && hasLabel">
      <div :class="lineClasses" />
      <span class="px-3 text-sm text-text-muted whitespace-nowrap">
        <slot>{{ label }}</slot>
      </span>
      <div :class="lineClasses" />
    </template>
  </div>
</template>
