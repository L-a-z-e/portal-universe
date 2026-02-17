<script setup lang="ts">
import { computed } from 'vue';
import { tagBase, tagVariants, tagSizes } from '@portal/design-core';
import type { TagProps, TagEmits } from './Tag.types';

const props = withDefaults(defineProps<TagProps>(), {
  variant: 'default',
  size: 'md',
  removable: false,
  clickable: false,
});

const emit = defineEmits<TagEmits>();

const tagClasses = computed(() => {
  const clickableClasses = props.clickable ? 'cursor-pointer hover:shadow-sm' : '';

  return [
    tagBase,
    tagVariants[props.variant],
    tagSizes[props.size],
    clickableClasses,
  ].join(' ');
});

const iconSizeClasses = computed(() => {
  const sizes = {
    sm: 'w-3 h-3',
    md: 'w-3.5 h-3.5',
    lg: 'w-4 h-4',
  };
  return sizes[props.size];
});

const handleClick = () => {
  if (props.clickable) {
    emit('click');
  }
};

const handleRemove = (e: Event) => {
  e.stopPropagation();
  emit('remove');
};
</script>

<template>
  <span
      :class="tagClasses"
      @click="handleClick"
  >
    <slot />

    <button
        v-if="removable"
        type="button"
        class="hover:text-text-heading transition-colors focus:outline-none focus:ring-2 focus:ring-brand-primary rounded"
        @click="handleRemove"
        aria-label="Remove tag"
    >
      <svg
          :class="iconSizeClasses"
          fill="currentColor"
          viewBox="0 0 20 20"
          xmlns="http://www.w3.org/2000/svg"
      >
        <path
            fill-rule="evenodd"
            d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z"
            clip-rule="evenodd"
        />
      </svg>
    </button>
  </span>
</template>