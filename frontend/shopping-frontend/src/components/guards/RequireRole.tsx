/**
 * RequireRole Guard
 * 특정 권한이 필요한 페이지를 보호합니다.
 */
import React from 'react'
import { Navigate } from 'react-router-dom'
import { useAuthStore } from '@/stores/authStore'

interface RequireRoleProps {
  children: React.ReactNode
  roles: string[]
  redirectTo?: string
}

/**
 * 역할을 정규화합니다.
 * 'admin' -> 'ROLE_ADMIN', 'ADMIN' -> 'ROLE_ADMIN' 등으로 변환
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
  const { user } = useAuthStore()

  // 역할 정규화 (ADMIN -> ROLE_ADMIN, admin -> ROLE_ADMIN)
  const normalizedRoles = roles.map(normalizeRole)
  const userRole = user?.role ? normalizeRole(user.role) : null

  // 사용자 역할 확인
  const hasRequiredRole = userRole && normalizedRoles.includes(userRole)

  if (!hasRequiredRole) {
    console.warn('[RequireRole] User does not have required role:', {
      userRole: user?.role,
      normalizedUserRole: userRole,
      requiredRoles: roles,
      normalizedRoles
    })
    return <Navigate to={redirectTo} replace />
  }

  return <>{children}</>
}

export default RequireRole
