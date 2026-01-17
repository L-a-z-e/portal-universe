<script setup lang="ts">
import { computed } from 'vue';
import type { DividerProps } from './Divider.types';

const props = withDefaults(defineProps<DividerProps>(), {
  orientation: 'horizontal',
  variant: 'solid',
  color: 'default',
  spacing: 'md',
});

const variantClasses: Record<NonNullable<DividerProps['variant']>, string> = {
  solid: 'border-solid',
  dashed: 'border-dashed',
  dotted: 'border-dotted',
};

const colorClasses: Record<NonNullable<DividerProps['color']>, string> = {
  default: 'border-border-default',
  muted: 'border-border-muted',
  strong: 'border-gray-400 dark:border-gray-500',
};

const spacingClasses: Record<NonNullable<DividerProps['spacing']>, string> = {
  none: '',
  sm: 'my-2',
  md: 'my-4',
  lg: 'my-6',
};

const verticalSpacingClasses: Record<NonNullable<DividerProps['spacing']>, string> = {
  none: '',
  sm: 'mx-2',
  md: 'mx-4',
  lg: 'mx-6',
};

const isHorizontal = computed(() => props.orientation === 'horizontal');
const hasLabel = computed(() => props.label || false);

const dividerClasses = computed(() => {
  if (isHorizontal.value) {
    if (hasLabel.value) {
      return [
        'flex items-center',
        spacingClasses[props.spacing],
      ];
    }
    return [
      'border-t',
      variantClasses[props.variant],
      colorClasses[props.color],
      spacingClasses[props.spacing],
    ];
  }
  return [
    'border-l self-stretch',
    variantClasses[props.variant],
    colorClasses[props.color],
    verticalSpacingClasses[props.spacing],
  ];
});

const lineClasses = computed(() => [
  'flex-1 border-t',
  variantClasses[props.variant],
  colorClasses[props.color],
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
