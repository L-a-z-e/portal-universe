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
  required: false
});

const emit = defineEmits<{
  'update:modelValue': [value: string | number]
}>();

function handleInput(event: Event) {
  const target = event.target as HTMLInputElement;
  emit('update:modelValue', target.value);
}
</script>

<template>
  <div class="input-wrapper">
    <!-- Label -->
    <label v-if="label" class="block text-sm font-medium text-gray-700 mb-1">
      {{ label }}
      <span v-if="required" class="text-red-500">*</span>
    </label>

    <!-- Input -->
    <input
        :type="type"
        :value="modelValue"
        :placeholder="placeholder"
        :disabled="disabled"
        @input="handleInput"
        :class="[
        'w-full px-4 py-2 border rounded-lg transition-all duration-200',
        'focus:outline-none focus:ring-2',
        error
          ? 'border-red-500 focus:ring-red-500/20'
          : 'border-gray-300 focus:ring-brand-500/20 focus:border-brand-500',
        disabled && 'bg-gray-100 cursor-not-allowed opacity-60'
      ]"
    />

    <!-- Error Message -->
    <p v-if="error && errorMessage" class="mt-1 text-sm text-red-600">
      {{ errorMessage }}
    </p>
  </div>
</template>