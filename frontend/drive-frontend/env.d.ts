interface ImportMetaEnv {
  readonly VITE_PROFILE: string
  readonly VITE_API_BASE_URL: string;
  readonly VITE_PORTAL_SHELL_REMOTE_URL: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
