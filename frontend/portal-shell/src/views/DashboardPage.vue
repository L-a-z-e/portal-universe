<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { useDashboard } from '../composables/useDashboard'
import { Badge } from '@portal/design-vue'
import { getRemoteConfigs } from '../config/remoteRegistry'
import { formatRelativeTime } from '../utils/dateUtils'
import MaterialIcon from '../components/MaterialIcon.vue'

const router = useRouter()
const authStore = useAuthStore()

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
    icon: config.icon || 'package_2',
    description: config.description || '',
    path: config.basePath,
  }))
})

// Quick actions (2x2 grid)
const quickActions = [
  { id: 'new-post', label: '새 글 작성', icon: 'edit_note', path: '/blog/write', color: 'text-teal-400' },
  { id: 'browse-products', label: '상품 둘러보기', icon: 'storefront', path: '/shopping', color: 'text-orange-400' },
  { id: 'my-orders', label: '주문 내역', icon: 'package_2', path: '/shopping/orders', color: 'text-blue-400' },
  { id: 'ai-agent', label: 'AI 에이전트', icon: 'smart_toy', path: '/prism', color: 'text-violet-400' },
]

// Activity dot colors by type
const activityDotColor: Record<string, string> = {
  POST_CREATED: 'bg-teal-400',
  COMMENT_CREATED: 'bg-blue-400',
  POST_LIKED: 'bg-red-400',
  ORDER_CREATED: 'bg-orange-400',
  ORDER_COMPLETED: 'bg-green-400',
  PAYMENT_COMPLETED: 'bg-violet-400',
}

// Stat icon accent colors
const statColors = ['text-teal-400', 'text-orange-400', 'text-red-400', 'text-violet-400']

// Greeting
const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 12) return 'Good morning'
  if (hour < 18) return 'Good afternoon'
  return 'Good evening'
})

// Service gradient colors
const serviceGradients: Record<string, string> = {
  blog: 'from-teal-500/20 to-teal-600/5',
  shopping: 'from-orange-500/20 to-orange-600/5',
  prism: 'from-violet-500/20 to-violet-600/5',
  drive: 'from-cyan-500/20 to-cyan-600/5',
  admin: 'from-red-500/20 to-red-600/5',
  seller: 'from-orange-400/20 to-orange-500/5',
}

const serviceAccents: Record<string, string> = {
  blog: 'bg-teal-500',
  shopping: 'bg-orange-500',
  prism: 'bg-violet-500',
  drive: 'bg-cyan-500',
  admin: 'bg-red-500',
  seller: 'bg-orange-400',
}

function navigateTo(path: string) {
  router.push(path)
}
</script>

<template>
  <div class="bg-bg-page text-text-body">
    <main class="max-w-7xl mx-auto px-6 py-8">
      <!-- Welcome Section -->
      <section class="mb-8">
        <h1 class="text-2xl font-bold text-text-heading">
          {{ greeting }}, {{ authStore.displayName }}
        </h1>
        <p class="text-text-meta mt-1">Here's what's happening today</p>
      </section>

      <!-- Stats Cards (4 columns) -->
      <section class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
        <div
          v-for="(stat, idx) in stats"
          :key="stat.label"
          class="rounded-xl p-5 bg-[rgba(20,21,22,0.7)] backdrop-blur-[10px] border border-white/[0.06] shadow-[0_4px_24px_rgba(0,0,0,0.2)] light:bg-white/80 light:border-gray-200/50 light:shadow-sm transition-all"
        >
          <!-- Loading -->
          <template v-if="stat.loading">
            <div class="animate-pulse">
              <div class="flex items-center justify-between mb-3">
                <div class="h-8 w-8 bg-bg-elevated rounded-lg"></div>
                <div class="h-4 w-10 bg-bg-elevated rounded"></div>
              </div>
              <div class="h-7 w-16 bg-bg-elevated rounded mb-1"></div>
              <div class="h-4 w-20 bg-bg-elevated rounded"></div>
            </div>
          </template>

          <!-- Error -->
          <template v-else-if="stat.error">
            <div class="flex items-center justify-between mb-3">
              <div class="w-10 h-10 rounded-lg bg-bg-elevated flex items-center justify-center">
                <MaterialIcon :name="stat.icon" :size="22" class="text-text-muted" />
              </div>
              <Badge variant="danger" size="sm">Error</Badge>
            </div>
            <p class="text-2xl font-bold text-text-meta">--</p>
            <p class="text-sm text-text-meta mt-1">{{ stat.label }}</p>
          </template>

          <!-- Normal -->
          <template v-else>
            <div class="flex items-center justify-between mb-3">
              <div class="w-10 h-10 rounded-lg bg-bg-elevated flex items-center justify-center">
                <MaterialIcon :name="stat.icon" :size="22" :class="statColors[idx]" />
              </div>
              <Badge v-if="stat.change" variant="success" size="sm">
                {{ stat.change }}
              </Badge>
            </div>
            <p class="text-2xl font-bold text-text-heading">{{ stat.value?.toLocaleString() }}</p>
            <p class="text-sm text-text-meta mt-1">{{ stat.label }}</p>
          </template>
        </div>
      </section>

      <!-- Main Grid: Activity (2/3) + Quick Actions (1/3) -->
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-8">
        <!-- Activity Timeline (2/3) -->
        <section class="lg:col-span-2">
          <div class="bg-bg-card border border-border-default rounded-xl p-5">
            <div class="flex items-center justify-between mb-5">
              <h2 class="text-lg font-semibold text-text-heading flex items-center gap-2">
                <MaterialIcon name="timeline" :size="20" class="text-brand-primary" />
                Activity
              </h2>
              <button
                @click="fetchAll"
                class="text-sm text-text-meta hover:text-text-heading transition-colors flex items-center gap-1"
              >
                <MaterialIcon name="refresh" :size="16" />
                Refresh
              </button>
            </div>

            <!-- Loading -->
            <div v-if="loading.activities" class="space-y-4">
              <div v-for="i in 3" :key="i" class="animate-pulse flex gap-4">
                <div class="w-3 h-3 bg-bg-elevated rounded-full mt-1.5 shrink-0"></div>
                <div class="flex-1">
                  <div class="h-4 w-3/4 bg-bg-elevated rounded mb-2"></div>
                  <div class="h-3 w-1/2 bg-bg-elevated rounded"></div>
                </div>
              </div>
            </div>

            <!-- Error -->
            <div v-else-if="errors.activities" class="text-center py-8 text-text-meta">
              <MaterialIcon name="error_outline" :size="32" class="mb-2" />
              <p>Failed to load activities</p>
              <button @click="fetchAll" class="text-sm text-brand-primary hover:underline mt-2">
                Try again
              </button>
            </div>

            <!-- Empty -->
            <div v-else-if="activities.length === 0" class="text-center py-8 text-text-meta">
              <MaterialIcon name="inbox" :size="32" class="mb-2" />
              <p>No recent activity</p>
              <p class="text-sm mt-1">Start by writing a post or browsing products</p>
            </div>

            <!-- Timeline -->
            <div v-else class="relative">
              <!-- Timeline line -->
              <div class="absolute left-[5px] top-2 bottom-2 w-px bg-border-default"></div>

              <div class="space-y-4">
                <div
                  v-for="activity in activities"
                  :key="activity.id"
                  class="flex gap-4 relative cursor-pointer group"
                  @click="activity.link && navigateTo(activity.link)"
                >
                  <!-- Dot -->
                  <div
                    :class="[
                      'w-[11px] h-[11px] rounded-full mt-1.5 shrink-0 ring-2 ring-bg-card z-10',
                      activityDotColor[activity.type] || 'bg-brand-primary'
                    ]"
                  ></div>

                  <!-- Content -->
                  <div class="flex-1 min-w-0 pb-1">
                    <p class="text-text-heading font-medium truncate group-hover:text-brand-primary transition-colors">
                      {{ activity.title }}
                    </p>
                    <p class="text-sm text-text-meta">{{ activity.description }}</p>
                    <span class="text-xs text-text-muted mt-0.5 inline-block">
                      {{ formatRelativeTime(activity.timestamp) }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

        <!-- Quick Actions (1/3, 2x2 grid) -->
        <section class="lg:col-span-1">
          <div class="bg-bg-card border border-border-default rounded-xl p-5">
            <h2 class="text-lg font-semibold text-text-heading mb-4 flex items-center gap-2">
              <MaterialIcon name="bolt" :size="20" class="text-brand-primary" />
              Quick Actions
            </h2>
            <div class="grid grid-cols-2 gap-3">
              <button
                v-for="action in quickActions"
                :key="action.id"
                @click="navigateTo(action.path)"
                class="flex flex-col items-center gap-2 p-4 rounded-xl bg-bg-elevated hover:bg-bg-hover border border-transparent hover:border-border-hover transition-all cursor-pointer"
              >
                <div class="w-10 h-10 rounded-full bg-bg-muted flex items-center justify-center">
                  <MaterialIcon :name="action.icon" :size="20" :class="action.color" />
                </div>
                <span class="text-xs font-medium text-text-body text-center leading-tight">{{ action.label }}</span>
              </button>
            </div>
          </div>
        </section>
      </div>

      <!-- Services Grid (4 columns) -->
      <section>
        <h2 class="text-lg font-semibold text-text-heading mb-4 flex items-center gap-2">
          <MaterialIcon name="apps" :size="20" class="text-brand-primary" />
          Services
        </h2>
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          <div
            v-for="service in services"
            :key="service.id"
            @click="navigateTo(service.path)"
            class="group relative overflow-hidden rounded-xl bg-bg-card border border-border-default hover:border-brand-primary/30 hover:-translate-y-1 transition-all duration-200 cursor-pointer"
          >
            <!-- Gradient banner -->
            <div
              :class="[
                'h-1',
                serviceAccents[service.id] || 'bg-brand-primary'
              ]"
            ></div>

            <div class="p-5">
              <div class="flex items-center gap-3 mb-3">
                <div class="w-10 h-10 rounded-lg flex items-center justify-center bg-gradient-to-br"
                  :class="serviceGradients[service.id] || 'from-brand-primary/20 to-brand-primary/5'"
                >
                  <MaterialIcon :name="service.icon" :size="22" class="text-text-heading" />
                </div>
                <h3 class="text-base font-semibold text-text-heading">{{ service.name }}</h3>
              </div>
              <p class="text-text-meta text-sm">{{ service.description }}</p>
            </div>

            <!-- Hover arrow -->
            <div class="absolute top-4 right-4 opacity-0 group-hover:opacity-100 transition-opacity">
              <MaterialIcon name="arrow_forward" :size="18" class="text-brand-primary" />
            </div>
          </div>
        </div>
      </section>

      <!-- Keyboard Shortcut Hint -->
      <div class="mt-8 text-center">
        <p class="text-sm text-text-meta">
          Press
          <kbd class="px-2 py-1 bg-bg-card border border-border-default rounded text-xs mx-0.5">&#8984;</kbd>
          <kbd class="px-2 py-1 bg-bg-card border border-border-default rounded text-xs mx-0.5">K</kbd>
          for quick search
        </p>
      </div>
    </main>
  </div>
</template>
