/**
 * Shopping Frontend Type Definitions
 * Domain DTOs re-exported from dto/ modules
 */

// Common types (includes @portal/design-core re-exports)
export * from './common'

// Domain DTOs
export * from '@/dto/common'
export * from '@/dto/product'
export * from '@/dto/inventory'
export * from '@/dto/cart'
export * from '@/dto/order'
export * from '@/dto/payment'
export * from '@/dto/delivery'
export * from '@/dto/coupon'
export * from '@/dto/timedeal'
export * from '@/dto/queue'
export * from '@/dto/search'
export * from '@/dto/review'

// UI types
export * from './ui'

// SelectOption (UI helper)
export interface SelectOption {
  value: string
  label: string
}
