// portal-shell/src/types/global.d.ts

declare global {
  interface Window {
    __PORTAL_ACCESS_TOKEN__?: string;
  }
}

export {};
