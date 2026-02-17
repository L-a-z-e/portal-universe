<script setup lang="ts">
import { computed, ref } from 'vue';
import type { TabsProps, TabsEmits, TabItem } from './Tabs.types';

const props = withDefaults(defineProps<TabsProps>(), {
  variant: 'default',
  size: 'md',
  fullWidth: false,
});

const emit = defineEmits<TabsEmits>();

const tablistRef = ref<HTMLDivElement | null>(null);

import { tabsSizes } from '@portal/design-core';

const containerClasses = computed(() => {
  const base = ['flex'];

  if (props.variant === 'default') {
    base.push('border-b border-border-default');
  } else if (props.variant === 'pills') {
    base.push('gap-1 p-1 bg-bg-muted rounded-lg');
  } else if (props.variant === 'underline') {
    base.push('border-b-2 border-border-default');
  }

  if (props.fullWidth) {
    base.push('w-full');
  }

  return base;
});

const getTabClasses = (tab: TabItem) => {
  const isActive = props.modelValue === tab.value;
  const base = [
    'relative inline-flex items-center justify-center font-medium transition-all duration-200',
    'focus:outline-none focus:ring-2 focus:ring-brand-primary/20 focus:ring-offset-2',
    tabsSizes[props.size],
  ];

  if (props.fullWidth) {
    base.push('flex-1');
  }

  if (tab.disabled) {
    base.push('cursor-not-allowed opacity-50');
  } else {
    base.push('cursor-pointer');
  }

  if (props.variant === 'default') {
    base.push(
      isActive
        ? 'text-brand-primary border-b-2 border-brand-primary -mb-px'
        : 'text-text-muted hover:text-text-body border-b-2 border-transparent -mb-px'
    );
  } else if (props.variant === 'pills') {
    base.push(
      'rounded-md',
      isActive
        ? 'bg-bg-elevated text-text-heading shadow-sm'
        : 'text-text-muted hover:text-text-body hover:bg-bg-hover'
    );
  } else if (props.variant === 'underline') {
    base.push(
      isActive
        ? 'text-brand-primary border-b-2 border-brand-primary -mb-0.5'
        : 'text-text-muted hover:text-text-body border-b-2 border-transparent -mb-0.5'
    );
  }

  return base;
};

const selectTab = (tab: TabItem) => {
  if (tab.disabled) return;
  emit('update:modelValue', tab.value);
  emit('change', tab.value);
};

const handleKeydown = (event: KeyboardEvent, currentIndex: number) => {
  const enabledTabs = props.items.filter(t => !t.disabled);
  const currentEnabledIndex = enabledTabs.findIndex(t => t.value === props.items[currentIndex].value);

  let newIndex = currentEnabledIndex;

  switch (event.key) {
    case 'ArrowLeft':
      event.preventDefault();
      newIndex = currentEnabledIndex > 0 ? currentEnabledIndex - 1 : enabledTabs.length - 1;
      break;
    case 'ArrowRight':
      event.preventDefault();
      newIndex = currentEnabledIndex < enabledTabs.length - 1 ? currentEnabledIndex + 1 : 0;
      break;
    case 'Home':
      event.preventDefault();
      newIndex = 0;
      break;
    case 'End':
      event.preventDefault();
      newIndex = enabledTabs.length - 1;
      break;
    default:
      return;
  }

  const newTab = enabledTabs[newIndex];
  if (newTab) {
    selectTab(newTab);
    // Focus the new tab
    const tabElements = tablistRef.value?.querySelectorAll('[role="tab"]');
    const newTabIndex = props.items.findIndex(t => t.value === newTab.value);
    (tabElements?.[newTabIndex] as HTMLElement)?.focus();
  }
};
</script>

<template>
  <div ref="tablistRef" role="tablist" :class="containerClasses">
    <button
      v-for="(tab, index) in items"
      :key="tab.value"
      role="tab"
      type="button"
      :aria-selected="modelValue === tab.value"
      :aria-disabled="tab.disabled"
      :tabindex="modelValue === tab.value ? 0 : -1"
      :class="getTabClasses(tab)"
      @click="selectTab(tab)"
      @keydown="handleKeydown($event, index)"
    >
      <slot name="tab" :tab="tab" :active="modelValue === tab.value">
        {{ tab.label }}
      </slot>
    </button>
  </div>
</template>
