/// <reference types="vite/client" />
import { createAppBootstrap } from '@portal/react-bootstrap';
import App from './App';
import { navigateTo, resetRouter, setAppActive } from './router';
import './styles/index.css';

/**
 * Shopping 앱 부트스트랩
 *
 * @portal/react-bootstrap의 createAppBootstrap을 사용하여
 * 287줄 → 25줄로 단순화됨
 */
const { mount } = createAppBootstrap({
  name: 'shopping',
  App,
  dataService: 'shopping',
  router: {
    navigateTo,
    resetRouter,
    setAppActive,
  },
});

// Module Federation에서 사용하는 mount 함수
export { mount };

// 기존 API 호환성 유지
export const mountShoppingApp = mount;
export default { mountShoppingApp };

// 타입 재export (기존 코드 호환성)
export type { MountOptions, AppInstance as ShoppingAppInstance } from '@portal/react-bootstrap';
