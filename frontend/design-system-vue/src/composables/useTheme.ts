/**
 * Theme Management Composable
 * Handles service context and light/dark mode toggling
 */

import { ref, onMounted } from 'vue';

export type ServiceType = 'portal' | 'blog' | 'shopping';
export type ThemeMode = 'light' | 'dark';

const currentService = ref<ServiceType>('portal');
const currentTheme = ref<ThemeMode>('light');

export function useTheme() {

  /**
   * Set service context
   * This determines which CSS variable overrides are applied
   */
  const setService = (service: ServiceType) => {
    currentService.value = service;

    if (typeof document !== 'undefined') {
      document.documentElement.setAttribute('data-service', service);
    }

    if (typeof localStorage !== 'undefined') {
      localStorage.setItem('portal-service', service);
    }
  };

  /**
   * Set theme mode (light/dark)
   */
  const setTheme = (mode: ThemeMode) => {
    currentTheme.value = mode;

    if (typeof document !== 'undefined') {
      document.documentElement.setAttribute('data-theme', mode);

      // Also set class for Tailwind darkMode: 'class'
      if (mode === 'dark') {
        document.documentElement.classList.add('dark');
      } else {
        document.documentElement.classList.remove('dark');
      }
    }

    if (typeof localStorage !== 'undefined') {
      localStorage.setItem('portal-theme', mode);
    }
  };

  /**
   * Toggle between light and dark mode
   */
  const toggleTheme = () => {
    const newTheme = currentTheme.value === 'light' ? 'dark' : 'light';
    setTheme(newTheme);
  };

  /**
   * Initialize theme from localStorage or system preference
   */
  const initTheme = () => {
    if (typeof window === 'undefined') return;

    const savedTheme = localStorage.getItem('portal-theme') as ThemeMode;
    const savedService = localStorage.getItem('portal-service') as ServiceType;

    if (savedTheme) {
      setTheme(savedTheme);
    } else {
      const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
      setTheme(prefersDark ? 'dark' : 'light');
    }

    if (savedService) {
      setService(savedService);
    }
  };

  /**
   * Watch for system theme changes
   */
  onMounted(() => {
    initTheme();

    if (typeof window !== 'undefined') {
      const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
      const handleChange = (e: MediaQueryListEvent) => {
        if (!localStorage.getItem('portal-theme')) {
          setTheme(e.matches ? 'dark' : 'light');
        }
      };

      mediaQuery.addEventListener('change', handleChange);

      return () => {
        mediaQuery.removeEventListener('change', handleChange);
      };
    }
  });

  return {
    currentService,
    currentTheme,
    setService,
    setTheme,
    toggleTheme,
    initTheme,
  };
}