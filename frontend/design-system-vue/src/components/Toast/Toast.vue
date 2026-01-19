<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue';
import type { ToastProps, ToastEmits } from './Toast.types';

const props = withDefaults(defineProps<ToastProps>(), {
  variant: 'info',
  duration: 5000,
  dismissible: true,
});

const emit = defineEmits<ToastEmits>();

const isVisible = ref(true);
let timeoutId: ReturnType<typeof setTimeout> | null = null;

const variantClasses = {
  info: {
    container: 'bg-white border-l-4 border-blue-500 dark:bg-gray-800',
    icon: 'text-blue-500',
  },
  success: {
    container: 'bg-white border-l-4 border-green-500 dark:bg-gray-800',
    icon: 'text-green-500',
  },
  warning: {
    container: 'bg-white border-l-4 border-yellow-500 dark:bg-gray-800',
    icon: 'text-yellow-500',
  },
  error: {
    container: 'bg-white border-l-4 border-red-500 dark:bg-gray-800',
    icon: 'text-red-500',
  },
};

const containerClasses = computed(() => [
  'flex items-start gap-3 p-4 rounded-lg shadow-lg',
  'border border-border-default dark:border-gray-700',
  variantClasses[props.variant].container,
]);

const dismiss = () => {
  isVisible.value = false;
  setTimeout(() => {
    emit('dismiss', props.id);
  }, 150);
};

const startTimer = () => {
  if (props.duration > 0) {
    timeoutId = setTimeout(dismiss, props.duration);
  }
};

const pauseTimer = () => {
  if (timeoutId) {
    clearTimeout(timeoutId);
    timeoutId = null;
  }
};

onMounted(() => {
  startTimer();
});

onUnmounted(() => {
  pauseTimer();
});
</script>

<template>
  <Transition
    enter-active-class="transition duration-200 ease-out"
    enter-from-class="transform translate-x-full opacity-0"
    enter-to-class="transform translate-x-0 opacity-100"
    leave-active-class="transition duration-150 ease-in"
    leave-from-class="transform translate-x-0 opacity-100"
    leave-to-class="transform translate-x-full opacity-0"
  >
    <div
      v-if="isVisible"
      role="alert"
      aria-live="polite"
      :class="containerClasses"
      @mouseenter="pauseTimer"
      @mouseleave="startTimer"
    >
      <!-- Icon -->
      <div :class="['flex-shrink-0', variantClasses[variant].icon]">
        <!-- Info icon -->
        <svg v-if="variant === 'info'" class="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
          <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clip-rule="evenodd" />
        </svg>
        <!-- Success icon -->
        <svg v-else-if="variant === 'success'" class="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
          <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
        </svg>
        <!-- Warning icon -->
        <svg v-else-if="variant === 'warning'" class="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
          <path fill-rule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clip-rule="evenodd" />
        </svg>
        <!-- Error icon -->
        <svg v-else-if="variant === 'error'" class="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
          <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd" />
        </svg>
      </div>

      <!-- Content -->
      <div class="flex-1 min-w-0">
        <h4 v-if="title" class="font-semibold text-text-heading text-sm">
          {{ title }}
        </h4>
        <p class="text-sm text-text-body">
          {{ message }}
        </p>
        <button
          v-if="action"
          type="button"
          class="mt-2 text-sm font-medium text-brand-600 hover:text-brand-700 dark:text-brand-400 dark:hover:text-brand-300"
          @click="action.onClick"
        >
          {{ action.label }}
        </button>
      </div>

      <!-- Dismiss button -->
      <button
        v-if="dismissible"
        type="button"
        class="flex-shrink-0 p-1 -m-1 rounded text-text-muted hover:text-text-body hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
        aria-label="Dismiss"
        @click="dismiss"
      >
        <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
          <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd" />
        </svg>
      </button>
    </div>
  </Transition>
</template>
