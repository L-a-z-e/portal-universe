export interface Product {
  id: number
  name: string
  description: string
  price: number
  stockQuantity?: number
  imageUrl?: string
  category?: string
  createdAt: string
  updatedAt?: string
}

export interface ProductCreateRequest {
  name: string
  description: string
  price: number
  imageUrl?: string
  category?: string
}

export interface ProductUpdateRequest {
  name?: string
  description?: string
  price?: number
  imageUrl?: string
  category?: string
}

// Admin Product Types
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
