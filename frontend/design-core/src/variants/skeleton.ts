import type { SkeletonVariant, SkeletonAnimation } from '../types/common';

export const skeletonBase = 'bg-bg-muted';

export const skeletonVariants: Record<SkeletonVariant, string> = {
  text: 'rounded h-4',
  circular: 'rounded-full',
  rectangular: 'rounded-none',
  rounded: 'rounded-lg',
};

export const skeletonAnimations: Record<SkeletonAnimation, string> = {
  pulse: 'animate-pulse',
  wave: 'overflow-hidden relative before:absolute before:inset-0 before:-translate-x-full before:animate-[shimmer_2s_infinite] before:bg-gradient-to-r before:from-transparent before:via-white/10 before:to-transparent',
  none: '',
};
