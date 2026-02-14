export const ROLES = {
  SUPER_ADMIN: 'ROLE_SUPER_ADMIN',
  ADMIN: 'ROLE_ADMIN',
  USER: 'ROLE_USER',
  SELLER: 'ROLE_SHOPPING_SELLER',
} as const;

export const ROLE_PREFIX = 'ROLE_';

/**
 * 서비스별 관리자 역할 생성
 * @example serviceAdminRole('BLOG') => 'ROLE_BLOG_ADMIN'
 */
export function serviceAdminRole(service: string): string {
  return `${ROLE_PREFIX}${service}_ADMIN`;
}
