<script setup lang="ts">
import type { SourceInfo } from '../../types/chat'

defineProps<{
  source: SourceInfo
}>()
</script>

<template>
  <div
    class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs bg-bg-page text-text-secondary"
  >
    <span class="font-medium truncate max-w-[120px]">{{ source.document }}</span>
    <span class="opacity-60">({{ (source.relevance_score * 100).toFixed(0) }}%)</span>
  </div>
</template>
