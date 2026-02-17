<script setup lang="ts">
import { useRouter } from 'vue-router'
import { Card, Button } from '@portal/design-vue'

const router = useRouter()
</script>

<template>
  <div class="min-h-[calc(100vh-200px)] flex items-center justify-center p-4 bg-bg-page">
    <Card variant="elevated" padding="lg" class="w-full max-w-2xl">
      <div class="text-center py-12">
        <div class="text-8xl mb-6">🚫</div>
        <div class="text-6xl font-bold text-status-error mb-4">403</div>
        <h1 class="text-3xl font-bold text-text-heading mb-3">
          접근 권한이 없습니다
        </h1>
        <p class="text-lg text-text-meta mb-8 max-w-md mx-auto">
          이 페이지에 접근할 수 있는 권한이 부족합니다.<br />
          필요한 권한이 있다면 관리자에게 문의해 주세요.
        </p>
        <div class="flex flex-wrap gap-3 justify-center">
          <Button variant="primary" size="lg" @click="router.push('/')">
            홈으로 이동
          </Button>
          <Button variant="secondary" size="lg" @click="router.back()">
            이전 페이지
          </Button>
        </div>
      </div>
    </Card>
  </div>
</template>
