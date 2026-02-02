/**
 * RequireRole Guard
 * 특정 권한이 필요한 페이지를 보호합니다.
 *
 * RBAC 역할 계층:
 * - ROLE_SUPER_ADMIN: 전체 시스템 관리자
 * - ROLE_SHOPPING_ADMIN: 쇼핑 서비스 관리자
 * - ROLE_BLOG_ADMIN: 블로그 서비스 관리자
 * - ROLE_SELLER: 판매자
 * - ROLE_USER: 일반 사용자
 */
import React from 'react'
import { Navigate } from 'react-router-dom'
import { usePortalAuth } from '@portal/react-bridge'

interface RequireRoleProps {
  children: React.ReactNode
  roles: string[]
  redirectTo?: string
}

/**
 * 역할을 정규화합니다.
 * 'admin' -> 'ROLE_ADMIN', 'SHOPPING_ADMIN' -> 'ROLE_SHOPPING_ADMIN' 등으로 변환
 */
const normalizeRole = (role: string): string => {
  const upperRole = role.toUpperCase()
  return upperRole.startsWith('ROLE_') ? upperRole : `ROLE_${upperRole}`
}

export const RequireRole: React.FC<RequireRoleProps> = ({
  children,
  roles,
  redirectTo = '/403'
}) => {
  const { roles: userRolesRaw } = usePortalAuth()

  // 필요한 역할 정규화
  const normalizedRequiredRoles = roles.map(normalizeRole)

  // 사용자 역할 정규화
  const userRoles = (userRolesRaw || []).map(normalizeRole)

  // 사용자가 필요한 역할 중 하나 이상을 보유하는지 확인
  const hasRequiredRole = normalizedRequiredRoles.some(required =>
    userRoles.includes(required)
  )

  if (!hasRequiredRole) {
    console.warn('[RequireRole] User does not have required role:', {
      userRoles: userRolesRaw,
      requiredRoles: roles,
      normalizedRequired: normalizedRequiredRoles
    })
    return <Navigate to={redirectTo} replace />
  }

  return <>{children}</>
}

export default RequireRole
