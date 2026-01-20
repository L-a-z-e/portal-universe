/**
 * Product List Page
 *
 * 상품 목록 조회 및 검색 페이지
 */
import React, { useState, useEffect, useCallback } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { productApi, inventoryApi } from '@/api/endpoints'
import type { Product, Inventory } from '@/types'
import ProductCard from '@/components/ProductCard'
import { Button, Spinner, Alert, Input } from '@portal/design-system-react'

const ProductListPage: React.FC = () => {
  // URL query params
  const [searchParams, setSearchParams] = useSearchParams()
  const currentPage = parseInt(searchParams.get('page') || '0')
  const searchKeyword = searchParams.get('keyword') || ''
  const category = searchParams.get('category') || ''

  // State
  const [products, setProducts] = useState<Product[]>([])
  const [inventories, setInventories] = useState<Map<number, Inventory>>(new Map())
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [searchInput, setSearchInput] = useState(searchKeyword)

  // Fetch products
  const fetchProducts = useCallback(async () => {
    setLoading(true)
    setError(null)

    try {
      let response
      if (searchKeyword) {
        response = await productApi.searchProducts(searchKeyword, currentPage, 12)
      } else {
        response = await productApi.getProducts(currentPage, 12, category || undefined)
      }

      if (response.success) {
        const productsData = response.data.content
        setProducts(productsData)
        setTotalPages(response.data.totalPages)

        // Fetch inventories for all products
        if (productsData.length > 0) {
          const productIds = productsData.map((p: Product) => p.id)
          try {
            const invResponse = await inventoryApi.getInventories(productIds)
            if (invResponse.success) {
              const invMap = new Map<number, Inventory>()
              invResponse.data.forEach((inv: Inventory) => {
                invMap.set(inv.productId, inv)
              })
              setInventories(invMap)
            }
          } catch (invError) {
            console.warn('Failed to fetch inventories:', invError)
          }
        }
      } else {
        setError(response.message || 'Failed to fetch products')
      }
    } catch (err: any) {
      setError(err.response?.data?.message || err.message || 'Failed to fetch products')
    } finally {
      setLoading(false)
    }
  }, [currentPage, searchKeyword, category])

  useEffect(() => {
    fetchProducts()
  }, [fetchProducts])

  // Handle search
  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault()
    const params = new URLSearchParams()
    if (searchInput) {
      params.set('keyword', searchInput)
    }
    params.set('page', '0')
    setSearchParams(params)
  }

  // Handle page change
  const handlePageChange = (page: number) => {
    const params = new URLSearchParams(searchParams)
    params.set('page', String(page))
    setSearchParams(params)
  }

  // Clear search
  const clearSearch = () => {
    setSearchInput('')
    setSearchParams({})
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-text-heading">
          Products
        </h1>

        {/* Search Form */}
        <form onSubmit={handleSearch} className="flex items-center gap-2">
          <Input
            type="text"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            placeholder="Search products..."
          />
          <Button type="submit" variant="primary">
            Search
          </Button>
          {searchKeyword && (
            <Button
              type="button"
              onClick={clearSearch}
              variant="secondary"
            >
              Clear
            </Button>
          )}
        </form>
      </div>

      {/* Search result info */}
      {searchKeyword && (
        <Alert variant="info">
          Search results for "{searchKeyword}"
        </Alert>
      )}

      {/* Loading */}
      {loading && (
        <div className="flex items-center justify-center py-20">
          <div className="flex flex-col items-center gap-4">
            <Spinner size="lg" />
            <p className="text-text-meta">Loading products...</p>
          </div>
        </div>
      )}

      {/* Error */}
      {error && !loading && (
        <Alert variant="error" className="text-center">
          <p className="mb-4">{error}</p>
          <Button onClick={fetchProducts} variant="primary">
            Retry
          </Button>
        </Alert>
      )}

      {/* Product Grid */}
      {!loading && !error && (
        <>
          {products.length === 0 ? (
            <div className="bg-bg-card border border-border-default rounded-lg p-12 text-center">
              <p className="text-text-meta text-lg mb-2">No products found</p>
              {searchKeyword && (
                <p className="text-text-meta text-sm">
                  Try different keywords or{' '}
                  <button
                    onClick={clearSearch}
                    className="text-brand-primary hover:underline"
                  >
                    clear the search
                  </button>
                </p>
              )}
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
              {products.map((product) => (
                <ProductCard
                  key={product.id}
                  product={product}
                  inventory={inventories.get(product.id)}
                />
              ))}
            </div>
          )}

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex items-center justify-center gap-2 mt-8">
              <Button
                onClick={() => handlePageChange(currentPage - 1)}
                disabled={currentPage === 0}
                variant="secondary"
              >
                Previous
              </Button>

              <div className="flex items-center gap-1">
                {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
                  let page: number
                  if (totalPages <= 5) {
                    page = i
                  } else if (currentPage < 3) {
                    page = i
                  } else if (currentPage > totalPages - 4) {
                    page = totalPages - 5 + i
                  } else {
                    page = currentPage - 2 + i
                  }

                  return (
                    <Button
                      key={page}
                      onClick={() => handlePageChange(page)}
                      variant={page === currentPage ? 'primary' : 'secondary'}
                      size="sm"
                      className="w-10 h-10"
                    >
                      {page + 1}
                    </Button>
                  )
                })}
              </div>

              <Button
                onClick={() => handlePageChange(currentPage + 1)}
                disabled={currentPage === totalPages - 1}
                variant="secondary"
              >
                Next
              </Button>
            </div>
          )}
        </>
      )}
    </div>
  )
}

export default ProductListPage
