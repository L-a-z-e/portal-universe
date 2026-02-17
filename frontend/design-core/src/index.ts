/**
 * @portal/design-core
 * Single Source of Truth for the Portal Design System
 * - Types: Framework-agnostic type definitions
 * - Variants: Shared Tailwind class maps
 * - Utils: cn() class merging utility
 */

// Types
export * from './types/common';
export * from './types/components';
export * from './types/api';
export * from './types/logger';

// Theme types (inline)
export type ServiceType = 'portal' | 'blog' | 'shopping';
export type ThemeMode = 'light' | 'dark' | 'system';

export interface ThemeConfig {
  service: ServiceType;
  mode: ThemeMode;
}

// Variants
export * from './variants';

// Utils
export { cn } from './utils';
