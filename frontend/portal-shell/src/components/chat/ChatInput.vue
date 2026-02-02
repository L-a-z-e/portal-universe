<script setup lang="ts">
import { ref } from 'vue'

const emit = defineEmits<{
  (e: 'send', message: string): void
}>()

defineProps<{
  disabled?: boolean
}>()

const input = ref('')

function handleSend() {
  const msg = input.value.trim()
  if (!msg) return
  emit('send', msg)
  input.value = ''
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}
</script>

<template>
  <div class="flex items-end gap-2 p-3 border-t border-border-default bg-bg-card">
    <textarea
      v-model="input"
      :disabled="disabled"
      placeholder="Ask a question..."
      rows="1"
      class="flex-1 resize-none rounded-xl border border-border-default bg-bg-page px-3 py-2 text-sm
             focus:outline-none focus:ring-2 focus:ring-brand-primary focus:border-transparent
             disabled:opacity-50 disabled:cursor-not-allowed"
      @keydown="handleKeydown"
    />
    <button
      :disabled="disabled || !input.trim()"
      class="shrink-0 rounded-xl bg-brand-primary px-3 py-2 text-white text-sm font-medium
             hover:opacity-90 transition-opacity
             disabled:opacity-50 disabled:cursor-not-allowed"
      @click="handleSend"
    >
      <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M22 2L11 13" /><path d="M22 2L15 22L11 13L2 9L22 2Z" />
      </svg>
    </button>
  </div>
</template>
