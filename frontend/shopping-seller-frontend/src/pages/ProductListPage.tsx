import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { sellerProductApi } from '@/api'
import { Button, Table } from '@portal/design-react'
import type { TableColumn } from '@portal/design-core'
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

  const columns: TableColumn<Product>[] = [
    {
      key: 'id',
      label: 'ID',
    },
    {
      key: 'name',
      label: 'Name',
      render: (value: unknown) => (
        <span className="font-medium">{value as string}</span>
      ),
    },
    {
      key: 'price',
      label: 'Price',
      render: (value: unknown) => `$${(value as number).toLocaleString()}`,
    },
    {
      key: 'category',
      label: 'Category',
      render: (value: unknown) => (value as string) || '-',
    },
    {
      key: 'id',
      label: 'Actions',
      align: 'right',
      render: (_value: unknown, row: unknown) => {
        const product = row as Product
        return (
          <Button
            variant="ghost"
            size="sm"
            onClick={(e: React.MouseEvent) => {
              e.stopPropagation()
              handleDelete(product.id)
            }}
            className="text-status-error hover:bg-status-error-bg"
          >
            Delete
          </Button>
        )
      },
    },
  ]

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-text-heading">Products</h1>
        <Button variant="primary" onClick={() => navigate('/products/new')}>
          New Product
        </Button>
      </div>

      <div className="bg-bg-card border border-border-default rounded-lg overflow-hidden shadow-sm">
        {!isLoading && products.length === 0 ? (
          <div className="p-12 text-center">
            <p className="text-lg text-text-heading mb-2">No products found</p>
            <Button variant="primary" onClick={() => navigate('/products/new')}>
              Create Product
            </Button>
          </div>
        ) : (
          <Table
            columns={columns}
            data={products}
            loading={isLoading}
            emptyText="No products found"
            onRowClick={(row: Product) => navigate(`/products/${row.id}`)}
          />
        )}
      </div>
    </div>
  )
}

export default ProductListPage
