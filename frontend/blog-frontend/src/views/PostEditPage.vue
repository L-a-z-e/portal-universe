<script setup lang="ts">
import {ref, onMounted, onBeforeUnmount, watch, nextTick} from 'vue';
import { useRouter } from 'vue-router';
import Editor from '@toast-ui/editor';
import '@toast-ui/editor/dist/toastui-editor.css';
import '@toast-ui/editor/dist/theme/toastui-editor-dark.css';
import codeSyntaxHighlight from '@toast-ui/editor-plugin-code-syntax-highlight';
import Prism from 'prismjs';
import { Button, Card, Input } from '@portal/design-system-vue';
import { getPostById, updatePost } from '../api/posts';
import { uploadFile } from '../api/files';
import type { PostUpdateRequest } from '@/dto/post';
import TagAutocomplete from '@/components/TagAutocomplete.vue';

// CSS 임포트
import 'prismjs/themes/prism.css';
import 'prismjs/themes/prism-okaidia.css';

const props = defineProps<{
  postId: string;
}>();

const router = useRouter();

// 다크모드 감지
const isDarkMode = ref(false);

// DOM에서 테마 확인하는 함수
function detectTheme() {
  const theme = document.documentElement.getAttribute('data-theme');
  isDarkMode.value = theme === 'dark';

  if (editorInstance) {
    updateEditorTheme();
  }
}

// Editor 테마 업데이트 함수
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

// Editor 인스턴스로 변경
const editorElement = ref<HTMLDivElement | null>(null);
let editorInstance: Editor | null = null;

// Form State
const title = ref('');
const tags = ref<string[]>([]);
const category = ref('');
const isSubmitting = ref(false);
const error = ref<string | null>(null);
const isLoading = ref(true);
const titleError = ref('');
const postData = ref<any>(null);

// Editor 초기화 함수
function initEditor(content: string) {
  console.log('🔍 [DEBUG] initEditor called');
  console.log('🔍 [DEBUG] editorElement exists:', !!editorElement.value);

  if (!editorElement.value) {
    console.error('❌ [ERROR] editorElement is null!');
    return;
  }

  // 기존 인스턴스가 있으면 제거
  if (editorInstance) {
    editorInstance.destroy();
    editorInstance = null;
  }

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

          const file = blob instanceof File
              ? blob
              : new File([blob], 'image.png', { type: blob.type });

          const response = await uploadFile(file);
          callback(response.url, file.name);

          console.log('✅ 이미지 업로드 성공:', response.url);
        } catch (error) {
          console.error('❌ 이미지 업로드 실패:', error);
          alert('이미지 업로드에 실패했습니다. 다시 시도해주세요.');
        }
      }
    }
  });

  // content 설정
  editorInstance.setMarkdown(content);
  console.log('✅ [SUCCESS] Editor initialized with content');

  // 초기 테마 적용
  updateEditorTheme();
}

watch(() => postData.value, async (newPost) => {
  if (newPost?.content) {
    console.log('🔍 [WATCH] Post loaded, waiting for DOM...');
    await nextTick();
    console.log('🔍 [WATCH] editorElement:', editorElement.value);

    if (editorElement.value) {
      initEditor(newPost.content);
    } else {
      console.error('❌ [WATCH ERROR] editorElement still null after nextTick');
    }
  }
});

onMounted(async () => {
  // 초기 테마 감지
  detectTheme();

  try {
    const post = await getPostById(props.postId);
    title.value = post.title;

    // 태그와 카테고리 로드
    if (post.tags) {
      tags.value = post.tags;
    }
    if (post.category) {
      category.value = post.category;
    }

    postData.value = post;

  } catch (err) {
    console.error('Failed to fetch post for editing:', err);
    error.value = 'Failed to load post data. Please try again.';
  } finally {
    isLoading.value = false;
  }

  // 테마 변경 감지 (MutationObserver)
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

  // cleanup 시 observer도 정리
  onBeforeUnmount(() => {
    observer.disconnect();
  });
});

function validate(): boolean {
  titleError.value = '';
  error.value = null;

  if (!title.value.trim()) {
    titleError.value = '제목을 입력해주세요.';
    return false;
  }

  // Editor 내용 검증
  const content = editorInstance?.getMarkdown() || '';
  if (!content.trim()) {
    error.value = '내용을 입력해주세요.';
    return false;
  }

  return true;
}

async function handleSubmit() {
  if (isSubmitting.value) return;

  if (!validate()) return;

  isSubmitting.value = true;
  error.value = null;

  try {
    // Editor에서 마크다운 가져오기
    const content = editorInstance?.getMarkdown() || '';

    const payload: PostUpdateRequest = {
      title: title.value.trim(),
      content: content,
      tags: tags.value, // [추가]
      category: category.value.trim() || undefined, // [추가]
    };

    const updatedPost = await updatePost(props.postId, payload);
    alert('게시글이 수정되었습니다!');
    await router.push(`/${updatedPost.id}`);

  } catch (err) {
    console.error('Failed to update post:', err);
    error.value = '게시글 수정에 실패했습니다. 다시 시도해주세요.';
  } finally {
    isSubmitting.value = false;
  }
}

function handleCancel() {
  const confirmed = confirm('수정을 취소하시겠습니까?');
  if (confirmed) {
    router.push(`/${props.postId}`);
  }
}

onBeforeUnmount(() => {
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
      <div>
        <h1 class="text-3xl font-bold text-text-heading">✏️ 게시글 수정</h1>
        <p class="text-text-meta mt-1">게시글을 수정하세요</p>
      </div>
      <Button variant="secondary" @click="handleCancel">
        취소
      </Button>
    </header>

    <!-- Loading -->
    <div v-if="isLoading" class="text-center py-20">
      <div class="inline-block w-12 h-12 border-4 border-brand-primary border-t-transparent rounded-full animate-spin"></div>
      <p class="mt-4 text-text-meta">게시글을 불러오는 중...</p>
    </div>

    <!-- Error (데이터 로드 실패) -->
    <Card
        v-else-if="error && !title"
        class="bg-status-error-bg border-status-error"
    >
      <div class="text-center py-8">
        <p class="text-xl text-status-error mb-4">❌ {{ error }}</p>
        <Button variant="secondary" @click="router.push('/')">
          목록으로 돌아가기
        </Button>
      </div>
    </Card>

    <!-- Edit Form -->
    <div v-else class="space-y-6">
      <!-- 제목 -->
      <div>
        <Input
            v-model="title"
            placeholder="제목을 입력하세요"
            size="lg"
            class="text-2xl font-bold"
            :disabled="isSubmitting"
        />
        <p v-if="titleError" class="mt-2 text-sm text-status-error">
          {{ titleError }}
        </p>
      </div>

      <!-- 카테고리 & 태그 -->
      <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div>
          <Input
              v-model="category"
              label="카테고리"
              placeholder="예: Vue.js, Spring Boot"
              :disabled="isSubmitting"
          />
        </div>

        <div>
          <TagAutocomplete
            :model-value="tags"
            @update:model-value="tags = $event"
          />
        </div>
      </div>

      <!-- Toast UI Editor -->
      <Card>
        <div ref="editorElement" :class="{ 'toastui-editor-dark': isDarkMode }"></div>
      </Card>

      <!-- Error Message (제출 실패) -->
      <div v-if="error" class="p-4 bg-status-error-bg border border-status-error rounded-lg">
        <p class="text-status-error">{{ error }}</p>
      </div>

      <!-- Actions -->
      <div class="flex items-center justify-between pt-6 border-t border-border-default">
        <div class="text-sm text-text-meta">
          ✨ 변경사항을 저장하면 즉시 반영됩니다
        </div>
        <div class="flex gap-3">
          <Button
              variant="secondary"
              size="lg"
              :disabled="isSubmitting"
              @click="handleCancel"
          >
            취소
          </Button>
          <Button
              variant="primary"
              size="lg"
              :disabled="isSubmitting"
              @click="handleSubmit"
          >
            {{ isSubmitting ? '저장 중...' : '💾 수정 완료' }}
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
  background: var(--color-bg-card) !important;
  border: 1px solid var(--color-border-default) !important;
}

:deep(.toastui-editor-toolbar) {
  background: var(--color-bg-page) !important;
  border-bottom: 1px solid var(--color-border-default) !important;
}

:deep(.toastui-editor-md-container),
:deep(.toastui-editor-ww-container) {
  background: var(--color-bg-card) !important;
  color: var(--color-text-body) !important;
}

/* 다크모드 스타일 */
.toastui-editor-dark :deep(.toastui-editor-defaultUI) {
  background: var(--color-bg-card) !important;
  border-color: var(--color-border-default) !important;
}

.toastui-editor-dark :deep(.toastui-editor-toolbar) {
  background: var(--color-bg-elevated) !important;
  border-bottom-color: var(--color-border-default) !important;
}

.toastui-editor-dark :deep(.toastui-editor-toolbar button) {
  color: var(--color-text-body) !important;
}

.toastui-editor-dark :deep(.toastui-editor-toolbar button:hover) {
  background: var(--color-bg-hover) !important;
}

.toastui-editor-dark :deep(.toastui-editor-toolbar .disabled),
.toastui-editor-dark :deep(.toastui-editor-toolbar button:disabled) {
  color: var(--color-text-muted) !important;
}

/* 편집 영역 배경 및 텍스트 */
.toastui-editor-dark :deep(.toastui-editor-md-container),
.toastui-editor-dark :deep(.toastui-editor-ww-container),
.toastui-editor-dark :deep(.toastui-editor-md-preview) {
  background: var(--color-bg-card) !important;
  color: var(--color-text-body) !important;
}

/* 에디터 본문 텍스트 색상 */
.toastui-editor-dark :deep(.ProseMirror) {
  color: var(--color-text-body) !important;
  caret-color: var(--color-text-body) !important;
}

/* 마크다운 편집 영역 텍스트 */
.toastui-editor-dark :deep(.toastui-editor-md-container .toastui-editor-contents) {
  color: var(--color-text-body) !important;
}

/* 마크다운 프리뷰 영역 모든 텍스트 요소 */
.toastui-editor-dark :deep(.toastui-editor-md-preview .toastui-editor-contents) {
  color: var(--color-text-body) !important;
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
  color: var(--color-text-body) !important;
}

/* Wysiwyg 모드의 모든 텍스트 요소 */
.toastui-editor-dark :deep(.toastui-editor-ww-container .toastui-editor-contents) {
  color: var(--color-text-body) !important;
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
  color: var(--color-text-body) !important;
}

/* 코드 블록 */
.toastui-editor-dark :deep(.toastui-editor-contents pre) {
  background: var(--color-bg-elevated) !important;
  border-color: var(--color-border-default) !important;
}

.toastui-editor-dark :deep(.toastui-editor-contents code) {
  background: var(--color-bg-muted) !important;
  color: var(--color-brand-primary) !important;
}

/* 구분선 */
.toastui-editor-dark :deep(.toastui-editor-contents hr) {
  border-color: var(--color-border-default) !important;
}

/* 테이블 */
.toastui-editor-dark :deep(.toastui-editor-contents table) {
  border-color: var(--color-border-default) !important;
}

.toastui-editor-dark :deep(.toastui-editor-contents th),
.toastui-editor-dark :deep(.toastui-editor-contents td) {
  border-color: var(--color-border-default) !important;
  background: var(--color-bg-card) !important;
  color: var(--color-text-body) !important;
}

.toastui-editor-dark :deep(.toastui-editor-contents th) {
  background: var(--color-bg-muted) !important;
  color: var(--color-text-body) !important;
}

/* 인용구 */
.toastui-editor-dark :deep(.toastui-editor-contents blockquote) {
  color: var(--color-text-meta) !important;
  border-left-color: var(--color-brand-primary) !important;
}

.toastui-editor-dark :deep(.toastui-editor-contents blockquote p) {
  color: var(--color-text-meta) !important;
}

/* 링크 */
.toastui-editor-dark :deep(.toastui-editor-contents a) {
  color: var(--color-text-link) !important;
}

.toastui-editor-dark :deep(.toastui-editor-contents a:hover) {
  color: var(--color-text-link-hover) !important;
}

/* 이미지 업로드 영역 */
.toastui-editor-dark :deep(.toastui-editor-popup) {
  background: var(--color-bg-elevated) !important;
  border-color: var(--color-border-default) !important;
  color: var(--color-text-body) !important;
}

.toastui-editor-dark :deep(.toastui-editor-popup input) {
  background: var(--color-bg-card) !important;
  border-color: var(--color-border-default) !important;
  color: var(--color-text-body) !important;
}

.toastui-editor-dark :deep(.toastui-editor-popup label) {
  color: var(--color-text-body) !important;
}

/* 툴바 구분선 */
.toastui-editor-dark :deep(.toastui-editor-toolbar-divider) {
  background: var(--color-border-default) !important;
}

/* 선택 영역 */
.toastui-editor-dark :deep(.ProseMirror-selectednode) {
  outline: 2px solid var(--color-brand-primary) !important;
}

/* 플레이스홀더 */
.toastui-editor-dark :deep(.ProseMirror .placeholder) {
  color: var(--color-text-muted) !important;
}

/* 리스트 마커 */
.toastui-editor-dark :deep(.toastui-editor-contents ul li::marker),
.toastui-editor-dark :deep(.toastui-editor-contents ol li::marker) {
  color: var(--color-text-body) !important;
}

/* 체크박스 */
.toastui-editor-dark :deep(.toastui-editor-contents input[type="checkbox"]) {
  border-color: var(--color-border-default) !important;
}

/* 강조 텍스트 */
.toastui-editor-dark :deep(.toastui-editor-contents strong),
.toastui-editor-dark :deep(.toastui-editor-contents b) {
  color: var(--color-text-heading) !important;
}

/* 기울임 텍스트 */
.toastui-editor-dark :deep(.toastui-editor-contents em),
.toastui-editor-dark :deep(.toastui-editor-contents i) {
  color: var(--color-text-body) !important;
}
</style>