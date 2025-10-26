import {base64UrlDecodeToString} from "./base64.ts";

/**
 * JWT 페이로드의 타입을 정의합니다. 실제 내용은 동적이므로 any 타입을 가진 레코드로 정의합니다.
 */
export type JwtPayload = Record<string, any>;

/**
 * JWT 문자열을 파싱하여 페이로드(payload) 객체를 반환합니다.
 * 서명(signature)을 검증하지는 않고, 단순히 페이로드 내용을 디코딩하는 역할만 합니다.
 * @param token JWT access token 문자열
 * @returns {JwtPayload | null} 파싱된 페이로드 객체. 파싱 실패 시 null을 반환합니다.
 */
export function parseJwtPayload(token: string): JwtPayload | null {
  try {
    // JWT는 '.'으로 구분된 세 부분(header, payload, signature)으로 구성됩니다.
    const parts = token.split('.');

    if (parts.length < 2) return null;

    // 두 번째 부분이 페이로드입니다.
    const payloadBase64Url = parts[1];

    if(!payloadBase64Url) return null;

    // Base64Url로 인코딩된 페이로드를 디코딩하여 JSON 문자열을 얻습니다.
    const json = base64UrlDecodeToString(payloadBase64Url);

    // JSON 문자열을 객체로 파싱합니다.
    return JSON.parse(json) as JwtPayload;
  } catch (e) {
    console.error('Invalid JWT token', e);
    return null;
  }
}