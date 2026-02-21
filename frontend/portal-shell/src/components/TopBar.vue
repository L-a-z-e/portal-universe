<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { NotificationBell } from './notification'
import MaterialIcon from './MaterialIcon.vue'
import { Dropdown, Button } from '@portal/design-vue'
import type { DropdownItem } from '@portal/design-vue'

const isMac = navigator.platform.toUpperCase().indexOf('MAC') >= 0

const emit = defineEmits<{
  (e: 'open-search'): void
}>()

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const userMenuItems: DropdownItem[] = [
  { label: 'My Profile', value: 'profile', icon: 'person' },
  { label: '', divider: true },
  { label: 'Logout', value: 'logout', icon: 'logout' },
]

const handleUserMenuSelect = async (item: DropdownItem) => {
  if (item.value === 'profile') {
    router.push('/profile')
  } else if (item.value === 'logout') {
    await authStore.logout()
    router.push('/')
  }
}

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
          {{ isMac ? '⌘' : 'Ctrl+' }}K
        </kbd>
      </button>

      <!-- Notification Bell -->
      <NotificationBell v-if="authStore.isAuthenticated" dropdown-direction="left" />

      <!-- User Avatar / Login -->
      <template v-if="authStore.isAuthenticated">
        <Dropdown
          :items="userMenuItems"
          placement="bottom-end"
          @select="handleUserMenuSelect"
        >
          <template #trigger>
            <button
              class="w-8 h-8 rounded-full bg-gradient-to-br from-nightfall-300 to-nightfall-500 flex items-center justify-center text-white text-xs font-semibold ring-2 ring-transparent hover:ring-nightfall-300/30 transition-all cursor-pointer"
            >
              {{ userInitial }}
            </button>
          </template>
          <template #item="{ item }">
            <span :class="['flex items-center gap-2.5', item.value === 'logout' && 'text-red-500']">
              <MaterialIcon :name="item.icon ?? ''" :size="18" />
              {{ item.label }}
            </span>
          </template>
        </Dropdown>
      </template>
      <template v-else>
        <Button variant="primary" size="sm" @click="handleLogin">
          <MaterialIcon name="login" :size="16" />
          Login
        </Button>
      </template>
    </div>
  </header>
</template>
