<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue';
import { useRouter } from 'vue-router';
import { Button, Input, Card, Textarea, useToast, useApiError } from '@portal/design-system-vue';
import Editor from '@toast-ui/editor';
import '@toast-ui/editor/dist/toastui-editor.css';
import '@toast-ui/editor/dist/theme/toastui-editor-dark.css';
import codeSyntaxHighlight from '@toast-ui/editor-plugin-code-syntax-highlight';
import Prism from 'prismjs';
import 'prismjs/themes/prism.css';
import 'prismjs/themes/prism-okaidia.css';
import { createPost } from '../api/posts';
import { uploadFile } from '../api/files';
import { getMySeries, addPostToSeries } from '../api/series';
import type { PostCreateRequest } from '@/types';
import type { SeriesListResponse } from '@/dto/series';
import TagAutocomplete from '@/components/TagAutocomplete.vue';

const router = useRouter();
const toast = useToast();
const { handleError } = useApiError();
const isDarkMode = ref(false);

function detectTheme() {
  const theme = document.documentElement.getAttribute('data-theme');
  isDarkMode.value = theme === 'dark';

  // Editor가 이미 생성되어 있으면 테마 변경
  if (editorInstance) {
    updateEditorTheme();
  }
}

function updateEditorTheme() {
  if (!editorInstance) return;

  const editorEl = editorElement.value;
  if (editorEl) {
    if (isDarkMode.value) {
      editorEl.classList.add('toastui-editor-dark');
    } else {
      editorEl.classList.remove('toastui-editor-dark');
    }
  }
}

// Editor 인스턴스 (Vue 3에서는 ref로 DOM 참조, Editor는 변수로)
const editorElement = ref<HTMLDivElement | null>(null);
let editorInstance: Editor | null = null;

// Form State
const form = ref<PostCreateRequest>({
  title: '',
  content: '',
  summary: '',
  tags: [],
  category: '',
  metaDescription: '',
  thumbnailUrl: '',
  publishImmediately: false
});

const isLoading = ref(false);
const isSubmitted = ref(false);
const autoSaveTimer = ref<number | null>(null);

// 시리즈 선택
const mySeriesList = ref<SeriesListResponse[]>([]);
const selectedSeriesId = ref<string>('');

// ==================== 임시 저장 ====================

const AUTOSAVE_KEY = 'blog_draft_autosave';
const AUTOSAVE_INTERVAL = 30000; // 30초

function loadDraft() {
  const saved = localStorage.getItem(AUTOSAVE_KEY);
  if (saved) {
    try {
      const draft = JSON.parse(saved);
      form.value = { ...form.value, ...draft };
      if (editorInstance && draft.content) {
        editorInstance.setMarkdown(draft.content);
      }
      console.log('✅ 임시 저장된 글을 불러왔습니다.');
    } catch (err) {
      console.error('❌ 임시 저장 불러오기 실패:', err);
    }
  }
}

function saveDraft() {
  try {
    const content = editorInstance?.getMarkdown() || '';
    const draft = {
      ...form.value,
      content,
      savedAt: new Date().toISOString()
    };
    localStorage.setItem(AUTOSAVE_KEY, JSON.stringify(draft));
    console.log('💾 임시 저장 완료:', new Date().toLocaleTimeString());
  } catch (err) {
    console.error('❌ 임시 저장 실패:', err);
  }
}

function clearDraft() {
  localStorage.removeItem(AUTOSAVE_KEY);
  console.log('🗑️ 임시 저장 삭제');
}

// ==================== 발행/저장 ====================

async function handleSubmit(publish: boolean) {
  if (!form.value.title.trim()) {
    toast.warning('제목을 입력해주세요.');
    return;
  }

  const content = editorInstance?.getMarkdown() || '';
  if (!content.trim()) {
    toast.warning('내용을 입력해주세요.');
    return;
  }

  try {
    isLoading.value = true;

    const payload: PostCreateRequest = {
      ...form.value,
      content,
      publishImmediately: publish
    };

    const newPost = await createPost(payload);

    // 선택된 시리즈가 있으면 포스트 추가
    if (selectedSeriesId.value && newPost.id) {
      try {
        await addPostToSeries(selectedSeriesId.value, newPost.id);
      } catch (seriesErr) {
        console.error('Failed to add post to series:', seriesErr);
      }
    }

    isSubmitted.value = true;
    clearDraft();
    toast.success(publish ? '글이 발행되었습니다!' : '초안으로 저장되었습니다!');
    router.push(`/${newPost.id}`);
  } catch (err) {
    console.error('❌ 게시물 저장 실패:', err);
    handleError(err, '게시물 저장에 실패했습니다.');
  } finally {
    isLoading.value = false;
  }
}

// ==================== Lifecycle ====================

onMounted(() => {
  // 현재 테마 감지 (에디터 생성 전에 호출해야 올바른 테마로 초기화됨)
  detectTheme();

  // Editor 인스턴스 생성 (Vue 3 방식)
  if (editorElement.value) {
    editorInstance = new Editor({
      el: editorElement.value,
      height: '600px',
      initialEditType: 'markdown',
      previewStyle: 'vertical',
      usageStatistics: false,
      theme: isDarkMode.value ? 'dark' : 'default',
      plugins: [[codeSyntaxHighlight, { highlighter: Prism }]],
      toolbarItems: [
        ['heading', 'bold', 'italic', 'strike'],
        ['hr', 'quote'],
        ['ul', 'ol', 'task', 'indent', 'outdent'],
        ['table', 'link', 'image'],
        ['code', 'codeblock'],
        ['scrollSync']
      ],
      placeholder: '내용을 입력하세요...',
      hooks: {
        addImageBlobHook: async (blob: Blob, callback: (url: string, alt: string) => void) => {
          try {
            console.log('📷 이미지 업로드 시작...', {
              size: blob.size,
              type: blob.type
            });

            // File 객체로 변환 (uploadFile 함수는 File 타입 요구)
            const file = blob instanceof File
                ? blob
                : new File([blob], 'image.png', { type: blob.type });

            // S3에 파일 업로드
            const response = await uploadFile(file);

            // 에디터에 이미지 삽입
            // callback(url, altText) 형식
            callback(response.url, file.name);

            console.log('✅ 이미지 업로드 성공:', response.url);
          } catch (error) {
            console.error('❌ 이미지 업로드 실패:', error);

            handleError(error, '이미지 업로드에 실패했습니다.');

          }
        }
      }
    });

    loadDraft();
    updateEditorTheme();
  }

  // 시리즈 목록 로드
  getMySeries().then(list => {
    mySeriesList.value = list;
  }).catch(() => {});

  const observer = new MutationObserver((mutations) => {
    mutations.forEach((mutation) => {
      if (mutation.type === 'attributes' && mutation.attributeName === 'data-theme') {
        detectTheme();
      }
    });
  });

  observer.observe(document.documentElement, {
    attributes: true,
    attributeFilter: ['data-theme']
  });

  // 자동 저장 타이머
  autoSaveTimer.value = setInterval(() => {
    saveDraft();
  }, AUTOSAVE_INTERVAL);

  onBeforeUnmount(() => {
    observer.disconnect();
  });
});

onBeforeUnmount(() => {
  if (!isSubmitted.value) {
    saveDraft();
  }

  if (autoSaveTimer.value) {
    clearInterval(autoSaveTimer.value);
  }

  // Editor 인스턴스 정리
  if (editorInstance) {
    editorInstance.destroy();
    editorInstance = null;
  }
});
</script>

<template>
  <div class="max-w-5xl mx-auto px-4 py-8">
    <!-- Header -->
    <header class="flex items-center justify-between mb-6">
      <h1 class="text-3xl font-bold text-text-heading">✍️ 새 글 작성</h1>
      <Button variant="secondary" @click="router.back()">
        취소
      </Button>
    </header>

    <!-- Form -->
    <div class="space-y-6">
      <!-- 제목 -->
      <div>
        <Input
            v-model="form.title"
            placeholder="제목을 입력하세요"
            size="lg"
            class="text-2xl font-bold"
        />
      </div>

      <!-- 카테고리 & 태그 -->
      <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div>
          <Input
              v-model="form.category"
              label="카테고리"
              placeholder="예: Vue.js, Spring Boot"
          />
        </div>

        <div>
          <TagAutocomplete
            :model-value="form.tags || []"
            @update:model-value="form.tags = $event"
          />
        </div>
      </div>

      <!-- 시리즈 선택 -->
      <div v-if="mySeriesList.length > 0" class="series-select-wrapper">
        <label class="block text-sm font-medium text-text-body mb-1">시리즈</label>
        <select
          v-model="selectedSeriesId"
          class="w-full px-4 py-2 border border-border-default rounded-lg bg-bg-card text-text-body focus:outline-none focus:ring-2 focus:ring-brand-primary focus:border-transparent"
        >
          <option value="">시리즈 없음</option>
          <option v-for="s in mySeriesList" :key="s.id" :value="s.id">
            {{ s.name }} ({{ s.postCount }}개)
          </option>
        </select>
      </div>

      <!-- Toast UI Editor (순수 JavaScript 방식) -->
      <Card>
        <div ref="editorElement" :class="{ 'toastui-editor-dark': isDarkMode }"></div>
      </Card>

      <!-- 고급 설정 -->
      <details class="border border-border-muted rounded-lg p-4">
        <summary class="cursor-pointer font-medium text-text-heading mb-3">
          고급 설정 (선택사항)
        </summary>
        <div class="space-y-4 mt-4">
          <div>
            <Textarea
                v-model="form.summary"
                label="요약 (최대 500자)"
                :rows="3"
                placeholder="검색 결과나 목록에 표시될 요약을 입력하세요"
            />
          </div>

          <div>
            <Input
                v-model="form.metaDescription"
                label="SEO 메타 설명 (최대 160자)"
                placeholder="검색 엔진 최적화를 위한 설명"
            />
          </div>

          <div>
            <Input
                v-model="form.thumbnailUrl"
                label="썸네일 URL"
                placeholder="https://example.com/image.jpg"
            />
          </div>
        </div>
      </details>

      <!-- 버튼 -->
      <div class="flex items-center justify-between pt-6 border-t border-border-muted">
        <div class="text-sm text-text-meta">
          💾 30초마다 자동 저장됩니다
        </div>
        <div class="flex gap-3">
          <Button
              variant="secondary"
              size="lg"
              :disabled="isLoading"
              @click="handleSubmit(false)"
          >
            임시 저장
          </Button>
          <Button
              variant="primary"
              size="lg"
              :disabled="isLoading"
              @click="handleSubmit(true)"
          >
            {{ isLoading ? '발행 중...' : '발행하기' }}
          </Button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Toast UI Editor 스타일 커스터마이징 */
/* 기본 스타일 (라이트모드) */
:deep(.toastui-editor-defaultUI) {
  background: var(--semantic-bg-card) !important;
  border: 1px solid var(--semantic-border-default) !important;
}

:deep(.toastui-editor-toolbar) {
  background: var(--semantic-bg-page) !important;
  border-bottom: 1px solid var(--semantic-border-default) !important;
}

:deep(.toastui-editor-md-container),
:deep(.toastui-editor-ww-container) {
  background: var(--semantic-bg-card) !important;
  color: var(--semantic-text-body) !important;
}

/* 다크모드 스타일 */
.toastui-editor-dark :deep(.toastui-editor-defaultUI) {
  background: var(--semantic-bg-card) !important;
  border-color: var(--semantic-border-default) !important;
}

.toastui-editor-dark :deep(.toastui-editor-toolbar) {
  background: var(--semantic-bg-elevated) !important;
  border-bottom-color: var(--semantic-border-default) !important;
}

.toastui-editor-dark :deep(.toastui-editor-toolbar button) {
  color: var(--semantic-text-body) !important;
}

.toastui-editor-dark :deep(.toastui-editor-toolbar button:hover) {
  background: var(--semantic-bg-hover) !important;
}

.toastui-editor-dark :deep(.toastui-editor-toolbar .disabled),
.toastui-editor-dark :deep(.toastui-editor-toolbar button:disabled) {
  color: var(--semantic-text-muted) !important;
}

/* [중요] 다크모드 편집 영역 배경 및 텍스트 */
.toastui-editor-dark :deep(.toastui-editor-md-container),
.toastui-editor-dark :deep(.toastui-editor-ww-container),
.toastui-editor-dark :deep(.toastui-editor-md-preview) {
  background: var(--semantic-bg-card) !important;
  color: var(--semantic-text-body) !important;
}

/* [중요] 에디터 본문 텍스트 색상 강제 적용 */
.toastui-editor-dark :deep(.ProseMirror) {
  color: var(--semantic-text-body) !important;
  caret-color: var(--semantic-text-body) !important;
}

/* 마크다운 편집 영역 텍스트 */
.toastui-editor-dark :deep(.toastui-editor-md-container .toastui-editor-contents) {
  color: var(--semantic-text-body) !important;
}

/* 마크다운 프리뷰 영역 모든 텍스트 요소 */
.toastui-editor-dark :deep(.toastui-editor-md-preview .toastui-editor-contents) {
  color: var(--semantic-text-body) !important;
}

.toastui-editor-dark :deep(.toastui-editor-md-preview .toastui-editor-contents p),
.toastui-editor-dark :deep(.toastui-editor-md-preview .toastui-editor-contents h1),
.toastui-editor-dark :deep(.toastui-editor-md-preview .toastui-editor-contents h2),
.toastui-editor-dark :deep(.toastui-editor-md-preview .toastui-editor-contents h3),
.toastui-editor-dark :deep(.toastui-editor-md-preview .toastui-editor-contents h4),
.toastui-editor-dark :deep(.toastui-editor-md-preview .toastui-editor-contents h5),
.toastui-editor-dark :deep(.toastui-editor-md-preview .toastui-editor-contents h6),
.toastui-editor-dark :deep(.toastui-editor-md-preview .toastui-editor-contents li),
.toastui-editor-dark :deep(.toastui-editor-md-preview .toastui-editor-contents span),
.toastui-editor-dark :deep(.toastui-editor-md-preview .toastui-editor-contents div) {
  color: var(--semantic-text-body) !important;
}

/* Wysiwyg 모드의 모든 텍스트 요소 */
.toastui-editor-dark :deep(.toastui-editor-ww-container .toastui-editor-contents) {
  color: var(--semantic-text-body) !important;
}

.toastui-editor-dark :deep(.toastui-editor-ww-container .toastui-editor-contents p),
.toastui-editor-dark :deep(.toastui-editor-ww-container .toastui-editor-contents h1),
.toastui-editor-dark :deep(.toastui-editor-ww-container .toastui-editor-contents h2),
.toastui-editor-dark :deep(.toastui-editor-ww-container .toastui-editor-contents h3),
.toastui-editor-dark :deep(.toastui-editor-ww-container .toastui-editor-contents h4),
.toastui-editor-dark :deep(.toastui-editor-ww-container .toastui-editor-contents h5),
.toastui-editor-dark :deep(.toastui-editor-ww-container .toastui-editor-contents h6),
.toastui-editor-dark :deep(.toastui-editor-ww-container .toastui-editor-contents li),
.toastui-editor-dark :deep(.toastui-editor-ww-container .toastui-editor-contents span),
.toastui-editor-dark :deep(.toastui-editor-ww-container .toastui-editor-contents div) {
  color: var(--semantic-text-body) !important;
}

/* 코드 블록 다크모드 */
.toastui-editor-dark :deep(.toastui-editor-contents pre) {
  background: var(--semantic-bg-elevated) !important;
  border-color: var(--semantic-border-default) !important;
}

.toastui-editor-dark :deep(.toastui-editor-contents code) {
  background: var(--semantic-bg-muted) !important;
  color: var(--semantic-brand-primary) !important;
}

/* 구분선 */
.toastui-editor-dark :deep(.toastui-editor-contents hr) {
  border-color: var(--semantic-border-default) !important;
}

/* 테이블 */
.toastui-editor-dark :deep(.toastui-editor-contents table) {
  border-color: var(--semantic-border-default) !important;
}

.toastui-editor-dark :deep(.toastui-editor-contents th),
.toastui-editor-dark :deep(.toastui-editor-contents td) {
  border-color: var(--semantic-border-default) !important;
  background: var(--semantic-bg-card) !important;
  color: var(--semantic-text-body) !important;
}

.toastui-editor-dark :deep(.toastui-editor-contents th) {
  background: var(--semantic-bg-muted) !important;
  color: var(--semantic-text-body) !important;
}

/* 인용구 (Blockquote) */
.toastui-editor-dark :deep(.toastui-editor-contents blockquote) {
  color: var(--semantic-text-meta) !important;
  border-left-color: var(--semantic-brand-primary) !important;
}

.toastui-editor-dark :deep(.toastui-editor-contents blockquote p) {
  color: var(--semantic-text-meta) !important;
}

/* 링크 */
.toastui-editor-dark :deep(.toastui-editor-contents a) {
  color: var(--semantic-text-link) !important;
}

.toastui-editor-dark :deep(.toastui-editor-contents a:hover) {
  color: var(--semantic-text-linkHover) !important;
}

/* 이미지 업로드 영역 */
.toastui-editor-dark :deep(.toastui-editor-popup) {
  background: var(--semantic-bg-elevated) !important;
  border-color: var(--semantic-border-default) !important;
  color: var(--semantic-text-body) !important;
}

.toastui-editor-dark :deep(.toastui-editor-popup input) {
  background: var(--semantic-bg-card) !important;
  border-color: var(--semantic-border-default) !important;
  color: var(--semantic-text-body) !important;
}

.toastui-editor-dark :deep(.toastui-editor-popup label) {
  color: var(--semantic-text-body) !important;
}

/* 툴바 구분선 */
.toastui-editor-dark :deep(.toastui-editor-toolbar-divider) {
  background: var(--semantic-border-default) !important;
}

/* 선택 영역 */
.toastui-editor-dark :deep(.ProseMirror-selectednode) {
  outline: 2px solid var(--semantic-brand-primary) !important;
}

/* 플레이스홀더 */
.toastui-editor-dark :deep(.ProseMirror .placeholder) {
  color: var(--semantic-text-muted) !important;
}

/* 리스트 마커 색상 */
.toastui-editor-dark :deep(.toastui-editor-contents ul li::marker),
.toastui-editor-dark :deep(.toastui-editor-contents ol li::marker) {
  color: var(--semantic-text-body) !important;
}

/* 체크박스 */
.toastui-editor-dark :deep(.toastui-editor-contents input[type="checkbox"]) {
  border-color: var(--semantic-border-default) !important;
}

/* 강조 텍스트 */
.toastui-editor-dark :deep(.toastui-editor-contents strong),
.toastui-editor-dark :deep(.toastui-editor-contents b) {
  color: var(--semantic-text-heading) !important;
}

/* 기울임 텍스트 */
.toastui-editor-dark :deep(.toastui-editor-contents em),
.toastui-editor-dark :deep(.toastui-editor-contents i) {
  color: var(--semantic-text-body) !important;
}
</style>