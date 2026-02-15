<script setup lang="ts">
import { computed, watch, onMounted, onActivated } from 'vue';
import { usePortalTheme, isEmbedded as checkEmbedded } from '@portal/vue-bridge';

const isEmbedded = computed(() => checkEmbedded());

function updateDataTheme() {
  const isDark = document.documentElement.classList.contains('dark');
  document.documentElement.setAttribute('data-theme', isDark ? 'dark' : 'light');
}

function applyDarkClass(isDark: boolean) {
  document.documentElement.classList.toggle('dark', isDark);
  updateDataTheme();
}

onMounted(() => {
  document.documentElement.setAttribute('data-service', 'admin');
  updateDataTheme();

  if (isEmbedded.value) {
    // Embedded 모드: Portal Shell의 themeAdapter 연동 (via vue-bridge)
    const { isDark } = usePortalTheme();

    applyDarkClass(isDark.value);

    watch(isDark, (newVal) => {
      applyDarkClass(newVal);
    });
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
