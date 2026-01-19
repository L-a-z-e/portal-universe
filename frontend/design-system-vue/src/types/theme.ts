/**
 * Design System Type Definitions
 */

export type ServiceType = 'portal' | 'blog' | 'shopping';
export type ThemeMode = 'light' | 'dark';

export interface ThemeConfig {
  service: ServiceType;
  mode: ThemeMode;
}

// Color token types (for TypeScript autocomplete)
export type BrandColor = 'primary' | 'primary-hover' | 'secondary';
export type TextColor = 'heading' | 'body' | 'meta' | 'muted' | 'inverse' | 'link' | 'link-hover';
export type BgColor = 'page' | 'card' | 'elevated' | 'muted' | 'hover';
export type BorderColor = 'default' | 'hover' | 'focus' | 'muted';
export type StatusColor = 'success' | 'success-bg' | 'error' | 'error-bg' | 'warning' | 'warning-bg' | 'info' | 'info-bg';