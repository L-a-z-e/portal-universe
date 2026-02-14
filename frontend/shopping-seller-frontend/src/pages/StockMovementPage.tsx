import React, { useState, useEffect } from 'react'
import { sellerInventoryApi, sellerProductApi } from '@/api'

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

  return (
    <div>
      <h1 className="text-2xl font-bold text-text-heading mb-6">Stock Movements</h1>

      <div className="mb-4">
        <label className="block text-sm font-medium text-text-heading mb-1">Select Product</label>
        <select
          className="w-full max-w-md px-3 py-2 border border-border-default rounded-md bg-bg-default text-text-body"
          value={selectedProductId ?? ''}
          onChange={(e) => setSelectedProductId(e.target.value ? parseInt(e.target.value, 10) : null)}
        >
          <option value="">-- Select a product --</option>
          {products.map((p: any) => (
            <option key={p.id} value={p.id}>{p.name} (ID: {p.id})</option>
          ))}
        </select>
      </div>

      <div className="bg-bg-card border border-border-default rounded-lg overflow-hidden shadow-sm">
        {!selectedProductId ? (
          <div className="p-12 text-center">
            <p className="text-text-meta">Select a product to view stock movement history.</p>
          </div>
        ) : isLoading ? (
          <div className="p-12 text-center">
            <p className="text-text-meta">Loading...</p>
          </div>
        ) : movements.length === 0 ? (
          <div className="p-12 text-center">
            <p className="text-text-meta">No stock movements found for this product.</p>
          </div>
        ) : (
          <table className="w-full">
            <thead className="bg-bg-subtle border-b border-border-default">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-medium text-text-meta uppercase">Date</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-text-meta uppercase">Type</th>
                <th className="px-4 py-3 text-right text-xs font-medium text-text-meta uppercase">Qty</th>
                <th className="px-4 py-3 text-right text-xs font-medium text-text-meta uppercase">Avail (Before)</th>
                <th className="px-4 py-3 text-right text-xs font-medium text-text-meta uppercase">Avail (After)</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-text-meta uppercase">Reference</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-text-meta uppercase">Reason</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border-default">
              {movements.map((m: any) => (
                <tr key={m.id} className="hover:bg-bg-hover transition-colors">
                  <td className="px-4 py-3 text-xs text-text-body">{m.createdAt?.substring(0, 19)}</td>
                  <td className="px-4 py-3 text-xs">
                    <span className={`px-2 py-1 rounded text-xs font-medium ${
                      m.movementType === 'ADD' ? 'bg-status-success-bg text-status-success'
                        : m.movementType === 'RESERVE' ? 'bg-status-warning-bg text-status-warning'
                        : m.movementType === 'DEDUCT' ? 'bg-status-error-bg text-status-error'
                        : 'bg-bg-subtle text-text-meta'
                    }`}>
                      {m.movementType}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-xs text-text-body text-right">{m.quantity}</td>
                  <td className="px-4 py-3 text-xs text-text-body text-right">{m.previousAvailable}</td>
                  <td className="px-4 py-3 text-xs text-text-body text-right">{m.afterAvailable}</td>
                  <td className="px-4 py-3 text-xs text-text-meta">{m.referenceType}{m.referenceId ? `:${m.referenceId}` : ''}</td>
                  <td className="px-4 py-3 text-xs text-text-meta">{m.reason || '-'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}

export default StockMovementPage
