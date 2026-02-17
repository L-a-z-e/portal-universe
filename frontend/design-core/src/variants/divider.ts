import type { DividerVariant, DividerColor, PaddingSize } from '../types/common';

export const dividerVariants: Record<DividerVariant, string> = {
  solid: 'border-solid',
  dashed: 'border-dashed',
  dotted: 'border-dotted',
};

export const dividerColors: Record<DividerColor, string> = {
  default: 'border-border-default',
  muted: 'border-border-muted',
  strong: 'border-border-hover',
};

export const dividerSpacing: Record<Exclude<PaddingSize, 'xl'>, string> = {
  none: 'my-0',
  sm: 'my-2',
  md: 'my-4',
  lg: 'my-6',
};

export const dividerVerticalSpacing: Record<Exclude<PaddingSize, 'xl'>, string> = {
  none: 'mx-0',
  sm: 'mx-2',
  md: 'mx-4',
  lg: 'mx-6',
};
