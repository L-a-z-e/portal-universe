<script setup lang="ts">
import { ref } from 'vue'
import type { ChatMessage } from '../../types/chat'
import ChatSourceBadge from './ChatSourceBadge.vue'

defineProps<{
  message: ChatMessage
}>()

const showSources = ref(false)
</script>

<template>
  <div
    :class="[
      'flex w-full',
      message.role === 'user' ? 'justify-end' : 'justify-start',
    ]"
  >
    <div
      :class="[
        'max-w-[85%] rounded-2xl px-4 py-2.5 text-sm leading-relaxed',
        message.role === 'user'
          ? 'bg-brand-primary text-white rounded-br-md'
          : 'bg-bg-elevated text-text-body rounded-bl-md',
      ]"
    >
      <!-- Message content -->
      <div class="whitespace-pre-wrap break-words">{{ message.content }}</div>

      <!-- Sources -->
      <div
        v-if="message.sources && message.sources.length > 0"
        class="mt-2 pt-2 border-t"
        :class="message.role === 'user' ? 'border-white/20' : 'border-border-default'"
      >
        <button
          class="text-xs opacity-70 hover:opacity-100 transition-opacity"
          @click="showSources = !showSources"
        >
          {{ showSources ? 'Hide' : 'Show' }} sources ({{ message.sources.length }})
        </button>
        <div v-if="showSources" class="mt-1 flex flex-wrap gap-1">
          <ChatSourceBadge
            v-for="(source, i) in message.sources"
            :key="i"
            :source="source"
          />
        </div>
      </div>
    </div>
  </div>
</template>
