<script setup lang="ts">
import { computed } from 'vue';
import type { TextareaProps } from './Input.types';

const props = withDefaults(defineProps<TextareaProps>(), {
  modelValue: '',
  rows: 4,
  resize: 'vertical',
  size: 'md',
  disabled: false,
  error: false,
  required: false,
});

const emit = defineEmits<{
  'update:modelValue': [value: string];
}>();

const sizeClasses: Record<string, string> = {
  sm: 'px-3 py-2 text-sm',
  md: 'px-3 py-2.5 text-base',
  lg: 'px-4 py-3 text-lg',
};

const resizeClasses: Record<string, string> = {
  none: 'resize-none',
  vertical: 'resize-y',
  horizontal: 'resize-x',
  both: 'resize',
};

const inputId = computed(() => props.id || `textarea-${Math.random().toString(36).slice(2, 9)}`);

const handleInput = (event: Event) => {
  const target = event.target as HTMLTextAreaElement;
  emit('update:modelValue', target.value);
};
</script>

<template>
  <div class="w-full">
    <label
      v-if="label"
      :for="inputId"
      :class="[
        'block mb-1.5 text-sm font-medium text-text-body',
        disabled && 'opacity-50',
      ]"
    >
      {{ label }}
      <span v-if="required" class="text-status-error ml-0.5">*</span>
    </label>

    <textarea
      :id="inputId"
      :value="modelValue"
      :placeholder="placeholder"
      :disabled="disabled"
      :rows="rows"
      :required="required"
      :name="name"
      :aria-invalid="error || undefined"
      :aria-describedby="error && errorMessage ? `${inputId}-error` : undefined"
      :class="[
        'w-full rounded-md border bg-bg-card text-text-body placeholder:text-text-muted',
        'transition-all duration-150',
        'focus:outline-none focus:ring-2 focus:ring-brand-primary focus:border-transparent',
        sizeClasses[size],
        resizeClasses[resize],
        error
          ? 'border-status-error focus:ring-status-error'
          : 'border-border-default hover:border-border-hover',
        disabled && 'opacity-50 cursor-not-allowed bg-bg-muted',
      ]"
      @input="handleInput"
    />

    <p
      v-if="error && errorMessage"
      :id="`${inputId}-error`"
      class="mt-1.5 text-sm text-status-error"
    >
      {{ errorMessage }}
    </p>
  </div>
</template>
