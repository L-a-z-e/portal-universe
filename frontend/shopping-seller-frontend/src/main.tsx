import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App'
import './styles/index.css'

const isEmbedded = window.__POWERED_BY_PORTAL_SHELL__ === true;
const mode = isEmbedded ? 'EMBEDDED' : 'STANDALONE';

console.log(`[Seller] Detected mode: ${mode}`);

if (isEmbedded) {
  console.log('[Seller] Waiting for Portal Shell to mount...');
} else {
  console.group('[Seller] Starting in STANDALONE mode');

  const appElement = document.getElementById('root');

  if (!appElement) {
    console.error('[Seller] #root element not found!');
    console.groupEnd();
    throw new Error('[Seller] Mount target not found');
  }

  try {
    document.documentElement.setAttribute('data-service', 'shopping');

    const root = ReactDOM.createRoot(appElement);
    root.render(
      <React.StrictMode>
        <App
          theme="light"
          locale="ko"
          userRole="admin"
          initialPath="/"
        />
      </React.StrictMode>
    );

    console.log('[Seller] Mounted successfully');
  } catch (err) {
    console.error('[Seller] Mount failed:', err);
  }

  console.groupEnd();
}
