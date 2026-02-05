/**
 * @portal/react-bootstrap
 *
 * React 마이크로프론트엔드 앱의 공통 부트스트랩 로직을 제공합니다.
 *
 * @example
 * ```tsx
 * import { createAppBootstrap } from '@portal/react-bootstrap';
 * import App from './App';
 *
 * export const { mount } = createAppBootstrap({
 *   name: 'shopping',
 *   App,
 *   dataService: 'shopping',
 * });
 * ```
 */

export { createAppBootstrap } from './createAppBootstrap';
export type {
  AppBootstrapConfig,
  MountOptions,
  AppInstance,
  AppInstanceState,
  Theme,
} from './types';
