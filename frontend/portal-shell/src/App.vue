<script setup lang="ts">
import { useAuthStore } from "./store/auth.ts";
import { login, logout } from "./services/authService.ts";
import { Button, Badge } from '@portal/design-system';

const authStore = useAuthStore();
</script>

<template>
  <div class="min-h-screen flex flex-col bg-white">

    <!-- Header - 개선된 버전 -->
    <header class="bg-white/80 backdrop-blur-md border-b border-gray-200 sticky top-0 z-50">
      <div class="max-w-7xl mx-auto px-4 py-4">
        <div class="flex items-center justify-between">

          <!-- Logo -->
          <router-link to="/" class="flex items-center gap-3 hover:opacity-80 transition-opacity">
            <div class="w-10 h-10 rounded-lg bg-gradient-to-br from-brand-600 to-accent-600 flex items-center justify-center shadow-lg">
              <span class="text-white font-bold text-lg">P</span>
            </div>
            <span class="text-xl font-bold text-gray-900 hidden sm:block">Portal Universe</span>
          </router-link>

          <!-- Navigation -->
          <nav class="hidden md:flex items-center gap-8">
            <router-link
                to="/"
                class="text-gray-600 hover:text-brand-600 font-medium transition-colors"
            >
              Home
            </router-link>
            <router-link
                to="/blog"
                class="text-gray-600 hover:text-brand-600 font-medium transition-colors"
            >
              Blog
            </router-link>
          </nav>

          <!-- Auth Section - 명확하게 개선 -->
          <div class="flex items-center gap-3">
            <template v-if="authStore.isAuthenticated">
              <div class="hidden md:flex items-center gap-2 px-4 py-2 rounded-lg bg-brand-50 border border-brand-100">
                <span class="text-sm font-semibold text-brand-700">{{ authStore.displayName }}</span>
                <Badge v-if="authStore.isAdmin" variant="danger" size="sm">ADMIN</Badge>
              </div>
              <Button variant="secondary" size="sm" @click="logout">
                Logout
              </Button>
            </template>
            <template v-else>
              <Button variant="primary" size="sm" @click="login">
                Login
              </Button>
            </template>
          </div>
        </div>
      </div>
    </header>

    <!-- Main Content -->
    <main class="flex-1">
      <Suspense>
        <template #default>
          <router-view :key="$route.path" v-slot="{ Component }">
            <component :is="Component" />
          </router-view>
        </template>
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

    <!-- Footer -->
    <footer class="bg-gray-50 border-t border-gray-200 py-8 mt-auto">
      <div class="max-w-7xl mx-auto px-4 text-center">
        <p class="text-sm text-gray-600">© 2025 Portal Universe. All rights reserved.</p>
      </div>
    </footer>

  </div>
</template>