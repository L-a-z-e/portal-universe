<script setup lang="ts">
/**
 * @file Card.vue
 * @description 컨텐츠를 감싸는 기본적인 카드 UI 컴포넌트입니다.
 * 그림자, 외곽선 등 다양한 시각적 스타일과 패딩 옵션을 제공합니다.
 */
import type { CardProps } from './Card.types';

/**
 * @property {'elevated' | 'outlined' | 'flat'} [variant='elevated'] - 카드의 시각적 스타일
 * @property {boolean} [hoverable=false] - 마우스 호버 시 확대 효과 및 커서 변경 여부
 * @property {'none' | 'sm' | 'md' | 'lg'} [padding='md'] - 카드 내부의 패딩 크기
 */
const props = withDefaults(defineProps<CardProps>(), {
  variant: 'elevated',
  hoverable: false,
  padding: 'md'
});

const variantClasses = {
  elevated: 'bg-white shadow-md hover:shadow-lg dark:bg-gray-800 dark:shadow-gray-900/50',
  outlined: 'bg-white border border-gray-200 dark:bg-gray-800 dark:border-gray-700',
  flat: 'bg-gray-50 dark:bg-gray-900'
};

const paddingClasses = {
  none: '',
  sm: 'p-4',
  md: 'p-6',
  lg: 'p-8'
};
</script>

<template>
  <div
      :class="[
      'rounded-lg transition-all duration-200',
      variantClasses[variant],
      paddingClasses[padding],
      { 'hover:scale-[1.02] cursor-pointer': hoverable } // hoverable 상태일 때 적용될 클래스
    ]"
  >
    <!-- @slot 카드 내부에 표시될 컨텐츠 -->
    <slot />
  </div>
</template>
