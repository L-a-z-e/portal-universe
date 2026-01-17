<script setup lang="ts">
import { computed, provide } from 'vue';
import type { FormFieldProps } from './FormField.types';

const props = withDefaults(defineProps<FormFieldProps>(), {
  required: false,
  error: false,
  disabled: false,
  size: 'md',
});

const uniqueId = computed(() => props.id || `form-field-${Math.random().toString(36).substr(2, 9)}`);
const errorId = computed(() => props.errorMessage ? `${uniqueId.value}-error` : undefined);
const helperId = computed(() => props.helperText ? `${uniqueId.value}-helper` : undefined);
const describedBy = computed(() => [errorId.value, helperId.value].filter(Boolean).join(' ') || undefined);

const sizeClasses = {
  sm: {
    label: 'text-xs',
    helper: 'text-xs',
  },
  md: {
    label: 'text-sm',
    helper: 'text-sm',
  },
  lg: {
    label: 'text-base',
    helper: 'text-base',
  },
};

// Provide context to child components
provide('formFieldId', uniqueId);
provide('formFieldError', computed(() => props.error));
provide('formFieldDisabled', computed(() => props.disabled));
provide('formFieldDescribedBy', describedBy);
</script>

<template>
  <div :class="['w-full', disabled && 'opacity-60']">
    <!-- Label -->
    <label
      v-if="label || $slots.label"
      :for="uniqueId"
      :class="[
        'block font-medium text-text-body mb-1.5',
        sizeClasses[size].label,
      ]"
    >
      <slot name="label">
        {{ label }}
        <span v-if="required" class="text-status-error ml-0.5">*</span>
      </slot>
    </label>

    <!-- Field content -->
    <div class="relative">
      <slot />
    </div>

    <!-- Helper text -->
    <p
      v-if="helperText && !error"
      :id="helperId"
      :class="[
        'mt-1.5 text-text-muted',
        sizeClasses[size].helper,
      ]"
    >
      <slot name="helper">
        {{ helperText }}
      </slot>
    </p>

    <!-- Error message -->
    <p
      v-if="error && errorMessage"
      :id="errorId"
      :class="[
        'mt-1.5 text-status-error',
        sizeClasses[size].helper,
      ]"
    >
      <slot name="error">
        {{ errorMessage }}
      </slot>
    </p>
  </div>
</template>
