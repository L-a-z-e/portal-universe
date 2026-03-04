import { getApiClient } from './client'
import type { ApiResponse, PageResponse } from '@/types'
import type { Product } from '@/dto/product'
import type { ProductWithReviews } from '@/dto/review'

const API_PREFIX = '/api/v1/shopping'

export const productApi = {
  getProducts: async (page = 1, size = 12, category?: string) => {
    const params = new URLSearchParams({
      page: String(page),
      size: String(size)
    })
    if (category) {
      params.append('category', category)
    }
    const response = await getApiClient().get<ApiResponse<PageResponse<Product>>>(
      `${API_PREFIX}/products?${params}`
    )
    return response.data
  },

  getCategories: async () => {
    const response = await getApiClient().get<ApiResponse<string[]>>(
      `${API_PREFIX}/products/categories`
    )
    return response.data
  },

  getProduct: async (id: number) => {
    const response = await getApiClient().get<ApiResponse<Product>>(
      `${API_PREFIX}/products/${id}`
    )
    return response.data
  },

  searchProducts: async (keyword: string, page = 1, size = 12) => {
    const params = new URLSearchParams({
      keyword,
      page: String(page),
      size: String(size)
    })
    const response = await getApiClient().get<ApiResponse<PageResponse<Product>>>(
      `${API_PREFIX}/search/products?${params}`
    )
    return response.data
  }
}

export const adminProductApi = {
  getProducts: async (params: {
    page?: number
    size?: number
    keyword?: string
    category?: string
    status?: string
    sortBy?: string
    sortOrder?: 'asc' | 'desc'
  }) => {
    const searchParams = new URLSearchParams()
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== '') {
        searchParams.append(key, String(value))
      }
    })

    const response = await getApiClient().get<ApiResponse<PageResponse<Product>>>(
      `${API_PREFIX}/products?${searchParams}`
    )
    return response.data
  },

  getProduct: async (id: number) => {
    const response = await getApiClient().get<ApiResponse<Product>>(
      `${API_PREFIX}/products/${id}`
    )
    return response.data
  }
}

export const productReviewApi = {
  getProductWithReviews: async (productId: number) => {
    const response = await getApiClient().get<ApiResponse<ProductWithReviews>>(
      `${API_PREFIX}/products/${productId}/with-reviews`
    )
    return response.data
  }
}
