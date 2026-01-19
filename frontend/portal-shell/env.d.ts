/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_PROFILE: string
  readonly VITE_API_BASE_URL: string
  readonly VITE_BLOG_REMOTE_URL: string
  readonly VITE_SHOPPING_REMOTE_URL: string

  // OIDC
  readonly VITE_OIDC_AUTHORITY: string
  readonly VITE_OIDC_CLIENT_ID: string
  readonly VITE_OIDC_REDIRECT_URI: string
  readonly VITE_OIDC_POST_LOGOUT_REDIRECT_URI: string
  readonly VITE_OIDC_RESPONSE_TYPE: string
  readonly VITE_OIDC_SCOPE: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

// Remote 앱과 공유하는 전역 변수
declare global {
  interface Window {
    __PORTAL_ACCESS_TOKEN__?: string
    __POWERED_BY_PORTAL_SHELL__?: boolean
  }
}
