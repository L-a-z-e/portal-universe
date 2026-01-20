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

// Linear-inspired sizing
const sizeClasses = {
  sm: 'h-8 px-3 text-sm',
  md: 'h-9 px-3 text-sm',
  lg: 'h-11 px-4 text-base'
};
</script>

<template>
  <div class="input-wrapper w-full">
    <!-- Label -->
    <label
      v-if="label"
      class="block text-sm font-medium text-[#b4b4b4] mb-1.5 light:text-gray-700"
    >
      {{ label }}
      <span v-if="required" class="text-[#E03131] ml-0.5">*</span>
    </label>

    <!-- Input - Linear style -->
    <input
      :type="type"
      :value="modelValue"
      :placeholder="placeholder"
      :disabled="disabled"
      @input="handleInput"
      :class="[
        'w-full rounded-md',
        // Dark mode (default) - Linear style
        'bg-[#0f1011]',
        'text-[#b4b4b4] placeholder:text-[#6b6b6b]',
        'border',
        // Light mode
        'light:bg-white light:text-gray-900 light:placeholder:text-gray-400',
        // Transitions
        'transition-all duration-150 ease-out',
        // Focus state
        'focus:outline-none focus:ring-2 focus:ring-[#5e6ad2]/30 focus:border-[#5e6ad2]',
        'light:focus:ring-[#5e6ad2]/20',
        // Sizing
        sizeClasses[size],
        // Error state
        error
          ? 'border-[#E03131] focus:border-[#E03131] focus:ring-[#E03131]/30'
          : 'border-[#2a2a2a] hover:border-[#3a3a3a] light:border-gray-200 light:hover:border-gray-300',
        // Disabled state
        disabled && 'bg-[#18191b] cursor-not-allowed opacity-50 light:bg-gray-100'
      ]"
    />

    <!-- Error Message -->
    <p
      v-if="error && errorMessage"
      class="mt-1.5 text-sm text-[#E03131]"
    >
      {{ errorMessage }}
    </p>
  </div>
</template>