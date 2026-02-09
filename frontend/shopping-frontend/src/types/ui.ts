/**
 * UI State & Component Types
 * admin.ts 대체 - UI 공통 타입
 */

import type { ProductFormData } from '@/dto/product'

// ============================================
// UI State Types
// ============================================

export interface ToastMessage {
  id: string
  type: 'success' | 'error' | 'warning' | 'info'
  title: string
  message?: string
  duration?: number
}

export interface ModalState {
  isOpen: boolean
  type: 'delete' | 'confirm' | null
  data?: any
}

// ============================================
// Table Types
// ============================================

export interface TableColumn<T> {
  key: keyof T | string
  header: string
  width?: string | number
  sortable?: boolean
  render?: (value: any, row: T) => React.ReactNode
}

export interface TableAction<T> {
  label: string
  icon?: React.ReactNode
  onClick: (row: T) => void
  variant?: 'primary' | 'danger' | 'default'
  visible?: (row: T) => boolean
}

// ============================================
// Component Props Types
// ============================================

export interface ConfirmModalProps {
  isOpen: boolean
  title: string
  message: string | React.ReactNode
  confirmText?: string
  cancelText?: string
  variant?: 'danger' | 'warning' | 'default'
  onConfirm: () => void | Promise<void>
  onCancel: () => void
  loading?: boolean
}

export interface PaginationProps {
  currentPage: number
  totalPages: number
  onPageChange: (page: number) => void
  disabled?: boolean
}

export interface ProductFormProps {
  mode: 'create' | 'edit'
  initialData?: Partial<ProductFormData>
  onSubmit: (data: ProductFormData) => void | Promise<void>
  onCancel: () => void
  isSubmitting?: boolean
}

// ============================================
// Auth Types
// ============================================

export interface User {
  id: string
  email: string
  name: string
  roles: string[]
  avatar?: string
}

export type UserRole = 'ROLE_USER' | 'ROLE_ADMIN'
