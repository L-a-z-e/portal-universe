import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { sellerProductApi } from '@/api'
import { Button } from '@portal/design-system-react'
import type { Product } from '@/types'

export const ProductListPage: React.FC = () => {
  const navigate = useNavigate()
  const [products, setProducts] = useState<Product[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [page, setPage] = useState(1)

  useEffect(() => {
    const loadProducts = async () => {
      setIsLoading(true)
      try {
        const response = await sellerProductApi.getProducts({ page, size: 10 })
        const apiData = response.data?.data
        const items = apiData?.content ?? apiData?.items ?? (Array.isArray(apiData) ? apiData : [])
        setProducts(items)
      } catch (err) {
        console.error('Failed to load products:', err)
        setProducts([])
      } finally {
        setIsLoading(false)
      }
    }
    loadProducts()
  }, [page])

  const handleDelete = async (id: number) => {
    if (!confirm('Delete this product?')) return
    try {
      await sellerProductApi.deleteProduct(id)
      setPage(1)
    } catch (err) {
      console.error('Failed to delete product:', err)
    }
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-text-heading">Products</h1>
        <Button variant="primary" onClick={() => navigate('/products/new')}>
          New Product
        </Button>
      </div>

      <div className="bg-bg-card border border-border-default rounded-lg overflow-hidden shadow-sm">
        {isLoading ? (
          <div className="p-12 text-center">
            <p className="text-text-meta">Loading...</p>
          </div>
        ) : products.length === 0 ? (
          <div className="p-12 text-center">
            <p className="text-lg text-text-heading mb-2">No products found</p>
            <Button variant="primary" onClick={() => navigate('/products/new')}>
              Create Product
            </Button>
          </div>
        ) : (
          <table className="w-full">
            <thead className="bg-bg-subtle border-b border-border-default">
              <tr>
                <th className="px-6 py-4 text-left text-xs font-medium text-text-meta uppercase">ID</th>
                <th className="px-6 py-4 text-left text-xs font-medium text-text-meta uppercase">Name</th>
                <th className="px-6 py-4 text-left text-xs font-medium text-text-meta uppercase">Price</th>
                <th className="px-6 py-4 text-left text-xs font-medium text-text-meta uppercase">Category</th>
                <th className="px-6 py-4 text-right text-xs font-medium text-text-meta uppercase">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border-default">
              {products.map((product) => (
                <tr
                  key={product.id}
                  className="hover:bg-bg-hover transition-colors cursor-pointer"
                  onClick={() => navigate(`/products/${product.id}`)}
                >
                  <td className="px-6 py-4 text-sm text-text-body">{product.id}</td>
                  <td className="px-6 py-4 text-sm text-text-body font-medium">{product.name}</td>
                  <td className="px-6 py-4 text-sm text-text-body">
                    ${product.price.toLocaleString()}
                  </td>
                  <td className="px-6 py-4 text-sm text-text-meta">{product.category || '-'}</td>
                  <td className="px-6 py-4 text-right">
                    <button
                      onClick={(e) => {
                        e.stopPropagation()
                        handleDelete(product.id)
                      }}
                      className="p-2 text-status-error hover:bg-status-error-bg rounded transition-colors"
                    >
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}

export default ProductListPage
