/**
 * Portal Universe Design System
 * Main entry point
 */

// Import global styles first
import './styles/index.css';
import './styles/themes/blog.css';
import './styles/themes/shopping.css';
import './styles/themes/prism.css';

// Export all components
export * from './components';

// Export composables
export { useTheme } from './composables/useTheme';
export { useToast } from './composables/useToast';
export type { UseToast } from './composables/useToast';

// Export types
export type { ServiceType, ThemeMode, ThemeConfig } from './types/theme';