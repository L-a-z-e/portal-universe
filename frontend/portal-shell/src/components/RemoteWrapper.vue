<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import type { Router } from "vue-router";
import type { MountOptions } from "blog_remote/bootstrap";

type RemoteApp = {
  router: Router;
  onParentNavigate: (path: string) => void;
  unmount: () => void;
};

const props = defineProps<{
  mountFn: ((el: HTMLElement, options: MountOptions) => RemoteApp) | null; // null 허용
  basePath: string;
  initialPath?: string;
  remoteName?: string;
}>();

const container = ref<HTMLElement | null>(null);
const shellRoute = useRoute();
const shellRouter = useRouter();
const error = ref<Error | null>(null);
const loading = ref(true);

let remoteApp: RemoteApp | null = null;

const onRemoteNavigate = (path: string) => {
  const newPath = `${props.basePath}${path === '/' ? '' : path}`;
  if (shellRoute.path !== newPath) {
    shellRouter.push(newPath);
  }
};

watch(() => shellRoute.path, (newPath) => {
  if (remoteApp) {
    try {
      const remotePath = newPath.substring(props.basePath.length) || '/';
      remoteApp.onParentNavigate(remotePath);
    } catch (err) {
      console.error('⚠️ Error in onParentNavigate:', err);
    }
  }
}, { immediate: true });

onMounted(async () => {
  if (!container.value) return;

  try {
    loading.value = true;
    error.value = null;

    // mountFn이 null이면 에러
    if (!props.mountFn) {
      throw new Error('Remote module not available');
    }

    const initialPath = props.initialPath || shellRoute.path.substring(props.basePath.length) || '/';

    remoteApp = props.mountFn(container.value, {
      initialPath,
      onNavigate: onRemoteNavigate,
    });

    loading.value = false;

  } catch (err) {
    console.error('❌ Failed to mount remote:', err);
    error.value = err as Error;
    loading.value = false;
  }
});

onUnmounted(() => {
  if (remoteApp) {
    try {
      remoteApp.unmount();
      remoteApp = null;
    } catch (err) {
      console.error('⚠️ Error during unmount:', err);
    }
  }
});
</script>

<template>
  <div class="remote-wrapper">
    <!-- 로딩 -->
    <div v-if="loading" class="loading">
      <div class="spinner"></div>
      <p>Loading {{ remoteName || 'Module' }}...</p>
    </div>

    <!-- 에러 (Fallback) -->
    <div v-else-if="error || !mountFn" class="error-fallback">
      <div class="error-icon">⚠️</div>
      <h2>{{ remoteName || 'Service' }} 를 사용할 수 없습니다</h2>
      <p class="error-message">
        {{ remoteName || '서비스' }}에 연결할 수 없습니다.
      </p>
      <p class="error-hint">
        잠시 후 다시 시도해주세요.
      </p>
      <div class="error-actions">
        <button @click="$router.push('/')" class="btn-primary">
          홈으로 돌아가기
        </button>
        <button @click="$router.go(0)" class="btn-secondary">
          새로고침
        </button>
      </div>
      <details v-if="error" class="error-details">
        <summary>Error Message</summary>
        <pre>{{ error.message }}</pre>
      </details>
    </div>

    <!-- Remote 컨테이너 (정상) -->
    <div v-else ref="container" class="remote-container"></div>
  </div>
</template>

<style scoped>
.remote-wrapper {
  width: 100%;
  min-height: 400px;
}

/* 로딩 */
.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  color: #666;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #1976d2;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 1rem;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 에러 Fallback */
.error-fallback {
  max-width: 600px;
  margin: 4rem auto;
  padding: 2rem;
  text-align: center;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.error-icon {
  font-size: 4rem;
  margin-bottom: 1rem;
}

.error-fallback h2 {
  color: #d32f2f;
  margin-bottom: 1rem;
}

.error-message {
  font-size: 1.1rem;
  color: #666;
  margin-bottom: 0.5rem;
}

.error-hint {
  color: #999;
  margin-bottom: 2rem;
}

.error-actions {
  display: flex;
  gap: 1rem;
  justify-content: center;
  margin-bottom: 2rem;
}

.btn-primary,
.btn-secondary {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 4px;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-primary {
  background: #1976d2;
  color: white;
}

.btn-primary:hover {
  background: #1565c0;
}

.btn-secondary {
  background: #f5f5f5;
  color: #666;
}

.btn-secondary:hover {
  background: #e0e0e0;
}

.error-details {
  text-align: left;
  margin-top: 2rem;
  padding: 1rem;
  background: #f5f5f5;
  border-radius: 4px;
  font-size: 0.9rem;
}

.error-details summary {
  cursor: pointer;
  color: #666;
  margin-bottom: 0.5rem;
}

.error-details pre {
  margin: 0;
  color: #d32f2f;
  white-space: pre-wrap;
  word-wrap: break-word;
}

/* Remote 컨테이너 */
.remote-container {
  width: 100%;
}
</style>