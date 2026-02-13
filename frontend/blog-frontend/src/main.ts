// blog-frontend/src/main.ts

import './style.css';
import { createApp } from 'vue';
import App from './App.vue';
import { createStandaloneBlogRouter } from './router';
import {createPinia} from "pinia";
import { setupErrorHandler } from '@portal/design-system-vue';

/**
 * 앱 모드 감지
 * - Portal Shell에서 로드될 때: Embedded 모드
 * - 직접 브라우저에서 접속할 때: Standalone 모드
 */
const isEmbedded = window.__POWERED_BY_PORTAL_SHELL__ === true;
const mode = isEmbedded ? 'EMBEDDED' : 'STANDALONE';

console.log(`🎯 [Blog] Detected mode: ${mode}`);

if (isEmbedded) {
  // ============================================
  // Embedded 모드: Portal Shell에서 mountBlogApp() 호출 대기
  // ============================================
  console.log('⏳ [Blog] Waiting for Portal Shell to mount...');

  // bootstrap.ts의 mountBlogApp이 export되므로 Portal Shell이 사용 가능

} else {
  // ============================================
  // Standalone 모드: 즉시 마운트
  // ============================================
  console.group('📦 [Blog] Starting in STANDALONE mode');

  const appElement = document.querySelector('#app') as HTMLElement | null;

  if (!appElement) {
    console.error('❌ [Blog] #app element not found!');
    console.groupEnd();
    throw new Error('[Blog] Mount target not found');
  }

  try {
    // ✅ 방법 1: Web History 사용 (권장)
    const app = createApp(App);
    const pinia = createPinia();
    const router = createStandaloneBlogRouter();

    setupErrorHandler(app, { moduleName: 'Blog' });
    app.use(pinia);
    app.use(router);
    app.mount(appElement);

    console.log('✅ [Blog] Mounted successfully');
    console.log(`   URL: ${window.location.href}`);
    console.log(`   Route: ${router.currentRoute.value.path}`);

    // ✅ 방법 2: mountBlogApp 재사용 (대안)
    // mountBlogApp(appElement, {
    //   onNavigate: (path) => {
    //     console.log(`📍 [Standalone] Navigation: ${path}`);
    //   }
    // });

  } catch (err) {
    console.error('❌ [Blog] Mount failed:', err);
  }

  console.groupEnd();
}

// ============================================
// Type Declarations
// ============================================
declare global {
  interface Window {
    /**
     * Portal Shell이 Blog을 로드할 때 설정하는 플래그
     * - true: Embedded 모드
     * - undefined: Standalone 모드
     */
    __POWERED_BY_PORTAL_SHELL__?: boolean;
  }
}