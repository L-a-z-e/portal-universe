<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useNotificationStore } from '../../store/notification'
import type { Notification } from '../../types/notification'

const props = defineProps<{
  notification: Notification
}>()

const router = useRouter()
const store = useNotificationStore()

// Parse date (handles both ISO string and array format from backend)
function parseDate(value: string | number[] | null | undefined): Date | null {
  if (!value) return null

  // Handle array format: [2026, 2, 3, 23, 53, 49, 901889000]
  if (Array.isArray(value) && value.length >= 3) {
    const arr = value as [number, number, number, ...number[]]
    return new Date(arr[0], arr[1] - 1, arr[2], arr[3] ?? 0, arr[4] ?? 0, arr[5] ?? 0)
  }

  // Handle ISO string format
  if (typeof value === 'string') {
    const date = new Date(value)
    return isNaN(date.getTime()) ? null : date
  }

  return null
}

// Time ago formatting
const timeAgo = computed(() => {
  const date = parseDate(props.notification.createdAt as string | number[])
  if (!date) return ''

  const now = new Date()
  const diff = now.getTime() - date.getTime()

  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 1) return '방금 전'
  if (minutes < 60) return `${minutes}분 전`
  if (hours < 24) return `${hours}시간 전`
  if (days < 7) return `${days}일 전`

  return date.toLocaleDateString('ko-KR', { month: 'short', day: 'numeric' })
})

// Get icon based on notification type
const icon = computed(() => {
  const type = props.notification.type
  if (type.startsWith('ORDER')) return '📦'
  if (type.startsWith('DELIVERY')) return '🚚'
  if (type.startsWith('PAYMENT')) return '💳'
  if (type.startsWith('COUPON')) return '🎟️'
  if (type.startsWith('TIMEDEAL')) return '⏰'
  if (type.startsWith('BLOG')) return '📝'
  if (type.startsWith('PRISM')) return '🤖'
  return '🔔'
})

// Check if notification is unread
const isUnread = computed(() => props.notification.status === 'UNREAD')

// Handle click - mark as read and navigate if link exists
async function handleClick() {
  // Mark as read
  if (isUnread.value) {
    await store.markAsRead(props.notification.id)
  }

  // Navigate if link exists
  if (props.notification.link) {
    store.closeDropdown()
    router.push(props.notification.link)
  }
}
</script>

<template>
  <div
    @click="handleClick"
    class="flex gap-3 px-4 py-3 hover:bg-bg-elevated cursor-pointer border-b border-border-default last:border-b-0 transition-colors"
    :class="{ 'bg-brand-primary/5': isUnread }"
  >
    <!-- Icon -->
    <div class="flex-shrink-0 text-lg">
      {{ icon }}
    </div>

    <!-- Content -->
    <div class="flex-1 min-w-0">
      <p
        class="text-sm font-medium truncate"
        :class="isUnread ? 'text-text-heading' : 'text-text-body'"
      >
        {{ notification.title }}
      </p>
      <p class="text-sm text-text-meta line-clamp-2">
        {{ notification.message }}
      </p>
      <p class="text-xs text-text-meta mt-1">
        {{ timeAgo }}
      </p>
    </div>

    <!-- Unread indicator -->
    <div v-if="isUnread" class="flex-shrink-0 self-center">
      <span class="w-2 h-2 bg-brand-primary rounded-full inline-block" />
    </div>
  </div>
</template>
