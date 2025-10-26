import './style.css';
import { createApp } from 'vue';
import App from './App.vue';
import { createStandaloneBlogRouter } from './router';

/**
 * @file main.ts
 * @description Blog Frontend 애플리케이션의 메인 진입점입니다.
 * 이 파일은 앱이 셸에 포함된 'Embedded' 모드인지, 단독 실행되는 'Standalone' 모드인지 감지하여
 * 그에 맞는 초기화 로직을 수행합니다.
 */

// Portal Shell이 이 앱을 로드할 때 window 객체에 __POWERED_BY_PORTAL_SHELL__ 플래그를 설정합니다.
const isEmbedded = window.__POWERED_BY_PORTAL_SHELL__ === true;
const mode = isEmbedded ? 'EMBEDDED' : 'STANDALONE';

console.log(`🎯 [Blog] Detected mode: ${mode}`);

if (isEmbedded) {
  // ===================================================================
  // Embedded Mode (셸에 포함된 경우)
  // ===================================================================
  // 이 경우에는 아무것도 하지 않습니다.
  // 실제 앱의 마운트(mount)는 Portal Shell이 `bootstrap.ts`에 있는 `mountBlogApp` 함수를
  // 동적으로 임포트하여 직접 호출함으로써 제어권을 갖게 됩니다.
  console.log('⏳ [Blog] Waiting for Portal Shell to mount...');

} else {
  // ===================================================================
  // Standalone Mode (단독 실행된 경우)
  // ===================================================================
  console.group('📦 [Blog] Starting in STANDALONE mode');

  const appElement = document.querySelector('#app') as HTMLElement | null;

  if (!appElement) {
    console.error('❌ [Blog] #app element not found!');
    console.groupEnd();
    throw new Error('[Blog] Mount target not found');
  }

  try {
    // 단독 실행에 적합한 Web History 기반의 라우터를 생성합니다.
    const app = createApp(App);
    const router = createStandaloneBlogRouter();

    app.use(router);
    app.mount(appElement);

    console.log('✅ [Blog] Mounted successfully in standalone mode');

  } catch (err) {
    console.error('❌ [Blog] Standalone mount failed:', err);
  }

  console.groupEnd();
}

// ===================================================================
// Type Declarations for Module Federation
// ===================================================================
declare global {
  interface Window {
    /**
     * Portal Shell이 이 앱을 로드할 때 주입하는 전역 플래그입니다.
     * 이 플래그의 존재 여부로 Embedded/Standalone 모드를 구분합니다.
     */
    __POWERED_BY_PORTAL_SHELL__?: boolean;
  }
}
