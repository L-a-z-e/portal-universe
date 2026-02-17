<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { useDashboard } from '../composables/useDashboard'
import { Button, Badge } from '@portal/design-vue'
import { getRemoteConfigs } from '../config/remoteRegistry'
import { formatRelativeTime } from '../utils/dateUtils'

const router = useRouter()
const authStore = useAuthStore()

// Dashboard composable - 실제 데이터 사용
const {
  stats,
  activities,
  loading,
  errors,
  fetchAll
} = useDashboard()

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
          <!-- 로딩 상태 -->
          <template v-if="stat.loading">
            <div class="animate-pulse">
              <div class="flex items-center justify-between mb-2">
                <div class="h-8 w-8 bg-bg-elevated rounded"></div>
                <div class="h-5 w-10 bg-bg-elevated rounded"></div>
              </div>
              <div class="h-8 w-16 bg-bg-elevated rounded mb-1"></div>
              <div class="h-4 w-20 bg-bg-elevated rounded"></div>
            </div>
          </template>

          <!-- 에러 상태 -->
          <template v-else-if="stat.error">
            <div class="flex items-center justify-between mb-2">
              <span class="text-2xl">{{ stat.icon }}</span>
              <Badge variant="danger" size="sm">에러</Badge>
            </div>
            <p class="text-2xl font-bold text-text-meta">--</p>
            <p class="text-sm text-text-meta">{{ stat.label }}</p>
          </template>

          <!-- 정상 상태 -->
          <template v-else>
            <div class="flex items-center justify-between mb-2">
              <span class="text-2xl">{{ stat.icon }}</span>
              <Badge v-if="stat.change" variant="success" size="sm">
                {{ stat.change }}
              </Badge>
            </div>
            <p class="text-2xl font-bold text-text-heading">{{ stat.value }}</p>
            <p class="text-sm text-text-meta">{{ stat.label }}</p>
          </template>
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
              <Button variant="ghost" size="sm" @click="fetchAll">새로고침</Button>
            </div>

            <!-- 로딩 상태 -->
            <div v-if="loading.activities" class="space-y-3">
              <div v-for="i in 3" :key="i" class="animate-pulse flex items-center gap-4 p-3">
                <div class="w-10 h-10 bg-bg-elevated rounded-full"></div>
                <div class="flex-1">
                  <div class="h-4 w-3/4 bg-bg-elevated rounded mb-2"></div>
                  <div class="h-3 w-1/2 bg-bg-elevated rounded"></div>
                </div>
                <div class="h-3 w-12 bg-bg-elevated rounded"></div>
              </div>
            </div>

            <!-- 에러 상태 -->
            <div v-else-if="errors.activities" class="text-center py-8 text-text-meta">
              <p>활동을 불러올 수 없습니다</p>
              <Button variant="ghost" size="sm" class="mt-2" @click="fetchAll">
                다시 시도
              </Button>
            </div>

            <!-- 빈 상태 -->
            <div v-else-if="activities.length === 0" class="text-center py-8 text-text-meta">
              <p>아직 활동이 없습니다</p>
              <p class="text-sm mt-1">글을 작성하거나 상품을 주문해보세요!</p>
            </div>

            <!-- 정상 상태 -->
            <div v-else class="space-y-3">
              <div
                v-for="activity in activities"
                :key="activity.id"
                class="flex items-center gap-4 p-3 rounded-lg hover:bg-bg-elevated transition-colors cursor-pointer"
                @click="activity.link && navigateTo(activity.link)"
              >
                <div class="w-10 h-10 rounded-full bg-brand-primary/10 flex items-center justify-center text-lg">
                  {{ activity.icon }}
                </div>
                <div class="flex-1 min-w-0">
                  <p class="text-text-heading font-medium truncate">
                    {{ activity.title }}
                  </p>
                  <p class="text-sm text-text-meta">{{ activity.description }}</p>
                </div>
                <span class="text-xs text-text-meta whitespace-nowrap">
                  {{ formatRelativeTime(activity.timestamp) }}
                </span>
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
