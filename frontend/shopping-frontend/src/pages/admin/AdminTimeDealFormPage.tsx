/**
 * AdminTimeDealFormPage
 * 관리자 타임딜 생성 페이지
 */
import { useState, useEffect } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useCreateTimeDeal } from '@/hooks/useAdminTimeDeals'
import { adminProductApi } from '@/api'
import type { Product } from '@/types'
import { Button, Card, Input, Select, Spinner, useApiError, useToast } from '@portal/design-react'
import type { SelectOption } from '@portal/design-core'

function formatPrice(price: number): string {
  return new Intl.NumberFormat('ko-KR').format(price)
}

interface FormData {
  name: string
  description: string
  productId: number
  dealPrice: number
  dealQuantity: number
  maxPerUser: number
  startsAt: string
  endsAt: string
}

export function AdminTimeDealFormPage() {
  const navigate = useNavigate()
  const { handleError } = useApiError()
  const { success } = useToast()
  const { mutateAsync: createTimeDeal, isPending } = useCreateTimeDeal()

  const [products, setProducts] = useState<Product[]>([])
  const [isLoadingProducts, setIsLoadingProducts] = useState(true)
  const [selectedProduct, setSelectedProduct] = useState<Product | null>(null)

  const [formData, setFormData] = useState<FormData>({
    name: '',
    description: '',
    productId: 0,
    dealPrice: 0,
    dealQuantity: 100,
    maxPerUser: 1,
    startsAt: '',
    endsAt: ''
  })

  const [errors, setErrors] = useState<Record<string, string>>({})

  // Load products
  useEffect(() => {
    const loadProducts = async () => {
      try {
        setIsLoadingProducts(true)
        const response = await adminProductApi.getProducts({ page: 1, size: 100 })
        if (response.success) {
          setProducts(response.data?.items ?? [])
        }
      } catch (err) {
        console.error('Failed to load products:', err)
      } finally {
        setIsLoadingProducts(false)
      }
    }
    loadProducts()
  }, [])

  const handleProductChange = (productId: number) => {
    const product = products.find((p) => p.id === productId)
    setSelectedProduct(product || null)
    setFormData((prev) => ({
      ...prev,
      productId,
      dealPrice: product ? Math.floor(product.price * 0.8) : 0,
      name: product ? `${product.name} 타임딜` : prev.name
    }))
    setErrors((prev) => ({ ...prev, productId: '' }))
  }

  const handleChange = (field: keyof FormData, value: number | string) => {
    setFormData((prev) => ({ ...prev, [field]: value }))
    setErrors((prev) => ({ ...prev, [field]: '' }))
  }

  const discountRate = selectedProduct
    ? Math.round(((selectedProduct.price - formData.dealPrice) / selectedProduct.price) * 100)
    : 0

  const productOptions: SelectOption[] = [
    { value: 0, label: '상품을 선택하세요' },
    ...products.map((product) => ({
      value: product.id,
      label: `${product.name} - ${formatPrice(product.price)}원`,
    })),
  ]

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {}

    if (!formData.name.trim()) {
      newErrors.name = '타임딜 이름을 입력해주세요'
    }
    if (!formData.productId) {
      newErrors.productId = '상품을 선택해주세요'
    }
    if (formData.dealPrice <= 0) {
      newErrors.dealPrice = '딜 가격을 입력해주세요'
    }
    if (selectedProduct && formData.dealPrice >= selectedProduct.price) {
      newErrors.dealPrice = '딜 가격은 정가보다 낮아야 합니다'
    }
    if (formData.dealQuantity <= 0) {
      newErrors.dealQuantity = '재고 수량을 입력해주세요'
    }
    if (formData.maxPerUser <= 0) {
      newErrors.maxPerUser = '구매 제한을 입력해주세요'
    }
    if (formData.maxPerUser > formData.dealQuantity) {
      newErrors.maxPerUser = '구매 제한은 재고보다 작아야 합니다'
    }
    if (!formData.startsAt) {
      newErrors.startsAt = '시작 시간을 선택해주세요'
    }
    if (!formData.endsAt) {
      newErrors.endsAt = '종료 시간을 선택해주세요'
    }
    if (formData.startsAt && formData.endsAt && formData.startsAt >= formData.endsAt) {
      newErrors.endsAt = '종료 시간은 시작 시간 이후여야 합니다'
    }

    setErrors(newErrors)
    return Object.keys(newErrors).length === 0
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()

    if (!validate()) return

    try {
      await createTimeDeal({
        name: formData.name,
        description: formData.description || undefined,
        startsAt: new Date(formData.startsAt).toISOString(),
        endsAt: new Date(formData.endsAt).toISOString(),
        products: [{
          productId: formData.productId,
          dealPrice: formData.dealPrice,
          dealQuantity: formData.dealQuantity,
          maxPerUser: formData.maxPerUser
        }]
      })
      success('타임딜이 생성되었습니다!')
      navigate('/admin/time-deals')
    } catch (err) {
      handleError(err, '타임딜 생성에 실패했습니다.')
    }
  }

  return (
    <div className="p-6 max-w-2xl">
      {/* 헤더 */}
      <div className="mb-6">
        <Link
          to="/admin/time-deals"
          className="inline-flex items-center text-text-meta hover:text-text-heading mb-4"
        >
          <svg className="w-5 h-5 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
          </svg>
          타임딜 목록
        </Link>
        <h1 className="text-2xl font-bold text-text-heading">새 타임딜 생성</h1>
      </div>

      {/* 폼 */}
      <form onSubmit={handleSubmit} className="space-y-6">
        {/* 타임딜 기본 정보 */}
        <Card variant="elevated" padding="lg">
          <h2 className="text-lg font-medium text-text-heading mb-4">기본 정보</h2>

          <div className="space-y-4">
            <Input
              label="타임딜 이름"
              required
              value={formData.name}
              onChange={(e: React.ChangeEvent<HTMLInputElement>) => handleChange('name', e.target.value)}
              placeholder="예: 겨울 특가 타임딜"
              error={!!errors.name}
              errorMessage={errors.name}
            />

            <Input
              label="설명"
              value={formData.description}
              onChange={(e: React.ChangeEvent<HTMLInputElement>) => handleChange('description', e.target.value)}
              placeholder="타임딜 설명 (선택)"
            />
          </div>
        </Card>

        {/* 상품 선택 */}
        <Card variant="elevated" padding="lg">
          <h2 className="text-lg font-medium text-text-heading mb-4">상품 선택</h2>

          {isLoadingProducts ? (
            <div className="flex justify-center py-4">
              <Spinner size="md" />
            </div>
          ) : (
            <Select
              label="타임딜 상품"
              required
              value={formData.productId}
              options={productOptions}
              onChange={(value: string | number | null) => handleProductChange(Number(value))}
              error={!!errors.productId}
              errorMessage={errors.productId}
            />
          )}

          {/* 선택된 상품 정보 */}
          {selectedProduct && (
            <div className="mt-4 p-4 bg-bg-muted rounded-lg">
              <div className="flex items-center gap-4">
                <div className="w-20 h-20 bg-bg-muted rounded flex items-center justify-center">
                  <svg className="w-8 h-8 text-text-muted" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                      d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                  </svg>
                </div>
                <div>
                  <h3 className="font-medium text-text-heading">{selectedProduct.name}</h3>
                  <p className="text-text-meta">정가: {formatPrice(selectedProduct.price)}원</p>
                  <p className="text-text-muted text-sm">재고: {selectedProduct.stockQuantity ?? '-'}개</p>
                </div>
              </div>
            </div>
          )}
        </Card>

        {/* 가격 설정 */}
        <Card variant="elevated" padding="lg">
          <h2 className="text-lg font-medium text-text-heading mb-4">가격 설정</h2>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Input
              label="딜 가격 (원)"
              required
              type="number"
              value={formData.dealPrice || ''}
              onChange={(e: React.ChangeEvent<HTMLInputElement>) => handleChange('dealPrice', parseInt(e.target.value) || 0)}
              placeholder="할인된 가격"
              min={0}
              error={!!errors.dealPrice}
              errorMessage={errors.dealPrice}
            />

            <div>
              <label className="block mb-1.5 text-sm font-medium text-text-body">
                할인율
              </label>
              <div className="h-9 px-3 py-2 border border-border-default rounded-md bg-bg-muted flex items-center">
                <span className={`text-lg font-bold ${discountRate > 0 ? 'text-status-error' : 'text-text-muted'}`}>
                  {discountRate > 0 ? `${discountRate}% OFF` : '-'}
                </span>
              </div>
            </div>
          </div>

          {selectedProduct && formData.dealPrice > 0 && (
            <div className="mt-4 p-4 bg-status-error/10 border border-status-error rounded-lg">
              <div className="flex items-baseline gap-2">
                <span className="text-text-muted line-through">{formatPrice(selectedProduct.price)}원</span>
                <span className="text-2xl font-bold text-status-error">{formatPrice(formData.dealPrice)}원</span>
                <span className="text-sm text-text-meta">
                  ({formatPrice(selectedProduct.price - formData.dealPrice)}원 할인)
                </span>
              </div>
            </div>
          )}
        </Card>

        {/* 수량 설정 */}
        <Card variant="elevated" padding="lg">
          <h2 className="text-lg font-medium text-text-heading mb-4">수량 설정</h2>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Input
              label="총 재고"
              required
              type="number"
              value={formData.dealQuantity || ''}
              onChange={(e: React.ChangeEvent<HTMLInputElement>) => handleChange('dealQuantity', parseInt(e.target.value) || 0)}
              placeholder="100"
              min={1}
              error={!!errors.dealQuantity}
              errorMessage={errors.dealQuantity}
            />

            <Input
              label="1인당 구매 제한"
              required
              type="number"
              value={formData.maxPerUser || ''}
              onChange={(e: React.ChangeEvent<HTMLInputElement>) => handleChange('maxPerUser', parseInt(e.target.value) || 0)}
              placeholder="1"
              min={1}
              error={!!errors.maxPerUser}
              errorMessage={errors.maxPerUser}
            />
          </div>
        </Card>

        {/* 기간 설정 */}
        <Card variant="elevated" padding="lg">
          <h2 className="text-lg font-medium text-text-heading mb-4">기간 설정</h2>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Input
              label="시작 시간"
              required
              type="datetime-local"
              value={formData.startsAt}
              onChange={(e: React.ChangeEvent<HTMLInputElement>) => handleChange('startsAt', e.target.value)}
              error={!!errors.startsAt}
              errorMessage={errors.startsAt}
            />

            <Input
              label="종료 시간"
              required
              type="datetime-local"
              value={formData.endsAt}
              onChange={(e: React.ChangeEvent<HTMLInputElement>) => handleChange('endsAt', e.target.value)}
              error={!!errors.endsAt}
              errorMessage={errors.endsAt}
            />
          </div>
        </Card>

        {/* 버튼 */}
        <div className="flex justify-end gap-3">
          <Link
            to="/admin/time-deals"
            className="inline-flex items-center justify-center h-9 px-4 text-sm font-medium rounded-md bg-transparent text-text-body border border-border-default hover:bg-bg-hover transition-colors"
          >
            취소
          </Link>
          <Button type="submit" variant="danger" loading={isPending}>
            {isPending ? '생성 중...' : '타임딜 생성'}
          </Button>
        </div>
      </form>
    </div>
  )
}

export default AdminTimeDealFormPage
