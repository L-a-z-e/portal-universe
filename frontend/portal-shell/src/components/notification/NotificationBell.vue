<script setup lang="ts">
import { useNotificationStore } from '../../store/notification'
import NotificationDropdown from './NotificationDropdown.vue'

// Props for dropdown direction
interface Props {
  dropdownDirection?: 'left' | 'right' | 'up'
}

const props = withDefaults(defineProps<Props>(), {
  dropdownDirection: 'right'
})

const store = useNotificationStore()
</script>

<template>
  <div class="relative notification-bell">
    <!-- Bell button -->
    <button
      @click.stop="store.toggleDropdown"
      class="relative p-2 rounded-lg hover:bg-bg-elevated transition-colors"
      aria-label="알림"
    >
      <!-- Bell icon -->
      <svg
        class="w-5 h-5 text-text-body"
        fill="none"
        stroke="currentColor"
        viewBox="0 0 24 24"
      >
        <path
          stroke-linecap="round"
          stroke-linejoin="round"
          stroke-width="2"
          d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"
        />
      </svg>

      <!-- Unread badge (green dot) -->
      <span
        v-if="store.hasUnread"
        class="absolute top-1.5 right-1.5 w-2.5 h-2.5 bg-status-success rounded-full border-2 border-bg-card"
      />
    </button>

    <!-- Dropdown -->
    <NotificationDropdown
      v-if="store.isDropdownOpen"
      :direction="dropdownDirection"
    />
  </div>
</template>
