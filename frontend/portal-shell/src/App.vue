<script setup lang="ts">
import { useAuthStore } from "./store/auth.ts";
import { login, logout } from "./services/authService.ts";
import { Button, Badge } from '@portal/design-system-vue';
import { useThemeStore } from "./store/theme.ts";
import { onMounted, watch} from "vue";
import ThemeToggle from "./components/ThemeToggle.vue";
import { useRoute } from "vue-router";

const authStore = useAuthStore();
const themeStore = useThemeStore();
const route = useRoute();

function updateDataTheme() {
  const isDark = document.documentElement.classList.contains('dark');
  document.documentElement.setAttribute('data-theme', isDark ? 'dark' : 'light');
  console.log(`[Portal-Shell] Theme updated: ${isDark ? 'dark' : 'light'}`);
}

/**
 * 🟢 data-service 초기화
 * 호스트 앱 경로로 이동할 때 data-service="portal"로 리셋
 */
function resetDataService() {
  // 현재 라우트가 Remote가 아닐 때만 리셋
  if (!route.meta.remoteName) {
    document.documentElement.setAttribute('data-service', 'portal');
    console.log('[Portal-Shell] Route change: Reset data-service="portal"');
    forceReflowToApplyCSSChanges();
  }
}

/**
 * 🟢 CSS 변수 강제 재계산
 * KeepAlive로 인해 Blog CSS가 <head>에 남아있을 때,
 * data-service 변경 후 CSS 변수를 다시 계산하도록 강제함
 */
function forceReflowToApplyCSSChanges() {
  // 트릭: DOM 재배치 강제 (reflow trigger)
  // 이렇게 하면 브라우저가 CSS 변수 재계산 → Tailwind 클래스 다시 적용
  const html = document.documentElement;
  const trigger = html.offsetHeight;
  void trigger; // 변수 사용 (no-op)
  console.log('[Portal-Shell] Forced CSS recalculation');
}

// 페이지 로드 시 로컬 스토리지 값 반영
onMounted(() => {
  themeStore.initialize();
  
  // 🟢 초기 data-service 설정
  resetDataService();
  
  updateDataTheme();
});

// 라우트 변경 감지: data-service 리셋
watch(() => route.path, () => {
  resetDataService();
});

// <html> 태그에 dark 클래스 토글 반영
watch(() => themeStore.isDark, (newVal) => {
  if (newVal) {
    document.documentElement.classList.add('dark');
  } else {
    document.documentElement.classList.remove('dark');
  }
  updateDataTheme();
  
  // 🟢 강제 reflow: CSS 변수 재계산
  forceReflowToApplyCSSChanges();
});

</script>

<template>
  <div class="min-h-screen flex flex-col bg-[#08090a] text-[#b4b4b4] light:bg-white light:text-gray-600 transition-colors duration-normal">
    <!-- Header - Linear style sticky header -->
    <header class="bg-[#0f1011]/80 backdrop-blur-md border-b border-[#2a2a2a] sticky top-0 z-50 light:bg-white/80 light:border-gray-200">
      <div class="max-w-7xl mx-auto px-4 py-3">
        <div class="flex items-center justify-between">
          <!-- Logo -->
          <router-link to="/" class="flex items-center gap-3 hover:opacity-80 transition-opacity">
            <div class="w-9 h-9 rounded-lg bg-[#5e6ad2] flex items-center justify-center">
              <span class="text-white font-bold text-base">P</span>
            </div>
            <span class="text-lg font-semibold hidden sm:block text-white light:text-gray-900">
              Portal Universe
            </span>
          </router-link>

          <!-- Navigation -->
          <nav class="flex items-center gap-1">
            <router-link
                to="/"
                class="px-3 py-2 rounded-md text-[#b4b4b4] hover:text-white hover:bg-[#18191b] font-medium transition-all light:text-gray-600 light:hover:text-gray-900 light:hover:bg-gray-100"
                active-class="!text-white !bg-[#18191b] light:!text-gray-900 light:!bg-gray-100"
            >
              Home
            </router-link>
            <router-link
                to="/blog"
                class="px-3 py-2 rounded-md text-[#b4b4b4] hover:text-white hover:bg-[#18191b] font-medium transition-all light:text-gray-600 light:hover:text-gray-900 light:hover:bg-gray-100"
                active-class="!text-white !bg-[#18191b] light:!text-gray-900 light:!bg-gray-100"
            >
              Blog
            </router-link>
            <router-link
                to="/shopping"
                class="px-3 py-2 rounded-md text-[#b4b4b4] hover:text-white hover:bg-[#18191b] font-medium transition-all light:text-gray-600 light:hover:text-gray-900 light:hover:bg-gray-100"
                active-class="!text-white !bg-[#18191b] light:!text-gray-900 light:!bg-gray-100"
            >
              Shopping
            </router-link>
          </nav>

          <!-- Auth Section -->
          <div class="flex items-center gap-3">
            <ThemeToggle />
            <template v-if="authStore.isAuthenticated">
              <div class="hidden md:flex items-center gap-2 px-3 py-1.5 rounded-md bg-[#18191b] border border-[#2a2a2a] light:bg-gray-100 light:border-gray-200">
                <span class="text-sm font-medium text-white light:text-gray-900">{{ authStore.displayName }}</span>
                <Badge v-if="authStore.isAdmin" variant="danger" size="sm">ADMIN</Badge>
              </div>
              <Button variant="secondary" size="sm" @click="logout">Logout</Button>
            </template>
            <template v-else>
              <div class="flex items-center gap-2">
                <router-link to="/signup">
                  <Button variant="secondary" size="sm">Sign Up</Button>
                </router-link>
                <Button variant="primary" size="sm" @click="login">Login</Button>
              </div>
            </template>
          </div>
        </div>
      </div>
    </header>

    <!-- Main Content -->
    <main class="flex-1">
      <Suspense>
        <template #default>
          <router-view v-slot="{ Component, route }">
            <!-- 🔧 FIX: keep-alive를 route.meta.keepAlive 기반으로 선택적 적용 -->
            <KeepAlive v-if="route.meta.keepAlive" :max="3">
              <component
                  :is="Component"
                  :key="route.meta.remoteName || route.name"
              />
            </KeepAlive>
            <component
                v-else
                :is="Component"
                :key="route.name"
            />
          </router-view>
        </template>
        <template #fallback>
          <div class="flex items-center justify-center min-h-[400px]">
            <div class="text-center">
              <div class="w-10 h-10 border-2 border-[#5e6ad2] border-t-transparent rounded-full animate-spin mx-auto"></div>
              <p class="mt-4 text-[#6b6b6b] text-sm font-medium light:text-gray-500">Loading...</p>
            </div>
          </div>
        </template>
      </Suspense>
    </main>

    <!-- Footer - Linear style minimal footer -->
    <footer class="bg-[#0f1011] border-t border-[#2a2a2a] py-6 mt-auto light:bg-gray-50 light:border-gray-200">
      <div class="max-w-7xl mx-auto px-4 text-center">
        <p class="text-sm text-[#6b6b6b] light:text-gray-500">© 2025 Portal Universe. All rights reserved.</p>
      </div>
    </footer>
  </div>
</template>