/**
 * Admin 전용 타입 정의
 * 관리자 기능에 필요한 타입들
 */

import type { Product } from './index'

// ============================================
// Paged Response Types
// ============================================

export interface PagedResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
}

// ============================================
// Product Admin Types
// ============================================

export type ProductStatus = 'ACTIVE' | 'INACTIVE' | 'OUT_OF_STOCK'

export interface AdminProduct extends Product {
  stock: number
  status?: ProductStatus
}

export interface ProductFilters {
  page: number
  size: number
  keyword?: string
  category?: string
  status?: ProductStatus
  sortBy?: 'name' | 'price' | 'createdAt'
  sortOrder?: 'asc' | 'desc'
}

export interface ProductFormData {
  name: string
  description: string
  price: number
  stock: number
  imageUrl?: string
  category?: string
}

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
