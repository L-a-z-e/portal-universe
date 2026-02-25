<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { getRemoteConfigs } from '../config/remoteRegistry'
import MaterialIcon from './MaterialIcon.vue'

const isMac = navigator.platform.toUpperCase().indexOf('MAC') >= 0

// Props & Emits
const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const router = useRouter()
const searchQuery = ref('')
const selectedIndex = ref(0)
const inputRef = ref<HTMLInputElement | null>(null)

// All available actions
const allActions = computed(() => {
  const configs = getRemoteConfigs()

  const navigationActions = configs.map(config => ({
    id: `nav-${config.key}`,
    type: 'navigation' as const,
    icon: config.icon || '📦',
    title: config.name,
    description: config.description || `${config.name} 서비스로 이동`,
    path: config.basePath,
    keywords: [config.name.toLowerCase(), config.key]
  }))

  const quickActions = [
    {
      id: 'action-new-post',
      type: 'action' as const,
      icon: 'edit_note',
      title: '새 글 작성',
      description: '블로그에 새 글을 작성합니다',
      path: '/blog/write',
      keywords: ['글', '작성', 'post', 'write', 'blog']
    },
    {
      id: 'action-browse-products',
      type: 'action' as const,
      icon: 'storefront',
      title: '상품 둘러보기',
      description: '쇼핑몰 상품 목록으로 이동',
      path: '/shopping',
      keywords: ['쇼핑', 'shopping', 'products', '상품']
    },
    {
      id: 'action-my-orders',
      type: 'action' as const,
      icon: 'package_2',
      title: '주문 내역',
      description: '내 주문 내역 확인',
      path: '/shopping/orders',
      keywords: ['주문', 'orders', '내역']
    },
    {
      id: 'action-home',
      type: 'navigation' as const,
      icon: 'dashboard',
      title: '홈으로',
      description: '메인 페이지로 이동',
      path: '/',
      keywords: ['홈', 'home', '메인']
    }
  ]

  return [...quickActions, ...navigationActions]
})

// Filtered actions based on search query
const filteredActions = computed(() => {
  if (!searchQuery.value.trim()) {
    return allActions.value
  }

  const query = searchQuery.value.toLowerCase()
  return allActions.value.filter(action => {
    return (
      action.title.toLowerCase().includes(query) ||
      action.description.toLowerCase().includes(query) ||
      action.keywords.some(k => k.includes(query))
    )
  })
})

// Reset selection when filtered results change
watch(filteredActions, () => {
  selectedIndex.value = 0
})

// Focus input when modal opens
watch(() => props.modelValue, (isOpen) => {
  if (isOpen) {
    searchQuery.value = ''
    selectedIndex.value = 0
    setTimeout(() => {
      inputRef.value?.focus()
    }, 50)
  }
})

// Execute selected action
function executeAction(action: typeof allActions.value[0]) {
  emit('update:modelValue', false)
  router.push(action.path)
}

// Handle keyboard navigation
function handleKeyDown(e: KeyboardEvent) {
  switch (e.key) {
    case 'ArrowDown':
      e.preventDefault()
      selectedIndex.value = Math.min(selectedIndex.value + 1, filteredActions.value.length - 1)
      break
    case 'ArrowUp':
      e.preventDefault()
      selectedIndex.value = Math.max(selectedIndex.value - 1, 0)
      break
    case 'Enter':
      e.preventDefault()
      const selectedAction = filteredActions.value[selectedIndex.value]
      if (selectedAction) {
        executeAction(selectedAction)
      }
      break
    case 'Escape':
      emit('update:modelValue', false)
      break
  }
}

// Global keyboard shortcut
function handleGlobalKeyDown(e: KeyboardEvent) {
  if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
    e.preventDefault()
    emit('update:modelValue', !props.modelValue)
  }
}

onMounted(() => {
  document.addEventListener('keydown', handleGlobalKeyDown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleGlobalKeyDown)
})
</script>

<template>
  <Teleport to="body">
    <Transition name="fade">
      <div
        v-if="modelValue"
        class="fixed inset-0 z-50 flex items-start justify-center pt-[15vh]"
        @click.self="$emit('update:modelValue', false)"
      >
        <!-- Backdrop -->
        <div class="absolute inset-0 bg-black/50 backdrop-blur-sm" @click="$emit('update:modelValue', false)" />

        <!-- Modal -->
        <div
          class="relative w-full max-w-xl mx-4 bg-bg-card border border-border-default rounded-xl shadow-2xl overflow-hidden"
          @keydown="handleKeyDown"
        >
          <!-- Search Input -->
          <div class="flex items-center gap-3 px-4 py-3 border-b border-border-default">
            <MaterialIcon name="search" :size="20" class="text-text-meta" />
            <input
              ref="inputRef"
              v-model="searchQuery"
              type="text"
              placeholder="검색 또는 빠른 이동..."
              class="flex-1 bg-transparent text-text-heading placeholder-text-meta outline-none text-base"
            />
            <kbd class="hidden sm:inline-flex items-center gap-1 px-2 py-1 text-xs text-text-meta bg-bg-elevated border border-border-default rounded">
              ESC
            </kbd>
          </div>

          <!-- Results -->
          <div class="max-h-[50vh] overflow-y-auto py-2">
            <template v-if="filteredActions.length > 0">
              <div
                v-for="(action, index) in filteredActions"
                :key="action.id"
                @click="executeAction(action)"
                @mouseenter="selectedIndex = index"
                class="flex items-center gap-3 px-4 py-3 cursor-pointer transition-colors"
                :class="[
                  index === selectedIndex
                    ? 'bg-brand-primary/10 text-text-heading'
                    : 'hover:bg-bg-elevated text-text-body'
                ]"
              >
                <MaterialIcon :name="action.icon" :size="20" class="text-brand-primary" />
                <div class="flex-1 min-w-0">
                  <p class="font-medium truncate">{{ action.title }}</p>
                  <p class="text-sm text-text-meta truncate">{{ action.description }}</p>
                </div>
                <span
                  v-if="index === selectedIndex"
                  class="text-xs text-text-meta bg-bg-elevated px-2 py-1 rounded"
                >
                  Enter
                </span>
              </div>
            </template>
            <div v-else class="px-4 py-8 text-center text-text-meta">
              <MaterialIcon name="search_off" :size="40" class="mb-2" />
              <p>검색 결과가 없습니다</p>
            </div>
          </div>

          <!-- Footer -->
          <div class="px-4 py-2 border-t border-border-default bg-bg-elevated/50">
            <div class="flex items-center justify-between text-xs text-text-meta">
              <div class="flex items-center gap-4">
                <span class="flex items-center gap-1">
                  <kbd class="px-1.5 py-0.5 bg-bg-card border border-border-default rounded">↑</kbd>
                  <kbd class="px-1.5 py-0.5 bg-bg-card border border-border-default rounded">↓</kbd>
                  이동
                </span>
                <span class="flex items-center gap-1">
                  <kbd class="px-1.5 py-0.5 bg-bg-card border border-border-default rounded">Enter</kbd>
                  선택
                </span>
              </div>
              <span class="flex items-center gap-1">
                <kbd class="px-1.5 py-0.5 bg-bg-card border border-border-default rounded">{{ isMac ? '⌘' : 'Ctrl' }}</kbd>
                <kbd class="px-1.5 py-0.5 bg-bg-card border border-border-default rounded">K</kbd>
                열기/닫기
              </span>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
