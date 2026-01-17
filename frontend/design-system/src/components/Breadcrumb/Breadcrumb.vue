<script setup lang="ts">
import { computed, ref } from 'vue';
import type { BreadcrumbProps, BreadcrumbItem } from './Breadcrumb.types';

const props = withDefaults(defineProps<BreadcrumbProps>(), {
  separator: '/',
  size: 'md',
});

const isExpanded = ref(false);

const sizeClasses = {
  sm: 'text-sm',
  md: 'text-base',
  lg: 'text-lg',
};

const displayItems = computed(() => {
  if (!props.maxItems || props.items.length <= props.maxItems || isExpanded.value) {
    return props.items;
  }

  // Show first item, ellipsis, and last (maxItems - 1) items
  const first = props.items[0];
  const lastItems = props.items.slice(-(props.maxItems - 1));

  return [first, { label: '...', isEllipsis: true } as BreadcrumbItem & { isEllipsis?: boolean }, ...lastItems];
});

const isRouterLink = (item: BreadcrumbItem) => !!item.to && !item.href;
const isLink = (item: BreadcrumbItem) => !!item.to || !!item.href;
const isLastItem = (index: number) => index === displayItems.value.length - 1;

const expand = () => {
  isExpanded.value = true;
};
</script>

<template>
  <nav aria-label="Breadcrumb" :class="sizeClasses[size]">
    <ol class="flex items-center flex-wrap">
      <li
        v-for="(item, index) in displayItems"
        :key="index"
        class="flex items-center"
      >
        <!-- Separator (not for first item) -->
        <span
          v-if="index > 0"
          class="mx-2 text-text-muted select-none"
          aria-hidden="true"
        >
          <slot name="separator">
            {{ separator }}
          </slot>
        </span>

        <!-- Ellipsis button -->
        <button
          v-if="(item as any).isEllipsis"
          type="button"
          class="px-1 text-text-muted hover:text-text-body focus:outline-none focus:ring-2 focus:ring-brand-500/20 rounded"
          aria-label="Show more breadcrumbs"
          @click="expand"
        >
          ...
        </button>

        <!-- Link item -->
        <component
          v-else-if="isLink(item) && !isLastItem(index)"
          :is="isRouterLink(item) ? 'router-link' : 'a'"
          :to="isRouterLink(item) ? item.to : undefined"
          :href="!isRouterLink(item) ? item.href : undefined"
          class="text-text-muted hover:text-text-link transition-colors duration-200 focus:outline-none focus:ring-2 focus:ring-brand-500/20 rounded"
        >
          <slot name="item" :item="item" :index="index" :is-last="false">
            {{ item.label }}
          </slot>
        </component>

        <!-- Current page (last item) -->
        <span
          v-else
          :aria-current="isLastItem(index) ? 'page' : undefined"
          :class="[
            isLastItem(index)
              ? 'text-text-body font-medium'
              : 'text-text-muted',
          ]"
        >
          <slot name="item" :item="item" :index="index" :is-last="isLastItem(index)">
            {{ item.label }}
          </slot>
        </span>
      </li>
    </ol>
  </nav>
</template>
