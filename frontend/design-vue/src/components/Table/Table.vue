<script setup lang="ts" generic="T">
import type { TableProps, TableColumn } from '@portal/design-core';
import { Spinner } from '../Spinner';

const props = withDefaults(defineProps<TableProps<T>>(), {
  loading: false,
  emptyText: 'No data available',
  striped: false,
  hoverable: true,
});

const getAlignClass = (align?: 'left' | 'center' | 'right') => {
  switch (align) {
    case 'center':
      return 'text-center';
    case 'right':
      return 'text-right';
    default:
      return 'text-left';
  }
};

const getCellValue = (row: T, column: TableColumn<T>) => {
  const value = (row as Record<string, unknown>)[column.key];
  return column.render ? column.render(value, row) : value;
};
</script>

<template>
  <div class="relative w-full overflow-auto">
    <table class="w-full border-collapse">
      <thead>
        <tr class="border-b border-border-default bg-bg-muted">
          <th
            v-for="column in columns"
            :key="column.key"
            :class="[
              'px-4 py-3 text-sm font-semibold text-text-heading',
              getAlignClass(column.align),
            ]"
            :style="{ width: column.width }"
          >
            {{ column.label }}
          </th>
        </tr>
      </thead>
      <tbody>
        <!-- Loading -->
        <tr v-if="loading">
          <td :colspan="columns.length" class="px-4 py-8 text-center">
            <Spinner size="md" />
          </td>
        </tr>

        <!-- Empty -->
        <tr v-else-if="data.length === 0">
          <td
            :colspan="columns.length"
            class="px-4 py-8 text-center text-text-muted"
          >
            {{ emptyText }}
          </td>
        </tr>

        <!-- Data rows -->
        <tr
          v-else
          v-for="(row, rowIndex) in data"
          :key="rowIndex"
          :class="[
            'border-b border-border-default transition-colors',
            striped && rowIndex % 2 === 1 && 'bg-bg-muted/50',
            hoverable && 'hover:bg-bg-hover',
          ]"
        >
          <td
            v-for="column in columns"
            :key="column.key"
            :class="[
              'px-4 py-3 text-sm text-text-body',
              getAlignClass(column.align),
            ]"
          >
            <slot :name="`cell-${column.key}`" :value="getCellValue(row, column)" :row="row">
              {{ getCellValue(row, column) }}
            </slot>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
