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
          <input
            type="text"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            placeholder="Search products..."
            className="px-4 py-2 border border-border-default rounded-lg bg-bg-input text-text-body placeholder:text-text-placeholder focus:outline-none focus:ring-2 focus:ring-brand-primary/20 focus:border-brand-primary"
          />
          <button
            type="submit"
            className="px-4 py-2 bg-brand-primary text-white rounded-lg hover:bg-brand-primary/90 transition-colors"
          >
            Search
          </button>
          {searchKeyword && (
            <button
              type="button"
              onClick={clearSearch}
              className="px-4 py-2 bg-bg-subtle text-text-body rounded-lg hover:bg-bg-hover transition-colors"
            >
              Clear
            </button>
          )}
        </form>
      </div>

      {/* Search result info */}
      {searchKeyword && (
        <div className="bg-status-info-bg border border-status-info/20 rounded-lg p-4">
          <p className="text-status-info text-sm">
            Search results for "{searchKeyword}"
          </p>
        </div>
      )}

      {/* Loading */}
      {loading && (
        <div className="flex items-center justify-center py-20">
          <div className="flex flex-col items-center gap-4">
            <div className="w-8 h-8 border-4 border-brand-primary border-t-transparent rounded-full animate-spin" />
            <p className="text-text-meta">Loading products...</p>
          </div>
        </div>
      )}

      {/* Error */}
      {error && !loading && (
        <div className="bg-status-error-bg border border-status-error/20 rounded-lg p-6 text-center">
          <p className="text-status-error mb-4">{error}</p>
          <button
            onClick={fetchProducts}
            className="px-4 py-2 bg-status-error text-white rounded-lg hover:bg-status-error/90 transition-colors"
          >
            Retry
          </button>
        </div>
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
              <button
                onClick={() => handlePageChange(currentPage - 1)}
                disabled={currentPage === 0}
                className="px-4 py-2 bg-bg-subtle text-text-body rounded-lg hover:bg-bg-hover disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              >
                Previous
              </button>

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
                    <button
                      key={page}
                      onClick={() => handlePageChange(page)}
                      className={`w-10 h-10 rounded-lg transition-colors ${
                        page === currentPage
                          ? 'bg-brand-primary text-white'
                          : 'bg-bg-subtle text-text-body hover:bg-bg-hover'
                      }`}
                    >
                      {page + 1}
                    </button>
                  )
                })}
              </div>

              <button
                onClick={() => handlePageChange(currentPage + 1)}
                disabled={currentPage === totalPages - 1}
                className="px-4 py-2 bg-bg-subtle text-text-body rounded-lg hover:bg-bg-hover disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              >
                Next
              </button>
            </div>
          )}
        </>
      )}
    </div>
  )
}

export default ProductListPage
