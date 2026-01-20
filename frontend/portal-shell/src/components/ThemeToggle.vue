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
      class="group relative flex items-center gap-2 px-3 py-2 rounded-lg
             bg-[#18191b] text-[#b4b4b4] border border-[#2a2a2a]
             hover:bg-[#2a2a2a] hover:text-white
             light:bg-gray-100 light:text-gray-600 light:border-gray-200
             light:hover:bg-gray-200 light:hover:text-gray-900
             transition-all duration-normal"
  >
    <span class="w-5 h-5 flex items-center justify-center">
      <svg v-if="themeStore.isDark" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z" />
      </svg>
      <svg v-else class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z" />
      </svg>
    </span>
    <span class="text-sm font-medium">
      {{ themeStore.isDark ? 'Dark' : 'Light' }}
    </span>
  </button>
</template>