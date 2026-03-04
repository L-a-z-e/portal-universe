export interface Product {
  id: number
  name: string
  description: string
  price: number
  discountPrice?: number
  imageUrl?: string
  images?: string[]
  category?: string
  featured?: boolean
  averageRating?: number
  reviewCount?: number
  createdAt: string
  updatedAt?: string
}

export interface ProductFilters {
  page: number
  size: number
  keyword?: string
  category?: string
  sortBy?: 'name' | 'price' | 'createdAt'
  sortOrder?: 'asc' | 'desc'
}
