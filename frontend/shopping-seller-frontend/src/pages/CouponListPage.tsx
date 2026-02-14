import React, { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { sellerCouponApi } from '@/api'
import { Button } from '@portal/design-system-react'

export const CouponListPage: React.FC = () => {
  const [coupons, setCoupons] = useState<any[]>([])
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    const load = async () => {
      try {
        const res = await sellerCouponApi.getCoupons({ page: 1, size: 10 })
        setCoupons(res.data?.items || [])
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
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold text-text-heading">Coupons</h1>
        <Link to="/coupons/new">
          <Button variant="primary">New Coupon</Button>
        </Link>
      </div>

      <div className="bg-bg-card border border-border-default rounded-lg p-6">
        {isLoading ? (
          <p className="text-text-meta">Loading...</p>
        ) : coupons.length === 0 ? (
          <p className="text-text-meta">No coupons found</p>
        ) : (
          <div className="space-y-2">
            {coupons.map((c: any) => (
              <div key={c.id} className="p-3 border border-border-default rounded">
                {c.name} - {c.code}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

export default CouponListPage
