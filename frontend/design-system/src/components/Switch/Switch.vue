<script setup lang="ts">
import { computed } from 'vue';
import type { SwitchProps, SwitchEmits } from './Switch.types';

const props = withDefaults(defineProps<SwitchProps>(), {
  modelValue: false,
  disabled: false,
  labelPosition: 'right',
  size: 'md',
  activeColor: 'primary',
});

const emit = defineEmits<SwitchEmits>();

const uniqueId = computed(() => props.id || `switch-${Math.random().toString(36).substr(2, 9)}`);

const sizeClasses = {
  sm: {
    track: 'w-8 h-4',
    thumb: 'w-3 h-3',
    translate: 'translate-x-4',
    label: 'text-sm',
  },
  md: {
    track: 'w-11 h-6',
    thumb: 'w-5 h-5',
    translate: 'translate-x-5',
    label: 'text-base',
  },
  lg: {
    track: 'w-14 h-7',
    thumb: 'w-6 h-6',
    translate: 'translate-x-7',
    label: 'text-lg',
  },
};

const activeColorClasses = {
  primary: 'bg-brand-600 dark:bg-brand-500',
  success: 'bg-status-success',
  warning: 'bg-status-warning',
  error: 'bg-status-error',
};

const trackClasses = computed(() => [
  'relative inline-flex items-center rounded-full transition-colors duration-200',
  sizeClasses[props.size].track,
  props.disabled
    ? 'bg-gray-200 cursor-not-allowed dark:bg-gray-600'
    : props.modelValue
      ? activeColorClasses[props.activeColor]
      : 'bg-gray-300 dark:bg-gray-600',
]);

const thumbClasses = computed(() => [
  'absolute left-0.5 rounded-full bg-white shadow-sm transition-transform duration-200',
  sizeClasses[props.size].thumb,
  props.modelValue ? sizeClasses[props.size].translate : 'translate-x-0',
  props.disabled ? 'bg-gray-100 dark:bg-gray-400' : '',
]);

const handleChange = () => {
  if (props.disabled) return;
  const newValue = !props.modelValue;
  emit('update:modelValue', newValue);
  emit('change', newValue);
};
</script>

<template>
  <label
    :for="uniqueId"
    :class="[
      'inline-flex items-center gap-3 cursor-pointer',
      labelPosition === 'left' ? 'flex-row-reverse' : 'flex-row',
      disabled && 'cursor-not-allowed opacity-60',
    ]"
  >
    <button
      :id="uniqueId"
      type="button"
      role="switch"
      :aria-checked="modelValue"
      :disabled="disabled"
      :name="name"
      :class="trackClasses"
      class="focus:outline-none focus:ring-2 focus:ring-brand-500 focus:ring-offset-2"
      @click="handleChange"
    >
      <span :class="thumbClasses" />
    </button>
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
</template>
