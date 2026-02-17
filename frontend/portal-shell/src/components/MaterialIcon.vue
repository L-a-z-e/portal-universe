<script setup lang="ts">
defineProps<{ name: string; size?: number; filled?: boolean }>()
</script>

<template>
  <span
    class="material-symbols-outlined select-none leading-none"
    :style="{
      fontSize: `${size || 20}px`,
      fontVariationSettings: filled ? `'FILL' 1` : `'FILL' 0`,
    }"
  >
    {{ name }}
  </span>
</template>
