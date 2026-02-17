import { useState, useEffect, useCallback } from 'react';
import type { ServiceType, ThemeMode } from '@portal/design-core';

export interface UseThemeOptions {
  defaultService?: ServiceType;
  defaultMode?: ThemeMode;
}

export interface UseThemeReturn {
  service: ServiceType;
  mode: ThemeMode;
  resolvedMode: 'light' | 'dark';
  setService: (service: ServiceType) => void;
  setMode: (mode: ThemeMode) => void;
  toggleMode: () => void;
}

export function useTheme(options: UseThemeOptions = {}): UseThemeReturn {
  const { defaultService = 'portal', defaultMode = 'system' } = options;

  const [service, setService] = useState<ServiceType>(defaultService);
  const [mode, setMode] = useState<ThemeMode>(defaultMode);
  const [systemMode, setSystemMode] = useState<'light' | 'dark'>('dark');

  // Listen to system preference changes
  useEffect(() => {
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
    setSystemMode(mediaQuery.matches ? 'dark' : 'light');

    const handler = (e: MediaQueryListEvent) => {
      setSystemMode(e.matches ? 'dark' : 'light');
    };

    mediaQuery.addEventListener('change', handler);
    return () => mediaQuery.removeEventListener('change', handler);
  }, []);

  // Calculate resolved mode
  const resolvedMode = mode === 'system' ? systemMode : mode;

  // Update DOM attributes
  useEffect(() => {
    document.documentElement.setAttribute('data-service', service);
    document.documentElement.setAttribute('data-theme', resolvedMode);
  }, [service, resolvedMode]);

  const toggleMode = useCallback(() => {
    setMode((current) => {
      if (current === 'light') return 'dark';
      if (current === 'dark') return 'light';
      return systemMode === 'dark' ? 'light' : 'dark';
    });
  }, [systemMode]);

  return {
    service,
    mode,
    resolvedMode,
    setService,
    setMode,
    toggleMode,
  };
}

export default useTheme;
