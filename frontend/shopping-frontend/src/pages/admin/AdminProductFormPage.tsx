/**
 * Admin Product Form Page
 * 상품 등록/수정 페이지
 */
import React, { useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useAdminProduct, useCreateProduct, useUpdateProduct } from '@/hooks/useAdminProducts'
import { Button } from '@/components/common/Button'
import { Input } from '@/components/form/Input'
import { TextArea } from '@/components/form/TextArea'
import type { ProductFormData } from '@/types/admin'

// Zod 스키마 정의
const productFormSchema = z.object({
  name: z
    .string()
    .min(1, 'Product name is required')
    .max(200, 'Product name must be less than 200 characters'),
  description: z
    .string()
    .min(1, 'Description is required')
    .max(2000, 'Description must be less than 2000 characters'),
  price: z
    .number({ message: 'Price must be a number' })
    .min(0, 'Price must be greater than or equal to 0'),
  stock: z
    .number({ message: 'Stock must be a number' })
    .int('Stock must be an integer')
    .min(0, 'Stock must be greater than or equal to 0'),
  imageUrl: z.string().optional(),
  category: z.string().optional(),
})

export const AdminProductFormPage: React.FC = () => {
  const navigate = useNavigate()
  const { id } = useParams<{ id: string }>()
  const isEdit = !!id && id !== 'new'
  const productId = isEdit ? parseInt(id, 10) : 0

  // React Hook Form
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

  // 데이터 조회 (수정 모드)
  const { data: productData } = useAdminProduct(productId)

  // Mutations
  const createMutation = useCreateProduct()
  const updateMutation = useUpdateProduct()

  // 수정 모드일 때 폼 데이터 초기화
  useEffect(() => {
    if (isEdit && productData?.data) {
      const product = productData.data
      reset({
        name: product.name,
        description: product.description,
        price: product.price,
        stock: (product as any).stock || 0,
        imageUrl: product.imageUrl || '',
        category: product.category || '',
      })
    }
  }, [isEdit, productData, reset])

  // 폼 제출
  const onSubmit = async (data: ProductFormData) => {
    try {
      if (isEdit) {
        await updateMutation.mutateAsync({ id: productId, data })
      } else {
        await createMutation.mutateAsync(data)
      }
      navigate('/admin/products')
    } catch (error) {
      console.error('Failed to save product:', error)
    }
  }

  return (
    <div>
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-text-heading">
          {isEdit ? 'Edit Product' : 'New Product'}
        </h1>
        <Button
          variant="ghost"
          onClick={() => navigate('/admin/products')}
        >
          Back
        </Button>
      </div>

      {/* Form */}
      <div className="bg-bg-card border border-border-default rounded-lg p-6 shadow-sm">
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
          {/* Name */}
          <Input
            label="Product Name"
            required
            error={errors.name?.message}
            {...register('name')}
            placeholder="Enter product name"
          />

          {/* Description */}
          <TextArea
            label="Description"
            required
            rows={5}
            error={errors.description?.message}
            {...register('description')}
            placeholder="Enter product description"
          />

          {/* Price & Stock */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Input
              label="Price"
              type="number"
              required
              step="0.01"
              error={errors.price?.message}
              {...register('price', { valueAsNumber: true })}
              placeholder="0.00"
            />

            <Input
              label="Stock"
              type="number"
              required
              error={errors.stock?.message}
              {...register('stock', { valueAsNumber: true })}
              placeholder="0"
            />
          </div>

          {/* Image URL */}
          <Input
            label="Image URL"
            error={errors.imageUrl?.message}
            {...register('imageUrl')}
            placeholder="https://example.com/image.jpg"
            helpText="Optional: URL of the product image"
          />

          {/* Category */}
          <Input
            label="Category"
            error={errors.category?.message}
            {...register('category')}
            placeholder="Electronics, Clothing, etc."
            helpText="Optional: Product category"
          />

          {/* Actions */}
          <div className="flex items-center justify-end gap-3 pt-4 border-t border-border-default">
            <Button
              type="button"
              variant="ghost"
              onClick={() => navigate('/admin/products')}
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

export default AdminProductFormPage
