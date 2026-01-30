// portal-shell/src/types/global.d.ts

declare global {
  interface Window {
    /** @deprecated Use __PORTAL_GET_ACCESS_TOKEN__() instead */
    __PORTAL_ACCESS_TOKEN__?: string;
    __PORTAL_GET_ACCESS_TOKEN__?: () => string | null;
    __PORTAL_ON_AUTH_ERROR__?: () => void;
    __PORTAL_API_CLIENT__?: unknown;
  }
}

export {};
