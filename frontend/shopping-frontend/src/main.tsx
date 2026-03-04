// shopping-frontend/src/main.tsx

/**
 * Shopping Frontend Entry Point
 *
 * - Embedded 모드: Portal Shell에서 bootstrap.tsx의 mountShoppingApp() 호출
 * - Standalone 모드: 직접 브라우저에서 접근 시 독립 실행
 */
import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App'
import './styles/index.css'

/**
 * 앱 모드 감지
 * - Portal Shell에서 로드될 때: Embedded 모드
 * - 직접 브라우저에서 접속할 때: Standalone 모드
 */
const isEmbedded = window.__POWERED_BY_PORTAL_SHELL__ === true;


if (isEmbedded) {
  // ============================================
  // Embedded 모드: Portal Shell에서 mountShoppingApp() 호출 대기
  // ============================================

  // bootstrap.tsx의 mountShoppingApp이 export되므로 Portal Shell이 사용 가능

} else {
  // ============================================
  // Standalone 모드: 즉시 마운트
  // ============================================
  console.group('📦 [Shopping] Starting in STANDALONE mode');

  const appElement = document.getElementById('root');

  if (!appElement) {
    console.error('❌ [Shopping] #root element not found!');
    console.groupEnd();
    throw new Error('[Shopping] Mount target not found');
  }

  try {
    // data-service 속성 설정
    document.documentElement.setAttribute('data-service', 'shopping');

    const root = ReactDOM.createRoot(appElement);
    root.render(
      <React.StrictMode>
        <App
          theme="light"
          locale="ko"
          userRole="guest"
          initialPath="/"
        />
      </React.StrictMode>
    );


  } catch (err) {
    console.error('❌ [Shopping] Mount failed:', err);
  }

  console.groupEnd();
}
