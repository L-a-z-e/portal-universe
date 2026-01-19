<script setup lang="ts">
import { computed } from 'vue';
import type { RadioProps, RadioEmits } from './Radio.types';

const props = withDefaults(defineProps<RadioProps>(), {
  direction: 'vertical',
  disabled: false,
  error: false,
  size: 'md',
});

const emit = defineEmits<RadioEmits>();

const groupId = computed(() => `radio-group-${Math.random().toString(36).substr(2, 9)}`);
const errorId = computed(() => props.errorMessage ? `${groupId.value}-error` : undefined);

const sizeClasses = {
  sm: {
    radio: 'w-4 h-4',
    dot: 'w-1.5 h-1.5',
    label: 'text-sm',
    gap: 'gap-2',
  },
  md: {
    radio: 'w-5 h-5',
    dot: 'w-2 h-2',
    label: 'text-base',
    gap: 'gap-3',
  },
  lg: {
    radio: 'w-6 h-6',
    dot: 'w-2.5 h-2.5',
    label: 'text-lg',
    gap: 'gap-3',
  },
};

const containerClasses = computed(() => [
  'flex',
  props.direction === 'horizontal' ? 'flex-row flex-wrap gap-4' : 'flex-col gap-2',
]);

const getRadioClasses = (option: RadioProps['options'][0]) => {
  const isSelected = props.modelValue === option.value;
  const isDisabled = props.disabled || option.disabled;

  return [
    'relative flex items-center justify-center rounded-full border-2 transition-all duration-200',
    sizeClasses[props.size].radio,
    isDisabled
      ? 'bg-gray-100 border-gray-300 cursor-not-allowed dark:bg-gray-700 dark:border-gray-600'
      : props.error
        ? 'border-status-error focus-within:ring-2 focus-within:ring-status-error/20'
        : isSelected
          ? 'border-brand-600 dark:border-brand-500'
          : 'border-border-default hover:border-brand-500 focus-within:ring-2 focus-within:ring-brand-500/20 dark:border-gray-500',
  ];
};

const getDotClasses = (option: RadioProps['options'][0]) => {
  const isSelected = props.modelValue === option.value;
  const isDisabled = props.disabled || option.disabled;

  return [
    'rounded-full transition-all duration-200',
    sizeClasses[props.size].dot,
    isSelected
      ? isDisabled
        ? 'bg-gray-400 dark:bg-gray-500'
        : 'bg-brand-600 dark:bg-brand-500'
      : 'bg-transparent',
  ];
};

const handleChange = (value: string | number) => {
  emit('update:modelValue', value);
  emit('change', value);
};
</script>

<template>
  <div
    role="radiogroup"
    :aria-describedby="errorId"
    :aria-invalid="error"
  >
    <div :class="containerClasses">
      <label
        v-for="option in options"
        :key="String(option.value)"
        :class="[
          'inline-flex items-center cursor-pointer',
          sizeClasses[size].gap,
          (disabled || option.disabled) && 'cursor-not-allowed opacity-60',
        ]"
      >
        <span :class="getRadioClasses(option)">
          <input
            type="radio"
            :name="name"
            :value="option.value"
            :checked="modelValue === option.value"
            :disabled="disabled || option.disabled"
            class="absolute inset-0 opacity-0 cursor-pointer disabled:cursor-not-allowed"
            @change="handleChange(option.value)"
          />
          <span :class="getDotClasses(option)" />
        </span>
        <span
          :class="[
            'text-text-body select-none',
            sizeClasses[size].label,
          ]"
        >
          {{ option.label }}
        </span>
      </label>
    </div>
    <p
      v-if="error && errorMessage"
      :id="errorId"
      class="mt-2 text-sm text-status-error"
    >
      {{ errorMessage }}
    </p>
  </div>
</template>
