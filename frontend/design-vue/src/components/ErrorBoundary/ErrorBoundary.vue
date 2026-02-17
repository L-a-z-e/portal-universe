<script setup lang="ts">
import { ref, onErrorCaptured } from 'vue';

interface Props {
  fallbackMessage?: string;
}

const props = withDefaults(defineProps<Props>(), {
  fallbackMessage: 'Something went wrong',
});

const hasError = ref(false);
const errorMessage = ref('');

onErrorCaptured((err: Error) => {
  hasError.value = true;
  errorMessage.value = err.message || props.fallbackMessage;
  console.error('[ErrorBoundary]', err);
  return false;
});

const reset = () => {
  hasError.value = false;
  errorMessage.value = '';
};
</script>

<template>
  <slot v-if="!hasError" />
  <div
    v-else
    class="flex flex-col items-center justify-center gap-4 p-8 rounded-lg border border-border-default bg-bg-muted"
  >
    <div class="text-center">
      <p class="text-lg font-semibold text-text-heading">{{ fallbackMessage }}</p>
      <p v-if="errorMessage" class="mt-1 text-sm text-text-muted">
        {{ errorMessage }}
      </p>
    </div>
    <button
      type="button"
      class="px-4 py-2 text-sm font-medium rounded-md bg-brand-primary text-white hover:opacity-90 transition-opacity"
      @click="reset"
    >
      Try Again
    </button>
  </div>
</template>
