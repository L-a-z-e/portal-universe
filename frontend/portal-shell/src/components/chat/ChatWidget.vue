<script setup lang="ts">
import { ref } from 'vue'
import ChatPanel from './ChatPanel.vue'

const isOpen = ref(false)

function toggle() {
  isOpen.value = !isOpen.value
}
</script>

<template>
  <!-- Chat Panel -->
  <ChatPanel v-model="isOpen" />

  <!-- Floating Button -->
  <button
    class="fixed bottom-4 right-4 z-50 w-12 h-12 rounded-full bg-brand-primary text-white
           shadow-lg hover:shadow-xl hover:scale-105 transition-all duration-200
           flex items-center justify-center sm:right-6"
    title="Chat Assistant"
    @click="toggle"
  >
    <svg
      v-if="!isOpen"
      xmlns="http://www.w3.org/2000/svg"
      class="w-5 h-5"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      stroke-width="2"
    >
      <path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z" />
    </svg>
    <svg
      v-else
      xmlns="http://www.w3.org/2000/svg"
      class="w-5 h-5"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      stroke-width="2"
    >
      <path d="M18 6L6 18M6 6l12 12" />
    </svg>
  </button>
</template>
