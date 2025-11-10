// portal_shell/src/utils/base64.ts

export function base64UrlEncodeFromString(str: string): string {
  // 문자열 -> UTF-8 바이트
  const bytes: Uint8Array = new TextEncoder().encode(str);

  // bytes -> binary string (btoa)
  let binary = '';
  for (const byte of bytes) {
    binary += String.fromCharCode(byte);
  }

  // btoa => Base64 => Base64URL
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

export function base64UrlDecodeToString(base64Url: string): string {
  // Base64URL => Base64
  let base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');

  // 패딩 보정 - Base64 길이가 4의 배수가 되도록 '=' 추가
  const pad = base64.length % 4;
  if (pad) base64 += '='.repeat(4 - pad);

  // base64 -> binary string
  const binary = atob(base64);

  // binary -> Uint8Array
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++)
    bytes[i] = binary.charCodeAt(i);

  // UTF-8 decode
  return new TextDecoder().decode(bytes);
}