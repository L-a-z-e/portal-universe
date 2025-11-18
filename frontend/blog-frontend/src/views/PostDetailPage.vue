<script setup lang="ts">
import {onMounted, onBeforeUnmount, ref, nextTick, watch} from "vue";
import { useRoute, useRouter } from "vue-router";
import Viewer from '@toast-ui/editor/dist/toastui-editor-viewer';
import '@toast-ui/editor/dist/toastui-editor-viewer.css';
import '@toast-ui/editor/dist/theme/toastui-editor-dark.css';
import codeSyntaxHighlight from '@toast-ui/editor-plugin-code-syntax-highlight';
import Prism from 'prismjs';
import 'prismjs/themes/prism.css';
import 'prismjs/themes/prism-okaidia.css';
import { getPostById } from "../api/posts";
import { Button, Tag, Avatar, Card } from "@portal/design-system";
import type { PostResponse } from "@/dto/post.ts";

const route = useRoute();
const router = useRouter();
const post = ref<PostResponse | null>(null);
const isLoading = ref(true);
const error = ref<string | null>(null);

const viewerElement = ref<HTMLDivElement | null>(null);
let viewerInstance: Viewer | null = null;

// 다크모드 감지
const isDarkMode = ref(false);

// 테마 감지 함수
function detectTheme() {
  const theme = document.documentElement.getAttribute('data-theme');
  isDarkMode.value = theme === 'dark';

  if (viewerInstance) {
    updateViewerTheme();
  }
}

// Viewer 테마 업데이트
function updateViewerTheme() {
  if (!viewerInstance || !viewerElement.value) return;

  if (isDarkMode.value) {
    viewerElement.value.classList.add('toastui-editor-dark');
  } else {
    viewerElement.value.classList.remove('toastui-editor-dark');
  }
}

// Viewer 초기화 함수
function initViewer(content: string) {

  console.log('🔍 [DEBUG] initViewer called with content:', content?.substring(0, 100));
  console.log('🔍 [DEBUG] viewerElement exists:', !!viewerElement.value);

  if (!viewerElement.value) return;

  // 기존 인스턴스가 있으면 제거
  if (viewerInstance) {
    viewerInstance.destroy();
    viewerInstance = null;
  }

  // 새 Viewer 인스턴스 생성
  viewerInstance = new Viewer({
    el: viewerElement.value,
    initialValue: content,
    plugins: [[codeSyntaxHighlight, { highlighter: Prism }]],
  });

  // 초기 테마 적용
  updateViewerTheme();
}

onMounted(async () => {
  // 초기 테마 감지
  detectTheme();

  const postId = route.params.postId as string;
  if (!postId) {
    error.value = "존재하지 않는 게시글입니다";
    isLoading.value = false;
    return;
  }

  try {
    isLoading.value = true;
    error.value = null;
    post.value = await getPostById(postId);

    console.log('🔍 [DEBUG] postId:', postId);
    console.log('🔍 [DEBUG] post loaded:', post.value);
    console.log('🔍 [DEBUG] post.content:', post.value?.content);
    console.log('🔍 [DEBUG] viewerElement:', viewerElement.value);

  } catch (err) {
    error.value = "게시글을 가져오지 못했습니다.";
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

  // cleanup 시 observer 정리
  onBeforeUnmount(() => {
    observer.disconnect();
  });
});

onBeforeUnmount(() => {
  // Viewer 인스턴스 정리
  if (viewerInstance) {
    viewerInstance.destroy();
    viewerInstance = null;
  }
});

// 수정 페이지로 이동
function handleEdit() {
  if (post.value) {
    router.push(`/edit/${post.value.id}`);
  }
}

watch(() => post.value, async (newPost) => {
  if (newPost?.content) {
    await nextTick(); // DOM 업데이트 대기
    console.log('🔍 [WATCH] post loaded, viewerElement:', viewerElement.value);
    if (viewerElement.value) {
      initViewer(newPost.content);
    }
  }
});

</script>

<template>
  <div class="max-w-4xl mx-auto px-4 py-8">
    <!-- Loading & Error -->
    <div v-if="isLoading" class="text-center py-24">
      <div class="w-10 h-10 border-4 border-brand-primary border-t-transparent rounded-full animate-spin mx-auto mb-4"></div>
      <p class="text-text-meta">게시글을 불러오는 중...</p>
    </div>

    <Card v-else-if="error" class="bg-status-error-bg border-status-error/30 py-16 text-center">
      <div class="text-2xl text-status-error mb-4">❌</div>
      <div class="text-status-error">{{ error }}</div>
      <Button variant="secondary" class="mt-5" @click="router.back()">돌아가기</Button>
    </Card>

    <!-- Post Detail -->
    <article v-else-if="post" class="space-y-8">
      <!-- Header -->
      <header class="space-y-4 border-b border-border-default pb-6">
        <h1 class="text-4xl font-bold text-text-heading break-words leading-tight">
          {{ post.title }}
        </h1>

        <!-- Author & Metadata -->
        <div class="flex items-center justify-between flex-wrap gap-4">
          <div class="flex items-center gap-3">
            <Avatar :name="post.authorName || post.authorId" size="md" />
            <div class="flex flex-col">
              <span class="font-semibold text-text-heading">
                {{ post.authorName || post.authorId }}
              </span>
              <span class="text-sm text-text-meta">
                {{ new Date(post.createdAt).toLocaleString('ko-KR') }}
              </span>
            </div>
          </div>

          <!-- Stats & Actions -->
          <div class="flex items-center gap-4">
            <span class="flex items-center gap-1 text-sm text-text-meta">
              <span>👁</span>{{ post.viewCount || 0 }}
            </span>
            <span class="flex items-center gap-1 text-sm text-text-meta">
              <span>❤️</span>{{ post.likeCount || 0 }}
            </span>
            <!-- 수정 버튼 (권한 체크 필요) -->
            <Button variant="secondary" size="sm" @click="handleEdit">
              ✏️ 수정
            </Button>
          </div>
        </div>

        <!-- Category & Tags -->
        <div class="flex flex-wrap items-center gap-3">
          <span v-if="post.category" class="text-sm font-medium text-brand-primary">
            📂 {{ post.category }}
          </span>
          <div v-if="post.tags && post.tags.length" class="flex flex-wrap gap-2">
            <Tag v-for="tag in post.tags" :key="tag" variant="default" size="sm">
              {{ tag }}
            </Tag>
          </div>
        </div>
      </header>

      <!-- Content (Toast UI Viewer) -->
      <section class="post-content">
        <!-- [변경] v-html → Toast UI Viewer -->
        <div
            ref="viewerElement"
            :class="{ 'toastui-editor-dark': isDarkMode }"
            class="markdown-viewer"
        ></div>
      </section>

      <!-- Footer -->
      <footer class="border-t border-border-default pt-6 space-y-2">
        <div class="text-sm text-text-meta space-y-1">
          <div v-if="post.publishedAt">
            📅 최초 발행: {{ new Date(post.publishedAt).toLocaleString('ko-KR') }}
          </div>
          <div>
            🔄 최종 수정: {{ new Date(post.updatedAt).toLocaleString('ko-KR') }}
          </div>
        </div>
      </footer>

      <!-- Action Buttons -->
      <div class="flex items-center justify-between pt-6 border-t border-border-default">
        <Button variant="secondary" @click="router.push('/')">
          ← 목록으로
        </Button>
        <div class="flex gap-3">
          <Button variant="outline" @click="handleEdit">
            ✏️ 수정
          </Button>
          <Button variant="primary">
            ❤️ 좋아요
          </Button>
        </div>
      </div>

      <!-- 댓글 영역 Placeholder -->
      <div class="mt-12">
        <h2 class="text-2xl font-bold text-text-heading mb-6">💬 댓글</h2>
        <Card class="bg-bg-muted border-border-muted text-center py-12">
          <div class="text-3xl mb-2">💬</div>
          <div class="text-text-meta">댓글 기능 준비중...</div>
        </Card>
      </div>
    </article>
  </div>
</template>

<style scoped>
/* Toast UI Viewer 기본 스타일 */
.markdown-viewer {
  min-height: 200px;
}

/* Viewer 컨테이너 스타일 (라이트모드) */
:deep(.toastui-editor-contents) {
  font-size: 1.0625rem; /* 17px */
  line-height: 1.75;
  color: var(--color-text-body);
}

/* 제목 스타일 */
:deep(.toastui-editor-contents h1),
:deep(.toastui-editor-contents h2),
:deep(.toastui-editor-contents h3),
:deep(.toastui-editor-contents h4),
:deep(.toastui-editor-contents h5),
:deep(.toastui-editor-contents h6) {
  color: var(--color-text-heading);
  font-weight: 600;
  margin-top: 2rem;
  margin-bottom: 1rem;
  line-height: 1.4;
}

:deep(.toastui-editor-contents h1) {
  font-size: 2rem;
  border-bottom: 2px solid var(--color-border-default);
  padding-bottom: 0.5rem;
}

:deep(.toastui-editor-contents h2) {
  font-size: 1.75rem;
  border-bottom: 1px solid var(--color-border-muted);
  padding-bottom: 0.5rem;
}

:deep(.toastui-editor-contents h3) {
  font-size: 1.5rem;
}

:deep(.toastui-editor-contents h4) {
  font-size: 1.25rem;
}

/* 문단 */
:deep(.toastui-editor-contents p) {
  margin-bottom: 1.25rem;
  color: var(--color-text-body);
}

/* 링크 */
:deep(.toastui-editor-contents a) {
  color: var(--color-text-link);
  text-decoration: underline;
  text-decoration-color: var(--color-text-link);
  text-decoration-thickness: 1px;
  text-underline-offset: 2px;
}

:deep(.toastui-editor-contents a:hover) {
  color: var(--color-text-link-hover);
  text-decoration-color: var(--color-text-link-hover);
}

/* 코드 블록 */
:deep(.toastui-editor-contents pre) {
  background: var(--color-bg-muted);
  border: 1px solid var(--color-border-default);
  border-radius: 0.5rem;
  padding: 1rem;
  overflow-x: auto;
  margin: 1.5rem 0;
}

:deep(.toastui-editor-contents code) {
  background: var(--color-bg-muted);
  color: var(--color-brand-primary);
  padding: 0.2rem 0.4rem;
  border-radius: 0.25rem;
  font-size: 0.875rem;
  font-family: var(--font-family-mono);
}

:deep(.toastui-editor-contents pre code) {
  background: transparent;
  padding: 0;
  color: inherit;
}

/* 인용구 */
:deep(.toastui-editor-contents blockquote) {
  border-left: 4px solid var(--color-brand-primary);
  padding-left: 1rem;
  margin: 1.5rem 0;
  color: var(--color-text-meta);
  font-style: italic;
}

:deep(.toastui-editor-contents blockquote p) {
  margin-bottom: 0.5rem;
}

/* 리스트 */
:deep(.toastui-editor-contents ul),
:deep(.toastui-editor-contents ol) {
  margin: 1rem 0;
  padding-left: 2rem;
}

:deep(.toastui-editor-contents li) {
  margin-bottom: 0.5rem;
  color: var(--color-text-body);
}

:deep(.toastui-editor-contents li::marker) {
  color: var(--color-brand-primary);
}

/* 테이블 */
:deep(.toastui-editor-contents table) {
  width: 100%;
  border-collapse: collapse;
  margin: 1.5rem 0;
  overflow-x: auto;
  display: block;
}

:deep(.toastui-editor-contents th),
:deep(.toastui-editor-contents td) {
  border: 1px solid var(--color-border-default);
  padding: 0.75rem;
  text-align: left;
}

:deep(.toastui-editor-contents th) {
  background: var(--color-bg-muted);
  font-weight: 600;
  color: var(--color-text-heading);
}

:deep(.toastui-editor-contents td) {
  background: var(--color-bg-card);
  color: var(--color-text-body);
}

/* 구분선 */
:deep(.toastui-editor-contents hr) {
  border: none;
  border-top: 2px solid var(--color-border-default);
  margin: 2rem 0;
}

/* 이미지 */
:deep(.toastui-editor-contents img) {
  max-width: 100%;
  height: auto;
  border-radius: 0.5rem;
  margin: 1.5rem 0;
}

/* 체크박스 리스트 */
:deep(.toastui-editor-contents .task-list-item) {
  list-style: none;
  margin-left: -2rem;
}

:deep(.toastui-editor-contents .task-list-item input[type="checkbox"]) {
  margin-right: 0.5rem;
}

/* ============================================
   다크모드 스타일
   ============================================ */

/* 다크모드 컨테이너 */
.toastui-editor-dark :deep(.toastui-editor-contents) {
  color: var(--color-text-body);
}

/* 다크모드 제목 */
.toastui-editor-dark :deep(.toastui-editor-contents h1),
.toastui-editor-dark :deep(.toastui-editor-contents h2),
.toastui-editor-dark :deep(.toastui-editor-contents h3),
.toastui-editor-dark :deep(.toastui-editor-contents h4),
.toastui-editor-dark :deep(.toastui-editor-contents h5),
.toastui-editor-dark :deep(.toastui-editor-contents h6) {
  color: var(--color-text-heading);
}

.toastui-editor-dark :deep(.toastui-editor-contents h1) {
  border-bottom-color: var(--color-border-default);
}

.toastui-editor-dark :deep(.toastui-editor-contents h2) {
  border-bottom-color: var(--color-border-muted);
}

/* 다크모드 문단 */
.toastui-editor-dark :deep(.toastui-editor-contents p) {
  color: var(--color-text-body);
}

/* 다크모드 링크 */
.toastui-editor-dark :deep(.toastui-editor-contents a) {
  color: var(--color-text-link);
}

.toastui-editor-dark :deep(.toastui-editor-contents a:hover) {
  color: var(--color-text-link-hover);
}

/* 다크모드 코드 블록 */
.toastui-editor-dark :deep(.toastui-editor-contents pre) {
  background: var(--color-bg-elevated);
  border-color: var(--color-border-default);
}

.toastui-editor-dark :deep(.toastui-editor-contents code) {
  background: var(--color-bg-muted);
  color: var(--color-brand-primary);
}

/* 다크모드 인용구 */
.toastui-editor-dark :deep(.toastui-editor-contents blockquote) {
  border-left-color: var(--color-brand-primary);
  color: var(--color-text-meta);
}

/* 다크모드 리스트 */
.toastui-editor-dark :deep(.toastui-editor-contents li) {
  color: var(--color-text-body);
}

.toastui-editor-dark :deep(.toastui-editor-contents li::marker) {
  color: var(--color-brand-primary);
}

/* 다크모드 테이블 */
.toastui-editor-dark :deep(.toastui-editor-contents th),
.toastui-editor-dark :deep(.toastui-editor-contents td) {
  border-color: var(--color-border-default);
}

.toastui-editor-dark :deep(.toastui-editor-contents th) {
  background: var(--color-bg-muted);
  color: var(--color-text-heading);
}

.toastui-editor-dark :deep(.toastui-editor-contents td) {
  background: var(--color-bg-card);
  color: var(--color-text-body);
}

/* 다크모드 구분선 */
.toastui-editor-dark :deep(.toastui-editor-contents hr) {
  border-top-color: var(--color-border-default);
}

/* 다크모드 강조 텍스트 */
.toastui-editor-dark :deep(.toastui-editor-contents strong),
.toastui-editor-dark :deep(.toastui-editor-contents b) {
  color: var(--color-text-heading);
}

/* 다크모드 기울임 텍스트 */
.toastui-editor-dark :deep(.toastui-editor-contents em),
.toastui-editor-dark :deep(.toastui-editor-contents i) {
  color: var(--color-text-body);
}
</style>
