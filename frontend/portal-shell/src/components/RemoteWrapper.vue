<script lang="ts">
// -------------------------
// Module-level CSS Registry
// 컴포넌트 인스턴스 간 CSS 추적 정보 공유
// <script setup>은 인스턴스별로 실행되므로, CSS 추적은 module-level에서 관리
// -------------------------
const cssRegistry = new Map<string, HTMLElement[]>();

function getTrackedCss(key: string): HTMLElement[] {
  if (!cssRegistry.has(key)) {
    cssRegistry.set(key, []);
  }
  return cssRegistry.get(key)!;
}
</script>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, onActivated, onDeactivated, watch, nextTick } from "vue";
import { useRoute, useRouter } from "vue-router";
import type { RemoteConfig } from "../config/remoteRegistry";
import { remoteLoader } from "../services/remoteLoader";
import { useThemeStore } from "../store/theme";
import { Spinner, Button, Card } from '@portal/design-vue';
import MaterialIcon from './MaterialIcon.vue';

interface RemoteAppInstance {
  unmount?: () => void;
  onParentNavigate?: (path: string) => void;
  onActivated?: () => void;
  onDeactivated?: () => void;
  onThemeChange?: (theme: string) => void;
}

type MountFn = (container: HTMLElement, options: Record<string, unknown>) => RemoteAppInstance;

// 간단한 debounce 유틸리티 (외부 의존성 없음)
function debounce<T extends (...args: any[]) => void>(fn: T, delay: number): T {
  let timeoutId: ReturnType<typeof setTimeout> | null = null;
  return ((...args: any[]) => {
    if (timeoutId) clearTimeout(timeoutId);
    timeoutId = setTimeout(() => fn(...args), delay);
  }) as T;
}

const props = defineProps<{
  config: RemoteConfig;
  initialPath?: string;
}>();

const container = ref<HTMLElement | null>(null);
const shellRoute = useRoute();
const shellRouter = useRouter();
const themeStore = useThemeStore();

const loading = ref(true);
const error = ref<Error | null>(null);
const isDev = computed(() => import.meta.env.DEV);

let remoteApp: RemoteAppInstance | null = null;
let mountFn: MountFn | null = null;

// -------------------------
// CSS Lifecycle Management
// Remote app의 CSS를 Portal Shell에서 중앙 관리
// module-level cssRegistry를 사용하여 인스턴스 간 공유
//
// MutationObserver는 Remote app이 동적으로 <link>/<style>을 document.head에
// 삽입할 때 이를 추적하기 위해 사용됩니다. 추적된 CSS 요소는 Remote app이
// unmount될 때 함께 정리되어 CSS 누수를 방지합니다.
// -------------------------
let cssObserver: MutationObserver | null = null;

function startCssTracking() {
  const tracked = getTrackedCss(props.config.key);
  cssObserver = new MutationObserver((mutations) => {
    for (const mutation of mutations) {
      for (const node of mutation.addedNodes) {
        if (node instanceof HTMLLinkElement && node.rel === 'stylesheet') {
          tracked.push(node);
        } else if (node instanceof HTMLStyleElement) {
          tracked.push(node);
        }
      }
    }
  });
  cssObserver.observe(document.head, { childList: true });
}

function stopCssTracking() {
  cssObserver?.disconnect();
  cssObserver = null;
}

function disableTrackedCss() {
  const tracked = getTrackedCss(props.config.key);
  for (const el of tracked) {
    if (el instanceof HTMLLinkElement) {
      el.disabled = true;
    } else if (el instanceof HTMLStyleElement) {
      el.setAttribute('media', 'not all');
    }
  }
}

function enableTrackedCss() {
  const tracked = getTrackedCss(props.config.key);
  for (const el of tracked) {
    if (!el.isConnected) continue;
    if (el instanceof HTMLLinkElement) {
      el.disabled = false;
    } else if (el instanceof HTMLStyleElement) {
      el.removeAttribute('media');
    }
  }
}

// -------------------------
// Remote Navigation Sync
// -------------------------
let isNavigating = false;
let lastNavigatedPath = ''; // 🆕 마지막 네비게이션 경로 추적
let isComponentActive = true; // 🆕 keep-alive 활성화 상태 추적

const onRemoteNavigate = (path: string) => {
  const newPath = `${props.config.basePath}${path === '/' ? '' : path}`;
  if (shellRoute.path !== newPath && !isNavigating) {
    isNavigating = true;
    lastNavigatedPath = newPath;
    console.log(`📤 [RemoteWrapper] Remote navigated to: ${path}, updating shell to: ${newPath}`);
    shellRouter.push(newPath)
        .catch((err: unknown) => {
          // NavigationDuplicated 등 Vue Router 내부 에러는 무시 가능
          if (err instanceof Error && !err.message.includes('Avoided redundant navigation')) {
            console.warn('[RemoteWrapper] Navigation error:', err.message);
          }
        })
        .finally(() => {
          setTimeout(() => { isNavigating = false; }, 100);
        });
  }
};

// 🆕 debounce 적용 - 빠른 연속 네비게이션 방지
const debouncedParentNavigate = debounce((remotePath: string) => {
  if (remoteApp?.onParentNavigate) {
    try {
      console.log(`📥 [RemoteWrapper] Shell route changed → ${remotePath}`);
      remoteApp.onParentNavigate(remotePath);
    } catch (err) {
      console.error('⚠️ Error in onParentNavigate:', err);
    }
  }
}, 50);

// ✅ 단일 watch (중복 watch 제거!)
watch(() => shellRoute.path, (newPath, oldPath) => {
  // 🆕 비활성화 상태이거나 현재 경로가 자신의 basePath로 시작하지 않으면 스킵
  if (!isComponentActive || !newPath.startsWith(props.config.basePath)) {
    return;
  }

  if (!isNavigating && newPath !== oldPath) {
    const newRemotePath = newPath.substring(props.config.basePath.length) || '/';
    const oldRemotePath = oldPath ? oldPath.substring(props.config.basePath.length) || '/' : '';

    // 🆕 중복 호출 방지: 이미 같은 경로면 스킵
    if (newRemotePath !== oldRemotePath && newPath !== lastNavigatedPath) {
      debouncedParentNavigate(newRemotePath);
    }
  }
});

// 🆕 keep-alive 훅 연동
onActivated(() => {
  isComponentActive = true; // 🆕 활성화 상태로 변경
  console.log(`🔄 [RemoteWrapper] ${props.config.name} activated (keep-alive)`);
  remoteApp?.onActivated?.();
});

onDeactivated(() => {
  isComponentActive = false; // 🆕 비활성화 상태로 변경
  console.log(`🔄 [RemoteWrapper] ${props.config.name} deactivated (keep-alive)`);
  remoteApp?.onDeactivated?.();
});

// 🆕 테마 변경 감지 및 Remote 앱에 전달
watch(() => themeStore.isDark, (isDark) => {
  if (remoteApp?.onThemeChange && isComponentActive) {
    const newTheme = isDark ? 'dark' : 'light';
    console.log(`🎨 [RemoteWrapper] Theme changed, notifying ${props.config.name}: ${newTheme}`);
    remoteApp.onThemeChange(newTheme);
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

    // CSS 관리: 이전에 tracked된 CSS가 있으면 재활성화
    const tracked = getTrackedCss(props.config.key);
    if (tracked.length > 0) {
      enableTrackedCss();
      console.log(`🎨 [RemoteWrapper] Re-enabled ${tracked.length} tracked CSS elements for ${props.config.name}`);
    }

    // ✅ 저장된 mountFn 사용 (중복 load 없음)
    // 🆕 theme prop 추가 - Portal Shell의 현재 테마 전달
    remoteApp = mountFn(container.value, {
      initialPath,
      onNavigate: onRemoteNavigate,
      theme: themeStore.isDark ? 'dark' : 'light',
    });

    console.log(`✅ [RemoteWrapper] ${props.config.name} mounted successfully`);
    loading.value = false;

  } catch (err: any) {
    stopCssTracking();
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

  // CSS tracking: 모듈 로드 시 주입되는 CSS를 캡처하기 위해 로드 전에 시작
  // module-level registry를 사용하므로 이미 tracked된 CSS가 있으면 observer 생략
  if (getTrackedCss(props.config.key).length === 0) {
    startCssTracking();
  }

  try {
    // Remote 로드 (mountFn 획득) - 이 과정에서 CSS가 <head>에 주입됨
    const result = await remoteLoader.loadRemote(props.config);

    // CSS tracking 종료 (로드 완료 후)
    stopCssTracking();

    if (!result.success || !result.mountFn) {
      throw result.error || new Error('Failed to load remote');
    }

    // ✅ mountFn 저장 (나중에 watch에서 사용)
    mountFn = result.mountFn as MountFn;

    console.log(`🎨 [RemoteWrapper] Tracked ${getTrackedCss(props.config.key).length} CSS elements for ${props.config.name}`);

    // ✅ loading을 false로 변경 → watch가 mountRemote() 호출
    loading.value = false;

  } catch (err: any) {
    stopCssTracking();
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

  // CSS disable (제거하지 않고 비활성화)
  disableTrackedCss();
  stopCssTracking();
  console.log(`🎨 [RemoteWrapper] Disabled ${getTrackedCss(props.config.key).length} CSS elements for ${props.config.name}`);

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
      mountFn = result.mountFn as MountFn;
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
  <div class="remote-wrapper w-full min-h-[400px]">
    <!-- 로딩 -->
    <div v-if="loading" class="flex flex-col items-center justify-center min-h-[400px]">
      <Spinner size="lg" class="mb-4" />
      <p class="text-text-meta">{{ config.name }} 로딩 중...</p>
    </div>

    <!-- 에러 Fallback -->
    <div v-else-if="error" class="max-w-2xl mx-auto my-16 px-4">
      <Card variant="elevated" padding="lg">
        <div class="text-center py-8">
          <MaterialIcon :name="config.icon || 'warning'" :size="64" class="text-text-muted mb-6" />
          <h2 class="text-2xl font-bold text-status-error mb-3">
            {{ config.name }} 서비스를 사용할 수 없습니다
          </h2>
          <p class="text-lg text-text-meta mb-8">
            {{ config.description }}에 일시적으로 연결할 수 없습니다.
          </p>

          <div class="flex flex-wrap gap-3 justify-center mb-8">
            <Button variant="primary" @click="retry">
              다시 시도
            </Button>
            <Button variant="secondary" @click="$router.push('/')">
              홈으로 돌아가기
            </Button>
          </div>

          <details v-if="isDev" class="text-left mt-8 p-4 bg-status-warningBg border border-status-warning/20 rounded-lg text-sm">
            <summary class="cursor-pointer font-bold mb-2 text-status-warning">개발자 정보</summary>
            <div class="space-y-2">
              <p><strong>Remote Key:</strong> {{ config.key }}</p>
              <p><strong>Module Path:</strong> {{ config.module }}</p>
              <p><strong>Error:</strong></p>
              <pre class="bg-bg-elevated p-2 rounded overflow-x-auto text-xs">{{ error.message }}</pre>
            </div>
          </details>
        </div>
      </Card>
    </div>

    <!-- Remote 컨테이너 -->
    <div v-else ref="container" class="w-full"></div>
  </div>
</template>