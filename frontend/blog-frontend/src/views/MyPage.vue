<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { Button, Spinner, Alert, Card } from '@portal/design-system-vue';
import UserProfileCard from '@/components/UserProfileCard.vue';
import ProfileEditForm from '@/components/ProfileEditForm.vue';
import MyPostList from '@/components/MyPostList.vue';
import MySeriesList from '@/components/MySeriesList.vue';
import type { UserProfileResponse } from '@/dto/user';
import { getMyProfile } from '@/api/users';

const route = useRoute();

// 상태
const user = ref<UserProfileResponse | null>(null);
const loading = ref(false);
const error = ref('');
const isEditMode = ref(false);
const activeTab = ref<'posts' | 'series'>(
  route.query.tab === 'series' ? 'series' : 'posts'
);

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

// 프로필 수정 모드 토글
const toggleEditMode = () => {
  isEditMode.value = !isEditMode.value;
};

// 프로필 수정 성공
const handleProfileUpdateSuccess = (updatedUser: UserProfileResponse) => {
  user.value = updatedUser;
  isEditMode.value = false;
};

// 프로필 수정 취소
const handleProfileUpdateCancel = () => {
  isEditMode.value = false;
};

// 초기 로드
onMounted(() => {
  fetchProfile();
});
</script>

<template>
  <div class="my-page">
    <!-- 로딩 -->
    <div v-if="loading" class="loading-container">
      <Spinner size="lg" />
    </div>

    <!-- 에러 -->
    <Alert v-else-if="error" variant="error" class="error-alert">
      {{ error }}
    </Alert>

    <!-- 마이페이지 콘텐츠 -->
    <div v-else-if="user" class="page-container">
      <!-- 프로필 섹션 -->
      <section class="profile-section">
        <!-- 프로필 보기 모드 -->
        <div v-if="!isEditMode" class="profile-view">
          <UserProfileCard :user="user" />
          <div class="profile-actions">
            <Button variant="primary" @click="toggleEditMode">
              프로필 수정
            </Button>
          </div>
        </div>

        <!-- 프로필 수정 모드 -->
        <Card v-else padding="lg">
          <ProfileEditForm
            :user="user"
            @success="handleProfileUpdateSuccess"
            @cancel="handleProfileUpdateCancel"
          />
        </Card>
      </section>

      <!-- 콘텐츠 섹션 -->
      <section class="content-section">
        <!-- 탭 네비게이션 -->
        <div class="tabs">
          <button
            :class="['tab', { active: activeTab === 'posts' }]"
            @click="activeTab = 'posts'"
          >
            내 게시글
          </button>
          <button
            :class="['tab', { active: activeTab === 'series' }]"
            @click="activeTab = 'series'"
          >
            내 시리즈
          </button>
        </div>

        <!-- 탭 콘텐츠 -->
        <div class="tab-content">
          <!-- 게시글 탭 -->
          <div v-if="activeTab === 'posts'" class="posts-tab">
            <MyPostList />
          </div>

          <!-- 시리즈 탭 -->
          <div v-else-if="activeTab === 'series'" class="series-tab">
            <MySeriesList />
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.my-page {
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

/* 페이지 컨테이너 */
.page-container {
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

.profile-view {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.profile-actions {
  display: flex;
  justify-content: center;
}

@media (min-width: 768px) {
  .profile-actions {
    justify-content: flex-start;
  }
}

/* 콘텐츠 섹션 */
.content-section {
  width: 100%;
}

/* 탭 네비게이션 */
.tabs {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 2rem;
  border-bottom: 1px solid var(--color-border-default);
}

.tab {
  padding: 0.75rem 1.5rem;
  font-size: 1rem;
  font-weight: 500;
  color: var(--color-text-meta);
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  cursor: pointer;
  transition: all 0.2s;
}

.tab:hover:not(:disabled) {
  color: var(--color-text-heading);
}

.tab.active {
  color: var(--color-brand-primary);
  border-bottom-color: var(--color-brand-primary);
}

.tab:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

/* 탭 콘텐츠 */
.tab-content {
  width: 100%;
}

.posts-tab,
.series-tab {
  width: 100%;
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

.empty-message {
  font-size: 1rem;
  color: var(--color-text-meta);
}
</style>
