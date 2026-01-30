<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../store/auth';
import { authService } from '../services/authService';
import { Spinner, Card } from '@portal/design-system-vue';

const router = useRouter();
const authStore = useAuthStore();
const error = ref<string>('');
const processing = ref(true);

onMounted(() => {
  handleOAuth2Callback();
});

async function handleOAuth2Callback() {
  console.log('[OAuth2 Callback] Processing OAuth2 callback...');

  try {
    // Extract tokens from URL fragment
    const hash = window.location.hash.substring(1);
    const params = new URLSearchParams(hash);

    const accessToken = params.get('access_token');
    const expiresIn = params.get('expires_in');

    console.log('[OAuth2 Callback] Received tokens:', {
      hasAccessToken: !!accessToken,
      expiresIn,
    });

    if (!accessToken) {
      throw new Error('Missing access token in callback URL');
    }

    // Access Token만 설정 (Refresh Token은 이미 HttpOnly cookie에 있음)
    authService.setTokens(accessToken);

    // Extract user info from JWT
    const userInfo = authService.getUserInfo();
    if (!userInfo) {
      throw new Error('Failed to extract user info from token');
    }

    // Update auth store
    authStore.setAuthenticated(true);
    authStore.setUser(userInfo);

    console.log('✅ [OAuth2 Callback] Login successful');

    // Redirect to home page
    await router.replace('/');

  } catch (err: any) {
    console.error('❌ [OAuth2 Callback] Error:', err);
    error.value = err.message || 'OAuth2 로그인 처리 중 오류가 발생했습니다.';
    processing.value = false;
  }
}
</script>

<template>
  <div class="min-h-[calc(100vh-200px)] flex items-center justify-center p-4 bg-bg-page">
    <Card variant="elevated" padding="lg" class="w-full max-w-md">
      <div v-if="processing && !error" class="text-center py-8">
        <Spinner size="lg" class="mx-auto mb-6" />
        <h2 class="text-2xl font-bold text-text-heading mb-2">로그인 처리 중...</h2>
        <p class="text-text-meta">
          소셜 로그인을 완료하고 있습니다. 잠시만 기다려 주세요.
        </p>
      </div>

      <div v-else-if="error" class="text-center py-8">
        <div class="text-6xl mb-6">⚠️</div>
        <h2 class="text-2xl font-bold text-status-error mb-2">로그인 실패</h2>
        <p class="text-text-meta mb-6">{{ error }}</p>
        <button
            @click="router.push('/')"
            class="px-6 py-3 bg-brand-primary text-white font-semibold rounded-lg hover:bg-brand-primaryHover transition-all"
        >
          홈으로 돌아가기
        </button>
      </div>
    </Card>
  </div>
</template>
