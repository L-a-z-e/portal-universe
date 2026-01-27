<script setup lang="ts">
import { ref, watch } from 'vue';
import { Input, Tag } from '@portal/design-system-vue';
import { searchTags } from '@/api/tags';
import type { TagResponse } from '@/types';

interface Props {
  modelValue: string[];
}
const props = defineProps<Props>();

const emit = defineEmits<{
  'update:modelValue': [tags: string[]];
}>();

const tagInput = ref('');
const suggestions = ref<TagResponse[]>([]);
const showSuggestions = ref(false);
const loading = ref(false);
let debounceTimer: ReturnType<typeof setTimeout> | null = null;

function addTag(tagName: string) {
  const tag = tagName.trim();
  if (tag && !props.modelValue.includes(tag)) {
    emit('update:modelValue', [...props.modelValue, tag]);
  }
  tagInput.value = '';
  suggestions.value = [];
  showSuggestions.value = false;
}

function removeTag(tagToRemove: string) {
  emit('update:modelValue', props.modelValue.filter(tag => tag !== tagToRemove));
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter') {
    e.preventDefault();
    if (tagInput.value.trim()) {
      addTag(tagInput.value);
    }
  }
  if (e.key === 'Escape') {
    showSuggestions.value = false;
  }
}

function selectSuggestion(tag: TagResponse) {
  addTag(tag.name);
}

watch(tagInput, (value) => {
  if (debounceTimer) clearTimeout(debounceTimer);

  if (!value.trim() || value.trim().length < 1) {
    suggestions.value = [];
    showSuggestions.value = false;
    return;
  }

  debounceTimer = setTimeout(async () => {
    loading.value = true;
    try {
      const results = await searchTags(value.trim());
      suggestions.value = results.filter(t => !props.modelValue.includes(t.name));
      showSuggestions.value = suggestions.value.length > 0;
    } catch (err) {
      console.error('Tag search failed:', err);
      suggestions.value = [];
      showSuggestions.value = false;
    } finally {
      loading.value = false;
    }
  }, 300);
});

function handleBlur() {
  // 클릭 이벤트가 처리될 수 있도록 약간의 지연
  setTimeout(() => {
    showSuggestions.value = false;
  }, 200);
}
</script>

<template>
  <div class="tag-autocomplete">
    <!-- 입력 영역 -->
    <div class="input-row">
      <div class="input-wrapper">
        <Input
          v-model="tagInput"
          label="태그 추가"
          placeholder="태그 입력 후 Enter"
          @keydown="handleKeydown"
          @focus="tagInput.trim().length > 0 && suggestions.length > 0 && (showSuggestions = true)"
          @blur="handleBlur"
        />

        <!-- 자동완성 드롭다운 -->
        <div v-if="showSuggestions" class="suggestions-dropdown">
          <div v-if="loading" class="suggestion-loading">
            검색 중...
          </div>
          <button
            v-for="tag in suggestions"
            :key="tag.name"
            class="suggestion-item"
            @mousedown.prevent="selectSuggestion(tag)"
          >
            <span class="suggestion-name">{{ tag.name }}</span>
            <span v-if="tag.postCount !== undefined" class="suggestion-count">{{ tag.postCount }}개 게시글</span>
          </button>
        </div>
      </div>
      <button
        type="button"
        class="add-btn"
        @click="addTag(tagInput)"
        :disabled="!tagInput.trim()"
      >
        추가
      </button>
    </div>

    <!-- 선택된 태그 목록 -->
    <div v-if="modelValue.length > 0" class="tag-list">
      <Tag
        v-for="tag in modelValue"
        :key="tag"
        variant="default"
        size="sm"
        closable
        @close="removeTag(tag)"
      >
        {{ tag }}
      </Tag>
    </div>
  </div>
</template>

<style scoped>
.tag-autocomplete {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.input-row {
  display: flex;
  gap: 0.5rem;
  align-items: flex-end;
}

.input-wrapper {
  position: relative;
  flex: 1;
}

.add-btn {
  padding: 0.5rem 1rem;
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--color-text-body);
  background: var(--color-bg-muted);
  border: 1px solid var(--color-border-default);
  border-radius: 0.5rem;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
  height: 2.5rem;
  margin-bottom: 0;
}

.add-btn:hover:not(:disabled) {
  background: var(--color-bg-hover);
}

.add-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.suggestions-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  z-index: 50;
  max-height: 200px;
  overflow-y: auto;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-default);
  border-radius: 0.5rem;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  margin-top: 0.25rem;
}

.suggestion-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 0.5rem 0.75rem;
  border: none;
  background: none;
  cursor: pointer;
  text-align: left;
  transition: background 0.15s;
}

.suggestion-item:hover {
  background: var(--color-bg-hover);
}

.suggestion-name {
  font-weight: 500;
  color: var(--color-text-heading);
}

.suggestion-count {
  font-size: 0.75rem;
  color: var(--color-text-meta);
}

.suggestion-loading {
  padding: 0.75rem;
  text-align: center;
  color: var(--color-text-meta);
  font-size: 0.875rem;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}
</style>
