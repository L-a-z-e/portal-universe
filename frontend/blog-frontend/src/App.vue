<script setup lang="ts">
import { computed, watch, onMounted, onActivated } from 'vue';
import { useRoute } from 'vue-router';
import { Alert, Button, ToastContainer } from '@portal/design-vue';
import { usePortalTheme, isEmbedded as checkEmbedded } from '@portal/vue-bridge';

const route = useRoute();
const isEmbedded = computed(() => checkEmbedded());

/**
 * data-theme 속성 동기화
 * - <html class="dark"> → <html data-theme="dark">
 * - [data-theme="dark"] CSS 선택자 활성화
 * - [data-service="blog"][data-theme="dark"] 서비스별 다크 테마 활성화
 */
function updateDataTheme() {
  const isDark = document.documentElement.classList.contains('dark');
  document.documentElement.setAttribute('data-theme', isDark ? 'dark' : 'light');
}

function applyDarkClass(isDark: boolean) {
  document.documentElement.classList.toggle('dark', isDark);
  updateDataTheme();
}

onMounted(() => {
  // Step 1: data-service="blog" 속성 설정 (CSS 선택자 활성화)
  document.documentElement.setAttribute('data-service', 'blog');

  // Step 2: 초기 data-theme 설정
  updateDataTheme();

  if (isEmbedded.value) {
    // Embedded 모드: Portal Shell의 themeAdapter 연동 (via vue-bridge)
    const { isDark } = usePortalTheme();

    // Step 3: 초기 다크모드 적용
    applyDarkClass(isDark.value);

    // Step 4: 다크모드 변경 감지 및 동기화
    watch(isDark, (newVal) => {
      applyDarkClass(newVal);
    });
  } else {
    // Standalone 모드: MutationObserver로 dark 클래스 감지
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

/**
 * KeepAlive 재활성화 시 data-service 복원
 * Shopping → Blog 전환 시 data-service="shopping"이 유지되는 문제 해결
 */
onActivated(() => {
  document.documentElement.setAttribute('data-service', 'blog');
  updateDataTheme();
});
</script>

<template>
  <!-- ✅ data-service="blog" 자동으로 설정됨 (JS에서) -->
  <!-- ✅ Semantic Classes 사용 (bg-bg-page) -->
  <div class="min-h-screen bg-bg-page">
    <ToastContainer />

    <!-- Header (Standalone 모드에서만 표시) -->
    <header
        v-if="!isEmbedded"
        class="bg-bg-card border-b border-border-default sticky top-0 z-50"
    >
      <div class="max-w-7xl mx-auto px-4 py-4">
        <div class="flex items-center justify-between">
          <!-- Logo -->
          <router-link to="/" class="flex items-center gap-3 hover:opacity-80 transition-opacity">
            <div class="w-10 h-10 rounded-lg bg-gradient-to-br from-brand-primary to-brand-secondary flex items-center justify-center shadow-lg">
              <span class="text-white font-bold text-lg">B</span>
            </div>
            <span class="text-xl font-bold text-text-heading">Blog</span>
          </router-link>

          <!-- Nav -->
          <nav class="flex items-center gap-6">
            <router-link
                to="/"
                class="text-text-body hover:text-brand-primary font-medium transition-colors"
                active-class="text-brand-primary font-bold"
            >
              📄 Posts
            </router-link>
            <router-link
                to="/my?tab=series"
                class="text-text-body hover:text-brand-primary font-medium transition-colors"
            >
              📚 Series
            </router-link>
            <router-link
                to="/write"
                class="text-text-body hover:text-brand-primary font-medium transition-colors"
                active-class="text-brand-primary font-bold"
            >
              ✍️ Write
            </router-link>
          </nav>

          <!-- Mode Badge (Standalone) -->
          <div class="px-3 py-1 bg-status-success-bg text-status-success text-sm font-medium rounded-full border border-status-success/20">
            📦 Standalone
          </div>
        </div>
      </div>
    </header>

    <!-- Main Content -->
    <main :class="isEmbedded ? 'py-4' : 'py-8'">
      <router-view v-slot="{ Component }">
        <component :is="Component" v-if="Component" />

        <!-- 404 Error -->
        <div v-else class="max-w-5xl mx-auto px-6">
          <Alert variant="error" class="text-center">
            <p class="text-xl mb-4">
              페이지를 찾을 수 없습니다: {{ route.path }}
            </p>
            <Button variant="primary" @click="$router.push('/')">
              홈으로 돌아가기
            </Button>
          </Alert>
        </div>
      </router-view>
    </main>

    <!-- Footer (Standalone 모드에서만) -->
    <footer
        v-if="!isEmbedded"
        class="bg-bg-card border-t border-border-default mt-auto"
    >
      <div class="max-w-7xl mx-auto px-4 py-6 text-center">
        <p class="text-sm text-text-meta">
          © 2025 Portal Universe Blog. All rights reserved.
        </p>
      </div>
    </footer>
  </div>
</template>