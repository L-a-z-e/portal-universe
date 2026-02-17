import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useForm, useFieldArray } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { sellerTimeDealApi, sellerProductApi } from '@/api'
import { Button, Input, Textarea } from '@portal/design-react'

const timeDealFormSchema = z.object({
  name: z.string().min(1, 'Name is required').max(100),
  description: z.string().optional(),
  startsAt: z.string().min(1, 'Start date is required'),
  endsAt: z.string().min(1, 'End date is required'),
  products: z.array(z.object({
    productId: z.number().min(1, 'Product is required'),
    dealPrice: z.number().min(0.01, 'Deal price must be > 0'),
    dealQuantity: z.number().int().min(1, 'Quantity must be >= 1'),
    maxPerUser: z.number().int().min(1, 'Max per user must be >= 1'),
  })).min(1, 'At least one product is required'),
})

type TimeDealFormData = z.infer<typeof timeDealFormSchema>

export const TimeDealFormPage: React.FC = () => {
  const navigate = useNavigate()
  const [availableProducts, setAvailableProducts] = useState<any[]>([])

  const {
    register,
    control,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<TimeDealFormData>({
    resolver: zodResolver(timeDealFormSchema),
    defaultValues: {
      name: '',
      description: '',
      startsAt: '',
      endsAt: '',
      products: [{ productId: 0, dealPrice: 0, dealQuantity: 1, maxPerUser: 1 }],
    },
  })

  const { fields, append, remove } = useFieldArray({ control, name: 'products' })

  useEffect(() => {
    sellerProductApi.getProducts({ size: 100 }).then(res => {
      const apiData = res.data?.data
      const items = apiData?.content ?? apiData?.items ?? (Array.isArray(apiData) ? apiData : [])
      setAvailableProducts(items)
    }).catch(() => {})
  }, [])

  const onSubmit = async (data: TimeDealFormData) => {
    try {
      await sellerTimeDealApi.createTimeDeal({
        ...data,
        startsAt: data.startsAt + ':00',
        endsAt: data.endsAt + ':00',
      })
      navigate('/time-deals')
    } catch (error) {
      console.error('Failed to create time deal:', error)
    }
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-text-heading">New Time Deal</h1>
        <Button variant="ghost" onClick={() => navigate('/time-deals')}>
          Back
        </Button>
      </div>

      <div className="bg-bg-card border border-border-default rounded-lg p-6 shadow-sm">
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
          <Input
            label="Deal Name"
            required
            error={!!errors.name}
            errorMessage={errors.name?.message}
            {...register('name')}
          />

          <Textarea
            label="Description"
            rows={3}
            error={!!errors.description}
            errorMessage={errors.description?.message}
            {...register('description')}
          />

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
              label="End Date"
              type="datetime-local"
              required
              error={!!errors.endsAt}
              errorMessage={errors.endsAt?.message}
              {...register('endsAt')}
            />
          </div>

          <div>
            <div className="flex items-center justify-between mb-3">
              <label className="text-sm font-medium text-text-heading">
                Products <span className="text-status-error">*</span>
              </label>
              <Button
                type="button"
                variant="ghost"
                onClick={() => append({ productId: 0, dealPrice: 0, dealQuantity: 1, maxPerUser: 1 })}
              >
                + Add Product
              </Button>
            </div>
            {errors.products?.message && (
              <p className="text-sm text-status-error mb-2">{errors.products.message}</p>
            )}
            <div className="space-y-3">
              {fields.map((field, index) => (
                <div key={field.id} className="p-4 border border-border-default rounded-lg">
                  <div className="grid grid-cols-1 md:grid-cols-4 gap-3">
                    <div>
                      <label className="block text-xs text-text-meta mb-1">Product</label>
                      <select
                        className="w-full px-3 py-2 border border-border-default rounded-md bg-bg-default text-text-body text-sm"
                        {...register(`products.${index}.productId`, { valueAsNumber: true })}
                      >
                        <option value={0}>Select product</option>
                        {availableProducts.map((p: any) => (
                          <option key={p.id} value={p.id}>{p.name}</option>
                        ))}
                      </select>
                    </div>
                    <Input
                      label="Deal Price"
                      type="number"
                      error={!!errors.products?.[index]?.dealPrice}
                      errorMessage={errors.products?.[index]?.dealPrice?.message}
                      {...register(`products.${index}.dealPrice`, { valueAsNumber: true })}
                    />
                    <Input
                      label="Quantity"
                      type="number"
                      error={!!errors.products?.[index]?.dealQuantity}
                      errorMessage={errors.products?.[index]?.dealQuantity?.message}
                      {...register(`products.${index}.dealQuantity`, { valueAsNumber: true })}
                    />
                    <div className="flex items-end gap-2">
                      <div className="flex-1">
                        <Input
                          label="Max/User"
                          type="number"
                          error={!!errors.products?.[index]?.maxPerUser}
                          errorMessage={errors.products?.[index]?.maxPerUser?.message}
                          {...register(`products.${index}.maxPerUser`, { valueAsNumber: true })}
                        />
                      </div>
                      {fields.length > 1 && (
                        <button
                          type="button"
                          onClick={() => remove(index)}
                          className="mb-1 p-2 text-status-error hover:bg-status-error-bg rounded transition-colors"
                        >
                          Remove
                        </button>
                      )}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className="flex items-center justify-end gap-3 pt-4 border-t border-border-default">
            <Button
              type="button"
              variant="ghost"
              onClick={() => navigate('/time-deals')}
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
              Create Time Deal
            </Button>
          </div>
        </form>
      </div>
    </div>
  )
}

export default TimeDealFormPage
