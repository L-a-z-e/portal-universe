<script setup lang="ts">
import { computed } from 'vue';
import type { SkeletonProps } from './Skeleton.types';

const props = withDefaults(defineProps<SkeletonProps>(), {
  variant: 'text',
  animation: 'pulse',
  lines: 1,
});

const variantClasses = {
  text: 'rounded h-4',
  circular: 'rounded-full',
  rectangular: '',
  rounded: 'rounded-lg',
};

const animationClasses = {
  pulse: 'animate-pulse',
  wave: 'skeleton-wave',
  none: '',
};

const skeletonClasses = computed(() => [
  'bg-gray-200 dark:bg-gray-700',
  variantClasses[props.variant],
  animationClasses[props.animation],
]);

const skeletonStyles = computed(() => {
  const styles: Record<string, string> = {};

  if (props.width) {
    styles.width = props.width;
  } else if (props.variant === 'circular') {
    styles.width = props.height || '40px';
  }

  if (props.height) {
    styles.height = props.height;
  } else if (props.variant === 'circular') {
    styles.height = props.width || '40px';
  } else if (props.variant === 'rectangular' || props.variant === 'rounded') {
    styles.height = '100px';
  }

  return styles;
});

const lineWidths = computed(() => {
  if (props.variant !== 'text' || props.lines <= 1) return [];

  return Array.from({ length: props.lines }, (_, i) => {
    // Last line is shorter
    if (i === props.lines - 1) {
      return `${60 + Math.random() * 20}%`;
    }
    return `${90 + Math.random() * 10}%`;
  });
});
</script>

<template>
  <div aria-busy="true" aria-label="Loading">
    <!-- Single skeleton or non-text variant -->
    <div
      v-if="variant !== 'text' || lines <= 1"
      :class="skeletonClasses"
      :style="skeletonStyles"
    />

    <!-- Multiple text lines -->
    <div v-else class="space-y-2">
      <div
        v-for="(lineWidth, index) in lineWidths"
        :key="index"
        :class="skeletonClasses"
        :style="{ width: lineWidth, height: height || '1rem' }"
      />
    </div>
  </div>
</template>

<style scoped>
.skeleton-wave {
  position: relative;
  overflow: hidden;
}

.skeleton-wave::after {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  transform: translateX(-100%);
  background: linear-gradient(
    90deg,
    transparent,
    rgba(255, 255, 255, 0.4),
    transparent
  );
  animation: wave 1.5s infinite;
}

@keyframes wave {
  100% {
    transform: translateX(100%);
  }
}

:global(.dark) .skeleton-wave::after {
  background: linear-gradient(
    90deg,
    transparent,
    rgba(255, 255, 255, 0.1),
    transparent
  );
}
</style>
