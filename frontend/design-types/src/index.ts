/**
 * @portal/design-types
 * Framework-agnostic type definitions for Portal Design System
 */

// Common types
export * from './common';

// Component props
export * from './components';

// Theme types
export type ServiceType = 'portal' | 'blog' | 'shopping';
export type ThemeMode = 'light' | 'dark' | 'system';

export interface ThemeConfig {
  service: ServiceType;
  mode: ThemeMode;
}
