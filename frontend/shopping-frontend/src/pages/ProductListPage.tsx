import React, { useState, useEffect, useCallback } from 'react'
import { useSearchParams } from 'react-router-dom'
import { productApi, inventoryApi, searchApi } from '@/api'
import type { Product, Inventory } from '@/types'
import ProductCard from '@/components/product/ProductCard'
import FilterChips from '@/components/product/FilterChips'
import SearchAutocomplete from '@/components/search/SearchAutocomplete'
import { Button, Spinner, Alert, Pagination } from '@portal/design-react'

const ProductListPage: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams()
  const currentPage = parseInt(searchParams.get('page') || '1')
  const searchKeyword = searchParams.get('keyword') || ''
  const category = searchParams.get('category') || ''

  const [products, setProducts] = useState<Product[]>([])
  const [categories, setCategories] = useState<string[]>([])
  const [inventories, setInventories] = useState<Map<number, Inventory>>(new Map())
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [searchInput, setSearchInput] = useState(searchKeyword)

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

      const productsData = response.data?.items ?? []
      setProducts(productsData)
      setTotalPages(response.data?.totalPages ?? 0)

      if (productsData.length > 0) {
        const productIds = productsData.map((p: Product) => p.id)
        try {
          const invResponse = await inventoryApi.getInventories(productIds)
          const invMap = new Map<number, Inventory>()
          invResponse.data.forEach((inv: Inventory) => {
            invMap.set(inv.productId, inv)
          })
          setInventories(invMap)
        } catch {
          // inventory API 실패 시 무시
        }
      }
    } catch (err: any) {
      setError(err.response?.data?.error?.message || err.message || 'Failed to fetch products')
    } finally {
      setLoading(false)
    }
  }, [currentPage, searchKeyword, category])

  useEffect(() => {
    fetchProducts()
  }, [fetchProducts])

  useEffect(() => {
    productApi.getCategories()
      .then(res => setCategories(res.data ?? []))
      .catch(() => {})
  }, [])

  const handleSearch = (keyword: string) => {
    const params = new URLSearchParams()
    if (keyword) {
      params.set('keyword', keyword)
      searchApi.addRecentKeyword(keyword).catch(() => {})
    }
    params.set('page', '1')
    setSearchParams(params)
    setSearchInput(keyword)
  }

  const handleCategoryChange = (cat: string) => {
    const params = new URLSearchParams(searchParams)
    if (cat) {
      params.set('category', cat)
    } else {
      params.delete('category')
    }
    params.set('page', '1')
    setSearchParams(params)
  }

  const handlePageChange = (page: number) => {
    const params = new URLSearchParams(searchParams)
    params.set('page', String(page))
    setSearchParams(params)
  }

  const clearSearch = () => {
    setSearchInput('')
    setSearchParams({})
  }

  return (
    <div className="space-y-8">
      {/* Hero Header */}
      <div className="space-y-2">
        <h1 className="text-4xl font-bold text-text-heading">Store</h1>
        <p className="text-text-meta">
          {searchKeyword
            ? `"${searchKeyword}" 검색 결과`
            : '다양한 상품을 만나보세요'}
        </p>
      </div>

      {/* Filter + Search Bar */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <FilterChips
          categories={categories}
          active={category}
          onChange={handleCategoryChange}
        />

        <div className="flex items-center gap-2 flex-shrink-0">
          <SearchAutocomplete
            value={searchInput}
            onChange={setSearchInput}
            onSearch={handleSearch}
            placeholder="Search products..."
          />
          {searchKeyword && (
            <Button type="button" onClick={clearSearch} variant="secondary" size="sm">
              Clear
            </Button>
          )}
        </div>
      </div>

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
          <Button onClick={fetchProducts} variant="primary">Retry</Button>
        </Alert>
      )}

      {/* Product Grid */}
      {!loading && !error && (
        <>
          {products.length === 0 ? (
            <div className="bg-bg-card border border-dashed border-border-default rounded-2xl p-12 text-center">
              <p className="text-text-meta text-lg mb-2">No products found</p>
              {searchKeyword && (
                <p className="text-text-meta text-sm">
                  Try different keywords or{' '}
                  <Button variant="ghost" size="sm" onClick={clearSearch} className="text-brand-primary hover:underline p-0 h-auto inline">
                    clear the search
                  </Button>
                </p>
              )}
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-x-8 gap-y-12">
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
            <div className="flex justify-center pt-4">
              <Pagination
                page={currentPage}
                totalPages={totalPages}
                onChange={handlePageChange}
              />
            </div>
          )}
        </>
      )}
    </div>
  )
}

export default ProductListPage
