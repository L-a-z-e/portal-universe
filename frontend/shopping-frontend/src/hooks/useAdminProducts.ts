/**
 * Admin Products Hooks (useState + useEffect 패턴)
 * 상품 관리를 위한 커스텀 훅
 */
import { useState, useEffect, useCallback } from 'react'
import { adminProductApi } from '@/api/endpoints'
import type { ProductFilters, ProductFormData, PagedResponse } from '@/types/admin'
import type { Product } from '@/types'

/**
 * Admin 상품 목록 조회 Hook
 */
export const useAdminProducts = (filters: ProductFilters) => {
  const [data, setData] = useState<PagedResponse<Product> | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<Error | null>(null)

  const fetchProducts = useCallback(async () => {
    setIsLoading(true)
    setError(null)
    try {
      const response = await adminProductApi.getProducts(filters)
      if (response.success) {
        setData(response.data)
      } else {
        setError(new Error(response.message || 'Failed to fetch products'))
      }
    } catch (err) {
      setError(err as Error)
    } finally {
      setIsLoading(false)
    }
  }, [filters.page, filters.size, filters.keyword, filters.sortBy, filters.sortOrder])

  useEffect(() => {
    fetchProducts()
  }, [fetchProducts])

  return {
    data: data ? { data } : null,
    isLoading,
    error,
    refetch: fetchProducts
  }
}

/**
 * Admin 상품 상세 조회 Hook
 */
export const useAdminProduct = (id: number) => {
  const [data, setData] = useState<Product | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<Error | null>(null)

  const fetchProduct = useCallback(async () => {
    if (!id || id <= 0) return

    setIsLoading(true)
    setError(null)
    try {
      const response = await adminProductApi.getProduct(id)
      if (response.success) {
        setData(response.data)
      } else {
        setError(new Error(response.message || 'Failed to fetch product'))
      }
    } catch (err) {
      setError(err as Error)
    } finally {
      setIsLoading(false)
    }
  }, [id])

  useEffect(() => {
    fetchProduct()
  }, [fetchProduct])

  return {
    data: data ? { data } : null,
    isLoading,
    error,
    refetch: fetchProduct
  }
}

/**
 * 상품 생성 Hook
 */
export const useCreateProduct = () => {
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<Error | null>(null)

  const createProduct = useCallback(async (data: ProductFormData) => {
    setIsLoading(true)
    setError(null)
    try {
      const response = await adminProductApi.createProduct(data)
      if (!response.success) {
        throw new Error(response.message || 'Failed to create product')
      }
      return response.data
    } catch (err) {
      setError(err as Error)
      throw err
    } finally {
      setIsLoading(false)
    }
  }, [])

  return {
    mutateAsync: createProduct,
    isPending: isLoading,
    error
  }
}

/**
 * 상품 수정 Hook
 */
export const useUpdateProduct = () => {
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<Error | null>(null)

  const updateProduct = useCallback(async ({ id, data }: { id: number; data: ProductFormData }) => {
    setIsLoading(true)
    setError(null)
    try {
      const response = await adminProductApi.updateProduct(id, data)
      if (!response.success) {
        throw new Error(response.message || 'Failed to update product')
      }
      return response.data
    } catch (err) {
      setError(err as Error)
      throw err
    } finally {
      setIsLoading(false)
    }
  }, [])

  return {
    mutateAsync: updateProduct,
    isPending: isLoading,
    error
  }
}

/**
 * 상품 삭제 Hook
 */
export const useDeleteProduct = () => {
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<Error | null>(null)

  const deleteProduct = useCallback(async (id: number) => {
    setIsLoading(true)
    setError(null)
    try {
      const response = await adminProductApi.deleteProduct(id)
      if (!response.success) {
        throw new Error(response.message || 'Failed to delete product')
      }
      return true
    } catch (err) {
      setError(err as Error)
      throw err
    } finally {
      setIsLoading(false)
    }
  }, [])

  return {
    mutateAsync: deleteProduct,
    isPending: isLoading,
    error
  }
}
