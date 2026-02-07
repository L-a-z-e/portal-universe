<script setup lang="ts">
import { computed, watch, onMounted, onActivated } from 'vue';

const isEmbedded = computed(() => window.__POWERED_BY_PORTAL_SHELL__ === true);

function updateDataTheme() {
  const isDark = document.documentElement.classList.contains('dark');
  document.documentElement.setAttribute('data-theme', isDark ? 'dark' : 'light');
}

onMounted(() => {
  document.documentElement.setAttribute('data-service', 'admin');
  updateDataTheme();

  if (isEmbedded.value) {
    import('portal/stores').then(({ useThemeStore }) => {
      const store = useThemeStore();
      if (store.isDark) {
        document.documentElement.classList.add('dark');
      } else {
        document.documentElement.classList.remove('dark');
      }
      updateDataTheme();

      watch(() => store.isDark, (newVal) => {
        if (newVal) {
          document.documentElement.classList.add('dark');
        } else {
          document.documentElement.classList.remove('dark');
        }
        updateDataTheme();
      });
    }).catch(() => {});
  } else {
    const observer = new MutationObserver((mutations) => {
      mutations.forEach((mutation) => {
        if (mutation.attributeName === 'class') {
          updateDataTheme();
        }
      });
    });
    observer.observe(document.documentElement, {
      attributes: true,
      attributeFilter: ['class']
    });
  }
});

onActivated(() => {
  document.documentElement.setAttribute('data-service', 'admin');
  updateDataTheme();
});
</script>

<template>
  <div class="min-h-screen bg-bg-page">
    <router-view />
  </div>
</template>
