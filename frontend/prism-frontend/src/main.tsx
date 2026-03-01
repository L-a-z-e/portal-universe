import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import './index.css';

/**
 * Prism Frontend Entry Point
 *
 * - Embedded 모드: Portal Shell에서 bootstrap.tsx의 mountPrismApp() 호출
 * - Standalone 모드: 직접 브라우저에서 접근 시 독립 실행
 */
const isEmbedded = window.__POWERED_BY_PORTAL_SHELL__ === true;
const mode = isEmbedded ? 'EMBEDDED' : 'STANDALONE';


if (isEmbedded) {
} else {
  console.group('📦 [Prism] Starting in STANDALONE mode');

  const appElement = document.getElementById('root');

  if (!appElement) {
    console.error('❌ [Prism] #root element not found!');
    console.groupEnd();
    throw new Error('[Prism] Mount target not found');
  }

  try {
    document.documentElement.setAttribute('data-service', 'prism');

    const root = ReactDOM.createRoot(appElement);
    root.render(
      <React.StrictMode>
        <App theme="light" locale="ko" userRole="guest" initialPath="/" />
      </React.StrictMode>
    );

  } catch (err) {
    console.error('❌ [Prism] Mount failed:', err);
  }

  console.groupEnd();
}
