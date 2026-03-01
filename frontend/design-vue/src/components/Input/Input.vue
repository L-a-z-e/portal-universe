<script setup lang="ts">
import { computed, inject, useAttrs, type ComputedRef, type Ref } from 'vue';
import type { InputProps } from './Input.types';

defineOptions({ inheritAttrs: false });

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

import { inputSizes } from '@portal/design-core';

// FormField context injection
const formFieldId = inject<Ref<string> | ComputedRef<string> | null>('formFieldId', null);
const formFieldError = inject<ComputedRef<boolean> | null>('formFieldError', null);
const formFieldDisabled = inject<ComputedRef<boolean> | null>('formFieldDisabled', null);
const formFieldDescribedBy = inject<ComputedRef<string | undefined> | null>('formFieldDescribedBy', null);

const inputId = computed(() => formFieldId?.value ?? undefined);
const isError = computed(() => props.error || formFieldError?.value);
const isDisabled = computed(() => props.disabled || formFieldDisabled?.value);
const ariaDescribedBy = computed(() => formFieldDescribedBy?.value);
</script>

<template>
  <div class="input-wrapper w-full">
    <!-- Label -->
    <label
      v-if="label"
      class="block text-sm font-medium text-text-body mb-1.5"
    >
      {{ label }}
      <span v-if="required" class="text-status-error ml-0.5">*</span>
    </label>

    <!-- Input - Using design tokens -->
    <input
      v-bind="$attrs"
      :id="inputId"
      :type="type"
      :value="modelValue"
      :placeholder="placeholder"
      :disabled="isDisabled"
      :aria-invalid="isError || undefined"
      :aria-describedby="ariaDescribedBy"
      @input="handleInput"
      :class="[
        'w-full rounded-md',
        // Use semantic design tokens (responds to theme automatically)
        'bg-bg-card',
        'text-text-body placeholder:text-text-muted',
        'border border-border-default',
        // Transitions
        'transition-all duration-150 ease-out',
        // Focus state
        'focus:outline-none focus:ring-2 focus:ring-brand-primary/30 focus:border-brand-primary',
        // Hover state
        'hover:border-border-hover',
        // Sizing
        inputSizes[size],
        // Error state
        isError
          ? 'border-status-error focus:border-status-error focus:ring-status-error/30'
          : '',
        // Disabled state
        isDisabled && 'bg-bg-elevated cursor-not-allowed opacity-50'
      ]"
    />

    <!-- Error Message (standalone, without FormField) -->
    <p
      v-if="!formFieldError && error && errorMessage"
      class="mt-1.5 text-sm text-status-error"
    >
      {{ errorMessage }}
    </p>
  </div>
</template>