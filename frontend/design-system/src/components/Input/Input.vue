<script setup lang="ts">
import type { InputProps } from './Input.types';

const props = withDefaults(defineProps<InputProps>(), {
  type: 'text',
  modelValue: '',
  placeholder: '',
  disabled: false,
  error: false,
  errorMessage: '',
  label: '',
  required: false,
  size: 'md'
});

const emit = defineEmits<{
  'update:modelValue': [value: string | number]
}>();

function handleInput(event: Event) {
  const target = event.target as HTMLInputElement;
  emit('update:modelValue', target.value);
}

const sizeClasses = {
  sm: 'px-3 py-1.5 text-sm',
  md: 'px-3 py-2 text-sm',
  lg: 'px-4 py-2.5 text-base'
};
</script>

<template>
  <div class="input-wrapper">
    <!-- Label -->
    <label
      v-if="label"
      class="block text-sm font-medium text-text-body mb-1.5"
    >
      {{ label }}
      <span v-if="required" class="text-status-error ml-0.5">*</span>
    </label>

    <!-- Input -->
    <input
      :type="type"
      :value="modelValue"
      :placeholder="placeholder"
      :disabled="disabled"
      @input="handleInput"
      :class="[
        'w-full rounded-md',
        'bg-transparent',
        'border',
        'text-text-body placeholder:text-text-muted',
        'transition-all duration-[160ms] ease-[cubic-bezier(0.25,0.1,0.25,1)]',
        'focus:outline-none focus:ring-2 focus:ring-brand-primary/20',
        sizeClasses[size],
        error
          ? 'border-status-error focus:border-status-error'
          : 'border-border-default focus:border-brand-primary hover:border-border-hover',
        disabled && 'bg-bg-muted cursor-not-allowed opacity-50'
      ]"
    />

    <!-- Error Message -->
    <p
      v-if="error && errorMessage"
      class="mt-1.5 text-sm text-status-error"
    >
      {{ errorMessage }}
    </p>
  </div>
</template>