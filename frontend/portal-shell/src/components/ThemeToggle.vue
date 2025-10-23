<script setup lang="ts">
import { useThemeStore } from '../store/theme';

const themeStore = useThemeStore();

function toggleTheme() {
themeStore.toggle();
}
</script>

<template>
  <button
      @click="toggleTheme"
      :aria-label="themeStore.isDark ? 'Switch to Light Mode' : 'Switch to Dark Mode'"
      class="p-2 rounded bg-gray-200 dark:bg-gray-700 text-gray-900 dark:text-gray-100 hover:ring-2 ring-brand-500 transition"
  >
    {{ themeStore.isDark ? '🌙 Dark' : '☀️ Light' }}
  </button>
</template>