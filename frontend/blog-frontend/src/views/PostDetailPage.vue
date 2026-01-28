<script setup lang="ts">
import {onMounted, onBeforeUnmount, ref, nextTick, watch, computed} from "vue";
import { useRoute, useRouter } from "vue-router";
import Viewer from '@toast-ui/editor/dist/toastui-editor-viewer';
import '@toast-ui/editor/dist/toastui-editor-viewer.css';
import '@toast-ui/editor/dist/theme/toastui-editor-dark.css';
import codeSyntaxHighlight from '@toast-ui/editor-plugin-code-syntax-highlight';
import Prism from 'prismjs';
import 'prismjs/themes/prism.css';
import 'prismjs/themes/prism-okaidia.css';
import { getPostById, deletePost } from "../api/posts";
import { getSeriesByPostId } from "../api/series";
import {Button, Tag, Avatar, Card, Modal} from "@portal/design-system-vue";
import type { PostResponse } from "@/dto/post.ts";
import LikeButton from "@/components/LikeButton.vue";
import LikersModal from "@/components/LikersModal.vue";
import SeriesBox from "@/components/SeriesBox.vue";
import RelatedPosts from "@/components/RelatedPosts.vue";
import PostNavigation from "@/components/PostNavigation.vue";
import CommentList from "@/components/CommentList.vue";

const route = useRoute();
const router = useRouter();
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

// JWT에서 현재 사용자 UUID 추출
function getCurrentUserUuid(): string | null {
  const token = window.__PORTAL_ACCESS_TOKEN__;
  if (!token) return null;
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    return payload.sub || null;
  } catch {
    return null;
  }
}

// 본인 게시글 여부
const isAuthor = computed(() => {
  if (!post.value) return false;
  const currentUuid = getCurrentUserUuid();
  if (!currentUuid) return false;
  return post.value.authorId === currentUuid;
});

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

// ✅ Viewer 초기화 함수 (안전하게)
function initViewer(content: string) {
  console.log('🔍 [VIEWER] initViewer called');

  if (!viewerElement.value) {
    console.warn('⚠️ [VIEWER] viewerElement is null, skipping');
    return;
  }

  // ✅ 기존 인스턴스가 있으면 제거
  if (viewerInstance) {
    console.log('🔄 [VIEWER] Destroying existing instance');
    try {
      viewerInstance.destroy();
    } catch (err) {
      console.error('⚠️ [VIEWER] Destroy error:', err);
    }
    viewerInstance = null;
  }

  try {
    console.log('✅ [VIEWER] Creating new instance');

    // 새 Viewer 인스턴스 생성
    viewerInstance = new Viewer({
      el: viewerElement.value,
      initialValue: content,
      plugins: [[codeSyntaxHighlight, { highlighter: Prism }]],
    });

    // 초기 테마 적용
    updateViewerTheme();

    console.log('✅ [VIEWER] Initialization complete');
  } catch (err) {
    console.error('❌ [VIEWER] Initialization failed:', err);
  }
}

// ✅ post와 viewerElement가 모두 준비되었을 때만 초기화
watch(
    [() => post.value, viewerElement],
    async ([newPost, newElement]) => {
      console.log('👀 [WATCH] Triggered:', {
        hasPost: !!newPost,
        hasContent: !!newPost?.content,
        hasElement: !!newElement
      });

      if (newPost?.content && newElement) {
        console.log('✅ [WATCH] Both ready, initializing viewer');
        await nextTick();
        initViewer(newPost.content);
      }
    },
    {
      immediate: false,  // ✅ immediate: false (onMounted 후에만 실행)
      flush: 'post'      // ✅ DOM 업데이트 후 실행
    }
);

// ✅ 데이터 로드
async function loadPost() {
  const postId = route.params.postId as string;

  if (!postId) {
    error.value = "존재하지 않는 게시글입니다";
    isLoading.value = false;
    return;
  }

  try {
    console.log('📍 [LOAD] Loading post:', postId);
    isLoading.value = true;
    error.value = null;

    post.value = await getPostById(postId);

    if (post.value) {
      // 좋아요 정보 설정
      likeCount.value = post.value.likeCount || 0;

      // 시리즈 정보 조회
      try {
        const seriesList = await getSeriesByPostId(postId);
        if (seriesList && seriesList.length > 0) {
          seriesId.value = seriesList[0].id;
        }
      } catch (seriesErr) {
        console.warn('Failed to load series info:', seriesErr);
      }
    }

  } catch (err) {
    console.error('❌ [ERROR] Failed to load post:', err);
    error.value = "게시글을 가져오지 못했습니다.";
  } finally {
    isLoading.value = false;
    console.log('✅ [LOAD] Post loaded, watch will handle viewer init');
  }
}

onMounted(async () => {
  console.log('📍 [MOUNTED] PostDetailPage mounted');

  // 초기 테마 감지
  detectTheme();

  // 데이터 로드 (watch가 viewer 초기화 처리)
  await loadPost();

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
  console.log('🔄 [CLEANUP] Destroying viewer instance');

  // Viewer 인스턴스 정리
  if (viewerInstance) {
    try {
      viewerInstance.destroy();
    } catch (err) {
      console.error('⚠️ [CLEANUP] Destroy error:', err);
    }
    viewerInstance = null;
  }
});

// 수정 페이지로 이동
function handleEdit() {
  if (post.value) {
    router.push(`/edit/${post.value.id}`);
  }
}

// 삭제 핸들러
async function handleDelete() {
  if (!post.value) return;
  isDeleting.value = true;
  try {
    await deletePost(post.value.id);
    showDeleteConfirm.value = false;
    router.push('/');
  } catch (err) {
    console.error('Failed to delete post:', err);
    alert('게시글 삭제에 실패했습니다.');
  } finally {
    isDeleting.value = false;
  }
}

// 좋아요 변경 핸들러
function handleLikeChanged(liked: boolean, count: number) {
  isLiked.value = liked;
  likeCount.value = count;
  if (post.value) {
    post.value.likeCount = count;
  }
}
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
      <!-- Series Box (시리즈에 속한 경우) -->
      <SeriesBox
        v-if="seriesId"
        :series-id="seriesId"
        :current-post-id="post.id"
      />

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

          <!-- Stats -->
          <div class="flex items-center gap-4">
            <span class="flex items-center gap-1 text-sm text-text-meta">
              <span>👁</span>{{ post.viewCount || 0 }}
            </span>
            <button class="flex items-center gap-1 text-sm text-text-meta hover:text-brand-primary transition-colors cursor-pointer" @click="showLikersModal = true">
              <span>❤️</span>{{ post.likeCount || 0 }}
            </button>
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

      <!-- Author Action Bar (작성자만 표시) -->
      <div v-if="isAuthor" class="flex items-center justify-end gap-3 py-3 px-4 bg-bg-elevated rounded-lg border border-border-default">
        <span class="text-sm text-text-meta mr-auto">이 게시글의 작성자입니다</span>
        <Button variant="primary" size="sm" @click="handleEdit">
          ✏️ 수정
        </Button>
        <Button variant="outline" size="sm" class="text-status-error border-status-error hover:bg-status-error-bg" @click="showDeleteConfirm = true">
          🗑️ 삭제
        </Button>
      </div>

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

      <!-- Like Button Section -->
      <div class="like-section">
        <div class="like-container">
          <p class="like-message">이 글이 마음에 드셨나요?</p>
          <LikeButton
            :post-id="post.id"
            :initial-liked="isLiked"
            :initial-count="likeCount"
            @like-changed="handleLikeChanged"
          />
        </div>
      </div>

      <!-- Action Buttons -->
      <div class="flex items-center justify-between pt-6 border-t border-border-default">
        <Button variant="secondary" @click="router.push('/')">
          목록으로
        </Button>
      </div>

      <!-- Post Navigation (이전/다음 게시글) -->
      <PostNavigation :post-id="post.id" />

      <!-- Related Posts (관련 게시글) -->
      <RelatedPosts
        :post-id="post.id"
        :tags="post.tags"
        :limit="4"
      />

      <!-- 댓글 영역 -->
      <CommentList :post-id="post.id" :current-user-id="getCurrentUserUuid() ?? undefined" />

      <!-- 삭제 확인 모달 -->
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
          <Button variant="primary" size="sm" class="bg-status-error hover:bg-red-700" @click="handleDelete" :disabled="isDeleting">
            {{ isDeleting ? '삭제 중...' : '삭제' }}
          </Button>
        </div>
      </Modal>

      <!-- 좋아요 사용자 목록 모달 -->
      <LikersModal
        :post-id="post.id"
        :is-open="showLikersModal"
        @close="showLikersModal = false"
      />
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

/* ============================================
   Like Section Styles
   ============================================ */
.like-section {
  padding: 2rem 0;
  border-top: 1px solid var(--color-border-default);
  border-bottom: 1px solid var(--color-border-default);
}

.like-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
}

.like-message {
  font-size: 1rem;
  font-weight: 500;
  color: var(--color-text-heading);
  margin: 0;
  text-align: center;
}

/* 반응형 - 모바일 */
@media (max-width: 640px) {
  .like-section {
    padding: 1.5rem 0;
  }

  .like-message {
    font-size: 0.9375rem;
  }
}
</style>
