/// <reference types="vite/client" />
import { createAppBootstrap } from '@portal/react-bootstrap';
import App from './App';
import { navigateTo, resetRouter, setAppActive } from './router';
import './styles/index.css';

const { mount } = createAppBootstrap({
  name: 'shopping-seller',
  App,
  dataService: 'shopping',
  router: {
    navigateTo,
    resetRouter,
    setAppActive,
  },
});

export { mount };
export const mountSellerApp = mount;
export default { mountSellerApp };
export type { MountOptions, AppInstance as ShoppingSellerAppInstance } from '@portal/react-bootstrap';
