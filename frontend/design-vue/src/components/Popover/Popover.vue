<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, computed } from 'vue';
import type { PopoverProps } from '@portal/design-core';

const props = withDefaults(defineProps<PopoverProps & { modelValue?: boolean }>(), {
  placement: 'bottom',
  trigger: 'click',
  closeOnClickOutside: true,
});

const emit = defineEmits<{
  'update:modelValue': [value: boolean];
}>();

const internalOpen = ref(false);
const containerRef = ref<HTMLElement | null>(null);

const isControlled = computed(() => props.modelValue !== undefined);
const isOpen = computed(() => (isControlled.value ? props.modelValue : internalOpen.value));

const setOpen = (value: boolean) => {
  if (!isControlled.value) {
    internalOpen.value = value;
  }
  emit('update:modelValue', value);
};

const placementClasses: Record<string, string> = {
  'bottom': 'top-full left-1/2 -translate-x-1/2 mt-2',
  'bottom-start': 'top-full left-0 mt-2',
  'bottom-end': 'top-full right-0 mt-2',
  'top': 'bottom-full left-1/2 -translate-x-1/2 mb-2',
  'top-start': 'bottom-full left-0 mb-2',
  'top-end': 'bottom-full right-0 mb-2',
};

const handleTriggerClick = () => {
  if (props.trigger === 'click') {
    setOpen(!isOpen.value);
  }
};

const handleTriggerMouseEnter = () => {
  if (props.trigger === 'hover') {
    setOpen(true);
  }
};

const handleMouseLeave = () => {
  if (props.trigger === 'hover') {
    setOpen(false);
  }
};

const handleClickOutside = (event: MouseEvent) => {
  if (
    props.closeOnClickOutside &&
    isOpen.value &&
    containerRef.value &&
    !containerRef.value.contains(event.target as Node)
  ) {
    setOpen(false);
  }
};

onMounted(() => {
  document.addEventListener('mousedown', handleClickOutside);
});

onBeforeUnmount(() => {
  document.removeEventListener('mousedown', handleClickOutside);
});
</script>

<template>
  <div
    ref="containerRef"
    class="relative inline-block"
    @mouseleave="handleMouseLeave"
  >
    <div
      @click="handleTriggerClick"
      @mouseenter="handleTriggerMouseEnter"
    >
      <slot />
    </div>

    <Transition
      enter-active-class="transition duration-150 ease-out"
      enter-from-class="opacity-0 scale-95"
      enter-to-class="opacity-100 scale-100"
      leave-active-class="transition duration-100 ease-in"
      leave-from-class="opacity-100 scale-100"
      leave-to-class="opacity-0 scale-95"
    >
      <div
        v-if="isOpen"
        :class="[
          'absolute z-50 p-4 rounded-lg border border-border-default',
          'bg-bg-card shadow-lg',
          placementClasses[placement],
        ]"
      >
        <slot name="content" />
      </div>
    </Transition>
  </div>
</template>
