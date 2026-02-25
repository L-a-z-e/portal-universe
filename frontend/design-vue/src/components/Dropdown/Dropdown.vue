<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted } from 'vue';
import type { DropdownProps, DropdownEmits, DropdownItem } from './Dropdown.types';

const props = withDefaults(defineProps<DropdownProps>(), {
  trigger: 'click',
  placement: 'bottom-start',
  disabled: false,
  closeOnSelect: true,
  width: 'auto',
});

const emit = defineEmits<DropdownEmits>();

const isOpen = ref(false);
const highlightedIndex = ref(-1);
const dropdownRef = ref<HTMLDivElement | null>(null);
const triggerRef = ref<HTMLDivElement | null>(null);
const menuRef = ref<HTMLDivElement | null>(null);

const menuId = computed(() => `dropdown-menu-${Math.random().toString(36).substr(2, 9)}`);

const placementClasses = {
  'bottom': 'top-full left-1/2 -translate-x-1/2 mt-1',
  'bottom-start': 'top-full left-0 mt-1',
  'bottom-end': 'top-full right-0 mt-1',
  'top': 'bottom-full left-1/2 -translate-x-1/2 mb-1',
  'top-start': 'bottom-full left-0 mb-1',
  'top-end': 'bottom-full right-0 mb-1',
};

const menuClasses = computed(() => [
  'absolute z-50 py-1 bg-bg-elevated border border-border-default rounded-lg shadow-lg whitespace-nowrap',
  placementClasses[props.placement],
]);

const menuStyles = computed(() => {
  if (props.width === 'auto') return {};
  if (props.width === 'trigger' && triggerRef.value) {
    return { width: `${triggerRef.value.offsetWidth}px` };
  }
  return { width: props.width };
});

const selectableItems = computed(() =>
  props.items.filter(item => !item.divider && !item.disabled)
);

const open = () => {
  if (props.disabled) return;
  isOpen.value = true;
  highlightedIndex.value = -1;
  emit('open');
};

const close = () => {
  isOpen.value = false;
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

const selectItem = (item: DropdownItem) => {
  if (item.disabled || item.divider) return;
  emit('select', item);
  if (props.closeOnSelect) {
    close();
  }
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
        selectableItems.value.length - 1
      );
      break;
    case 'ArrowUp':
      event.preventDefault();
      highlightedIndex.value = Math.max(highlightedIndex.value - 1, 0);
      break;
    case 'Enter':
    case ' ':
      event.preventDefault();
      if (highlightedIndex.value >= 0 && selectableItems.value[highlightedIndex.value]) {
        selectItem(selectableItems.value[highlightedIndex.value]);
      }
      break;
    case 'Escape':
      event.preventDefault();
      close();
      break;
    case 'Tab':
      close();
      break;
  }
};

const handleClickOutside = (event: MouseEvent) => {
  if (dropdownRef.value && !dropdownRef.value.contains(event.target as Node)) {
    close();
  }
};

const handleMouseEnter = () => {
  if (props.trigger === 'hover') {
    open();
  }
};

const handleMouseLeave = () => {
  if (props.trigger === 'hover') {
    close();
  }
};

const getItemIndex = (item: DropdownItem): number => {
  return selectableItems.value.indexOf(item);
};

onMounted(() => {
  document.addEventListener('click', handleClickOutside);
});

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside);
});
</script>

<template>
  <div
    ref="dropdownRef"
    class="relative inline-block"
    @mouseenter="handleMouseEnter"
    @mouseleave="handleMouseLeave"
  >
    <!-- Trigger -->
    <div
      ref="triggerRef"
      role="button"
      :aria-haspopup="'menu'"
      :aria-expanded="isOpen"
      :aria-controls="menuId"
      :aria-disabled="disabled"
      :tabindex="disabled ? -1 : 0"
      :class="[disabled && 'cursor-not-allowed opacity-60']"
      @click="trigger === 'click' && toggle()"
      @keydown="handleKeydown"
    >
      <slot name="trigger">
        <button
          type="button"
          class="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-text-body bg-bg-elevated border border-border-default rounded-lg hover:bg-bg-hover focus:outline-none focus:ring-2 focus:ring-brand-primary focus:ring-offset-2"
        >
          Options
          <svg
            :class="['w-4 h-4 transition-transform', isOpen && 'rotate-180']"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
          </svg>
        </button>
      </slot>
    </div>

    <!-- Menu -->
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
        :id="menuId"
        ref="menuRef"
        role="menu"
        :class="menuClasses"
        :style="menuStyles"
      >
        <template v-for="(item, index) in items" :key="index">
          <!-- Divider -->
          <div
            v-if="item.divider"
            class="my-1 border-t border-border-default"
            role="separator"
          />
          <!-- Menu item -->
          <button
            v-else
            type="button"
            role="menuitem"
            :disabled="item.disabled"
            :class="[
              'w-full flex items-center gap-2 px-4 py-2 text-sm text-left transition-colors',
              item.disabled
                ? 'text-text-muted cursor-not-allowed'
                : getItemIndex(item) === highlightedIndex
                  ? 'bg-bg-hover text-brand-primary'
                  : 'text-text-body hover:bg-bg-hover',
            ]"
            @click="selectItem(item)"
            @mouseenter="highlightedIndex = getItemIndex(item)"
          >
            <slot name="item" :item="item">
              {{ item.label }}
            </slot>
          </button>
        </template>
      </div>
    </Transition>
  </div>
</template>
