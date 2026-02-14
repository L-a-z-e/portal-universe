import React from 'react'
import { useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { sellerCouponApi } from '@/api'
import { Button, Input, Textarea } from '@portal/design-system-react'

const couponFormSchema = z.object({
  code: z.string().min(1, 'Coupon code is required').max(50),
  name: z.string().min(1, 'Coupon name is required').max(100),
  description: z.string().optional(),
  discountType: z.enum(['FIXED', 'PERCENTAGE']),
  discountValue: z.number().min(0.01, 'Discount value must be > 0'),
  minimumOrderAmount: z.number().min(0).optional().nullable(),
  maximumDiscountAmount: z.number().min(0).optional().nullable(),
  totalQuantity: z.number().int().min(1, 'Total quantity must be >= 1'),
  startsAt: z.string().min(1, 'Start date is required'),
  expiresAt: z.string().min(1, 'Expiry date is required'),
})

type CouponFormData = z.infer<typeof couponFormSchema>

export const CouponFormPage: React.FC = () => {
  const navigate = useNavigate()

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<CouponFormData>({
    resolver: zodResolver(couponFormSchema),
    defaultValues: {
      code: '',
      name: '',
      description: '',
      discountType: 'FIXED',
      discountValue: 0,
      minimumOrderAmount: null,
      maximumDiscountAmount: null,
      totalQuantity: 100,
      startsAt: '',
      expiresAt: '',
    },
  })

  const onSubmit = async (data: CouponFormData) => {
    try {
      await sellerCouponApi.createCoupon({
        ...data,
        startsAt: data.startsAt + ':00',
        expiresAt: data.expiresAt + ':00',
        minimumOrderAmount: data.minimumOrderAmount || null,
        maximumDiscountAmount: data.maximumDiscountAmount || null,
      })
      navigate('/coupons')
    } catch (error) {
      console.error('Failed to create coupon:', error)
    }
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-text-heading">New Coupon</h1>
        <Button variant="ghost" onClick={() => navigate('/coupons')}>
          Back
        </Button>
      </div>

      <div className="bg-bg-card border border-border-default rounded-lg p-6 shadow-sm">
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Input
              label="Coupon Code"
              required
              error={!!errors.code}
              errorMessage={errors.code?.message}
              {...register('code')}
            />
            <Input
              label="Coupon Name"
              required
              error={!!errors.name}
              errorMessage={errors.name?.message}
              {...register('name')}
            />
          </div>

          <Textarea
            label="Description"
            rows={3}
            error={!!errors.description}
            errorMessage={errors.description?.message}
            {...register('description')}
          />

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-text-heading mb-1">
                Discount Type <span className="text-status-error">*</span>
              </label>
              <select
                className="w-full px-3 py-2 border border-border-default rounded-md bg-bg-default text-text-body"
                {...register('discountType')}
              >
                <option value="FIXED">Fixed Amount</option>
                <option value="PERCENTAGE">Percentage</option>
              </select>
            </div>
            <Input
              label="Discount Value"
              type="number"
              required
              error={!!errors.discountValue}
              errorMessage={errors.discountValue?.message}
              {...register('discountValue', { valueAsNumber: true })}
            />
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <Input
              label="Min Order Amount"
              type="number"
              error={!!errors.minimumOrderAmount}
              errorMessage={errors.minimumOrderAmount?.message}
              {...register('minimumOrderAmount', { valueAsNumber: true })}
            />
            <Input
              label="Max Discount Amount"
              type="number"
              error={!!errors.maximumDiscountAmount}
              errorMessage={errors.maximumDiscountAmount?.message}
              {...register('maximumDiscountAmount', { valueAsNumber: true })}
            />
            <Input
              label="Total Quantity"
              type="number"
              required
              error={!!errors.totalQuantity}
              errorMessage={errors.totalQuantity?.message}
              {...register('totalQuantity', { valueAsNumber: true })}
            />
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Input
              label="Start Date"
              type="datetime-local"
              required
              error={!!errors.startsAt}
              errorMessage={errors.startsAt?.message}
              {...register('startsAt')}
            />
            <Input
              label="Expiry Date"
              type="datetime-local"
              required
              error={!!errors.expiresAt}
              errorMessage={errors.expiresAt?.message}
              {...register('expiresAt')}
            />
          </div>

          <div className="flex items-center justify-end gap-3 pt-4 border-t border-border-default">
            <Button
              type="button"
              variant="ghost"
              onClick={() => navigate('/coupons')}
              disabled={isSubmitting}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              variant="primary"
              loading={isSubmitting}
              disabled={isSubmitting}
            >
              Create Coupon
            </Button>
          </div>
        </form>
      </div>
    </div>
  )
}

export default CouponFormPage
