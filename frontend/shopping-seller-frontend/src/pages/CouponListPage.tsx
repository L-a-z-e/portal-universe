import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { sellerCouponApi } from '@/api'
import { Button, Table } from '@portal/design-react'
import type { TableColumn } from '@portal/design-core'

export const CouponListPage: React.FC = () => {
  const navigate = useNavigate()
  const [coupons, setCoupons] = useState<any[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [page, _setPage] = useState(0)
  const [refreshKey, setRefreshKey] = useState(0)

  useEffect(() => {
    const load = async () => {
      setIsLoading(true)
      try {
        const res = await sellerCouponApi.getCoupons({ page, size: 10 })
        const apiData = res.data?.data
        const items = apiData?.content ?? apiData?.items ?? (Array.isArray(apiData) ? apiData : [])
        setCoupons(items)
      } catch (err) {
        console.error('Failed to load coupons:', err)
        setCoupons([])
      } finally {
        setIsLoading(false)
      }
    }
    load()
  }, [page, refreshKey])

  const handleDeactivate = async (id: number) => {
    if (!confirm('Deactivate this coupon?')) return
    try {
      await sellerCouponApi.deleteCoupon(id)
      setRefreshKey(prev => prev + 1)
    } catch (err) {
      console.error('Failed to deactivate coupon:', err)
    }
  }

  const columns: TableColumn<any>[] = [
    {
      key: 'code',
      label: 'Code',
      render: (value: unknown) => (
        <span className="font-mono text-brand-primary">{value as string}</span>
      ),
    },
    {
      key: 'name',
      label: 'Name',
      render: (value: unknown) => (
        <span className="font-medium">{value as string}</span>
      ),
    },
    {
      key: 'discountValue',
      label: 'Discount',
      render: (_value: unknown, row: unknown) => {
        const c = row as any
        return c.discountType === 'PERCENTAGE' ? `${c.discountValue}%` : `$${c.discountValue}`
      },
    },
    {
      key: 'status',
      label: 'Status',
      render: (value: unknown) => {
        const status = value as string
        return (
          <span className={`px-2 py-1 rounded text-xs font-medium ${
            status === 'ACTIVE' ? 'bg-status-success-bg text-status-success' : 'bg-bg-subtle text-text-meta'
          }`}>
            {status}
          </span>
        )
      },
    },
    {
      key: 'issuedQuantity',
      label: 'Qty',
      render: (_value: unknown, row: unknown) => {
        const c = row as any
        return `${c.issuedQuantity}/${c.totalQuantity}`
      },
    },
    {
      key: 'id',
      label: 'Actions',
      align: 'right',
      render: (_value: unknown, row: unknown) => {
        const c = row as any
        return c.status === 'ACTIVE' ? (
          <Button
            variant="ghost"
            size="sm"
            onClick={() => handleDeactivate(c.id)}
            className="text-status-error hover:bg-status-error-bg"
          >
            Deactivate
          </Button>
        ) : null
      },
    },
  ]

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold text-text-heading">Coupons</h1>
        <Button variant="primary" onClick={() => navigate('/coupons/new')}>
          New Coupon
        </Button>
      </div>

      <div className="bg-bg-card border border-border-default rounded-lg overflow-hidden shadow-sm">
        {!isLoading && coupons.length === 0 ? (
          <div className="p-12 text-center">
            <p className="text-lg text-text-heading mb-2">No coupons found</p>
            <Button variant="primary" onClick={() => navigate('/coupons/new')}>
              Create Coupon
            </Button>
          </div>
        ) : (
          <Table
            columns={columns}
            data={coupons}
            loading={isLoading}
            emptyText="No coupons found"
          />
        )}
      </div>
    </div>
  )
}

export default CouponListPage
