<script setup lang="ts">
import { computed } from 'vue';
import type { PaginationProps } from '@portal/design-core';

const props = withDefaults(defineProps<PaginationProps & { modelValue?: number }>(), {
  siblingCount: 1,
  showFirstLast: true,
  size: 'md',
});

const emit = defineEmits<{
  'update:modelValue': [value: number];
  change: [value: number];
}>();

const currentPage = computed(() => props.modelValue ?? props.page);

const sizeMap: Record<string, string> = {
  sm: 'h-7 w-7 text-xs',
  md: 'h-8 w-8 text-sm',
  lg: 'h-10 w-10 text-base',
};

const pages = computed(() => {
  const total = props.totalPages;
  const current = currentPage.value;
  const siblings = props.siblingCount;
  const result: (number | 'ellipsis')[] = [];

  const leftSibling = Math.max(current - siblings, 1);
  const rightSibling = Math.min(current + siblings, total);

  const showLeftEllipsis = leftSibling > 2;
  const showRightEllipsis = rightSibling < total - 1;

  if (!showLeftEllipsis && showRightEllipsis) {
    const leftRange = 1 + 2 * siblings + 1;
    for (let i = 1; i <= Math.min(leftRange, total); i++) result.push(i);
    if (leftRange < total) {
      result.push('ellipsis');
      result.push(total);
    }
  } else if (showLeftEllipsis && !showRightEllipsis) {
    result.push(1);
    const rightRange = total - (2 * siblings + 1);
    if (rightRange > 1) result.push('ellipsis');
    for (let i = Math.max(rightRange + 1, 2); i <= total; i++) result.push(i);
  } else if (showLeftEllipsis && showRightEllipsis) {
    result.push(1);
    result.push('ellipsis');
    for (let i = leftSibling; i <= rightSibling; i++) result.push(i);
    result.push('ellipsis');
    result.push(total);
  } else {
    for (let i = 1; i <= total; i++) result.push(i);
  }

  return result;
});

const goToPage = (page: number) => {
  if (page < 1 || page > props.totalPages || page === currentPage.value) return;
  emit('update:modelValue', page);
  emit('change', page);
};
</script>

<template>
  <nav role="navigation" aria-label="Pagination">
    <ul class="flex items-center gap-1">
      <!-- First -->
      <li v-if="showFirstLast">
        <button
          type="button"
          :disabled="currentPage <= 1"
          :class="[
            'inline-flex items-center justify-center rounded-md transition-colors',
            sizeMap[size],
            currentPage <= 1
              ? 'text-text-muted cursor-not-allowed'
              : 'text-text-body hover:bg-bg-hover',
          ]"
          aria-label="First page"
          @click="goToPage(1)"
        >
          <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M11 19l-7-7 7-7m8 14l-7-7 7-7" />
          </svg>
        </button>
      </li>

      <!-- Previous -->
      <li>
        <button
          type="button"
          :disabled="currentPage <= 1"
          :class="[
            'inline-flex items-center justify-center rounded-md transition-colors',
            sizeMap[size],
            currentPage <= 1
              ? 'text-text-muted cursor-not-allowed'
              : 'text-text-body hover:bg-bg-hover',
          ]"
          aria-label="Previous page"
          @click="goToPage(currentPage - 1)"
        >
          <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M15 19l-7-7 7-7" />
          </svg>
        </button>
      </li>

      <!-- Pages -->
      <li v-for="(item, index) in pages" :key="index">
        <span
          v-if="item === 'ellipsis'"
          :class="['inline-flex items-center justify-center text-text-muted', sizeMap[size]]"
        >
          ...
        </span>
        <button
          v-else
          type="button"
          :aria-current="item === currentPage ? 'page' : undefined"
          :class="[
            'inline-flex items-center justify-center rounded-md font-medium transition-colors',
            sizeMap[size],
            item === currentPage
              ? 'bg-brand-primary text-white'
              : 'text-text-body hover:bg-bg-hover',
          ]"
          @click="goToPage(item as number)"
        >
          {{ item }}
        </button>
      </li>

      <!-- Next -->
      <li>
        <button
          type="button"
          :disabled="currentPage >= totalPages"
          :class="[
            'inline-flex items-center justify-center rounded-md transition-colors',
            sizeMap[size],
            currentPage >= totalPages
              ? 'text-text-muted cursor-not-allowed'
              : 'text-text-body hover:bg-bg-hover',
          ]"
          aria-label="Next page"
          @click="goToPage(currentPage + 1)"
        >
          <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M9 5l7 7-7 7" />
          </svg>
        </button>
      </li>

      <!-- Last -->
      <li v-if="showFirstLast">
        <button
          type="button"
          :disabled="currentPage >= totalPages"
          :class="[
            'inline-flex items-center justify-center rounded-md transition-colors',
            sizeMap[size],
            currentPage >= totalPages
              ? 'text-text-muted cursor-not-allowed'
              : 'text-text-body hover:bg-bg-hover',
          ]"
          aria-label="Last page"
          @click="goToPage(totalPages)"
        >
          <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M13 5l7 7-7 7M5 5l7 7-7 7" />
          </svg>
        </button>
      </li>
    </ul>
  </nav>
</template>
