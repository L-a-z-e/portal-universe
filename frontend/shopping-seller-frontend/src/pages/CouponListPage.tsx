import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { sellerCouponApi } from '@/api'
import { Button } from '@portal/design-react'

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

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold text-text-heading">Coupons</h1>
        <Button variant="primary" onClick={() => navigate('/coupons/new')}>
          New Coupon
        </Button>
      </div>

      <div className="bg-bg-card border border-border-default rounded-lg overflow-hidden shadow-sm">
        {isLoading ? (
          <div className="p-12 text-center">
            <p className="text-text-meta">Loading...</p>
          </div>
        ) : coupons.length === 0 ? (
          <div className="p-12 text-center">
            <p className="text-lg text-text-heading mb-2">No coupons found</p>
            <Button variant="primary" onClick={() => navigate('/coupons/new')}>
              Create Coupon
            </Button>
          </div>
        ) : (
          <table className="w-full">
            <thead className="bg-bg-subtle border-b border-border-default">
              <tr>
                <th className="px-6 py-4 text-left text-xs font-medium text-text-meta uppercase">Code</th>
                <th className="px-6 py-4 text-left text-xs font-medium text-text-meta uppercase">Name</th>
                <th className="px-6 py-4 text-left text-xs font-medium text-text-meta uppercase">Discount</th>
                <th className="px-6 py-4 text-left text-xs font-medium text-text-meta uppercase">Status</th>
                <th className="px-6 py-4 text-left text-xs font-medium text-text-meta uppercase">Qty</th>
                <th className="px-6 py-4 text-right text-xs font-medium text-text-meta uppercase">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border-default">
              {coupons.map((c: any) => (
                <tr key={c.id} className="hover:bg-bg-hover transition-colors">
                  <td className="px-6 py-4 text-sm font-mono text-brand-primary">{c.code}</td>
                  <td className="px-6 py-4 text-sm text-text-body font-medium">{c.name}</td>
                  <td className="px-6 py-4 text-sm text-text-body">
                    {c.discountType === 'PERCENTAGE' ? `${c.discountValue}%` : `$${c.discountValue}`}
                  </td>
                  <td className="px-6 py-4 text-sm">
                    <span className={`px-2 py-1 rounded text-xs font-medium ${
                      c.status === 'ACTIVE' ? 'bg-status-success-bg text-status-success' : 'bg-bg-subtle text-text-meta'
                    }`}>
                      {c.status}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-sm text-text-body">{c.issuedQuantity}/{c.totalQuantity}</td>
                  <td className="px-6 py-4 text-right">
                    {c.status === 'ACTIVE' && (
                      <button
                        onClick={() => handleDeactivate(c.id)}
                        className="p-2 text-status-error hover:bg-status-error-bg rounded transition-colors text-sm"
                      >
                        Deactivate
                      </button>
                    )}
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

export default CouponListPage
