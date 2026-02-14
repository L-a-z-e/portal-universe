import React from 'react'
import { useNavigate } from 'react-router-dom'
import { Button } from '@portal/design-system-react'

export const CouponFormPage: React.FC = () => {
  const navigate = useNavigate()
  return (
    <div>
      <h1 className="text-2xl font-bold text-text-heading mb-6">New Coupon</h1>
      <div className="bg-bg-card border border-border-default rounded-lg p-6">
        <p className="text-text-meta mb-4">Coupon form placeholder</p>
        <Button variant="ghost" onClick={() => navigate('/coupons')}>
          Back
        </Button>
      </div>
    </div>
  )
}

export default CouponFormPage
