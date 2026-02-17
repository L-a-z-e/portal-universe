import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { sellerOrderApi } from '@/api'
import { Table } from '@portal/design-react'
import type { TableColumn } from '@portal/design-core'

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

  const columns: TableColumn<any>[] = [
    {
      key: 'orderNumber',
      label: 'Order Number',
      render: (value: unknown) => (
        <span className="font-mono text-brand-primary">{value as string}</span>
      ),
    },
    {
      key: 'status',
      label: 'Status',
    },
    {
      key: 'finalAmount',
      label: 'Amount',
      align: 'right',
      render: (_value: unknown, row: unknown) => {
        const order = row as any
        return (order.finalAmount?.toLocaleString() || order.totalAmount?.toLocaleString()) ?? '-'
      },
    },
  ]

  return (
    <div>
      <h1 className="text-2xl font-bold text-text-heading mb-6">Orders</h1>

      {error && (
        <div className="mb-4 p-4 bg-status-warning-bg border border-status-warning rounded-lg">
          <p className="text-sm text-status-warning">{error}</p>
        </div>
      )}

      <div className="bg-bg-card border border-border-default rounded-lg overflow-hidden">
        <Table
          columns={columns}
          data={orders}
          loading={isLoading}
          emptyText="No orders found"
          onRowClick={(row: any) => navigate(`/orders/${row.orderNumber}`)}
        />
      </div>
    </div>
  )
}

export default OrderListPage
