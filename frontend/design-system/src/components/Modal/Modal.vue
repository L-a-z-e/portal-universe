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
      enter-active-class="transition-opacity duration-[160ms] ease-[cubic-bezier(0.25,0.1,0.25,1)]"
      leave-active-class="transition-opacity duration-[100ms] ease-[cubic-bezier(0.25,0.1,0.25,1)]"
      enter-from-class="opacity-0"
      leave-to-class="opacity-0"
    >
      <div
        v-if="isOpen"
        class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm"
        @click.self="handleBackdropClick"
      >
        <Transition
          enter-active-class="transition-all duration-[160ms] ease-[cubic-bezier(0.25,0.1,0.25,1)]"
          leave-active-class="transition-all duration-[100ms] ease-[cubic-bezier(0.25,0.1,0.25,1)]"
          enter-from-class="opacity-0 scale-95 translate-y-2"
          leave-to-class="opacity-0 scale-95 translate-y-2"
        >
          <div
            v-if="isOpen"
            :class="[
              'bg-bg-elevated rounded-xl shadow-2xl w-full',
              'border border-border-default',
              sizeClasses[size]
            ]"
            @click.stop
          >
            <!-- Header -->
            <div
              v-if="title || showClose"
              class="flex items-center justify-between px-5 py-4 border-b border-border-muted"
            >
              <h3 v-if="title" class="text-lg font-semibold text-text-heading">
                {{ title }}
              </h3>
              <button
                v-if="showClose"
                @click="close"
                class="p-1.5 hover:bg-bg-hover rounded-md transition-colors duration-[100ms] text-text-muted hover:text-text-body"
                aria-label="Close"
              >
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>

            <!-- Body -->
            <div class="px-5 py-5">
              <slot />
            </div>

            <!-- Footer (optional) -->
            <div
              v-if="$slots.footer"
              class="px-5 py-4 bg-bg-muted rounded-b-xl border-t border-border-muted"
            >
              <slot name="footer" />
            </div>
          </div>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>