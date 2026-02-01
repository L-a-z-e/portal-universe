<script setup lang="ts">
import { computed, ref } from 'vue';
import type { AvatarProps } from '@portal/design-types';

const props = withDefaults(defineProps<AvatarProps>(), {
  src: '',
  alt: '',
  name: '',
  size: 'md',
  shape: 'circle',
});

const imageError = ref(false);

const sizeClasses = {
  xs: 'w-6 h-6 text-xs',
  sm: 'w-8 h-8 text-sm',
  md: 'w-10 h-10 text-base',
  lg: 'w-12 h-12 text-lg',
  xl: 'w-16 h-16 text-xl',
  '2xl': 'w-20 h-20 text-2xl',
};

const statusSizeClasses = {
  xs: 'w-1.5 h-1.5',
  sm: 'w-2 h-2',
  md: 'w-2.5 h-2.5',
  lg: 'w-3 h-3',
  xl: 'w-4 h-4',
  '2xl': 'w-5 h-5',
};

const avatarClasses = computed(() => {
  const baseClasses = 'relative inline-flex items-center justify-center overflow-hidden flex-shrink-0';
  const shapeClasses = props.shape === 'circle' ? 'rounded-full' : 'rounded-lg';

  return [
    baseClasses,
    sizeClasses[props.size],
    shapeClasses,
  ].join(' ');
});

const imageClasses = computed(() => {
  return 'w-full h-full object-cover';
});

const fallbackClasses = computed(() => {
  return 'w-full h-full flex items-center justify-center bg-brand-primary text-white font-semibold uppercase select-none';
});

const statusClasses = computed(() => {
  if (!props.status) return '';

  const baseClasses = 'absolute bottom-0 right-0 block rounded-full ring-2 ring-bg-card';

  const statusColorClasses = {
    online: 'bg-status-success',
    offline: 'bg-text-muted',
    busy: 'bg-status-error',
    away: 'bg-status-warning',
  };

  return [
    baseClasses,
    statusSizeClasses[props.size],
    statusColorClasses[props.status],
  ].join(' ');
});

const initials = computed(() => {
  if (!props.name) return '?';

  const parts = props.name.trim().split(' ');

  if (parts.length >= 2) {
    // First and last name initials
    return `${parts[0][0]}${parts[parts.length - 1][0]}`.toUpperCase();
  }

  // Single name - take first 2 chars
  return props.name.substring(0, 2).toUpperCase();
});

const handleImageError = () => {
  imageError.value = true;
};
</script>

<template>
  <div :class="avatarClasses">
    <!-- Image -->
    <img
        v-if="src && !imageError"
        :src="src"
        :alt="alt || name"
        :class="imageClasses"
        @error="handleImageError"
        loading="lazy"
    />

    <!-- Fallback with initials -->
    <div
        v-else
        :class="fallbackClasses"
    >
      {{ initials }}
    </div>

    <!-- Status indicator -->
    <span
        v-if="status"
        :class="statusClasses"
        :title="status"
    />
  </div>
</template>