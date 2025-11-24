<script setup lang="ts">
import { computed } from 'vue';
import type { SearchBarProps } from './SearchBar.types';
import { Button } from '../Button';

const props = withDefaults(defineProps<SearchBarProps>(), {
  modelValue: '',
  placeholder: '검색...',
  loading: false,
  disabled: false,
  autofocus: false
});

const emit = defineEmits<{
  'update:modelValue': [value: string];
  'search': [keyword: string];
  'clear': [];
}>();

// 검색어가 있는지 확인
const hasValue = computed(() => props.modelValue.trim().length > 0);

// 입력 처리
function handleInput(event: Event) {
  const target = event.target as HTMLInputElement;
  emit('update:modelValue', target.value);
}

// 검색 실행
function handleSearch() {
  if (props.disabled || props.loading) return;

  const keyword = props.modelValue.trim();
  if (keyword) {
    emit('search', keyword);
  }
}

// Enter 키 처리
function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter') {
    event.preventDefault();
    handleSearch();
  }
}

// 검색어 초기화
function handleClear() {
  emit('update:modelValue', '');
  emit('clear');
}
</script>

<template>
  <div class="search-bar">
    <div class="search-bar__input-wrapper">
      <!-- 검색 아이콘 -->
      <svg
          class="search-bar__icon search-bar__icon--search"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
      >
        <path
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="2"
            d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
        />
      </svg>

      <!-- 입력 필드 -->
      <input
          :value="modelValue"
          :placeholder="placeholder"
          :disabled="disabled || loading"
          :autofocus="autofocus"
          type="text"
          class="search-bar__input"
          @input="handleInput"
          @keydown="handleKeydown"
      />

      <!-- 로딩 스피너 -->
      <div v-if="loading" class="search-bar__spinner">
        <div class="spinner"></div>
      </div>

      <!-- 클리어 버튼 -->
      <button
          v-else-if="hasValue && !disabled"
          type="button"
          class="search-bar__clear"
          @click="handleClear"
          aria-label="검색어 지우기"
      >
        <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M6 18L18 6M6 6l12 12"
          />
        </svg>
      </button>
    </div>

    <!-- 검색 버튼 -->
    <Button
        variant="primary"
        size="md"
        :disabled="!hasValue || disabled || loading"
        @click="handleSearch"
        class="search-bar__button"
    >
      {{ loading ? '검색 중...' : '검색' }}
    </Button>
  </div>
</template>

<style scoped>
.search-bar {
  display: flex;
  gap: 0.75rem;
  width: 100%;
  max-width: 600px;
}

.search-bar__input-wrapper {
  position: relative;
  flex: 1;
  display: flex;
  align-items: center;
}

.search-bar__input {
  width: 100%;
  padding: 0.625rem 2.75rem 0.625rem 2.75rem;
  font-size: 0.9375rem;
  line-height: 1.5;
  color: var(--color-text-body);
  background-color: var(--color-bg-card);
  border: 1px solid var(--color-border-default);
  border-radius: 0.5rem;
  transition: all 0.2s ease;
}

.search-bar__input:focus {
  outline: none;
  border-color: var(--color-brand-primary);
  box-shadow: 0 0 0 3px rgba(18, 184, 134, 0.1);
}

.search-bar__input:disabled {
  background-color: var(--color-bg-muted);
  cursor: not-allowed;
  opacity: 0.6;
}

.search-bar__input::placeholder {
  color: var(--color-text-meta);
}

/* 아이콘 공통 */
.search-bar__icon {
  position: absolute;
  width: 1.25rem;
  height: 1.25rem;
  color: var(--color-text-meta);
  pointer-events: none;
}

.search-bar__icon--search {
  left: 0.875rem;
}

/* 클리어 버튼 */
.search-bar__clear {
  position: absolute;
  right: 0.75rem;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 1.25rem;
  height: 1.25rem;
  padding: 0;
  background: none;
  border: none;
  color: var(--color-text-meta);
  cursor: pointer;
  transition: color 0.2s ease;
}

.search-bar__clear:hover {
  color: var(--color-text-body);
}

.search-bar__clear svg {
  width: 100%;
  height: 100%;
}

/* 로딩 스피너 */
.search-bar__spinner {
  position: absolute;
  right: 0.75rem;
  display: flex;
  align-items: center;
  justify-content: center;
}

.spinner {
  width: 1.25rem;
  height: 1.25rem;
  border: 2px solid var(--color-border-default);
  border-top-color: var(--color-brand-primary);
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* 검색 버튼 */
.search-bar__button {
  flex-shrink: 0;
  min-width: 80px;
}

/* 반응형 */
@media (max-width: 640px) {
  .search-bar {
    flex-direction: column;
    gap: 0.5rem;
  }

  .search-bar__button {
    width: 100%;
  }
}
</style>