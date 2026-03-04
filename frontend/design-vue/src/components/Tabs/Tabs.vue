<script setup lang="ts">
import { computed, ref } from 'vue';
import type { TabsProps, TabsEmits, TabItem } from './Tabs.types';

const props = withDefaults(defineProps<TabsProps>(), {
  variant: 'underline',
  size: 'md',
  fullWidth: false,
});

const emit = defineEmits<TabsEmits>();

const tablistRef = ref<HTMLDivElement | null>(null);

import { tabsBase, tabsVariants, tabsItemBase, tabsItemVariants, tabsSizes } from '@portal/design-core';

const containerClasses = computed(() => {
  const classes = [tabsBase, tabsVariants[props.variant]];

  if (props.fullWidth) {
    classes.push('w-full');
  }

  return classes;
});

const getTabClasses = (tab: TabItem) => {
  const isActive = props.modelValue === tab.value;
  const variantStyles = tabsItemVariants[props.variant];
  const classes = [
    tabsItemBase,
    'focus:outline-none focus:ring-2 focus:ring-brand-primary/20 focus:ring-offset-2',
    tabsSizes[props.size],
    isActive ? variantStyles.active : variantStyles.inactive,
  ];

  if (props.fullWidth) {
    classes.push('flex-1');
  }

  if (tab.disabled) {
    classes.push('cursor-not-allowed opacity-50');
  }

  return classes;
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
