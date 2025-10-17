/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_PROFILE: string
  readonly VITE_API_BASE_URL: string
  readonly VITE_BLOG_REMOTE_URL: string
  readonly VITE_SHOP_REMOTE_URL: string

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
