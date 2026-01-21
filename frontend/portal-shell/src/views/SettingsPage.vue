<script setup lang="ts">
import { ref } from 'vue';
import { useThemeStore, type ThemeMode } from '../store/theme';
import { useSettingsStore, type Language } from '../store/settings';
import { useAuthStore } from '../store/auth';

const themeStore = useThemeStore();
const settingsStore = useSettingsStore();
const authStore = useAuthStore();

// Theme options
const themeOptions: { value: ThemeMode; label: string; icon: string }[] = [
  { value: 'dark', label: 'Dark', icon: '🌙' },
  { value: 'light', label: 'Light', icon: '☀️' },
  { value: 'system', label: 'System', icon: '💻' },
];

// Language options
const languageOptions: { value: Language; label: string; flag: string }[] = [
  { value: 'ko', label: '한국어', flag: '🇰🇷' },
  { value: 'en', label: 'English', flag: '🇺🇸' },
];

// Save feedback
const showSaved = ref(false);
const showSavedMessage = () => {
  showSaved.value = true;
  setTimeout(() => {
    showSaved.value = false;
  }, 2000);
};

// Handlers
const handleThemeChange = (mode: ThemeMode) => {
  themeStore.setMode(mode);
  showSavedMessage();
};

const handleLanguageChange = (lang: Language) => {
  settingsStore.setLanguage(lang);
  showSavedMessage();
};

const handleNotificationChange = (key: 'email' | 'push' | 'marketing', value: boolean) => {
  settingsStore.setNotification(key, value);
  showSavedMessage();
};

const handleCompactModeChange = (value: boolean) => {
  settingsStore.setCompactMode(value);
  showSavedMessage();
};

const handleResetSettings = () => {
  if (confirm('모든 설정을 기본값으로 초기화하시겠습니까?')) {
    settingsStore.resetToDefaults();
    themeStore.setMode('dark');
    showSavedMessage();
  }
};
</script>

<template>
  <div class="max-w-3xl mx-auto px-4 py-8">
    <!-- Header -->
    <div class="mb-8">
      <h1 class="text-2xl font-bold text-text-heading mb-2">Settings</h1>
      <p class="text-text-meta">Customize your Portal Universe experience</p>
    </div>

    <!-- Save Indicator -->
    <Transition name="fade">
      <div
        v-if="showSaved"
        class="fixed top-20 right-4 bg-status-success text-white px-4 py-2 rounded-lg shadow-lg z-50 flex items-center gap-2"
      >
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
        </svg>
        <span>Saved</span>
      </div>
    </Transition>

    <!-- Settings Sections -->
    <div class="space-y-6">
      <!-- Appearance Section -->
      <section class="bg-bg-card rounded-xl border border-border-default p-6">
        <h2 class="text-lg font-semibold text-text-heading mb-4 flex items-center gap-2">
          <span>🎨</span>
          <span>Appearance</span>
        </h2>

        <!-- Theme -->
        <div class="mb-6">
          <label class="block text-sm font-medium text-text-body mb-3">Theme</label>
          <div class="grid grid-cols-3 gap-3">
            <button
              v-for="option in themeOptions"
              :key="option.value"
              @click="handleThemeChange(option.value)"
              :class="[
                'flex flex-col items-center gap-2 p-4 rounded-lg border-2 transition-all',
                themeStore.mode === option.value
                  ? 'border-brand-primary bg-brand-primary/10'
                  : 'border-border-default hover:border-border-hover bg-bg-elevated'
              ]"
            >
              <span class="text-2xl">{{ option.icon }}</span>
              <span class="text-sm font-medium" :class="themeStore.mode === option.value ? 'text-brand-primary' : 'text-text-body'">
                {{ option.label }}
              </span>
            </button>
          </div>
        </div>

        <!-- Compact Mode -->
        <div class="flex items-center justify-between py-3 border-t border-border-default">
          <div>
            <p class="font-medium text-text-body">Compact Mode</p>
            <p class="text-sm text-text-meta">Reduce spacing and padding</p>
          </div>
          <button
            @click="handleCompactModeChange(!settingsStore.compactMode)"
            :class="[
              'relative w-12 h-6 rounded-full transition-colors',
              settingsStore.compactMode ? 'bg-brand-primary' : 'bg-border-default'
            ]"
          >
            <span
              :class="[
                'absolute top-0.5 left-0.5 w-5 h-5 bg-white rounded-full transition-transform shadow-sm',
                settingsStore.compactMode ? 'translate-x-6' : 'translate-x-0'
              ]"
            />
          </button>
        </div>
      </section>

      <!-- Language Section -->
      <section class="bg-bg-card rounded-xl border border-border-default p-6">
        <h2 class="text-lg font-semibold text-text-heading mb-4 flex items-center gap-2">
          <span>🌐</span>
          <span>Language</span>
        </h2>

        <div class="grid grid-cols-2 gap-3">
          <button
            v-for="option in languageOptions"
            :key="option.value"
            @click="handleLanguageChange(option.value)"
            :class="[
              'flex items-center gap-3 p-4 rounded-lg border-2 transition-all',
              settingsStore.language === option.value
                ? 'border-brand-primary bg-brand-primary/10'
                : 'border-border-default hover:border-border-hover bg-bg-elevated'
            ]"
          >
            <span class="text-2xl">{{ option.flag }}</span>
            <span class="font-medium" :class="settingsStore.language === option.value ? 'text-brand-primary' : 'text-text-body'">
              {{ option.label }}
            </span>
          </button>
        </div>
      </section>

      <!-- Notifications Section -->
      <section class="bg-bg-card rounded-xl border border-border-default p-6">
        <h2 class="text-lg font-semibold text-text-heading mb-4 flex items-center gap-2">
          <span>🔔</span>
          <span>Notifications</span>
        </h2>

        <div class="space-y-1">
          <!-- Email Notifications -->
          <div class="flex items-center justify-between py-3">
            <div>
              <p class="font-medium text-text-body">Email Notifications</p>
              <p class="text-sm text-text-meta">Receive important updates via email</p>
            </div>
            <button
              @click="handleNotificationChange('email', !settingsStore.notifications.email)"
              :class="[
                'relative w-12 h-6 rounded-full transition-colors',
                settingsStore.notifications.email ? 'bg-brand-primary' : 'bg-border-default'
              ]"
            >
              <span
                :class="[
                  'absolute top-0.5 left-0.5 w-5 h-5 bg-white rounded-full transition-transform shadow-sm',
                  settingsStore.notifications.email ? 'translate-x-6' : 'translate-x-0'
                ]"
              />
            </button>
          </div>

          <!-- Push Notifications -->
          <div class="flex items-center justify-between py-3 border-t border-border-default">
            <div>
              <p class="font-medium text-text-body">Push Notifications</p>
              <p class="text-sm text-text-meta">Get real-time updates in your browser</p>
            </div>
            <button
              @click="handleNotificationChange('push', !settingsStore.notifications.push)"
              :class="[
                'relative w-12 h-6 rounded-full transition-colors',
                settingsStore.notifications.push ? 'bg-brand-primary' : 'bg-border-default'
              ]"
            >
              <span
                :class="[
                  'absolute top-0.5 left-0.5 w-5 h-5 bg-white rounded-full transition-transform shadow-sm',
                  settingsStore.notifications.push ? 'translate-x-6' : 'translate-x-0'
                ]"
              />
            </button>
          </div>

          <!-- Marketing Notifications -->
          <div class="flex items-center justify-between py-3 border-t border-border-default">
            <div>
              <p class="font-medium text-text-body">Marketing Emails</p>
              <p class="text-sm text-text-meta">News, promotions, and product updates</p>
            </div>
            <button
              @click="handleNotificationChange('marketing', !settingsStore.notifications.marketing)"
              :class="[
                'relative w-12 h-6 rounded-full transition-colors',
                settingsStore.notifications.marketing ? 'bg-brand-primary' : 'bg-border-default'
              ]"
            >
              <span
                :class="[
                  'absolute top-0.5 left-0.5 w-5 h-5 bg-white rounded-full transition-transform shadow-sm',
                  settingsStore.notifications.marketing ? 'translate-x-6' : 'translate-x-0'
                ]"
              />
            </button>
          </div>
        </div>
      </section>

      <!-- Account Section (Only for authenticated users) -->
      <section v-if="authStore.isAuthenticated" class="bg-bg-card rounded-xl border border-border-default p-6">
        <h2 class="text-lg font-semibold text-text-heading mb-4 flex items-center gap-2">
          <span>👤</span>
          <span>Account</span>
        </h2>

        <div class="space-y-3">
          <router-link
            to="/profile"
            class="flex items-center justify-between p-4 rounded-lg bg-bg-elevated hover:bg-bg-elevated/80 transition-colors group"
          >
            <div class="flex items-center gap-3">
              <div class="w-10 h-10 rounded-full bg-brand-primary/20 flex items-center justify-center">
                <span class="text-brand-primary font-medium">
                  {{ authStore.displayName?.charAt(0)?.toUpperCase() || 'U' }}
                </span>
              </div>
              <div>
                <p class="font-medium text-text-body">{{ authStore.displayName }}</p>
                <p class="text-sm text-text-meta">View and edit your profile</p>
              </div>
            </div>
            <svg class="w-5 h-5 text-text-meta group-hover:text-text-body transition-colors" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
            </svg>
          </router-link>
        </div>
      </section>

      <!-- Danger Zone -->
      <section class="bg-bg-card rounded-xl border border-status-error/30 p-6">
        <h2 class="text-lg font-semibold text-status-error mb-4 flex items-center gap-2">
          <span>⚠️</span>
          <span>Reset Settings</span>
        </h2>

        <p class="text-text-meta mb-4">
          This will reset all settings to their default values. This action cannot be undone.
        </p>

        <button
          @click="handleResetSettings"
          class="px-4 py-2 rounded-lg border border-status-error text-status-error hover:bg-status-error hover:text-white transition-colors"
        >
          Reset to Defaults
        </button>
      </section>
    </div>
  </div>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
