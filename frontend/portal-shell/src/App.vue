<script setup lang="ts">
/**
 * @file App.vue
 * @description 포털 셸의 최상위 루트 컴포넌트입니다.
 * 전체적인 레이아웃(헤더, 푸터), 다크 모드, 인증 상태에 따른 UI 변화를 관리합니다.
 */
import { useAuthStore } from "./store/auth.ts";
import { login, logout } from "./services/authService.ts";
import { Button, Badge } from '@portal/design-system';
import { useThemeStore } from "./store/theme.ts";
import { onMounted, watch } from "vue";
import ThemeToggle from "./components/ThemeToggle.vue";

const authStore = useAuthStore();
const themeStore = useThemeStore();

// 컴포넌트가 마운트될 때, 로컬 스토리지에 저장된 테마 설정을 불러와 적용합니다.
onMounted(() => {
  themeStore.initialize();
});

// isDark 상태가 변경될 때마다 <html> 태그에 'dark' 클래스를 토글합니다.
watch(() => themeStore.isDark, (newVal) => {
  if (newVal) {
    document.documentElement.classList.add('dark');
  } else {
    document.documentElement.classList.remove('dark');
  }
});
</script>

<template>
  <div class="min-h-screen flex flex-col bg-white text-gray-900 dark:bg-gray-900 dark:text-gray-100 transition-colors duration-300">
    <!-- Header: 모든 페이지 상단에 고정되는 헤더 -->
    <header class="bg-white/95 dark:bg-gray-800 backdrop-blur-md border-b border-gray-200 dark:border-gray-700 sticky top-0 z-50">
      <div class="max-w-7xl mx-auto px-4 py-4">
        <div class="flex items-center justify-between">
          <!-- Logo -->
          <router-link to="/" class="flex items-center gap-3 hover:opacity-80 transition-opacity">
            <div class="w-10 h-10 rounded-lg bg-gradient-to-br from-brand-600 to-accent-600 flex items-center justify-center shadow-lg">
              <span class="text-white font-bold text-lg">P</span>
            </div>
            <span class="text-xl font-bold hidden sm:block" :class="{'text-gray-900': !themeStore.isDark, 'text-gray-100': themeStore.isDark}">
              Portal Universe
            </span>
          </router-link>

          <!-- Navigation Menu -->
          <nav class="flex items-center gap-8">
            <router-link
                to="/"
                class="text-gray-600 dark:text-gray-300 hover:text-brand-600 font-medium transition-colors"
                active-class="text-brand-600 font-bold"
            >
              Home
            </router-link>
            <router-link
                to="/blog"
                class="text-gray-600 dark:text-gray-300 hover:text-brand-600 font-medium transition-colors"
                active-class="text-brand-600 font-bold"
            >
              Blog
            </router-link>
          </nav>

          <!-- Auth Section: 인증 상태에 따라 다른 UI를 표시 -->
          <div class="flex items-center gap-3">
            <ThemeToggle />
            <template v-if="authStore.isAuthenticated">
              <div class="hidden md:flex items-center gap-2 px-4 py-2 rounded-lg bg-brand-50 dark:bg-brand-700 border border-brand-100 dark:border-brand-700">
                <span class="text-sm font-semibold text-brand-700 dark:text-brand-300">{{ authStore.displayName }}</span>
                <Badge v-if="authStore.isAdmin" variant="danger" size="sm">ADMIN</Badge>
              </div>
              <Button variant="secondary" size="sm" @click="logout">Logout</Button>
            </template>
            <template v-else>
              <Button variant="primary" size="sm" @click="login">Login</Button>
            </template>
          </div>
        </div>
      </div>
    </header>

    <!-- Main Content: 라우터 뷰가 렌더링되는 영역 -->
    <main class="flex-1">
      <!-- Suspense: 비동기 컴포넌트(Remote 앱 등) 로딩을 처리 -->
      <Suspense>
        <template #default>
          <router-view :key="$route.path" v-slot="{ Component }">
            <component :is="Component" />
          </router-view>
        </template>
        <!-- Fallback: 비동기 컴포넌트가 로드되는 동안 보여줄 UI -->
        <template #fallback>
          <div class="flex items-center justify-center min-h-[400px]">
            <div class="text-center">
              <div class="w-12 h-12 border-4 border-brand-600 border-t-transparent rounded-full animate-spin mx-auto"></div>
              <p class="mt-4 text-gray-600 font-medium">Loading...</p>
            </div>
          </div>
        </template>
      </Suspense>
    </main>

    <!-- Footer: 모든 페이지 하단에 표시되는 푸터 -->
    <footer class="bg-gray-50 dark:bg-gray-800 dark:text-gray-100 border-t border-gray-200 dark:border-gray-700 py-8 mt-auto">
      <div class="max-w-7xl mx-auto px-4 text-center">
        <p class="text-sm">© 2025 Portal Universe. All rights reserved.</p>
      </div>
    </footer>
  </div>
</template>
