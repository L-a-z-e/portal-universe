<script setup lang="ts">
import {onMounted, onBeforeUnmount, ref, nextTick, watch, computed} from "vue";
import { useRoute, useRouter } from "vue-router";
import Viewer from '@toast-ui/editor/dist/toastui-editor-viewer';
import '@toast-ui/editor/dist/toastui-editor-viewer.css';
import '@toast-ui/editor/dist/theme/toastui-editor-dark.css';
import '@/assets/styles/toastui-dark-viewer.css';
import codeSyntaxHighlight from '@toast-ui/editor-plugin-code-syntax-highlight';
import Prism from 'prismjs';
import 'prismjs/themes/prism.css';
import 'prismjs/themes/prism-okaidia.css';
import { getPostById, deletePost } from "../api/posts";
import { getSeriesByPostId } from "../api/series";
import {Button, Tag, Avatar, Card, Modal, useApiError} from "@portal/design-vue";
import type { PostResponse } from "@/dto/post.ts";
import LikeButton from "@/components/LikeButton.vue";

import LikersModal from "@/components/LikersModal.vue";
import SeriesBox from "@/components/SeriesBox.vue";
import RelatedPosts from "@/components/RelatedPosts.vue";
import PostNavigation from "@/components/PostNavigation.vue";
import CommentList from "@/components/CommentList.vue";
import { useThemeDetection } from "@/composables/useThemeDetection";
import { usePortalAuth } from '@portal/vue-bridge';
const route = useRoute();
const router = useRouter();
const { handleError } = useApiError();
const post = ref<PostResponse | null>(null);

const isLoading = ref(true);
const error = ref<string | null>(null);

// 좋아요 상태
const likeCount = ref(0);
const isLiked = ref(false);

// 시리즈 정보
const seriesId = ref<string | null>(null);

// 삭제 확인 다이얼로그
const showDeleteConfirm = ref(false);
const isDeleting = ref(false);

// 좋아요 사용자 모달
const showLikersModal = ref(false);

const { userUuid } = usePortalAuth();

// 본인 게시글 여부
const isAuthor = computed(() => {
  if (!post.value) return false;
  if (!userUuid.value) return false;
  return post.value.authorId === userUuid.value;
});

const viewerElement = ref<HTMLDivElement | null>(null);
let viewerInstance: Viewer | null = null;

// 다크모드 감지
const { isDarkMode } = useThemeDetection();

watch(isDarkMode, () => {
  if (viewerInstance) {
    updateViewerTheme();
  }
});

// Viewer 테마 업데이트
function updateViewerTheme() {
  if (!viewerInstance || !viewerElement.value) return;

  if (isDarkMode.value) {
    viewerElement.value.classList.add('toastui-editor-dark');
  } else {
    viewerElement.value.classList.remove('toastui-editor-dark');
  }
}

function initViewer(content: string) {
  if (!viewerElement.value) return;

  if (viewerInstance) {
    try {
      viewerInstance.destroy();
    } catch {
      // ignore destroy errors
    }
    viewerInstance = null;
  }

  try {
    viewerInstance = new Viewer({
      el: viewerElement.value,
      initialValue: content,
      plugins: [[codeSyntaxHighlight, { highlighter: Prism }]],
    });

    updateViewerTheme();
  } catch {
    // viewer initialization failed
  }
}

watch(
    [() => post.value, viewerElement],
    async ([newPost, newElement]) => {
      if (newPost?.content && newElement) {
        await nextTick();
        initViewer(newPost.content);
      }
    },
    {
      immediate: false,
      flush: 'post'
    }
);

async function loadPost() {
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

    if (post.value) {
      likeCount.value = post.value.likeCount || 0;

      try {
        const seriesList = await getSeriesByPostId(postId);
        const firstSeries = seriesList?.[0];
        if (firstSeries) {
          seriesId.value = firstSeries.id;
        }
      } catch {
        // series info load failed - non-critical
      }
    }

  } catch {
    error.value = "게시글을 가져오지 못했습니다.";
  } finally {
    isLoading.value = false;
  }
}

watch(
  () => route.params.postId,
  (newId, oldId) => {
    if (newId && newId !== oldId) {
      seriesId.value = null;
      likeCount.value = 0;
      isLiked.value = false;
      loadPost();
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }
);

onMounted(async () => {
  await loadPost();
});

onBeforeUnmount(() => {
  if (viewerInstance) {
    try {
      viewerInstance.destroy();
    } catch {
      // ignore destroy errors
    }
    viewerInstance = null;
  }
});

function handleEdit() {
  if (post.value) {
    router.push(`/edit/${post.value.id}`);
  }
}

async function handleDelete() {
  if (!post.value) return;
  isDeleting.value = true;
  try {
    await deletePost(post.value.id);
    showDeleteConfirm.value = false;
    router.push('/');
  } catch (err) {
    handleError(err, '게시글 삭제에 실패했습니다.');
  } finally {
    isDeleting.value = false;
  }
}

function handleLikeChanged(liked: boolean, count: number) {
  isLiked.value = liked;
  likeCount.value = count;
  if (post.value) {
    post.value.likeCount = count;
  }
}
</script>

<template>
  <div class="w-full min-h-screen">
    <div class="max-w-3xl mx-auto px-6 py-8">
      <!-- Loading -->
      <div v-if="isLoading" class="flex justify-center py-24">
        <div class="w-8 h-8 border-2 border-border-default border-t-brand-primary rounded-full animate-spin"></div>
      </div>

      <!-- Error -->
      <Card v-else-if="error" class="bg-status-error-bg border-status-error/30 py-16 text-center">
        <div class="text-status-error mb-2">{{ error }}</div>
        <Button variant="secondary" size="sm" class="mt-4" @click="router.back()">돌아가기</Button>
      </Card>

      <!-- Post Detail -->
      <article v-else-if="post">
        <!-- Series Box -->
        <SeriesBox
          v-if="seriesId"
          :series-id="seriesId"
          :current-post-id="post.id"
          class="mb-10"
        />

        <!-- Article Header -->
        <header class="mb-10 pb-8 border-b border-border-default">
          <!-- Category Badge -->
          <span
            v-if="post.category"
            class="inline-flex px-2.5 py-1 rounded bg-brand-primary/10 text-brand-primary text-xs font-medium mb-4"
          >
            {{ post.category }}
          </span>

          <!-- Title -->
          <h1 class="text-4xl font-bold text-text-heading break-words leading-tight tracking-tight mb-6">
            {{ post.title }}
          </h1>

          <!-- Author Meta -->
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-4">
              <Avatar :name="post.authorName || '사용자'" size="md" class="border border-border-default" />
              <div class="flex flex-col">
                <span class="text-sm font-medium text-text-heading">
                  {{ post.authorName || '사용자' }}
                </span>
                <span class="text-xs text-text-meta">
                  {{ new Date(post.createdAt).toLocaleDateString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric' }) }}
                </span>
              </div>
            </div>

            <!-- Stats -->
            <div class="flex items-center gap-4 text-sm text-text-meta">
              <span class="flex items-center gap-1">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                </svg>
                {{ post.viewCount || 0 }}
              </span>
              <button
                class="flex items-center gap-1 hover:text-brand-primary transition-colors"
                @click="showLikersModal = true"
              >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                </svg>
                {{ likeCount }}
              </button>
            </div>
          </div>

          <!-- Tags -->
          <div v-if="post.tags && post.tags.length" class="flex flex-wrap gap-2 mt-6">
            <Tag v-for="tag in post.tags" :key="tag" variant="default" size="sm">
              {{ tag }}
            </Tag>
          </div>
        </header>

        <!-- Author Action Bar -->
        <div v-if="isAuthor" class="flex items-center justify-end gap-3 py-3 px-4 mb-8 bg-bg-elevated rounded-lg border border-border-default">
          <span class="text-sm text-text-meta mr-auto">이 게시글의 작성자입니다</span>
          <Button variant="primary" size="sm" @click="handleEdit">수정</Button>
          <Button variant="outline" size="sm" class="text-status-error border-status-error hover:bg-status-error-bg" @click="showDeleteConfirm = true">삭제</Button>
        </div>

        <!-- Content (Toast UI Viewer) -->
        <section class="post-content mb-16">
          <div
            ref="viewerElement"
            :class="{ 'toastui-editor-dark': isDarkMode }"
            class="markdown-viewer"
          ></div>
        </section>

        <!-- Like Section -->
        <div class="flex flex-col items-center gap-4 py-10 border-t border-b border-border-default">
          <p class="text-base font-medium text-text-heading">이 글이 마음에 드셨나요?</p>
          <LikeButton
            :post-id="post.id"
            :initial-liked="isLiked"
            :initial-count="likeCount"
            @like-changed="handleLikeChanged"
          />
        </div>

        <!-- Footer Meta -->
        <div class="flex items-center justify-between py-6 border-b border-border-default text-xs text-text-meta">
          <div class="space-y-1">
            <div v-if="post.publishedAt">
              최초 발행: {{ new Date(post.publishedAt).toLocaleDateString('ko-KR') }}
            </div>
            <div>
              최종 수정: {{ new Date(post.updatedAt).toLocaleDateString('ko-KR') }}
            </div>
          </div>
          <Button variant="secondary" size="sm" @click="router.push('/')">
            목록으로
          </Button>
        </div>

        <!-- Post Navigation -->
        <PostNavigation :post-id="post.id" class="my-8" />

        <!-- Related Posts -->
        <RelatedPosts
          :post-id="post.id"
          :tags="post.tags"
          :limit="4"
          class="my-8"
        />

        <!-- Comments -->
        <section class="mt-12">
          <CommentList :post-id="post.id" :current-user-id="userUuid ?? undefined" />
        </section>

        <!-- Delete Confirm Modal -->
        <Modal
          :model-value="showDeleteConfirm"
          title="게시글 삭제"
          size="sm"
          @update:model-value="showDeleteConfirm = $event"
          @close="showDeleteConfirm = false"
        >
          <p class="text-text-body mb-4">이 게시글을 정말 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.</p>
          <div class="flex justify-end gap-2">
            <Button variant="secondary" size="sm" @click="showDeleteConfirm = false" :disabled="isDeleting">취소</Button>
            <Button variant="danger" size="sm" @click="handleDelete" :disabled="isDeleting">
              {{ isDeleting ? '삭제 중...' : '삭제' }}
            </Button>
          </div>
        </Modal>

        <!-- Likers Modal -->
        <LikersModal
          :post-id="post.id"
          :is-open="showLikersModal"
          @close="showLikersModal = false"
        />
      </article>
    </div>
  </div>
</template>

<style scoped>
/* Toast UI Viewer */
.markdown-viewer {
  min-height: 200px;
}

:deep(.toastui-editor-contents) {
  font-size: 1.0625rem;
  line-height: 1.75;
  color: var(--semantic-text-body);
}

/* Headings */
:deep(.toastui-editor-contents h1),
:deep(.toastui-editor-contents h2),
:deep(.toastui-editor-contents h3),
:deep(.toastui-editor-contents h4),
:deep(.toastui-editor-contents h5),
:deep(.toastui-editor-contents h6) {
  color: var(--semantic-text-heading);
  font-weight: 600;
  margin-top: 2.5rem;
  margin-bottom: 1rem;
  line-height: 1.4;
}

:deep(.toastui-editor-contents h1) {
  font-size: 2rem;
  border-bottom: 2px solid var(--semantic-border-default);
  padding-bottom: 0.5rem;
}

:deep(.toastui-editor-contents h2) {
  font-size: 1.5rem;
  border-bottom: 1px solid var(--semantic-border-muted);
  padding-bottom: 0.5rem;
}

:deep(.toastui-editor-contents h3) {
  font-size: 1.25rem;
}

:deep(.toastui-editor-contents h4) {
  font-size: 1.125rem;
}

/* Paragraph */
:deep(.toastui-editor-contents p) {
  margin-bottom: 1.5rem;
  color: var(--semantic-text-body);
}

/* Links */
:deep(.toastui-editor-contents a) {
  color: var(--semantic-text-link);
  text-decoration: underline;
  text-decoration-color: var(--semantic-text-link);
  text-decoration-thickness: 1px;
  text-underline-offset: 2px;
}

:deep(.toastui-editor-contents a:hover) {
  color: var(--semantic-text-link-hover);
  text-decoration-color: var(--semantic-text-link-hover);
}

/* Code block */
:deep(.toastui-editor-contents pre) {
  background: var(--semantic-bg-muted);
  border: 1px solid var(--semantic-border-default);
  border-radius: 0.5rem;
  padding: 1rem;
  overflow-x: auto;
  margin: 1.5rem 0;
}

:deep(.toastui-editor-contents code) {
  background: var(--semantic-bg-muted);
  color: var(--semantic-brand-primary);
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

/* Blockquote */
:deep(.toastui-editor-contents blockquote) {
  border-left: 4px solid var(--semantic-brand-primary);
  background: var(--semantic-brand-primary-bg, rgba(107, 144, 128, 0.05));
  padding: 1rem 1rem 1rem 1.5rem;
  margin: 1.5rem 0;
  border-radius: 0 0.5rem 0.5rem 0;
  color: var(--semantic-text-body);
  font-style: italic;
}

:deep(.toastui-editor-contents blockquote p) {
  margin-bottom: 0.5rem;
}

:deep(.toastui-editor-contents blockquote p:last-child) {
  margin-bottom: 0;
}

/* Lists */
:deep(.toastui-editor-contents ul),
:deep(.toastui-editor-contents ol) {
  margin: 1rem 0;
  padding-left: 2rem;
}

:deep(.toastui-editor-contents li) {
  margin-bottom: 0.5rem;
  color: var(--semantic-text-body);
}

:deep(.toastui-editor-contents li::marker) {
  color: var(--semantic-brand-primary);
}

/* Table */
:deep(.toastui-editor-contents table) {
  width: 100%;
  border-collapse: collapse;
  margin: 1.5rem 0;
  overflow-x: auto;
  display: block;
}

:deep(.toastui-editor-contents th),
:deep(.toastui-editor-contents td) {
  border: 1px solid var(--semantic-border-default);
  padding: 0.75rem;
  text-align: left;
}

:deep(.toastui-editor-contents th) {
  background: var(--semantic-bg-muted);
  font-weight: 600;
  color: var(--semantic-text-heading);
}

:deep(.toastui-editor-contents td) {
  background: var(--semantic-bg-card);
  color: var(--semantic-text-body);
}

/* HR */
:deep(.toastui-editor-contents hr) {
  border: none;
  border-top: 2px solid var(--semantic-border-default);
  margin: 2rem 0;
}

/* Images */
:deep(.toastui-editor-contents img) {
  max-width: 100%;
  height: auto;
  border-radius: 0.5rem;
  margin: 1.5rem 0;
}

/* Task list */
:deep(.toastui-editor-contents .task-list-item) {
  list-style: none;
  margin-left: -2rem;
}

:deep(.toastui-editor-contents .task-list-item input[type="checkbox"]) {
  margin-right: 0.5rem;
}

/* Selection */
::selection {
  background: var(--semantic-brand-primary);
  color: white;
  opacity: 0.3;
}
</style>
