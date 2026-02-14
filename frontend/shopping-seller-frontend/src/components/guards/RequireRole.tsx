import React from 'react'
import { Navigate } from 'react-router-dom'
import { usePortalAuth } from '@portal/react-bridge'

interface RequireRoleProps {
  children: React.ReactNode
  roles: string[]
  redirectTo?: string
}

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
  const normalizedRequiredRoles = roles.map(normalizeRole)
  const userRoles = (userRolesRaw || []).map(normalizeRole)
  const hasRequiredRole = normalizedRequiredRoles.some(required =>
    userRoles.includes(required)
  )

  if (!hasRequiredRole) {
    return <Navigate to={redirectTo} replace />
  }

  return <>{children}</>
}

export default RequireRole
