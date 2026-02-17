import type { FormSize } from '../types/common';

export type TabsVariant = 'underline' | 'pill' | 'boxed';

export const tabsBase = [
  'flex',
].join(' ');

export const tabsVariants: Record<TabsVariant, string> = {
  underline: [
    'border-b border-border-default',
    'gap-0',
  ].join(' '),

  pill: [
    'bg-bg-muted rounded-lg p-1',
    'gap-1',
  ].join(' '),

  boxed: [
    'border border-border-default rounded-lg',
    'gap-0',
  ].join(' '),
};

export const tabsItemBase = [
  'inline-flex items-center justify-center',
  'font-medium cursor-pointer',
  'transition-colors duration-fast',
  'whitespace-nowrap',
].join(' ');

export const tabsItemVariants: Record<TabsVariant, { active: string; inactive: string }> = {
  underline: {
    active: [
      'text-text-heading',
      'border-b-2 border-brand-primary',
      '-mb-px',
    ].join(' '),
    inactive: [
      'text-text-muted',
      'border-b-2 border-transparent',
      '-mb-px',
      'hover:text-text-body hover:border-border-hover',
    ].join(' '),
  },

  pill: {
    active: [
      'bg-bg-elevated text-text-heading',
      'rounded-md shadow-sm',
    ].join(' '),
    inactive: [
      'text-text-muted',
      'rounded-md',
      'hover:text-text-body hover:bg-bg-hover',
    ].join(' '),
  },

  boxed: {
    active: [
      'bg-bg-elevated text-text-heading',
      'border-r border-border-default',
      'first:rounded-l-lg last:rounded-r-lg',
    ].join(' '),
    inactive: [
      'text-text-muted',
      'border-r border-border-default last:border-r-0',
      'first:rounded-l-lg last:rounded-r-lg',
      'hover:text-text-body hover:bg-bg-hover',
    ].join(' '),
  },
};

export const tabsSizes: Record<FormSize, string> = {
  sm: 'text-sm px-3 py-1.5',
  md: 'text-base px-4 py-2',
  lg: 'text-lg px-5 py-2.5',
};
