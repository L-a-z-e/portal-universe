<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from "vue";
import { useRoute, useRouter } from "vue-router";
import type { RemoteConfig } from "../config/remoteRegistry";
import { remoteLoader } from "../services/remoteLoader";

const props = defineProps<{
  config: RemoteConfig;
  initialPath?: string;
}>();

const container = ref<HTMLElement | null>(null);
const shellRoute = useRoute();
const shellRouter = useRouter();

const loading = ref(true);
const error = ref<Error | null>(null);
const isDev = computed(() => import.meta.env.DEV);

let remoteApp: any = null;
let mountFn: any = null; // ✅ load 결과 저장 (중복 load 방지)

// -------------------------
// Remote Navigation Sync
// -------------------------
const onRemoteNavigate = (path: string) => {
  const newPath = `${props.config.basePath}${path === '/' ? '' : path}`;
  if (shellRoute.path !== newPath) {
    shellRouter.push(newPath).catch(() => {});
  }
};

// -------------------------
// Parent → Child route sync
// -------------------------
watch(() => shellRoute.path, (newPath) => {
  if (remoteApp?.onParentNavigate) {
    try {
      const remotePath = newPath.substring(props.config.basePath.length) || '/';
      remoteApp.onParentNavigate(remotePath);
    } catch (err) {
      console.error('⚠️ Error in onParentNavigate:', err);
    }
  }
});

// -------------------------
// ✅ Mount 로직 (저장된 mountFn 사용)
// -------------------------
async function mountRemote() {
  // Container 준비 확인
  if (!container.value) {
    console.warn('⚠️ [RemoteWrapper] Container not ready, waiting...');
    await nextTick();

    if (!container.value) {
      console.error('❌ [RemoteWrapper] Container still null after nextTick!');
      return;
    }
  }

  // mountFn 확인
  if (!mountFn) {
    console.error('❌ [RemoteWrapper] mountFn not available!');
    error.value = new Error('Mount function not loaded');
    loading.value = false;
    return;
  }

  console.log(`📍 [RemoteWrapper] Mounting ${props.config.name}...`);

  try {
    const initialPath = props.initialPath ||
        shellRoute.path.substring(props.config.basePath.length) || '/';

    console.log(`🚀 [RemoteWrapper] Calling mount function...`);
    console.log(`   Container:`, container.value);
    console.log(`   Initial path: ${initialPath}`);

    // ✅ 저장된 mountFn 사용 (중복 load 없음)
    remoteApp = mountFn(container.value, {
      initialPath,
      onNavigate: onRemoteNavigate,
    });

    console.log(`✅ [RemoteWrapper] ${props.config.name} mounted successfully`);
    loading.value = false;

  } catch (err: any) {
    console.error(`❌ [RemoteWrapper] Mount failed:`, err);
    error.value = err;
    loading.value = false;
  }
}

// -------------------------
// ✅ loading이 false가 되면 mount
// -------------------------
watch(loading, async (isLoading, wasLoading) => {
  // loading이 true → false로 변경되고, 에러가 없을 때
  if (wasLoading && !isLoading && !error.value) {
    await nextTick();  // DOM 렌더링 완료 대기
    await mountRemote();
  }
});

// -------------------------
// ✅ 초기 로드 (mountFn만 가져오기)
// -------------------------
onMounted(async () => {
  console.log(`📍 [RemoteWrapper] Component mounted for ${props.config.name}`);

  try {
    // Remote 로드 (mountFn 획득)
    const result = await remoteLoader.loadRemote(props.config);

    if (!result.success || !result.mountFn) {
      throw result.error || new Error('Failed to load remote');
    }

    // ✅ mountFn 저장 (나중에 watch에서 사용)
    mountFn = result.mountFn;

    // ✅ loading을 false로 변경 → watch가 mountRemote() 호출
    loading.value = false;

  } catch (err: any) {
    console.error(`❌ [RemoteWrapper] Load failed:`, err);
    error.value = err;
    loading.value = false;
  }
});

// -------------------------
// ✅ Cleanup
// -------------------------
onUnmounted(() => {
  if (remoteApp?.unmount) {
    try {
      console.log(`🔄 [RemoteWrapper] Unmounting ${props.config.name}`);
      remoteApp.unmount();
    } catch (err) {
      console.error('⚠️ Error during unmount:', err);
    }
  }

  remoteApp = null;
  mountFn = null;

  if (container.value) {
    container.value.innerHTML = '';
  }
});

// -------------------------
// ✅ Retry
// -------------------------
async function retry() {
  console.log(`🔄 [RemoteWrapper] Retrying ${props.config.name}...`);

  // 기존 앱 정리
  if (remoteApp?.unmount) {
    try {
      remoteApp.unmount();
    } catch (err) {
      console.error('⚠️ Cleanup error:', err);
    }
  }

  remoteApp = null;
  mountFn = null;

  if (container.value) {
    container.value.innerHTML = '';
  }

  remoteLoader.clearCache(props.config.key);
  loading.value = true;
  error.value = null;

  // onMounted 로직 재실행
  try {
    const result = await remoteLoader.loadRemote(props.config);

    if (result.success && result.mountFn) {
      mountFn = result.mountFn;
      loading.value = false;  // watch가 mountRemote() 호출
    } else {
      error.value = result.error;
      loading.value = false;
    }
  } catch (err: any) {
    error.value = err;
    loading.value = false;
  }
}
</script>

<template>
  <div class="remote-wrapper">
    <!-- 로딩 -->
    <div v-if="loading" class="loading">
      <div class="spinner"></div>
      <p>{{ config.name }} 로딩 중...</p>
    </div>

    <!-- 에러 Fallback -->
    <div v-else-if="error" class="error-fallback">
      <div class="error-icon">{{ config.icon || '⚠️' }}</div>
      <h2>{{ config.name }} 서비스를 사용할 수 없습니다</h2>
      <p class="error-message">
        {{ config.description }}에 일시적으로 연결할 수 없습니다.
      </p>

      <div class="error-actions">
        <button @click="retry" class="btn-primary">
          다시 시도
        </button>
        <button @click="$router.push('/')" class="btn-secondary">
          홈으로 돌아가기
        </button>
      </div>

      <details v-if="isDev" class="error-details">
        <summary>개발자 정보</summary>
        <div>
          <p><strong>Remote Key:</strong> {{ config.key }}</p>
          <p><strong>Module Path:</strong> {{ config.module }}</p>
          <p><strong>Error:</strong></p>
          <pre>{{ error.message }}</pre>
        </div>
      </details>
    </div>

    <!-- Remote 컨테이너 -->
    <div v-else ref="container" class="remote-container"></div>
  </div>
</template>

<style scoped>
/* 기존 스타일 그대로 */
.remote-wrapper {
  width: 100%;
  min-height: 400px;
}

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
  background: #fff3cd;
  border: 1px solid #ffc107;
  border-radius: 4px;
  font-size: 0.9rem;
}

.error-details summary {
  cursor: pointer;
  font-weight: bold;
  margin-bottom: 0.5rem;
}

.error-details pre {
  background: #f5f5f5;
  padding: 0.5rem;
  border-radius: 4px;
  overflow-x: auto;
  font-size: 0.85rem;
}

.remote-container {
  width: 100%;
}
</style>