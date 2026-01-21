// portal-shell/src/store/settings.ts
// User settings store - Local-First with Optional Sync

import { defineStore } from 'pinia';

export type Language = 'ko' | 'en';

export interface NotificationSettings {
  email: boolean;
  push: boolean;
  marketing: boolean;
}

export interface SettingsState {
  language: Language;
  notifications: NotificationSettings;
  sidebarCollapsed: boolean;
  compactMode: boolean;
}

const STORAGE_KEY = 'portal-settings';

const defaultSettings: SettingsState = {
  language: 'ko',
  notifications: {
    email: true,
    push: true,
    marketing: false,
  },
  sidebarCollapsed: false,
  compactMode: false,
};

function loadFromStorage(): Partial<SettingsState> {
  try {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved) {
      return JSON.parse(saved);
    }
  } catch (e) {
    console.warn('Failed to load settings from localStorage:', e);
  }
  return {};
}

function saveToStorage(settings: SettingsState): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(settings));
  } catch (e) {
    console.warn('Failed to save settings to localStorage:', e);
  }
}

export const useSettingsStore = defineStore('settings', {
  state: (): SettingsState => ({
    ...defaultSettings,
    ...loadFromStorage(),
  }),

  getters: {
    currentLanguage: (state) => state.language,
    isKorean: (state) => state.language === 'ko',
    notificationPreferences: (state) => state.notifications,
  },

  actions: {
    setLanguage(lang: Language) {
      this.language = lang;
      this.persist();
    },

    setNotification(key: keyof NotificationSettings, value: boolean) {
      this.notifications[key] = value;
      this.persist();
    },

    setAllNotifications(settings: NotificationSettings) {
      this.notifications = { ...settings };
      this.persist();
    },

    setSidebarCollapsed(collapsed: boolean) {
      this.sidebarCollapsed = collapsed;
      this.persist();
    },

    setCompactMode(compact: boolean) {
      this.compactMode = compact;
      this.persist();
    },

    resetToDefaults() {
      this.$patch(defaultSettings);
      this.persist();
    },

    persist() {
      saveToStorage({
        language: this.language,
        notifications: this.notifications,
        sidebarCollapsed: this.sidebarCollapsed,
        compactMode: this.compactMode,
      });
    },

    initialize() {
      const saved = loadFromStorage();
      if (Object.keys(saved).length > 0) {
        this.$patch(saved);
      }
    },
  },
});
