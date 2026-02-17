import { useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { sellerProductApi } from '@/api'
import { Button, Input, Textarea } from '@portal/design-react'
import type { ProductFormData } from '@/types'

const productFormSchema = z.object({
  name: z.string().min(1, 'Product name is required').max(200),
  description: z.string().min(1, 'Description is required').max(2000),
  price: z.number().min(0, 'Price must be >= 0'),
  stock: z.number().int().min(0, 'Stock must be >= 0'),
  imageUrl: z.string().optional(),
  category: z.string().optional(),
})

export const ProductFormPage = () => {
  const navigate = useNavigate()
  const { id } = useParams<{ id: string }>()
  const isEdit = !!id && id !== 'new'
  const productId = isEdit ? parseInt(id, 10) : 0

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<ProductFormData>({
    resolver: zodResolver(productFormSchema),
    defaultValues: {
      name: '',
      description: '',
      price: 0,
      stock: 0,
      imageUrl: '',
      category: '',
    },
  })

  useEffect(() => {
    if (isEdit) {
      sellerProductApi.getProduct(productId).then(res => {
        const product = res.data?.data
        if (product) {
          reset({
            name: product.name,
            description: product.description,
            price: product.price,
            stock: (product as any).stock || 0,
            imageUrl: product.imageUrl || '',
            category: product.category || '',
          })
        }
      }).catch(() => {})
    }
  }, [isEdit, productId, reset])

  const onSubmit = async (data: ProductFormData) => {
    try {
      if (isEdit) {
        await sellerProductApi.updateProduct(productId, data)
      } else {
        await sellerProductApi.createProduct(data)
      }
      navigate('/products')
    } catch (error) {
      console.error('Failed to save product:', error)
    }
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-text-heading">
          {isEdit ? 'Edit Product' : 'New Product'}
        </h1>
        <Button variant="ghost" onClick={() => navigate('/products')}>
          Back
        </Button>
      </div>

      <div className="bg-bg-card border border-border-default rounded-lg p-6 shadow-sm">
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
          <Input
            label="Product Name"
            required
            error={!!errors.name}
            errorMessage={errors.name?.message}
            {...register('name')}
          />

          <Textarea
            label="Description"
            required
            rows={5}
            error={!!errors.description}
            errorMessage={errors.description?.message}
            {...register('description')}
          />

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Input
              label="Price"
              type="number"
              required
              error={!!errors.price}
              errorMessage={errors.price?.message}
              {...register('price', { valueAsNumber: true })}
            />

            <Input
              label="Stock"
              type="number"
              required
              error={!!errors.stock}
              errorMessage={errors.stock?.message}
              {...register('stock', { valueAsNumber: true })}
            />
          </div>

          <Input
            label="Image URL"
            error={!!errors.imageUrl}
            errorMessage={errors.imageUrl?.message}
            {...register('imageUrl')}
          />

          <Input
            label="Category"
            error={!!errors.category}
            errorMessage={errors.category?.message}
            {...register('category')}
          />

          <div className="flex items-center justify-end gap-3 pt-4 border-t border-border-default">
            <Button
              type="button"
              variant="ghost"
              onClick={() => navigate('/products')}
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
              {isEdit ? 'Update Product' : 'Create Product'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  )
}

export default ProductFormPage
