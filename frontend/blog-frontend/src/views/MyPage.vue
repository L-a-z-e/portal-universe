<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { Button, Spinner, Alert, Card } from '@portal/design-vue';
import UserProfileCard from '@/components/UserProfileCard.vue';
import ProfileEditForm from '@/components/ProfileEditForm.vue';
import MyPostList from '@/components/MyPostList.vue';
import MySeriesList from '@/components/MySeriesList.vue';
import AuthorCategories from '@/components/AuthorCategories.vue';
import AuthorTags from '@/components/AuthorTags.vue';
import AuthorStats from '@/components/AuthorStats.vue';
import type { UserProfileResponse } from '@/dto/user';
import { getMyProfile } from '@/api/users';

const route = useRoute();
const router = useRouter();

// 상태
const user = ref<UserProfileResponse | null>(null);
const loading = ref(false);
const error = ref('');

type TabType = 'posts' | 'series' | 'categories' | 'tags' | 'stats' | 'about' | 'write';

const validTabs: TabType[] = ['posts', 'series', 'categories', 'tags', 'stats', 'about', 'write'];

const resolveTab = (query: string | undefined): TabType => {
  if (query && validTabs.includes(query as TabType)) return query as TabType;
  return 'posts';
};

const activeTab = ref<TabType>(resolveTab(route.query.tab as string));

const tabs: { label: string; value: TabType }[] = [
  { label: '글', value: 'posts' },
  { label: '시리즈', value: 'series' },
  { label: '카테고리', value: 'categories' },
  { label: '태그', value: 'tags' },
  { label: '통계', value: 'stats' },
  { label: '소개', value: 'about' },
  { label: '글쓰기', value: 'write' },
];

// URL 쿼리 동기화
const handleTabChange = (tab: TabType) => {
  if (tab === 'write') {
    router.push('/write');
    return;
  }
  activeTab.value = tab;
  router.replace({ query: { tab } });
};

// 프로필 조회
const fetchProfile = async () => {
  loading.value = true;
  error.value = '';

  try {
    user.value = await getMyProfile();
  } catch (err: any) {
    error.value = err.response?.data?.message || '프로필을 불러오는데 실패했습니다.';
  } finally {
    loading.value = false;
  }
};

// 프로필 수정 → "소개" 탭으로 이동
const goToAboutTab = () => {
  handleTabChange('about');
};

// 프로필 수정 성공
const handleProfileUpdateSuccess = (updatedUser: UserProfileResponse) => {
  user.value = updatedUser;
  handleTabChange('posts');
};

// 프로필 수정 취소
const handleProfileUpdateCancel = () => {
  handleTabChange('posts');
};

// URL 변경 감지 (뒤로가기 등)
watch(() => route.query.tab, (newTab) => {
  activeTab.value = resolveTab(newTab as string);
});

// 초기 로드
onMounted(() => {
  fetchProfile();
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

      <!-- 마이페이지 콘텐츠 -->
      <template v-else-if="user">
        <!-- 프로필 섹션 -->
        <section class="mb-8">
          <UserProfileCard :user="user" :is-current-user="true" />
          <div class="flex justify-center mt-4">
            <Button variant="secondary" size="sm" class="!rounded-full !px-6" @click="goToAboutTab">
              프로필 수정
            </Button>
          </div>
        </section>

        <!-- 탭 -->
        <div class="flex items-center gap-1 mb-8 border-b border-border-default overflow-x-auto">
          <button
            v-for="tab in tabs"
            :key="tab.value"
            class="pb-3 px-3 text-sm font-medium transition-all border-b-2 whitespace-nowrap"
            :class="activeTab === tab.value
              ? 'text-brand-primary border-brand-primary'
              : 'text-text-meta border-transparent hover:text-text-heading hover:border-border-hover'"
            @click="handleTabChange(tab.value)"
          >
            {{ tab.label }}
          </button>
        </div>

        <!-- 탭 콘텐츠 -->
        <div v-if="activeTab === 'posts'">
          <MyPostList />
        </div>

        <div v-else-if="activeTab === 'series'">
          <MySeriesList />
        </div>

        <div v-else-if="activeTab === 'categories'">
          <AuthorCategories :author-id="String(user.id)" />
        </div>

        <div v-else-if="activeTab === 'tags'">
          <AuthorTags :author-id="String(user.id)" />
        </div>

        <div v-else-if="activeTab === 'stats'">
          <AuthorStats :author-id="String(user.id)" />
        </div>

        <div v-else-if="activeTab === 'about'">
          <Card padding="lg">
            <ProfileEditForm
              :user="user"
              @success="handleProfileUpdateSuccess"
              @cancel="handleProfileUpdateCancel"
            />
          </Card>
        </div>
      </template>
    </div>
  </div>
</template>
