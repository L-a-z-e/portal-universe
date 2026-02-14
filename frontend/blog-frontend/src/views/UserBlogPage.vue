<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue';
import { useRouter } from 'vue-router';
import { Spinner, Alert } from '@portal/design-system-vue';
import UserProfileCard from '@/components/UserProfileCard.vue';
import PostCard from '@/components/PostCard.vue';
import type { UserProfileResponse } from '@/dto/user';
import type { PostSummaryResponse } from '@/dto/post';
import { getPublicProfile } from '@/api/users';
import { getPostsByAuthor } from '@/api/posts';

interface Props {
  username: string;
}
const props = defineProps<Props>();

const router = useRouter();

// 상태
const user = ref<UserProfileResponse | null>(null);
const posts = ref<PostSummaryResponse[]>([]);
const loading = ref(false);
const postsLoading = ref(false);
const error = ref('');
const currentPage = ref(1);
const totalPages = ref(0);
const hasMore = ref(false);

// 사용자 프로필 조회
const fetchUserProfile = async () => {
  loading.value = true;
  error.value = '';

  try {
    user.value = await getPublicProfile(props.username);
    // 프로필 조회 성공 후 게시글 조회
    fetchUserPosts(1);
  } catch (err: any) {
    if (err.response?.status === 404) {
      error.value = '사용자를 찾을 수 없습니다.';
    } else {
      error.value = err.response?.data?.message || '프로필을 불러오는데 실패했습니다.';
    }
  } finally {
    loading.value = false;
  }
};

// 사용자 게시글 조회
const fetchUserPosts = async (page: number = 1) => {
  if (!user.value) return;

  postsLoading.value = true;

  try {
    const response = await getPostsByAuthor(String(user.value.id), page, 12);

    if (page === 1) {
      posts.value = response.items;
    } else {
      posts.value = [...posts.value, ...response.items];
    }

    currentPage.value = response.page;
    totalPages.value = response.totalPages;
    hasMore.value = response.page < response.totalPages;
  } catch (err: any) {
    console.error('Failed to fetch user posts:', err);
  } finally {
    postsLoading.value = false;
  }
};

// 더 보기
const loadMore = () => {
  if (!postsLoading.value && hasMore.value) {
    fetchUserPosts(currentPage.value + 1);
  }
};

// 무한 스크롤
const handleScroll = () => {
  const scrollTop = window.scrollY;
  const windowHeight = window.innerHeight;
  const documentHeight = document.documentElement.scrollHeight;

  if (scrollTop + windowHeight >= documentHeight - 200) {
    loadMore();
  }
};

// 게시글 클릭
const handlePostClick = (postId: string) => {
  router.push(`/${postId}`);
};

// 초기 로드
onMounted(() => {
  fetchUserProfile();
  window.addEventListener('scroll', handleScroll);
});

// Username 변경 감지
watch(() => props.username, () => {
  fetchUserProfile();
});

// Cleanup: scroll listener 제거
onBeforeUnmount(() => {
  window.removeEventListener('scroll', handleScroll);
});
</script>

<template>
  <div class="user-blog-page">
    <!-- 로딩 -->
    <div v-if="loading" class="loading-container">
      <Spinner size="lg" />
    </div>

    <!-- 에러 -->
    <Alert v-else-if="error" variant="error" class="error-alert">
      {{ error }}
    </Alert>

    <!-- 사용자 블로그 -->
    <div v-else-if="user" class="blog-container">
      <!-- 프로필 헤더 -->
      <section class="profile-section">
        <UserProfileCard :user="user" />
      </section>

      <!-- 게시글 목록 -->
      <section class="posts-section">
        <h2 class="section-title">게시글</h2>

        <!-- 게시글 그리드 -->
        <div v-if="posts.length > 0" class="posts-grid">
          <PostCard
            v-for="post in posts"
            :key="post.id"
            :post="post"
            @click="handlePostClick"
          />
        </div>

        <!-- 빈 상태 -->
        <div v-else-if="!postsLoading" class="empty-state">
          <svg class="empty-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
            />
          </svg>
          <p class="empty-message">작성한 게시글이 없습니다.</p>
        </div>

        <!-- 로딩 (더 보기) -->
        <div v-if="postsLoading && posts.length > 0" class="loading-more">
          <Spinner size="md" />
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.user-blog-page {
  width: 100%;
  min-height: 100vh;
  padding: 2rem 1rem;
}

/* 로딩 */
.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 50vh;
}

/* 에러 */
.error-alert {
  max-width: 600px;
  margin: 0 auto;
}

/* 블로그 컨테이너 */
.blog-container {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 3rem;
}

/* 프로필 섹션 */
.profile-section {
  width: 100%;
}

/* 게시글 섹션 */
.posts-section {
  width: 100%;
}

.section-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--semantic-text-heading);
  margin-bottom: 1.5rem;
}

/* 게시글 그리드 */
.posts-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 1.5rem;
}

@media (min-width: 640px) {
  .posts-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (min-width: 1024px) {
  .posts-grid {
    grid-template-columns: repeat(3, 1fr);
  }
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
}

/* 로딩 (더 보기) */
.loading-more {
  display: flex;
  justify-content: center;
  padding: 2rem 0;
}
</style>
