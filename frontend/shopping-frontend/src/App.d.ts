import './App.scss';
/**
 * App Props 인터페이스
 * Portal Shell(Host)에서 전달받는 Props
 */
interface AppProps {
    /** 테마 설정 */
    theme?: 'light' | 'dark';
    /** 언어/로케일 설정 */
    locale?: string;
    /** 사용자 역할 */
    userRole?: 'guest' | 'user' | 'admin';
    /** 기타 Props */
    [key: string]: any;
}
/**
 * Shopping Frontend 루트 컴포넌트
 *
 * 특징:
 * - Portal Shell과 Props 기반으로 통신
 * - data-service="shopping" CSS 활성화
 * - data-theme 속성으로 테마 동기화
 * - Portal Shell의 themeStore와 연동 (Embedded 모드)
 */
declare function App({ theme, locale, userRole, ...otherProps }: AppProps): import("react/jsx-runtime").JSX.Element;
export default App;
//# sourceMappingURL=App.d.ts.map