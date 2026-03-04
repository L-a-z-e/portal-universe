<script setup lang="ts">
import { computed, ref, nextTick, onMounted, onUnmounted, watch } from 'vue';
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

import { modalSizes } from '@portal/design-core';

const modalRef = ref<HTMLDivElement | null>(null);
const previousActiveElement = ref<HTMLElement | null>(null);
const titleId = `modal-title-${Math.random().toString(36).slice(2, 9)}`;

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

function trapFocus(e: KeyboardEvent) {
  if (e.key !== 'Tab' || !modalRef.value) return;

  const focusableEls = modalRef.value.querySelectorAll<HTMLElement>(
    'a[href], button:not([disabled]), textarea:not([disabled]), input:not([disabled]), select:not([disabled]), [tabindex]:not([tabindex="-1"])'
  );
  if (focusableEls.length === 0) return;

  const first = focusableEls[0];
  const last = focusableEls[focusableEls.length - 1];

  if (e.shiftKey && document.activeElement === first) {
    e.preventDefault();
    last.focus();
  } else if (!e.shiftKey && document.activeElement === last) {
    e.preventDefault();
    first.focus();
  }
}

watch(isOpen, async (value) => {
  if (value) {
    previousActiveElement.value = document.activeElement as HTMLElement;
    document.body.style.overflow = 'hidden';
    await nextTick();
    const firstFocusable = modalRef.value?.querySelector<HTMLElement>(
      'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
    );
    firstFocusable?.focus();
  } else {
    document.body.style.overflow = '';
    previousActiveElement.value?.focus();
    previousActiveElement.value = null;
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
        role="dialog"
        aria-modal="true"
        :aria-labelledby="title ? titleId : undefined"
        class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm"
        @click.self="handleBackdropClick"
        @keydown="trapFocus"
      >
        <Transition
          enter-active-class="transition-all duration-150 ease-out"
          leave-active-class="transition-all duration-100 ease-out"
          enter-from-class="opacity-0 scale-95 translate-y-2"
          leave-to-class="opacity-0 scale-95 translate-y-2"
        >
          <div
            v-if="isOpen"
            ref="modalRef"
            :class="[
              // Linear dark mode first
              'bg-[#18191b] rounded-xl w-full',
              'border border-[#2a2a2a]',
              'shadow-[0_16px_48px_rgba(0,0,0,0.6)]',
              // Light mode
              'light:bg-white light:border-gray-200 light:shadow-2xl',
              modalSizes[size]
            ]"
            @click.stop
          >
            <!-- Header -->
            <div
              v-if="title || showClose"
              class="flex items-center justify-between px-5 py-4 border-b border-[#2a2a2a] light:border-gray-200"
            >
              <h3 v-if="title" :id="titleId" class="text-lg font-semibold text-white light:text-gray-900">
                {{ title }}
              </h3>
              <button
                v-if="showClose"
                @click="close"
                class="p-1.5 hover:bg-white/5 rounded-md transition-colors duration-100 text-[#6b6b6b] hover:text-[#b4b4b4] light:hover:bg-gray-100 light:text-gray-400 light:hover:text-gray-600"
                aria-label="Close"
              >
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>

            <!-- Body -->
            <div class="px-5 py-5 text-[#b4b4b4] light:text-gray-600">
              <slot />
            </div>

            <!-- Footer (optional) -->
            <div
              v-if="$slots.footer"
              class="px-5 py-4 bg-[#0f1011] rounded-b-xl border-t border-[#2a2a2a] light:bg-gray-50 light:border-gray-200"
            >
              <slot name="footer" />
            </div>
          </div>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>