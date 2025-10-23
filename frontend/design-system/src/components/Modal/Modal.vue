<script setup lang="ts">
import { computed, onMounted, onUnmounted, watch } from 'vue';
import type { ModalProps } from './Modal.types';

const props = withDefaults(defineProps<ModalProps>(), {
  modelValue: false,
  title: '',
  size: 'md',
  showClose: true,
  closeOnBackdrop: true
});

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'close': []
}>();

const sizeClasses = {
  sm: 'max-w-md',
  md: 'max-w-lg',
  lg: 'max-w-2xl',
  xl: 'max-w-4xl'
};

const isOpen = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
});

function close() {
  isOpen.value = false;
  emit('close');
}

function handleBackdropClick() {
  if (props.closeOnBackdrop) {
    close();
  }
}

function handleEscape(e: KeyboardEvent) {
  if (e.key === 'Escape' && isOpen.value) {
    close();
  }
}

watch(isOpen, (value) => {
  if (value) {
    document.body.style.overflow = 'hidden';
  } else {
    document.body.style.overflow = '';
  }
});

onMounted(() => {
  document.addEventListener('keydown', handleEscape);
});

onUnmounted(() => {
  document.removeEventListener('keydown', handleEscape);
  document.body.style.overflow = '';
});
</script>

<template>
  <Teleport to="body">
    <Transition
        enter-active-class="transition-opacity duration-200"
        leave-active-class="transition-opacity duration-200"
        enter-from-class="opacity-0"
        leave-to-class="opacity-0"
    >
      <div
          v-if="isOpen"
          class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm"
          @click.self="handleBackdropClick"
      >
        <Transition
            enter-active-class="transition-all duration-200"
            leave-active-class="transition-all duration-200"
            enter-from-class="opacity-0 scale-95"
            leave-to-class="opacity-0 scale-95"
        >
          <div
              v-if="isOpen"
              :class="[
              'bg-white dark:bg-gray-800 rounded-xl shadow-2xl w-full',
              sizeClasses[size]
            ]"
              @click.stop
          >
            <!-- Header -->
            <div v-if="title || showClose" class="flex items-center justify-between px-6 py-4 border-b border-gray-200 dark:border-gray-700">
              <h3 v-if="title" class="text-xl font-bold text-gray-900 dark:text-gray-100">
                {{ title }}
              </h3>
              <button
                  v-if="showClose"
                  @click="close"
                  class="p-2 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors"
                  aria-label="Close"
              >
                <svg class="w-5 h-5 text-gray-500 dark:text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>

            <!-- Body -->
            <div class="px-6 py-6">
              <slot />
            </div>

            <!-- Footer (optional) -->
            <div v-if="$slots.footer" class="px-6 py-4 bg-gray-50 dark:bg-gray-900 rounded-b-xl border-t border-gray-200 dark:border-gray-700">
              <slot name="footer" />
            </div>
          </div>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>