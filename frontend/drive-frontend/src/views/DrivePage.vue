<script setup lang="ts">
import { ref, onMounted } from 'vue';

const isLoading = ref(true);
const files = ref<Array<{ name: string; size: string; type: string; modified: string }>>([]);

onMounted(() => {
  // Placeholder data
  files.value = [
    { name: 'documents', size: '-', type: 'folder', modified: '2026-02-10' },
    { name: 'images', size: '-', type: 'folder', modified: '2026-02-09' },
    { name: 'report.pdf', size: '2.4 MB', type: 'file', modified: '2026-02-11' },
    { name: 'presentation.pptx', size: '8.1 MB', type: 'file', modified: '2026-02-10' },
  ];
  isLoading.value = false;
});
</script>

<template>
  <div class="max-w-6xl mx-auto px-6">
    <!-- Header -->
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-2xl font-bold text-text-heading">My Drive</h1>
      <button class="px-4 py-2 bg-brand-primary text-white rounded-lg hover:opacity-90 transition-opacity">
        Upload
      </button>
    </div>

    <!-- File List -->
    <div v-if="isLoading" class="flex items-center justify-center py-20">
      <div class="text-text-meta">Loading...</div>
    </div>

    <div v-else class="bg-bg-card rounded-lg border border-border-default overflow-hidden">
      <table class="w-full">
        <thead>
          <tr class="border-b border-border-default bg-bg-page">
            <th class="text-left px-4 py-3 text-sm font-medium text-text-meta">Name</th>
            <th class="text-left px-4 py-3 text-sm font-medium text-text-meta">Size</th>
            <th class="text-left px-4 py-3 text-sm font-medium text-text-meta">Modified</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="file in files"
            :key="file.name"
            class="border-b border-border-default last:border-b-0 hover:bg-bg-page transition-colors cursor-pointer"
          >
            <td class="px-4 py-3 flex items-center gap-2">
              <span v-if="file.type === 'folder'" class="text-lg">📁</span>
              <span v-else class="text-lg">📄</span>
              <span class="text-text-body">{{ file.name }}</span>
            </td>
            <td class="px-4 py-3 text-sm text-text-meta">{{ file.size }}</td>
            <td class="px-4 py-3 text-sm text-text-meta">{{ file.modified }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
