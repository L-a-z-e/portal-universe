import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { sellerOrderApi } from '@/api'

export const OrderListPage: React.FC = () => {
  const navigate = useNavigate()
  const [orders, setOrders] = useState<any[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const load = async () => {
      try {
        const res = await sellerOrderApi.getOrders({ page: 0, size: 20 })
        const apiData = res.data?.data
        const items = apiData?.content ?? apiData?.items ?? (Array.isArray(apiData) ? apiData : [])
        setOrders(items)
      } catch (err: any) {
        const status = err?.response?.status
        if (status === 404 || status === 502 || status === 503) {
          setError('Order data is not available yet. Cross-service integration with shopping-service is required.')
        } else {
          console.error(err)
          setError('Failed to load orders.')
        }
      } finally {
        setIsLoading(false)
      }
    }
    load()
  }, [])

  return (
    <div>
      <h1 className="text-2xl font-bold text-text-heading mb-6">Orders</h1>

      {error && (
        <div className="mb-4 p-4 bg-status-warning-bg border border-status-warning rounded-lg">
          <p className="text-sm text-status-warning">{error}</p>
        </div>
      )}

      <div className="bg-bg-card border border-border-default rounded-lg overflow-hidden">
        {isLoading ? (
          <div className="p-12 text-center">
            <p className="text-text-meta">Loading...</p>
          </div>
        ) : orders.length === 0 && !error ? (
          <div className="p-12 text-center">
            <p className="text-text-meta">No orders found</p>
          </div>
        ) : orders.length > 0 ? (
          <table className="w-full">
            <thead className="bg-bg-subtle border-b border-border-default">
              <tr>
                <th className="px-6 py-4 text-left text-xs font-medium text-text-meta">Order Number</th>
                <th className="px-6 py-4 text-left text-xs font-medium text-text-meta">Status</th>
                <th className="px-6 py-4 text-right text-xs font-medium text-text-meta">Amount</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border-default">
              {orders.map((order: any) => (
                <tr
                  key={order.id}
                  onClick={() => navigate(`/orders/${order.orderNumber}`)}
                  className="hover:bg-bg-hover transition-colors cursor-pointer"
                >
                  <td className="px-6 py-4 text-sm font-mono text-brand-primary">
                    {order.orderNumber}
                  </td>
                  <td className="px-6 py-4 text-sm text-text-body">{order.status}</td>
                  <td className="px-6 py-4 text-sm text-text-body text-right">
                    {order.finalAmount?.toLocaleString() || order.totalAmount?.toLocaleString()}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : null}
      </div>
    </div>
  )
}

export default OrderListPage
