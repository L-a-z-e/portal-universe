<script setup lang="ts">
import type { TextareaProps } from './Input.types';

const props = withDefaults(defineProps<TextareaProps>(), {
  modelValue: '',
  placeholder: '',
  disabled: false,
  error: false,
  errorMessage: '',
  label: '',
  required: false,
  rows: 5
});

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>();

function handleInput(event: Event) {
  const target = event.target as HTMLTextAreaElement;
  emit('update:modelValue', target.value);
}
</script>

<template>
  <div class="textarea-wrapper">
    <!-- Label -->
    <label v-if="label" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
      {{ label }}
      <span v-if="required" class="text-red-500">*</span>
    </label>

    <!-- Textarea -->
    <textarea
        :value="modelValue"
        :placeholder="placeholder"
        :disabled="disabled"
        :rows="rows"
        @input="handleInput"
        :class="[
        'w-full px-4 py-2 border rounded-lg transition-all duration-200',
        'focus:outline-none focus:ring-2 resize-vertical',
        'dark:bg-gray-800 dark:text-gray-100',
        error
          ? 'border-red-500 focus:ring-red-500/20'
          : 'border-gray-300 focus:ring-brand-500/20 focus:border-brand-500 dark:border-gray-600 dark:focus:border-brand-500',
        disabled && 'bg-gray-100 dark:bg-gray-700 cursor-not-allowed opacity-60'
      ]"
    />

    <!-- Error Message -->
    <p v-if="error && errorMessage" class="mt-1 text-sm text-red-600 dark:text-red-400">
      {{ errorMessage }}
    </p>
  </div>
</template>