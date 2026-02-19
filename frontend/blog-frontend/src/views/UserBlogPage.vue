<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { Spinner, Alert } from '@portal/design-vue';
import UserProfileCard from '@/components/UserProfileCard.vue';
import PostCard from '@/components/PostCard.vue';
import AuthorSeriesList from '@/components/AuthorSeriesList.vue';
import AuthorCategories from '@/components/AuthorCategories.vue';
import AuthorTags from '@/components/AuthorTags.vue';
import AuthorStats from '@/components/AuthorStats.vue';
import type { UserProfileResponse } from '@/dto/user';
import type { PostSummaryResponse } from '@/dto/post';
import { getPublicProfile } from '@/api/users';
import { getPostsByAuthor } from '@/api/posts';

interface Props {
  username: string;
}
const props = defineProps<Props>();

const router = useRouter();
const route = useRoute();

// 상태
const user = ref<UserProfileResponse | null>(null);
const posts = ref<PostSummaryResponse[]>([]);
const loading = ref(false);
const postsLoading = ref(false);
const error = ref('');
const currentPage = ref(1);
const totalPages = ref(0);
const hasMore = ref(false);

// 탭
type TabType = 'posts' | 'series' | 'categories' | 'tags' | 'stats' | 'about';

const validTabs: TabType[] = ['posts', 'series', 'categories', 'tags', 'stats', 'about'];

const resolveTab = (query: string | undefined): TabType => {
  if (query && validTabs.includes(query as TabType)) return query as TabType;
  return 'posts';
};

const currentTab = ref<TabType>(resolveTab(route.query.tab as string));

const tabs: { label: string; value: TabType }[] = [
  { label: '글', value: 'posts' },
  { label: '시리즈', value: 'series' },
  { label: '카테고리', value: 'categories' },
  { label: '태그', value: 'tags' },
  { label: '통계', value: 'stats' },
  { label: '소개', value: 'about' },
];

const handleTabChange = (tab: TabType) => {
  currentTab.value = tab;
  router.replace({ query: { tab } });
};

// 사용자 프로필 조회
const fetchUserProfile = async () => {
  loading.value = true;
  error.value = '';

  try {
    user.value = await getPublicProfile(props.username);
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
    const response = await getPostsByAuthor(user.value.uuid, page, 12);

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
  if (currentTab.value !== 'posts') return;
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

// URL 변경 감지
watch(() => route.query.tab, (newTab) => {
  currentTab.value = resolveTab(newTab as string);
});

// 초기 로드
onMounted(() => {
  fetchUserProfile();
  window.addEventListener('scroll', handleScroll);
});

// Username 변경 감지
watch(() => props.username, () => {
  fetchUserProfile();
});

// Cleanup
onBeforeUnmount(() => {
  window.removeEventListener('scroll', handleScroll);
});
</script>

<template>
  <div class="w-full min-h-screen">
    <div class="max-w-3xl mx-auto px-6 pt-16 pb-32">
      <!-- 로딩 -->
      <div v-if="loading" class="flex justify-center items-center min-h-[50vh]">
        <Spinner size="lg" />
      </div>

      <!-- 에러 -->
      <Alert v-else-if="error" variant="error" class="max-w-md mx-auto">
        {{ error }}
      </Alert>

      <!-- 사용자 블로그 -->
      <template v-else-if="user">
        <!-- 프로필 -->
        <UserProfileCard :user="user" />

        <!-- 탭 -->
        <div class="flex items-center gap-1 mb-12 border-b border-border-default overflow-x-auto">
          <button
            v-for="tab in tabs"
            :key="tab.value"
            class="pb-3 px-3 text-sm font-medium transition-all border-b-2 whitespace-nowrap"
            :class="currentTab === tab.value
              ? 'text-brand-primary border-brand-primary'
              : 'text-text-meta border-transparent hover:text-text-heading hover:border-border-hover'"
            @click="handleTabChange(tab.value)"
          >
            {{ tab.label }}
          </button>
        </div>

        <!-- 글 탭 -->
        <template v-if="currentTab === 'posts'">
          <div v-if="posts.length > 0">
            <PostCard
              v-for="post in posts"
              :key="post.id"
              :post="post"
              @click="handlePostClick"
            />
          </div>

          <!-- 빈 상태 -->
          <div v-else-if="!postsLoading" class="flex flex-col items-center justify-center py-16 text-center">
            <svg class="w-16 h-16 text-text-meta mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
              />
            </svg>
            <p class="text-text-meta">작성한 게시글이 없습니다.</p>
          </div>

          <!-- 로딩 (더 보기) -->
          <div v-if="postsLoading && posts.length > 0" class="flex justify-center py-12">
            <div class="w-8 h-8 border-2 border-border-default border-t-brand-primary rounded-full animate-spin"></div>
          </div>
        </template>

        <!-- 시리즈 탭 -->
        <template v-else-if="currentTab === 'series'">
          <AuthorSeriesList :author-id="user.uuid" />
        </template>

        <!-- 카테고리 탭 -->
        <template v-else-if="currentTab === 'categories'">
          <AuthorCategories :author-id="user.uuid" />
        </template>

        <!-- 태그 탭 -->
        <template v-else-if="currentTab === 'tags'">
          <AuthorTags :author-id="user.uuid" />
        </template>

        <!-- 통계 탭 -->
        <template v-else-if="currentTab === 'stats'">
          <AuthorStats :author-id="user.uuid" />
        </template>

        <!-- 소개 탭 -->
        <template v-else-if="currentTab === 'about'">
          <div class="py-8">
            <p v-if="user.bio" class="text-text-body leading-relaxed whitespace-pre-wrap">{{ user.bio }}</p>
            <p v-else class="text-text-meta text-center py-16">소개글이 없습니다.</p>
          </div>
        </template>
      </template>
    </div>
  </div>
</template>
