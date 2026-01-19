<script setup lang="ts">
import { computed, watch, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { Button } from '@portal/design-system-vue';

const route = useRoute();
const isEmbedded = computed(() => window.__POWERED_BY_PORTAL_SHELL__ === true);

// 다크모드 감지
let themeStore: any = null;

/**
 * data-theme 속성 동기화
 * - <html class="dark"> → <html data-theme="dark">
 * - [data-theme="dark"] CSS 선택자 활성화
 * - [data-service="blog"][data-theme="dark"] 서비스별 다크 테마 활성화
 */
function updateDataTheme() {
  const isDark = document.documentElement.classList.contains('dark');
  document.documentElement.setAttribute('data-theme', isDark ? 'dark' : 'light');
  console.log(`[Blog] Theme synced: data-theme="${isDark ? 'dark' : 'light'}"`);
}

onMounted(() => {
  // 🟢 Step 1: data-service="blog" 속성 설정 (CSS 선택자 활성화)
  document.documentElement.setAttribute('data-service', 'blog');
  console.log('[Blog] Set data-service="blog"');

  // 🟢 Step 2: 초기 data-theme 설정
  updateDataTheme();

  if (isEmbedded.value) {
    // ============================================
    // Embedded 모드: Portal Shell의 themeStore 연동
    // ============================================
    try {
      import('portal/themeStore').then(({ useThemeStore }) => {
        themeStore = useThemeStore();

        // 🟢 Step 3: 초기 다크모드 적용
        if (themeStore.isDark) {
          document.documentElement.classList.add('dark');
        } else {
          document.documentElement.classList.remove('dark');
        }
        updateDataTheme();  // ← data-theme 속성도 함께 업데이트

        // 🟢 Step 4: 다크모드 변경 감지 및 동기화
        watch(() => themeStore.isDark, (newVal) => {
          if (newVal) {
            document.documentElement.classList.add('dark');
          } else {
            document.documentElement.classList.remove('dark');
          }
          updateDataTheme();  // ← data-theme 속성도 함께 업데이트
          console.log(`[Blog] Theme toggled: isDark=${newVal}`);
        });

        console.log('[Blog] Portal Shell themeStore connected');
      }).catch((err) => {
        console.warn('[Blog] Failed to load portal themeStore:', err);
      });
    } catch (err) {
      console.warn('[Blog] themeStore import failed:', err);
    }
  } else {
    // ============================================
    // Standalone 모드: MutationObserver로 dark 클래스 감지
    // ============================================
    const observer = new MutationObserver((mutations) => {
      mutations.forEach((mutation) => {
        if (mutation.attributeName === 'class') {
          updateDataTheme();  // ← 클래스 변경 시 data-theme도 함께 업데이트
        }
      });
    });

    observer.observe(document.documentElement, {
      attributes: true,
      attributeFilter: ['class']
    });

    console.log('[Blog] Standalone mode: MutationObserver registered');
  }
});
</script>

<template>
  <!-- ✅ data-service="blog" 자동으로 설정됨 (JS에서) -->
  <!-- ✅ Semantic Classes 사용 (bg-bg-page) -->
  <div class="min-h-screen bg-bg-page">

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

    <!-- Embedded Mode Badge -->
    <div
        v-else
        class="bg-status-warning-bg border-b border-status-warning/20"
    >
      <div class="max-w-7xl mx-auto px-4 py-2">
        <p class="text-xs text-status-warning font-medium">
          🔗 Embedded Mode (Portal Shell)
        </p>
      </div>
    </div>

    <!-- Main Content -->
    <main :class="isEmbedded ? 'py-4' : 'py-8'">
      <router-view v-slot="{ Component }">
        <component :is="Component" v-if="Component" />

        <!-- 404 Error -->
        <div v-else class="max-w-5xl mx-auto px-6">
          <div class="bg-status-error-bg border border-status-error/20 rounded-lg p-8 text-center">
            <p class="text-xl text-status-error mb-4">
              ❌ 페이지를 찾을 수 없습니다: {{ route.path }}
            </p>
            <Button variant="primary" @click="$router.push('/')">
              홈으로 돌아가기
            </Button>
          </div>
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