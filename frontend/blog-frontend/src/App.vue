<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import { Button } from '@portal/design-system';

const route = useRoute();
const isEmbedded = computed(() => window.__POWERED_BY_PORTAL_SHELL__ === true);
</script>

<template>
  <div class="min-h-screen bg-gray-50">
    <!-- Header (Embedded 모드에서만 표시) -->
    <header v-if="!isEmbedded" class="bg-white border-b border-gray-200 sticky top-0 z-50">
      <div class="max-w-7xl mx-auto px-4 py-4">
        <div class="flex items-center justify-between">
          <!-- Logo -->
          <router-link to="/" class="flex items-center gap-3 hover:opacity-80 transition-opacity">
            <div class="w-10 h-10 rounded-lg bg-gradient-to-br from-brand-600 to-accent-600 flex items-center justify-center shadow-lg">
              <span class="text-white font-bold text-lg">B</span>
            </div>
            <span class="text-xl font-bold text-gray-900">Blog</span>
          </router-link>

          <!-- Nav -->
          <nav class="flex items-center gap-6">
            <router-link
                to="/"
                class="text-gray-600 hover:text-brand-600 font-medium transition-colors"
                active-class="text-brand-600 font-bold"
            >
              📄 Posts
            </router-link>
            <router-link
                to="/write"
                class="text-gray-600 hover:text-brand-600 font-medium transition-colors"
                active-class="text-brand-600 font-bold"
            >
              ✍️ Write
            </router-link>
          </nav>

          <!-- Mode Badge (Standalone) -->
          <div class="px-3 py-1 bg-green-50 text-green-600 text-sm font-medium rounded-full">
            📦 Standalone
          </div>
        </div>
      </div>
    </header>

    <!-- Embedded Mode Badge (작은 표시) -->
    <div v-else class="bg-orange-50 border-b border-orange-200">
      <div class="max-w-7xl mx-auto px-4 py-2">
        <p class="text-xs text-orange-600 font-medium">
          🔗 Embedded Mode (Portal Shell)
        </p>
      </div>
    </div>

    <!-- Main Content -->
    <main :class="isEmbedded ? 'py-4' : 'py-8'">
      <router-view v-slot="{ Component }">
        <component :is="Component" v-if="Component" />
        <div v-else class="max-w-5xl mx-auto px-6">
          <div class="bg-yellow-50 border border-yellow-200 rounded-lg p-8 text-center">
            <p class="text-xl text-yellow-800 mb-4">❌ 페이지를 찾을 수 없습니다: {{ route.path }}</p>
            <Button variant="primary" @click="$router.push('/')">
              홈으로 돌아가기
            </Button>
          </div>
        </div>
      </router-view>
    </main>

    <!-- Footer (Standalone 모드에서만) -->
    <footer v-if="!isEmbedded" class="bg-white border-t border-gray-200 mt-auto">
      <div class="max-w-7xl mx-auto px-4 py-6 text-center">
        <p class="text-sm text-gray-600">© 2025 Portal Universe Blog. All rights reserved.</p>
      </div>
    </footer>
  </div>
</template>