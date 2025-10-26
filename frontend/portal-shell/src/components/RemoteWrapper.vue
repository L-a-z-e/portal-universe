<script setup lang="ts">
/**
 * @file RemoteWrapper.vue
 * @description 마이크로 프론트엔드(Remote) 앱을 동적으로 로드하고 마운트하는 래퍼(Wrapper) 컴포넌트입니다.
 * 로딩, 에러 상태에 대한 UI(Fallback)를 제공하며, 셸과 Remote 앱 간의 라우팅을 동기화합니다.
 */
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from "vue";
import { useRoute, useRouter } from "vue-router";
import type { RemoteConfig } from "../config/remoteRegistry";
import { remoteLoader } from "../services/remoteLoader";

/**
 * @property {RemoteConfig} config - 로드할 Remote 앱의 설정 객체. (remoteRegistry.ts 참고)
 * @property {string} [initialPath] - Remote 앱에 전달할 초기 경로.
 */
const props = defineProps<{
  config: RemoteConfig;
  initialPath?: string;
}>();

const container = ref<HTMLElement | null>(null); // Remote 앱이 마운트될 DOM 컨테이너
const shellRoute = useRoute();
const shellRouter = useRouter();

const loading = ref(true); // 로딩 상태
const error = ref<Error | null>(null); // 에러 상태
const isDev = computed(() => import.meta.env.DEV); // 개발 모드 여부

let remoteApp: any = null; // 마운트된 Remote 앱 인스턴스

/**
 * Remote 앱 내부에서 라우팅이 변경되었을 때 호출되는 콜백 함수입니다.
 * Remote의 경로 변경을 셸의 경로에 반영합니다.
 * @param path Remote 앱 내부의 새 경로 (예: '/post/123')
 */
const onRemoteNavigate = (path: string) => {
  const newPath = `${props.config.basePath}${path === '/' ? '' : path}`;
  if (shellRoute.path !== newPath) {
    shellRouter.push(newPath).catch(() => {});
  }
};

/**
 * 셸 라우터의 경로 변경을 감지하여 Remote 앱에 전파합니다.
 */
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

/**
 * Remote 앱을 컨테이너에 마운트하는 핵심 함수입니다.
 */
async function mountRemote() {
  if (!container.value) {
    console.error('❌ [RemoteWrapper] Container element is not available to mount.');
    return;
  }

  console.log(`📍 [RemoteWrapper] Mounting ${props.config.name}...`);

  try {
    // remoteLoader를 통해 마운트 함수를 가져옵니다.
    const result = await remoteLoader.loadRemote(props.config);

    if (!result.success || !result.mountFn) {
      throw result.error || new Error('Failed to load remote');
    }

    // Remote 앱에 전달할 초기 경로를 계산합니다.
    const initialPath = props.initialPath ||
        shellRoute.path.substring(props.config.basePath.length) || '/';

    // 마운트 함수를 호출하여 Remote 앱을 DOM에 연결합니다.
    remoteApp = result.mountFn(container.value, {
      initialPath,
      onNavigate: onRemoteNavigate,
    });

    console.log(`✅ [RemoteWrapper] ${props.config.name} mounted successfully`);

  } catch (err: any) {
    console.error(`❌ [RemoteWrapper] Mount failed:`, err);
    error.value = err;
  }
}

// 컴포넌트가 마운트되면 Remote 앱 로딩을 시작합니다.
onMounted(async () => {
  console.log(`📍 [RemoteWrapper] Component mounted for ${props.config.name}`);
  try {
    // 마운트 함수를 미리 로드만 해둡니다.
    const result = await remoteLoader.loadRemote(props.config);
    if (!result.success || !result.mountFn) {
      throw result.error || new Error('Failed to load remote');
    }
    // 로딩이 성공하면 loading 상태를 false로 변경합니다.
    loading.value = false;
  } catch (err: any) { 
    console.error(`❌ [RemoteWrapper] Load failed:`, err);
    error.value = err;
    loading.value = false;
  }
});

// loading 상태가 true -> false로 변경되면 (즉, DOM이 준비되면) 마운트를 실행합니다.
watch(loading, async (isLoading, wasLoading) => {
  if (wasLoading && !isLoading && !error.value) {
    await nextTick(); // DOM 렌더링이 완료될 때까지 대기
    await mountRemote();
  }
});

// 컴포넌트가 언마운트될 때 Remote 앱도 함께 언마운트하여 메모리 누수를 방지합니다.
onUnmounted(() => {
  if (remoteApp?.unmount) {
    try {
      console.log(`🔄 [RemoteWrapper] Unmounting ${props.config.name}`);
      remoteApp.unmount();
      remoteApp = null;
    } catch (err) {
      console.error('⚠️ Error during unmount:', err);
    }
  }
});

/**
 * Remote 앱 로딩 실패 시, 재시도를 위한 함수입니다.
 */
async function retry() {
  console.log(`🔄 [RemoteWrapper] Retrying ${props.config.name}...`);
  remoteLoader.clearCache(props.config.key);

  error.value = null;
  loading.value = true; // 로딩 상태로 전환

  // onMounted 로직과 유사하게 재시도
  await onMounted();
}
</script>

<template>
  <div class="remote-wrapper">
    <!-- 로딩 상태 UI -->
    <div v-if="loading" class="loading">
      <div class="spinner"></div>
      <p>{{ config.name }} 로딩 중...</p>
    </div>

    <!-- 에러 발생 시 Fallback UI -->
    <div v-else-if="error" class="error-fallback">
      <div class="error-icon">{{ config.icon || '⚠️' }}</div>
      <h2>{{ config.name }} 서비스를 사용할 수 없습니다</h2>
      <p class="error-message">
        {{ config.description }}에 일시적으로 연결할 수 없습니다.
      </p>

      <div class="error-actions">
        <button @click="retry" class="btn-primary">다시 시도</button>
        <button @click="$router.push('/')" class="btn-secondary">홈으로 돌아가기</button>
      </div>

      <!-- 개발 모드에서만 에러 상세 정보 표시 -->
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

    <!-- Remote 앱이 마운트될 컨테이너 -->
    <div v-else ref="container" class="remote-container"></div>
  </div>
</template>

<style scoped>
/* ... 스타일은 변경 없음 ... */
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
