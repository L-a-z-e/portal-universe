// portal-shell/src/store/theme.ts
// Linear-inspired dark mode first design system

import { defineStore } from 'pinia';

export type ThemeMode = 'dark' | 'light' | 'system';

export const useThemeStore = defineStore('theme', {
  state: () => ({
    // Dark mode is the default (Linear style)
    isDark: true,
    mode: 'dark' as ThemeMode,
  }),
  actions: {
    toggle() {
      this.isDark = !this.isDark;
      this.mode = this.isDark ? 'dark' : 'light';
      this.applyTheme();
    },
    setMode(mode: ThemeMode) {
      this.mode = mode;
      if (mode === 'system') {
        this.isDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
      } else {
        this.isDark = mode === 'dark';
      }
      this.applyTheme();
    },
    applyTheme() {
      if (this.isDark) {
        document.documentElement.classList.remove('light');
        document.documentElement.classList.add('dark');
      } else {
        document.documentElement.classList.remove('dark');
        document.documentElement.classList.add('light');
      }
      localStorage.setItem('theme', this.mode);
    },
    initialize() {
      const saved = localStorage.getItem('theme') as ThemeMode | null;

      if (saved === 'system') {
        this.mode = 'system';
        this.isDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
      } else if (saved === 'light') {
        this.mode = 'light';
        this.isDark = false;
      } else {
        // Default to dark mode (Linear style)
        this.mode = saved === 'dark' ? 'dark' : 'dark';
        this.isDark = true;
      }

      this.applyTheme();

      // Listen for system theme changes
      window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (e) => {
        if (this.mode === 'system') {
          this.isDark = e.matches;
          this.applyTheme();
        }
      });
    }
  }
});