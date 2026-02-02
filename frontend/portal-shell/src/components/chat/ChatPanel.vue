<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import { useChat } from '../../composables/useChat'
import { useAuthStore } from '../../store/auth'
import ChatMessageComp from './ChatMessage.vue'
import ChatInput from './ChatInput.vue'

defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const authStore = useAuthStore()
const {
  messages,
  loading,
  streaming,
  sendMessageStream,
  startNewConversation,
} = useChat()

const messagesContainer = ref<HTMLElement | null>(null)

function close() {
  emit('update:modelValue', false)
}

async function handleSend(message: string) {
  await sendMessageStream(message)
  scrollToBottom()
}

function handleNewChat() {
  startNewConversation()
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

watch(
  () => messages.value.length,
  () => scrollToBottom()
)

watch(
  () => streaming.value,
  () => {
    if (streaming.value) scrollToBottom()
  }
)
</script>

<template>
  <Transition name="slide-up">
    <div
      v-if="modelValue"
      class="fixed bottom-20 right-4 z-50 w-[380px] max-h-[600px] flex flex-col
             rounded-2xl shadow-2xl border border-border-default bg-bg-card overflow-hidden
             sm:right-6"
    >
      <!-- Header -->
      <div class="flex items-center justify-between px-4 py-3 border-b border-border-default bg-bg-elevated">
        <div class="flex items-center gap-2">
          <div class="w-2 h-2 rounded-full bg-green-500"></div>
          <span class="text-sm font-semibold text-text-heading">Chat Assistant</span>
        </div>
        <div class="flex items-center gap-1">
          <button
            class="p-1 rounded-lg hover:bg-bg-page transition-colors text-text-meta"
            title="New chat"
            @click="handleNewChat"
          >
            <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 5v14M5 12h14" />
            </svg>
          </button>
          <button
            class="p-1 rounded-lg hover:bg-bg-page transition-colors text-text-meta"
            @click="close"
          >
            <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M18 6L6 18M6 6l12 12" />
            </svg>
          </button>
        </div>
      </div>

      <!-- Auth check -->
      <template v-if="!authStore.isAuthenticated">
        <div class="flex-1 flex items-center justify-center p-6 text-center">
          <div>
            <p class="text-text-meta text-sm mb-3">Please log in to use the chat.</p>
            <button
              class="px-4 py-2 bg-brand-primary text-white rounded-lg text-sm hover:opacity-90"
              @click="authStore.requestLogin()"
            >
              Log in
            </button>
          </div>
        </div>
      </template>

      <template v-else>
        <!-- Messages -->
        <div
          ref="messagesContainer"
          class="flex-1 overflow-y-auto p-4 space-y-3 min-h-[300px] max-h-[420px]"
        >
          <div v-if="messages.length === 0" class="flex items-center justify-center h-full text-text-meta text-sm">
            Ask me anything about our documentation!
          </div>
          <ChatMessageComp
            v-for="msg in messages"
            :key="msg.message_id"
            :message="msg"
          />
          <!-- Streaming indicator -->
          <div v-if="streaming" class="flex justify-start">
            <div class="bg-bg-elevated rounded-2xl rounded-bl-md px-4 py-2.5">
              <div class="flex gap-1">
                <span class="w-1.5 h-1.5 bg-text-meta rounded-full animate-bounce" style="animation-delay: 0ms"></span>
                <span class="w-1.5 h-1.5 bg-text-meta rounded-full animate-bounce" style="animation-delay: 150ms"></span>
                <span class="w-1.5 h-1.5 bg-text-meta rounded-full animate-bounce" style="animation-delay: 300ms"></span>
              </div>
            </div>
          </div>
        </div>

        <!-- Input -->
        <ChatInput :disabled="loading || streaming" @send="handleSend" />
      </template>
    </div>
  </Transition>
</template>

<style scoped>
.slide-up-enter-active,
.slide-up-leave-active {
  transition: all 0.3s ease;
}
.slide-up-enter-from,
.slide-up-leave-to {
  opacity: 0;
  transform: translateY(20px);
}
</style>
