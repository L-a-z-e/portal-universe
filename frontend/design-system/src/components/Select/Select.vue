<script setup lang="ts">
import { computed, ref, watch, onMounted, onUnmounted } from 'vue';
import type { SelectProps, SelectEmits } from './Select.types';

const props = withDefaults(defineProps<SelectProps>(), {
  placeholder: 'Select an option',
  disabled: false,
  error: false,
  required: false,
  clearable: false,
  searchable: false,
  size: 'md',
});

const emit = defineEmits<SelectEmits>();

const isOpen = ref(false);
const searchQuery = ref('');
const highlightedIndex = ref(-1);
const selectRef = ref<HTMLDivElement | null>(null);
const inputRef = ref<HTMLInputElement | null>(null);
const listRef = ref<HTMLUListElement | null>(null);

const uniqueId = computed(() => props.id || `select-${Math.random().toString(36).substr(2, 9)}`);
const errorId = computed(() => props.errorMessage ? `${uniqueId.value}-error` : undefined);
const listboxId = computed(() => `${uniqueId.value}-listbox`);

const sizeClasses = {
  sm: {
    trigger: 'h-8 px-3 text-sm',
    option: 'px-3 py-1.5 text-sm',
    label: 'text-sm',
  },
  md: {
    trigger: 'h-10 px-4 text-base',
    option: 'px-4 py-2 text-base',
    label: 'text-sm',
  },
  lg: {
    trigger: 'h-12 px-5 text-lg',
    option: 'px-5 py-3 text-lg',
    label: 'text-base',
  },
};

const selectedOption = computed(() =>
  props.options.find(opt => opt.value === props.modelValue)
);

const filteredOptions = computed(() => {
  if (!props.searchable || !searchQuery.value) {
    return props.options;
  }
  const query = searchQuery.value.toLowerCase();
  return props.options.filter(opt =>
    opt.label.toLowerCase().includes(query)
  );
});

const triggerClasses = computed(() => [
  'w-full flex items-center justify-between rounded-lg border transition-all duration-200',
  'focus:outline-none focus:ring-2',
  sizeClasses[props.size].trigger,
  props.disabled
    ? 'bg-gray-100 border-gray-300 cursor-not-allowed text-gray-400 dark:bg-gray-700 dark:border-gray-600 dark:text-gray-500'
    : props.error
      ? 'border-status-error focus:ring-status-error/20 bg-white dark:bg-gray-800'
      : isOpen.value
        ? 'border-brand-500 ring-2 ring-brand-500/20 bg-white dark:bg-gray-800'
        : 'border-border-default hover:border-brand-500 bg-white dark:bg-gray-800 dark:border-gray-600',
]);

const open = () => {
  if (props.disabled) return;
  isOpen.value = true;
  highlightedIndex.value = props.modelValue
    ? filteredOptions.value.findIndex(opt => opt.value === props.modelValue)
    : 0;
  emit('open');

  if (props.searchable) {
    setTimeout(() => inputRef.value?.focus(), 0);
  }
};

const close = () => {
  isOpen.value = false;
  searchQuery.value = '';
  highlightedIndex.value = -1;
  emit('close');
};

const toggle = () => {
  if (isOpen.value) {
    close();
  } else {
    open();
  }
};

const selectOption = (option: SelectProps['options'][0]) => {
  if (option.disabled) return;
  emit('update:modelValue', option.value);
  emit('change', option.value);
  close();
};

const clear = (event: Event) => {
  event.stopPropagation();
  emit('update:modelValue', null);
  emit('change', null);
};

const handleKeydown = (event: KeyboardEvent) => {
  if (!isOpen.value) {
    if (event.key === 'Enter' || event.key === ' ' || event.key === 'ArrowDown') {
      event.preventDefault();
      open();
    }
    return;
  }

  switch (event.key) {
    case 'ArrowDown':
      event.preventDefault();
      highlightedIndex.value = Math.min(
        highlightedIndex.value + 1,
        filteredOptions.value.length - 1
      );
      scrollToHighlighted();
      break;
    case 'ArrowUp':
      event.preventDefault();
      highlightedIndex.value = Math.max(highlightedIndex.value - 1, 0);
      scrollToHighlighted();
      break;
    case 'Enter':
      event.preventDefault();
      if (highlightedIndex.value >= 0 && filteredOptions.value[highlightedIndex.value]) {
        selectOption(filteredOptions.value[highlightedIndex.value]);
      }
      break;
    case 'Escape':
      event.preventDefault();
      close();
      break;
    case 'Home':
      event.preventDefault();
      highlightedIndex.value = 0;
      scrollToHighlighted();
      break;
    case 'End':
      event.preventDefault();
      highlightedIndex.value = filteredOptions.value.length - 1;
      scrollToHighlighted();
      break;
  }
};

const scrollToHighlighted = () => {
  if (listRef.value && highlightedIndex.value >= 0) {
    const items = listRef.value.querySelectorAll('[role="option"]');
    items[highlightedIndex.value]?.scrollIntoView({ block: 'nearest' });
  }
};

const handleClickOutside = (event: MouseEvent) => {
  if (selectRef.value && !selectRef.value.contains(event.target as Node)) {
    close();
  }
};

const handleSearchInput = (event: Event) => {
  const value = (event.target as HTMLInputElement).value;
  searchQuery.value = value;
  highlightedIndex.value = 0;
  emit('search', value);
};

watch(searchQuery, () => {
  highlightedIndex.value = 0;
});

onMounted(() => {
  document.addEventListener('click', handleClickOutside);
});

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside);
});
</script>

<template>
  <div ref="selectRef" class="relative w-full">
    <!-- Label -->
    <label
      v-if="label"
      :for="uniqueId"
      :class="['block font-medium text-text-body mb-1', sizeClasses[size].label]"
    >
      {{ label }}
      <span v-if="required" class="text-status-error">*</span>
    </label>

    <!-- Trigger -->
    <button
      :id="uniqueId"
      type="button"
      role="combobox"
      :aria-expanded="isOpen"
      :aria-controls="listboxId"
      :aria-haspopup="'listbox'"
      :aria-describedby="errorId"
      :aria-invalid="error"
      :disabled="disabled"
      :name="name"
      :class="triggerClasses"
      @click="toggle"
      @keydown="handleKeydown"
    >
      <span :class="[!selectedOption && 'text-text-muted']">
        {{ selectedOption?.label || placeholder }}
      </span>
      <div class="flex items-center gap-1">
        <!-- Clear button -->
        <span
          v-if="clearable && selectedOption && !disabled"
          class="p-1 hover:bg-gray-100 rounded dark:hover:bg-gray-700"
          @click="clear"
        >
          <svg class="w-4 h-4 text-text-muted" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </span>
        <!-- Chevron -->
        <svg
          :class="[
            'w-5 h-5 text-text-muted transition-transform duration-200',
            isOpen && 'rotate-180',
          ]"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
        >
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
        </svg>
      </div>
    </button>

    <!-- Dropdown -->
    <Transition
      enter-active-class="transition duration-100 ease-out"
      enter-from-class="transform scale-95 opacity-0"
      enter-to-class="transform scale-100 opacity-100"
      leave-active-class="transition duration-75 ease-in"
      leave-from-class="transform scale-100 opacity-100"
      leave-to-class="transform scale-95 opacity-0"
    >
      <div
        v-if="isOpen"
        class="absolute z-50 w-full mt-1 bg-white border border-border-default rounded-lg shadow-lg dark:bg-gray-800 dark:border-gray-600"
      >
        <!-- Search input -->
        <div v-if="searchable" class="p-2 border-b border-border-default dark:border-gray-600">
          <input
            ref="inputRef"
            type="text"
            :value="searchQuery"
            placeholder="Search..."
            class="w-full px-3 py-2 text-sm border border-border-default rounded-md focus:outline-none focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500 dark:bg-gray-700 dark:border-gray-600 dark:text-gray-100"
            @input="handleSearchInput"
            @keydown="handleKeydown"
          />
        </div>

        <!-- Options list -->
        <ul
          :id="listboxId"
          ref="listRef"
          role="listbox"
          class="max-h-60 overflow-auto py-1"
        >
          <li
            v-for="(option, index) in filteredOptions"
            :key="String(option.value)"
            role="option"
            :aria-selected="modelValue === option.value"
            :aria-disabled="option.disabled"
            :class="[
              'cursor-pointer transition-colors duration-100',
              sizeClasses[size].option,
              option.disabled
                ? 'text-text-muted cursor-not-allowed'
                : highlightedIndex === index
                  ? 'bg-brand-50 text-brand-700 dark:bg-brand-900/30 dark:text-brand-300'
                  : modelValue === option.value
                    ? 'bg-gray-100 dark:bg-gray-700'
                    : 'hover:bg-gray-50 dark:hover:bg-gray-700',
            ]"
            @click="selectOption(option)"
            @mouseenter="highlightedIndex = index"
          >
            <div class="flex items-center justify-between">
              <span>{{ option.label }}</span>
              <svg
                v-if="modelValue === option.value"
                class="w-5 h-5 text-brand-600 dark:text-brand-400"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
              </svg>
            </div>
          </li>
          <li
            v-if="filteredOptions.length === 0"
            class="px-4 py-3 text-center text-text-muted"
          >
            No options found
          </li>
        </ul>
      </div>
    </Transition>

    <!-- Error message -->
    <p
      v-if="error && errorMessage"
      :id="errorId"
      class="mt-1 text-sm text-status-error"
    >
      {{ errorMessage }}
    </p>
  </div>
</template>
