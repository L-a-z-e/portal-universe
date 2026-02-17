/**
 * Portal Universe Design System
 * Main entry point
 */

// Import global styles (themes are loaded via index.css from @portal/design-core)
import './styles/index.css';

// Export all components
export * from './components';

// Export composables
export { useTheme } from './composables/useTheme';
export { useToast } from './composables/useToast';
export type { UseToast } from './composables/useToast';
export { useApiError } from './composables/useApiError';
export type { UseApiError, ApiErrorInfo } from './composables/useApiError';
export { useLogger } from './composables/useLogger';
export { setupErrorHandler } from './composables/useErrorHandler';
export type { ErrorHandlerOptions } from './composables/useErrorHandler';

// Export types (from core)
export type { ServiceType, ThemeMode, ThemeConfig } from '@portal/design-core';