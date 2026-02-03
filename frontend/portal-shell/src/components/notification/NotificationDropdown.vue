<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import { useNotificationStore } from '../../store/notification'
import NotificationItem from './NotificationItem.vue'

const store = useNotificationStore()

// Handle click outside to close dropdown
function handleClickOutside(event: MouseEvent) {
  const target = event.target as HTMLElement
  if (!target.closest('.notification-dropdown') && !target.closest('.notification-bell')) {
    store.closeDropdown()
  }
}

// Handle scroll to load more
function handleScroll(event: Event) {
  const target = event.target as HTMLElement
  const threshold = 50

  if (
    target.scrollHeight - target.scrollTop <= target.clientHeight + threshold &&
    store.hasMore &&
    !store.isLoading
  ) {
    store.loadMore()
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  store.fetchNotifications(0, true)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<template>
  <div
    class="notification-dropdown absolute right-0 mt-2 w-80 max-h-[32rem] bg-bg-card rounded-lg shadow-lg border border-border-default z-50 overflow-hidden"
  >
    <!-- Header -->
    <div
      class="sticky top-0 bg-bg-card px-4 py-3 border-b border-border-default flex justify-between items-center"
    >
      <h3 class="font-semibold text-text-heading">알림</h3>
      <button
        v-if="store.hasUnread"
        @click="store.markAllAsRead"
        class="text-sm text-brand-primary hover:text-brand-primary-hover flex items-center gap-1 transition-colors"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
        </svg>
        모두 읽음
      </button>
    </div>

    <!-- Notification list -->
    <div
      v-if="store.notifications.length > 0"
      class="overflow-y-auto max-h-[28rem]"
      @scroll="handleScroll"
    >
      <NotificationItem
        v-for="notification in store.notifications"
        :key="notification.id"
        :notification="notification"
      />

      <!-- Loading more indicator -->
      <div v-if="store.isLoading" class="p-4 text-center text-text-meta text-sm">
        불러오는 중...
      </div>
    </div>

    <!-- Empty state -->
    <div v-else-if="!store.isLoading" class="p-8 text-center">
      <div class="text-4xl mb-2">🔔</div>
      <p class="text-text-meta">알림이 없습니다</p>
    </div>

    <!-- Initial loading -->
    <div v-else class="p-8 text-center text-text-meta">
      불러오는 중...
    </div>
  </div>
</template>
