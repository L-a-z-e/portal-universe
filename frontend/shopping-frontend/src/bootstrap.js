import { jsx as _jsx } from "react/jsx-runtime";
/// <reference types="vite/client" />
import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import './styles/index.scss';
// 앱 인스턴스 관리
let root = null;
let currentProps = {};
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
export function mount(el, options = {}) {
    console.group('🚀 [Shopping] Mounting app in EMBEDDED mode');
    // ✅ 필수 파라미터 검증 (Blog의 패턴 따름)
    const container = typeof el === 'string'
        ? document.getElementById(el)
        : el;
    if (!container) {
        console.error('❌ [Shopping] Mount element is null!');
        console.groupEnd();
        throw new Error('[Shopping] Mount element is required');
    }
    console.log('📍 Mount target:', container.tagName, container.className || '(no class)');
    const { initialProps = {}, onUpdate } = options;
    console.log('📍 Initial Props:', initialProps);
    console.log('📍 Options:', { onUpdate });
    try {
        // ✅ Step 1: React 루트 생성
        root = ReactDOM.createRoot(container);
        currentProps = { ...initialProps };
        // ✅ Step 2: data-service="shopping" 속성 설정 (CSS 선택자 활성화)
        document.documentElement.setAttribute('data-service', 'shopping');
        console.log('[Shopping] Set data-service="shopping"');
        // ✅ Step 3: 초기 Props로 렌더링
        root.render(_jsx(React.StrictMode, { children: _jsx(App, { ...currentProps }) }));
        console.log('✅ [Shopping] App mounted successfully');
        console.groupEnd();
        // ✅ Step 4: 앱 인스턴스 반환 (Blog의 BlogAppInstance 패턴)
        return {
            /**
             * Parent(Portal Shell)로부터 Props 변경 수신
             * Blog의 onParentNavigate와 동일한 역할
             */
            onParentUpdate: (newProps) => {
                console.log(`📥 [Shopping] Received props from parent:`, newProps);
                if (!root) {
                    console.error('❌ [Shopping] Root is null');
                    return;
                }
                // Props 머지 (기존 + 새로운 것)
                currentProps = { ...currentProps, ...newProps };
                // React의 Diff 알고리즘으로 필요한 부분만 업데이트
                root.render(_jsx(React.StrictMode, { children: _jsx(App, { ...currentProps }) }));
                console.log('✅ [Shopping] Props updated and re-rendered');
            },
            /**
             * 앱 언마운트 및 클린업
             * Blog의 unmount와 동일한 역할
             *
             * 🔴 핵심: <head>의 Shopping CSS 스타일 태그 제거!
             */
            unmount: () => {
                console.group('🔄 [Shopping] Unmounting app');
                try {
                    if (root) {
                        root.unmount();
                        root = null;
                    }
                    container.innerHTML = '';
                    console.log('✅ [Shopping] App unmounted successfully');
                    // 🟢 Step 1: <head>의 모든 <style> 태그 중 Shopping CSS 제거
                    const styleTags = document.querySelectorAll('style');
                    console.log(`🔍 [Shopping] Found ${styleTags.length} <style> tags, searching for Shopping CSS...`);
                    styleTags.forEach((styleTag, index) => {
                        const content = styleTag.textContent || '';
                        // Shopping 관련 CSS 마커 확인
                        if (content.includes('[data-service="shopping"]') ||
                            content.includes('shopping-') ||
                            content.includes('@import') && content.includes('shopping')) {
                            console.log(`   📍 [Shopping] Found Shopping CSS at index ${index}, removing...`);
                            styleTag.remove();
                        }
                    });
                    // 🟢 Step 2: <link> 태그 중 Shopping CSS 제거 (있는 경우)
                    const linkTags = document.querySelectorAll('link[rel="stylesheet"]');
                    linkTags.forEach((linkTag) => {
                        const href = linkTag.getAttribute('href') || '';
                        if (href.includes('shopping') || href.includes('shopping-frontend')) {
                            console.log(`   📍 [Shopping] Found Shopping CSS link: ${href}, removing...`);
                            linkTag.remove();
                        }
                    });
                    // 🟢 Step 3: data-service 속성 정리
                    if (document.documentElement.getAttribute('data-service') === 'shopping') {
                        console.log('   📍 [Shopping] Resetting data-service attribute...');
                        document.documentElement.removeAttribute('data-service');
                    }
                    // Props 초기화
                    currentProps = {};
                    console.log('✅ [Shopping] Cleanup completed - CSS removed from <head>');
                }
                catch (err) {
                    console.error('❌ [Shopping] Unmount failed:', err);
                }
                console.groupEnd();
            }
        };
    }
    catch (error) {
        console.error('❌ [Shopping] Mount failed:', error);
        console.groupEnd();
        throw error;
    }
}
/**
 * 개발 환경에서 직접 실행될 때 (Host 없이)
 * Blog의 standalone 모드와 동일
 */
if (import.meta.env.DEV && !window.__FEDERATION__) {
    const container = document.getElementById('root');
    if (container) {
        console.log('🔧 [Shopping] Dev mode - mounting directly');
        mount(container, {
            initialProps: {
                theme: 'light',
                locale: 'ko',
                userRole: 'guest'
            }
        });
    }
}
// 호환성을 위한 기본 export
export default { mount };
//# sourceMappingURL=bootstrap.js.map