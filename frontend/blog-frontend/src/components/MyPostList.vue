<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { Button, Tag, Alert, Spinner, Tabs, useApiError } from '@portal/design-system-vue';
import type { TabItem } from '@portal/design-system-vue';
import type { PostSummaryResponse, PostStatus } from '@/dto/post';
import { getMyPosts, deletePost, changePostStatus } from '@/api/posts';

// 상태 필터 옵션
type FilterStatus = 'ALL' | PostStatus;

const router = useRouter();
const { handleError } = useApiError();

// 상태
const posts = ref<PostSummaryResponse[]>([]);
const loading = ref(false);
const error = ref('');
const currentFilter = ref<FilterStatus>('ALL');
const currentPage = ref(0);
const totalPages = ref(0);
const hasMore = ref(false);

// 필터링된 게시글
const filteredPosts = computed(() => {
  if (currentFilter.value === 'ALL') {
    return posts.value;
  }
  // 상태 필터링은 서버에서 처리되므로 여기서는 그대로 반환
  return posts.value;
});

// 게시글 조회
const fetchPosts = async (page: number = 0) => {
  loading.value = true;
  error.value = '';

  try {
    const statusParam = currentFilter.value === 'ALL' ? undefined : currentFilter.value;
    const response = await getMyPosts(statusParam, page, 20);

    if (page === 0) {
      posts.value = response.content;
    } else {
      posts.value = [...posts.value, ...response.content];
    }

    currentPage.value = response.number;
    totalPages.value = response.totalPages;
    hasMore.value = !response.last;
  } catch (err: unknown) {
    const { message } = handleError(err, '게시글을 불러오는데 실패했습니다.');
    error.value = message;
  } finally {
    loading.value = false;
  }
};

// 탭 아이템
const filterTabs: TabItem[] = [
  { label: '전체', value: 'ALL' },
  { label: '발행됨', value: 'PUBLISHED' },
  { label: '임시저장', value: 'DRAFT' },
];

// 필터 변경
const handleFilterChange = (filter: string | number) => {
  currentFilter.value = filter as FilterStatus;
  currentPage.value = 0;
  fetchPosts(0);
};

// 더 보기
const loadMore = () => {
  if (!loading.value && hasMore.value) {
    fetchPosts(currentPage.value + 1);
  }
};

// 게시글 수정
const handleEdit = (postId: string) => {
  router.push(`/edit/${postId}`);
};

// 게시글 삭제
const handleDelete = async (postId: string) => {
  if (!confirm('정말 이 게시글을 삭제하시겠습니까?')) return;

  try {
    await deletePost(postId);
    posts.value = posts.value.filter((p) => p.id !== postId);
  } catch (err: unknown) {
    handleError(err, '게시글 삭제에 실패했습니다.');
  }
};

// 게시글 발행/임시저장
const handleStatusChange = async (postId: string, newStatus: PostStatus) => {
  try {
    await changePostStatus(postId, { status: newStatus });
    // 게시글 목록 새로고침
    fetchPosts(0);
  } catch (err: unknown) {
    handleError(err, '상태 변경에 실패했습니다.');
  }
};

// 날짜 포맷팅
const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });
};

// 상태 레이블
const getStatusLabel = (status: string) => {
  const labels: Record<string, string> = {
    DRAFT: '임시저장',
    PUBLISHED: '발행됨',
    ARCHIVED: '보관됨',
  };
  return labels[status] || status;
};

// 상태 색상
const getStatusVariant = (status: string) => {
  const variants: Record<string, any> = {
    DRAFT: 'warning',
    PUBLISHED: 'success',
    ARCHIVED: 'default',
  };
  return variants[status] || 'default';
};

onMounted(() => {
  fetchPosts();
});
</script>

<template>
  <div class="my-post-list">
    <!-- 필터 탭 -->
    <div class="filter-tabs-wrapper">
      <Tabs
        :model-value="currentFilter"
        @update:model-value="handleFilterChange"
        :items="filterTabs"
        variant="underline"
        size="sm"
      />
    </div>

    <!-- 에러 메시지 -->
    <Alert v-if="error" variant="error" class="mb-4">
      {{ error }}
    </Alert>

    <!-- 로딩 -->
    <div v-if="loading && posts.length === 0" class="loading-container">
      <Spinner size="lg" />
    </div>

    <!-- 게시글 목록 -->
    <div v-else-if="filteredPosts.length > 0" class="posts-container">
      <div
        v-for="post in filteredPosts"
        :key="post.id"
        class="post-item"
      >
        <!-- 제목 및 상태 -->
        <div class="post-header">
          <h3 class="post-title">{{ post.title }}</h3>
          <Tag :variant="getStatusVariant(post.status ?? 'DRAFT')" size="sm">
            {{ getStatusLabel(post.status ?? 'DRAFT') }}
          </Tag>
        </div>

        <!-- 메타 정보 -->
        <div class="post-meta">
          <span class="meta-item">{{ formatDate(post.publishedAt) }}</span>
          <span class="meta-separator">·</span>
          <span class="meta-item">조회 {{ post.viewCount ?? 0 }}</span>
          <span class="meta-separator">·</span>
          <span class="meta-item">좋아요 {{ post.likeCount ?? 0 }}</span>
        </div>

        <!-- 액션 버튼 -->
        <div class="post-actions">
          <Button
            variant="secondary"
            size="sm"
            @click="handleEdit(post.id)"
          >
            수정
          </Button>
          <Button
            v-if="post.status === 'DRAFT'"
            variant="primary"
            size="sm"
            @click="handleStatusChange(post.id, 'PUBLISHED')"
          >
            발행
          </Button>
          <Button
            variant="danger"
            size="sm"
            @click="handleDelete(post.id)"
          >
            삭제
          </Button>
        </div>
      </div>

      <!-- 더 보기 버튼 -->
      <div v-if="hasMore" class="load-more-container">
        <Button
          variant="secondary"
          @click="loadMore"
          :loading="loading"
        >
          더 보기
        </Button>
      </div>
    </div>

    <!-- 빈 상태 -->
    <div v-else class="empty-state">
      <svg class="empty-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path
          stroke-linecap="round"
          stroke-linejoin="round"
          stroke-width="2"
          d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
        />
      </svg>
      <p class="empty-message">작성한 게시글이 없습니다.</p>
      <Button variant="primary" @click="router.push('/write')">
        글 작성하기
      </Button>
    </div>
  </div>
</template>

<style scoped>
.my-post-list {
  width: 100%;
}

/* 필터 탭 */
.filter-tabs {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1.5rem;
  border-bottom: 1px solid var(--semantic-border-default);
}

.filter-tab {
  padding: 0.75rem 1rem;
  font-size: 0.9375rem;
  font-weight: 500;
  color: var(--semantic-text-meta);
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  cursor: pointer;
  transition: all 0.2s;
}

.filter-tab:hover {
  color: var(--semantic-text-heading);
}

.filter-tab.active {
  color: var(--semantic-brand-primary);
  border-bottom-color: var(--semantic-brand-primary);
}

/* 로딩 */
.loading-container {
  display: flex;
  justify-content: center;
  padding: 3rem 0;
}

/* 게시글 목록 */
.posts-container {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.post-item {
  padding: 1.25rem;
  border: 1px solid var(--semantic-border-default);
  border-radius: 0.5rem;
  background: var(--semantic-bg-card);
  transition: box-shadow 0.2s;
}

.post-item:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.post-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 0.75rem;
}

.post-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--semantic-text-heading);
  margin: 0;
  flex: 1;
}

.post-meta {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  color: var(--semantic-text-meta);
  margin-bottom: 1rem;
}

.meta-separator {
  color: var(--semantic-text-meta);
}

.post-actions {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

/* 더 보기 */
.load-more-container {
  display: flex;
  justify-content: center;
  padding: 2rem 0;
}

/* 빈 상태 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem 1rem;
  text-align: center;
}

.empty-icon {
  width: 4rem;
  height: 4rem;
  color: var(--semantic-text-meta);
  margin-bottom: 1rem;
}

.empty-message {
  font-size: 1rem;
  color: var(--semantic-text-meta);
  margin-bottom: 1.5rem;
}
</style>
