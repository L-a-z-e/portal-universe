<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

function goHome() {
  router.push('/')
}

function goBack() {
  router.back()
}
</script>

<template>
  <div class="forbidden-page">
    <div class="forbidden-content">
      <h1 class="forbidden-code">403</h1>
      <h2 class="forbidden-title">접근 권한이 없습니다</h2>
      <p class="forbidden-description">
        이 페이지에 접근할 수 있는 권한이 부족합니다.<br />
        필요한 권한이 있다면 관리자에게 문의해 주세요.
      </p>
      <div class="forbidden-actions">
        <button class="btn btn-secondary" @click="goBack">
          이전 페이지
        </button>
        <button class="btn btn-primary" @click="goHome">
          홈으로 이동
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.forbidden-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  padding: 2rem;
}

.forbidden-content {
  text-align: center;
  max-width: 480px;
}

.forbidden-code {
  font-size: 6rem;
  font-weight: 800;
  color: var(--color-text-tertiary, #9ca3af);
  line-height: 1;
  margin-bottom: 0.5rem;
}

.forbidden-title {
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--color-text-primary, #111827);
  margin-bottom: 1rem;
}

.forbidden-description {
  font-size: 0.95rem;
  color: var(--color-text-secondary, #6b7280);
  line-height: 1.6;
  margin-bottom: 2rem;
}

.forbidden-actions {
  display: flex;
  gap: 0.75rem;
  justify-content: center;
}

.btn {
  padding: 0.625rem 1.25rem;
  border-radius: 0.5rem;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s ease;
  border: none;
}

.btn-primary {
  background: var(--color-brand-primary, #3b82f6);
  color: white;
}

.btn-primary:hover {
  background: var(--color-brand-primary-hover, #2563eb);
}

.btn-secondary {
  background: var(--color-bg-secondary, #f3f4f6);
  color: var(--color-text-primary, #374151);
}

.btn-secondary:hover {
  background: var(--color-bg-tertiary, #e5e7eb);
}
</style>
