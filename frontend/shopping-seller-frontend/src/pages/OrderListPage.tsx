import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { sellerOrderApi } from '@/api'

export const OrderListPage: React.FC = () => {
  const navigate = useNavigate()
  const [orders, setOrders] = useState<any[]>([])
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    const load = async () => {
      try {
        const res = await sellerOrderApi.getOrders({ page: 1, size: 20 })
        setOrders(res.data?.items || [])
      } catch (err) {
        console.error(err)
      } finally {
        setIsLoading(false)
      }
    }
    load()
  }, [])

  return (
    <div>
      <h1 className="text-2xl font-bold text-text-heading mb-6">Orders</h1>
      <div className="bg-bg-card border border-border-default rounded-lg overflow-hidden">
        {isLoading ? (
          <div className="p-12 text-center">
            <p className="text-text-meta">Loading...</p>
          </div>
        ) : orders.length === 0 ? (
          <div className="p-12 text-center">
            <p className="text-text-meta">No orders found</p>
          </div>
        ) : (
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
        )}
      </div>
    </div>
  )
}

export default OrderListPage
