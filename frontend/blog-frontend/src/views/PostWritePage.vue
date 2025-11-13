<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue';
import { useRouter } from 'vue-router';
import Editor from '@toast-ui/editor';
import codeSyntaxHighlight from '@toast-ui/editor-plugin-code-syntax-highlight';
import Prism from 'prismjs';
import { Button, Input, Card, Tag } from '@portal/design-system';
import { createPost } from '../api/posts';
import { uploadFile } from '../api/files';
import type { PostCreateRequest } from '../types';

// CSS 임포트
import '@toast-ui/editor/dist/toastui-editor.css';
import 'prismjs/themes/prism.css';

const router = useRouter();

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

const tagInput = ref('');
const isLoading = ref(false);
const autoSaveTimer = ref<number | null>(null);

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

// ==================== 태그 관리 ====================

function addTag() {
  const tag = tagInput.value.trim();
  if (tag && !form.value.tags?.includes(tag)) {
    form.value.tags = [...(form.value.tags || []), tag];
    tagInput.value = '';
  }
}

function removeTag(tagToRemove: string) {
  form.value.tags = form.value.tags?.filter(tag => tag !== tagToRemove);
}

function handleTagKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter') {
    e.preventDefault();
    addTag();
  }
}

// ==================== 발행/저장 ====================

async function handleSubmit(publish: boolean) {
  if (!form.value.title.trim()) {
    alert('제목을 입력해주세요.');
    return;
  }

  const content = editorInstance?.getMarkdown() || '';
  if (!content.trim()) {
    alert('내용을 입력해주세요.');
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

    clearDraft();
    alert(publish ? '글이 발행되었습니다!' : '초안으로 저장되었습니다!');
    router.push(`/${newPost.id}`);
  } catch (err) {
    console.error('❌ 게시물 저장 실패:', err);
    alert('게시물 저장에 실패했습니다. 다시 시도해주세요.');
  } finally {
    isLoading.value = false;
  }
}

// ==================== Lifecycle ====================

onMounted(() => {
  // Editor 인스턴스 생성 (Vue 3 방식)
  if (editorElement.value) {
    editorInstance = new Editor({
      el: editorElement.value,
      height: '600px',
      initialEditType: 'markdown',
      previewStyle: 'vertical',
      usageStatistics: false,
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

            // 사용자에게 에러 알림
            alert('이미지 업로드에 실패했습니다. 다시 시도해주세요.');

          }
        }
      }
    });

    loadDraft();
  }

  // 자동 저장 타이머
  autoSaveTimer.value = setInterval(() => {
    saveDraft();
  }, AUTOSAVE_INTERVAL);
});

onBeforeUnmount(() => {
  saveDraft();

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
          <label class="block text-sm font-medium text-text-heading mb-2">
            카테고리
          </label>
          <Input
              v-model="form.category"
              placeholder="예: Vue.js, Spring Boot"
          />
        </div>

        <div>
          <label class="block text-sm font-medium text-text-heading mb-2">
            태그 추가
          </label>
          <div class="flex gap-2">
            <Input
                v-model="tagInput"
                placeholder="태그 입력 후 Enter"
                @keydown="handleTagKeydown"
            />
            <Button variant="secondary" size="sm" @click="addTag">
              추가
            </Button>
          </div>
        </div>
      </div>

      <!-- 태그 목록 -->
      <div v-if="form.tags && form.tags.length > 0" class="flex flex-wrap gap-2">
        <Tag
            v-for="tag in form.tags"
            :key="tag"
            variant="default"
            size="sm"
            closable
            @close="removeTag(tag)"
        >
          {{ tag }}
        </Tag>
      </div>

      <!-- Toast UI Editor (순수 JavaScript 방식) -->
      <Card>
        <div ref="editorElement"></div>
      </Card>

      <!-- 고급 설정 -->
      <details class="border border-border-muted rounded-lg p-4">
        <summary class="cursor-pointer font-medium text-text-heading mb-3">
          고급 설정 (선택사항)
        </summary>
        <div class="space-y-4 mt-4">
          <div>
            <label class="block text-sm font-medium text-text-heading mb-2">
              요약 (최대 500자)
            </label>
            <textarea
                v-model="form.summary"
                class="w-full px-3 py-2 border border-border-muted rounded-lg resize-none"
                rows="3"
                maxlength="500"
                placeholder="검색 결과나 목록에 표시될 요약을 입력하세요"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-text-heading mb-2">
              SEO 메타 설명 (최대 160자)
            </label>
            <Input
                v-model="form.metaDescription"
                placeholder="검색 엔진 최적화를 위한 설명"
                maxlength="160"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-text-heading mb-2">
              썸네일 URL
            </label>
            <Input
                v-model="form.thumbnailUrl"
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
:deep(.toastui-editor-defaultUI) {
  border: 1px solid var(--color-border-default);
  border-radius: 0.5rem;
}

:deep(.toastui-editor-toolbar) {
  background: var(--color-bg-page);  /* ← 수정 */
  border-bottom: 1px solid var(--color-border-default);
}

/* 다크모드 */
.dark :deep(.toastui-editor-defaultUI) {
  background: var(--color-bg-page);  /* ← 수정 */
}

.dark :deep(.toastui-editor-md-container),
.dark :deep(.toastui-editor-md-preview) {
  background: var(--color-bg-page);  /* ← 수정 */
  color: var(--color-text-body);
}
</style>
