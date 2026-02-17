import React, { useState, useEffect } from 'react'
import { sellerInventoryApi, sellerProductApi } from '@/api'
import { Table, Select } from '@portal/design-react'
import type { TableColumn, SelectOption } from '@portal/design-core'

export const StockMovementPage: React.FC = () => {
  const [products, setProducts] = useState<any[]>([])
  const [selectedProductId, setSelectedProductId] = useState<number | null>(null)
  const [movements, setMovements] = useState<any[]>([])
  const [isLoading, setIsLoading] = useState(false)

  useEffect(() => {
    sellerProductApi.getProducts({ size: 100 }).then(res => {
      const apiData = res.data?.data
      const items = apiData?.content ?? apiData?.items ?? (Array.isArray(apiData) ? apiData : [])
      setProducts(items)
    }).catch(() => {})
  }, [])

  useEffect(() => {
    if (!selectedProductId) {
      setMovements([])
      return
    }
    const load = async () => {
      setIsLoading(true)
      try {
        const res = await sellerInventoryApi.getMovements(selectedProductId, { page: 0, size: 50 })
        const apiData = res.data?.data
        const items = apiData?.content ?? apiData?.items ?? (Array.isArray(apiData) ? apiData : [])
        setMovements(items)
      } catch (err) {
        console.error('Failed to load movements:', err)
        setMovements([])
      } finally {
        setIsLoading(false)
      }
    }
    load()
  }, [selectedProductId])

  const productOptions: SelectOption[] = [
    { value: '', label: '-- Select a product --' },
    ...products.map((p: any) => ({
      value: String(p.id),
      label: `${p.name} (ID: ${p.id})`,
    })),
  ]

  const columns: TableColumn<any>[] = [
    {
      key: 'createdAt',
      label: 'Date',
      render: (value: unknown) => (value as string)?.substring(0, 19) ?? '-',
    },
    {
      key: 'movementType',
      label: 'Type',
      render: (value: unknown) => {
        const type = value as string
        return (
          <span className={`px-2 py-1 rounded text-xs font-medium ${
            type === 'ADD' ? 'bg-status-success-bg text-status-success'
              : type === 'RESERVE' ? 'bg-status-warning-bg text-status-warning'
              : type === 'DEDUCT' ? 'bg-status-error-bg text-status-error'
              : 'bg-bg-subtle text-text-meta'
          }`}>
            {type}
          </span>
        )
      },
    },
    {
      key: 'quantity',
      label: 'Qty',
      align: 'right',
    },
    {
      key: 'previousAvailable',
      label: 'Avail (Before)',
      align: 'right',
    },
    {
      key: 'afterAvailable',
      label: 'Avail (After)',
      align: 'right',
    },
    {
      key: 'referenceType',
      label: 'Reference',
      render: (_value: unknown, row: unknown) => {
        const m = row as any
        return `${m.referenceType}${m.referenceId ? `:${m.referenceId}` : ''}`
      },
    },
    {
      key: 'reason',
      label: 'Reason',
      render: (value: unknown) => (value as string) || '-',
    },
  ]

  return (
    <div>
      <h1 className="text-2xl font-bold text-text-heading mb-6">Stock Movements</h1>

      <div className="mb-4 max-w-md">
        <Select
          label="Select Product"
          options={productOptions}
          value={selectedProductId !== null ? String(selectedProductId) : ''}
          onChange={(val) => setSelectedProductId(val ? parseInt(val as string, 10) : null)}
        />
      </div>

      <div className="bg-bg-card border border-border-default rounded-lg overflow-hidden shadow-sm">
        {!selectedProductId ? (
          <div className="p-12 text-center">
            <p className="text-text-meta">Select a product to view stock movement history.</p>
          </div>
        ) : (
          <Table
            columns={columns}
            data={movements}
            loading={isLoading}
            emptyText="No stock movements found for this product."
          />
        )}
      </div>
    </div>
  )
}

export default StockMovementPage
