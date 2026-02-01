<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { Button, Badge } from '@portal/design-system-vue'
import { getRemoteConfigs } from '../config/remoteRegistry'

const router = useRouter()
const authStore = useAuthStore()

// Get available services from remote registry
const services = computed(() => {
  const configs = getRemoteConfigs()
  return configs.map(config => ({
    id: config.key,
    name: config.name,
    icon: config.icon || '📦',
    description: config.description || '',
    path: config.basePath,
    isActive: true
  }))
})

// Quick actions for the dashboard
const quickActions = [
  { id: 'new-post', label: '새 글 작성', icon: '✏️', path: '/blog/write', shortcut: 'N' },
  { id: 'browse-products', label: '상품 둘러보기', icon: '🛍️', path: '/shopping', shortcut: 'S' },
  { id: 'my-orders', label: '주문 내역', icon: '📦', path: '/shopping/orders', shortcut: 'O' },
]

// Recent activity (mock data - will be replaced with real API)
const recentActivity = ref([
  { id: 1, type: 'blog', action: '글 작성', title: 'Vue 3 Composition API 가이드', time: '2시간 전', icon: '📝' },
  { id: 2, type: 'shopping', action: '주문 완료', title: '상품 3개 주문', time: '1일 전', icon: '🛒' },
  { id: 3, type: 'blog', action: '댓글 작성', title: 'React vs Vue 토론', time: '2일 전', icon: '💬' },
])

// Stats summary
const stats = computed(() => [
  { label: '작성한 글', value: 12, icon: '📄', change: '+3' },
  { label: '주문 건수', value: 5, icon: '📦', change: '+1' },
  { label: '받은 좋아요', value: 48, icon: '❤️', change: '+12' },
])

// Current time greeting
const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 12) return '좋은 아침이에요'
  if (hour < 18) return '좋은 오후에요'
  return '좋은 저녁이에요'
})

function navigateTo(path: string) {
  router.push(path)
}
</script>

<template>
  <div class="bg-bg-page text-text-body">
    <!-- Dashboard Header -->
    <header class="border-b border-border-default bg-bg-card/50 backdrop-blur-sm">
      <div class="max-w-7xl mx-auto px-4 py-4 sm:py-6">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-text-meta text-sm">{{ greeting }},</p>
            <h1 class="text-2xl font-bold text-text-heading leading-tight">
              {{ authStore.displayName }}
            </h1>
          </div>
          <div class="flex items-center gap-3">
            <Button variant="secondary" size="sm" @click="navigateTo('/blog/write')">
              <span class="mr-2">✏️</span>
              새 글 작성
            </Button>
          </div>
        </div>
      </div>
    </header>

    <main class="max-w-7xl mx-auto px-4 py-8">
      <!-- Stats Overview -->
      <section class="grid grid-cols-1 md:grid-cols-3 gap-4 mb-8">
        <div
          v-for="stat in stats"
          :key="stat.label"
          class="bg-bg-card border border-border-default rounded-xl p-5 hover:border-brand-primary/30 transition-colors"
        >
          <div class="flex items-center justify-between mb-2">
            <span class="text-2xl">{{ stat.icon }}</span>
            <Badge variant="success" size="sm">{{ stat.change }}</Badge>
          </div>
          <p class="text-2xl font-bold text-text-heading">{{ stat.value }}</p>
          <p class="text-sm text-text-meta">{{ stat.label }}</p>
        </div>
      </section>

      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <!-- Quick Actions -->
        <section class="lg:col-span-1">
          <div class="bg-bg-card border border-border-default rounded-xl p-5">
            <h2 class="text-lg font-semibold text-text-heading mb-4 flex items-center gap-2">
              <span>⚡</span>
              빠른 작업
            </h2>
            <div class="space-y-2">
              <Button
                v-for="action in quickActions"
                :key="action.id"
                variant="ghost"
                @click="navigateTo(action.path)"
                class="w-full flex items-center justify-between p-3"
              >
                <span class="flex items-center gap-3">
                  <span class="text-xl">{{ action.icon }}</span>
                  <span>{{ action.label }}</span>
                </span>
                <kbd class="hidden sm:inline-block px-2 py-1 text-xs bg-bg-card border border-border-default rounded text-text-meta">
                  {{ action.shortcut }}
                </kbd>
              </Button>
            </div>
          </div>
        </section>

        <!-- Recent Activity -->
        <section class="lg:col-span-2">
          <div class="bg-bg-card border border-border-default rounded-xl p-5">
            <div class="flex items-center justify-between mb-4">
              <h2 class="text-lg font-semibold text-text-heading flex items-center gap-2">
                <span>📊</span>
                최근 활동
              </h2>
              <Button variant="ghost" size="sm">전체 보기</Button>
            </div>
            <div class="space-y-3">
              <div
                v-for="activity in recentActivity"
                :key="activity.id"
                class="flex items-center gap-4 p-3 rounded-lg hover:bg-bg-elevated transition-colors cursor-pointer"
              >
                <div class="w-10 h-10 rounded-full bg-brand-primary/10 flex items-center justify-center text-lg">
                  {{ activity.icon }}
                </div>
                <div class="flex-1 min-w-0">
                  <p class="text-text-heading font-medium truncate">{{ activity.title }}</p>
                  <p class="text-sm text-text-meta">{{ activity.action }}</p>
                </div>
                <span class="text-xs text-text-meta whitespace-nowrap">{{ activity.time }}</span>
              </div>
            </div>
          </div>
        </section>
      </div>

      <!-- Services Grid -->
      <section class="mt-8">
        <h2 class="text-lg font-semibold text-text-heading mb-4 flex items-center gap-2">
          <span>🚀</span>
          서비스
        </h2>
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          <div
            v-for="service in services"
            :key="service.id"
            @click="navigateTo(service.path)"
            class="group relative overflow-hidden rounded-xl p-6 bg-bg-card border border-border-default hover:border-brand-primary/50 transition-all cursor-pointer"
          >
            <div class="relative z-10">
              <div class="text-4xl mb-4">{{ service.icon }}</div>
              <h3 class="text-lg font-semibold text-text-heading mb-1">{{ service.name }}</h3>
              <p class="text-text-meta text-sm">{{ service.description }}</p>
            </div>
            <div class="absolute inset-0 bg-brand-primary/5 opacity-0 group-hover:opacity-100 transition-opacity"></div>
            <div class="absolute top-4 right-4 opacity-0 group-hover:opacity-100 transition-opacity">
              <span class="text-brand-primary">→</span>
            </div>
          </div>
        </div>
      </section>

      <!-- Keyboard Shortcut Hint -->
      <div class="mt-8 text-center">
        <p class="text-sm text-text-meta">
          <kbd class="px-2 py-1 bg-bg-card border border-border-default rounded text-xs mr-1">⌘</kbd>
          <kbd class="px-2 py-1 bg-bg-card border border-border-default rounded text-xs mr-2">K</kbd>
          를 눌러 빠른 검색을 사용하세요
        </p>
      </div>
    </main>
  </div>
</template>
