import './styles/index.scss';
/**
 * Mount 옵션
 */
export type MountOptions = {
    /** 초기 Props */
    initialProps?: Record<string, any>;
    /** Parent에게 상태 변경 알림 (필요 시) */
    onUpdate?: (data: any) => void;
};
/**
 * Mount된 Shopping 앱 인스턴스
 */
export type ShoppingAppInstance = {
    /** Parent로부터 Props 변경 수신 */
    onParentUpdate: (newProps: any) => void;
    /** 앱 언마운트 */
    unmount: () => void;
};
/**
 * Shopping 앱을 지정된 컨테이너에 마운트 (Embedded 모드)
 *
 * @param el - 마운트할 HTML 엘리먼트
 * @param options - 마운트 옵션
 * @returns Shopping 앱 인스턴스
 *
 * @example
 * ```
 * const shoppingApp = mount(container, {
 *   initialProps: {
 *     theme: 'light',
 *     locale: 'ko',
 *     userRole: 'user'
 *   },
 *   onUpdate: (data) => console.log('Shopping updated:', data)
 * });
 * ```
 */
export declare function mount(el: HTMLElement | string, options?: MountOptions): ShoppingAppInstance;
export interface MountAPI {
    onParentUpdate: (newProps: any) => void;
    unmount: () => void;
}
declare const _default: {
    mount: typeof mount;
};
export default _default;
//# sourceMappingURL=bootstrap.d.ts.map