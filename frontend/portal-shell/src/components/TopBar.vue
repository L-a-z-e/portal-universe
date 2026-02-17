<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { NotificationBell } from './notification'
import MaterialIcon from './MaterialIcon.vue'

const emit = defineEmits<{
  (e: 'open-search'): void
}>()

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const breadcrumbs = computed(() => {
  const matched = route.matched
    .filter(r => r.meta.title || r.name)
    .map(r => ({
      title: (r.meta.title as string) || String(r.name || ''),
      path: r.path,
    }))

  if (matched.length === 0) {
    const name = route.meta.remoteName as string
    if (name) {
      return [{ title: name.charAt(0).toUpperCase() + name.slice(1), path: route.path }]
    }
    return [{ title: 'Home', path: '/' }]
  }

  return matched
})

const userInitial = computed(() =>
  authStore.displayName?.charAt(0)?.toUpperCase() || 'U'
)

const handleLogin = () => {
  authStore.requestLogin()
}
</script>

<template>
  <header class="h-14 flex items-center justify-between px-6 bg-bg-card/80 backdrop-blur-md border-b border-border-default sticky top-0 z-30">
    <!-- Left: Breadcrumbs -->
    <nav class="flex items-center gap-1.5 text-sm">
      <template v-for="(crumb, idx) in breadcrumbs" :key="crumb.path">
        <MaterialIcon v-if="idx === 0 && breadcrumbs.length === 1 && crumb.title === 'Home'" name="home" :size="18" class="text-text-meta mr-1" />
        <span v-if="idx > 0" class="text-text-muted">/</span>
        <router-link
          v-if="idx < breadcrumbs.length - 1"
          :to="crumb.path"
          class="text-text-meta hover:text-text-heading transition-colors"
        >
          {{ crumb.title }}
        </router-link>
        <span v-else class="text-text-heading font-medium">{{ crumb.title }}</span>
      </template>
    </nav>

    <!-- Right: Actions -->
    <div class="flex items-center gap-3">
      <!-- Search trigger -->
      <button
        @click="emit('open-search')"
        class="flex items-center gap-2 px-3 py-1.5 rounded-lg text-sm text-text-meta bg-bg-muted border border-border-default hover:border-border-hover hover:text-text-body transition-colors"
      >
        <MaterialIcon name="search" :size="16" />
        <span class="hidden sm:inline">Search</span>
        <kbd class="hidden sm:inline-flex items-center gap-0.5 ml-2 px-1.5 py-0.5 text-[10px] bg-bg-card border border-border-default rounded font-mono">
          <span>&#8984;</span>K
        </kbd>
      </button>

      <!-- Notification Bell -->
      <NotificationBell v-if="authStore.isAuthenticated" dropdown-direction="right" />

      <!-- User Avatar / Login -->
      <template v-if="authStore.isAuthenticated">
        <button
          @click="router.push('/profile')"
          class="w-8 h-8 rounded-full bg-gradient-to-br from-nightfall-300 to-nightfall-500 flex items-center justify-center text-white text-xs font-semibold ring-2 ring-transparent hover:ring-nightfall-300/30 transition-all cursor-pointer"
        >
          {{ userInitial }}
        </button>
      </template>
      <template v-else>
        <button
          @click="handleLogin"
          class="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-medium bg-brand-primary text-white hover:bg-brand-primaryHover transition-colors"
        >
          <MaterialIcon name="login" :size="16" />
          Login
        </button>
      </template>
    </div>
  </header>
</template>
