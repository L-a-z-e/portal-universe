<script setup lang="ts">
import { computed, ref, watch, onMounted } from 'vue';
import type { CheckboxProps, CheckboxEmits } from './Checkbox.types';

const props = withDefaults(defineProps<CheckboxProps>(), {
  modelValue: false,
  indeterminate: false,
  disabled: false,
  error: false,
  size: 'md',
});

const emit = defineEmits<CheckboxEmits>();

const inputRef = ref<HTMLInputElement | null>(null);
const uniqueId = computed(() => props.id || `checkbox-${Math.random().toString(36).substr(2, 9)}`);
const errorId = computed(() => props.errorMessage ? `${uniqueId.value}-error` : undefined);

const sizeClasses = {
  sm: {
    box: 'w-4 h-4',
    label: 'text-sm',
    icon: 'w-3 h-3',
  },
  md: {
    box: 'w-5 h-5',
    label: 'text-base',
    icon: 'w-3.5 h-3.5',
  },
  lg: {
    box: 'w-6 h-6',
    label: 'text-lg',
    icon: 'w-4 h-4',
  },
};

const boxClasses = computed(() => [
  'relative flex items-center justify-center rounded border-2 transition-all duration-200',
  sizeClasses[props.size].box,
  props.disabled
    ? 'bg-gray-100 border-gray-300 cursor-not-allowed dark:bg-gray-700 dark:border-gray-600'
    : props.error
      ? 'border-status-error focus-within:ring-2 focus-within:ring-status-error/20'
      : props.modelValue || props.indeterminate
        ? 'bg-brand-600 border-brand-600 dark:bg-brand-500 dark:border-brand-500'
        : 'border-border-default hover:border-brand-500 focus-within:ring-2 focus-within:ring-brand-500/20 dark:border-gray-500',
]);

const handleChange = (event: Event) => {
  if (props.disabled) return;
  const target = event.target as HTMLInputElement;
  emit('update:modelValue', target.checked);
  emit('change', target.checked);
};

watch(
  () => props.indeterminate,
  (value) => {
    if (inputRef.value) {
      inputRef.value.indeterminate = value;
    }
  }
);

onMounted(() => {
  if (inputRef.value && props.indeterminate) {
    inputRef.value.indeterminate = true;
  }
});
</script>

<template>
  <div class="inline-flex flex-col">
    <label
      :for="uniqueId"
      :class="[
        'inline-flex items-center gap-2 cursor-pointer',
        disabled && 'cursor-not-allowed opacity-60',
      ]"
    >
      <span :class="boxClasses">
        <input
          :id="uniqueId"
          ref="inputRef"
          type="checkbox"
          :checked="modelValue"
          :disabled="disabled"
          :name="name"
          :value="value"
          :aria-checked="indeterminate ? 'mixed' : modelValue"
          :aria-describedby="errorId"
          :aria-invalid="error"
          class="absolute inset-0 opacity-0 cursor-pointer disabled:cursor-not-allowed"
          @change="handleChange"
        />
        <!-- Check icon -->
        <svg
          v-if="modelValue && !indeterminate"
          :class="['text-white', sizeClasses[size].icon]"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          stroke-width="3"
        >
          <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
        </svg>
        <!-- Indeterminate icon -->
        <svg
          v-if="indeterminate"
          :class="['text-white', sizeClasses[size].icon]"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          stroke-width="3"
        >
          <path stroke-linecap="round" stroke-linejoin="round" d="M5 12h14" />
        </svg>
      </span>
      <span
        v-if="label"
        :class="[
          'text-text-body select-none',
          sizeClasses[size].label,
        ]"
      >
        {{ label }}
      </span>
      <slot />
    </label>
    <p
      v-if="error && errorMessage"
      :id="errorId"
      class="mt-1 text-sm text-status-error"
    >
      {{ errorMessage }}
    </p>
  </div>
</template>
