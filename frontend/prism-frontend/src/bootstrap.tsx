/// <reference types="vite/client" />
import { createAppBootstrap } from '@portal/react-bootstrap';
import App from './App';
import { navigateTo, resetRouter, setAppActive } from './router';
import './index.css';

/**
 * Prism 앱 부트스트랩
 *
 * @portal/react-bootstrap의 createAppBootstrap을 사용하여
 * 235줄 → 25줄로 단순화됨
 */
const { mount } = createAppBootstrap({
  name: 'prism',
  App,
  dataService: 'prism',
  router: {
    navigateTo,
    resetRouter,
    setAppActive,
  },
});

// Module Federation에서 사용하는 mount 함수
export { mount };

// 기존 API 호환성 유지
export const mountPrismApp = mount;
export default { mountPrismApp };

// 타입 재export (기존 코드 호환성)
export type { MountOptions, AppInstance as PrismAppInstance } from '@portal/react-bootstrap';
