// portal-shell/src/utils/jwt.ts

import {base64UrlDecodeToString} from "./base64.ts";

export type JwtPayload = Record<string, any>;

/**
 * JWT 문자열을 파싱하여 페이로드 객체를 반환합니다.
 * 파싱에 실패하면 null을 반환합니다.
 * @param token JWT 문자열
 * @returns JwtPayload | null
 */
export function parseJwtPayload(token: string): JwtPayload | null {
  try {
    const parts = token.split('.');

    if (parts.length < 2) return null;

    const payloadBase64Url = parts[1];

    if(!payloadBase64Url) return null;

    const json = base64UrlDecodeToString(payloadBase64Url);

    return JSON.parse(json) as JwtPayload;
  } catch (e) {
    console.error('Invalid JWT token', e);
    return null;
  }
}
