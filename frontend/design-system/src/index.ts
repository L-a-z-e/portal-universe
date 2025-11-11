/**
 * Portal Universe Design System
 * Main entry point
 */

// Import global styles first
import './styles/index.css';
import './styles/themes/blog.css';
import './styles/themes/shopping.css';

// Export all components
export * from './components';

// Export composables
export { useTheme } from './composables/useTheme';

// Export types
export type { ServiceType, ThemeMode, ThemeConfig } from './types/theme';