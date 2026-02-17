<script setup lang="ts">
import { useRouter } from 'vue-router';
import { Card, Button } from '@portal/design-vue';

const router = useRouter();
</script>

<template>
  <div class="min-h-[calc(100vh-200px)] flex items-center justify-center p-4 bg-bg-page">
    <Card variant="elevated" padding="lg" class="w-full max-w-2xl">
      <div class="text-center py-12">
        <!-- 404 Icon -->
        <div class="text-8xl mb-6">🔍</div>

        <!-- Error Code -->
        <div class="text-6xl font-bold text-brand-primary mb-4">404</div>

        <!-- Error Message -->
        <h1 class="text-3xl font-bold text-text-heading mb-3">
          페이지를 찾을 수 없습니다
        </h1>
        <p class="text-lg text-text-meta mb-8 max-w-md mx-auto">
          요청하신 페이지가 존재하지 않거나 이동되었을 수 있습니다.
          URL을 다시 확인해주세요.
        </p>

        <!-- Actions -->
        <div class="flex flex-wrap gap-3 justify-center">
          <Button
            variant="primary"
            size="lg"
            @click="router.push('/')"
          >
            홈으로 돌아가기
          </Button>
          <Button
            variant="secondary"
            size="lg"
            @click="router.back()"
          >
            이전 페이지로
          </Button>
        </div>

        <!-- Helpful Links -->
        <div class="mt-12 pt-8 border-t border-border-default">
          <p class="text-sm text-text-meta mb-4">도움이 필요하신가요?</p>
          <div class="flex flex-wrap gap-4 justify-center text-sm">
            <router-link
              to="/"
              class="text-brand-primary hover:text-brand-primaryHover font-medium"
            >
              홈
            </router-link>
            <router-link
              to="/blog"
              class="text-brand-primary hover:text-brand-primaryHover font-medium"
            >
              블로그
            </router-link>
            <router-link
              to="/shopping"
              class="text-brand-primary hover:text-brand-primaryHover font-medium"
            >
              쇼핑
            </router-link>
          </div>
        </div>
      </div>
    </Card>
  </div>
</template>