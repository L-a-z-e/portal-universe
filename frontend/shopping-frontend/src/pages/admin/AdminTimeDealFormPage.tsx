/**
 * AdminTimeDealFormPage
 * 관리자 타임딜 생성 페이지
 */
import { useState, useEffect } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useCreateTimeDeal } from '@/hooks/useAdminTimeDeals'
import { adminProductApi } from '@/api/endpoints'
import type { TimeDealCreateRequest, Product } from '@/types'

function formatPrice(price: number): string {
  return new Intl.NumberFormat('ko-KR').format(price)
}

export function AdminTimeDealFormPage() {
  const navigate = useNavigate()
  const { mutateAsync: createTimeDeal, isPending } = useCreateTimeDeal()

  const [products, setProducts] = useState<Product[]>([])
  const [isLoadingProducts, setIsLoadingProducts] = useState(true)
  const [selectedProduct, setSelectedProduct] = useState<Product | null>(null)

  const [formData, setFormData] = useState<TimeDealCreateRequest>({
    productId: 0,
    dealPrice: 0,
    totalStock: 100,
    purchaseLimit: 1,
    startsAt: '',
    endsAt: ''
  })

  const [errors, setErrors] = useState<Record<string, string>>({})

  // Load products
  useEffect(() => {
    const loadProducts = async () => {
      try {
        setIsLoadingProducts(true)
        const response = await adminProductApi.getProducts({ page: 0, size: 100 })
        if (response.success) {
          setProducts(response.data.content)
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
      dealPrice: product ? Math.floor(product.price * 0.8) : 0 // Default 20% discount
    }))
    setErrors((prev) => ({ ...prev, productId: '' }))
  }

  const handleChange = (field: keyof TimeDealCreateRequest, value: number | string) => {
    setFormData((prev) => ({ ...prev, [field]: value }))
    setErrors((prev) => ({ ...prev, [field]: '' }))
  }

  const discountRate = selectedProduct
    ? Math.round(((selectedProduct.price - formData.dealPrice) / selectedProduct.price) * 100)
    : 0

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {}

    if (!formData.productId) {
      newErrors.productId = '상품을 선택해주세요'
    }
    if (formData.dealPrice <= 0) {
      newErrors.dealPrice = '딜 가격을 입력해주세요'
    }
    if (selectedProduct && formData.dealPrice >= selectedProduct.price) {
      newErrors.dealPrice = '딜 가격은 정가보다 낮아야 합니다'
    }
    if (formData.totalStock <= 0) {
      newErrors.totalStock = '재고 수량을 입력해주세요'
    }
    if (formData.purchaseLimit <= 0) {
      newErrors.purchaseLimit = '구매 제한을 입력해주세요'
    }
    if (formData.purchaseLimit > formData.totalStock) {
      newErrors.purchaseLimit = '구매 제한은 재고보다 작아야 합니다'
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
        ...formData,
        startsAt: new Date(formData.startsAt).toISOString(),
        endsAt: new Date(formData.endsAt).toISOString()
      })
      alert('타임딜이 생성되었습니다!')
      navigate('/admin/time-deals')
    } catch (err) {
      const message = err instanceof Error ? err.message : '타임딜 생성에 실패했습니다'
      alert(message)
    }
  }

  return (
    <div className="p-6 max-w-2xl">
      {/* 헤더 */}
      <div className="mb-6">
        <Link
          to="/admin/time-deals"
          className="inline-flex items-center text-gray-600 hover:text-gray-900 mb-4"
        >
          <svg className="w-5 h-5 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
          </svg>
          타임딜 목록
        </Link>
        <h1 className="text-2xl font-bold text-gray-900">새 타임딜 생성</h1>
      </div>

      {/* 폼 */}
      <form onSubmit={handleSubmit} className="space-y-6">
        {/* 상품 선택 */}
        <div className="bg-white border border-gray-200 rounded-lg p-6">
          <h2 className="text-lg font-medium text-gray-900 mb-4">상품 선택</h2>

          {isLoadingProducts ? (
            <div className="flex justify-center py-4">
              <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-red-600"></div>
            </div>
          ) : (
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                타임딜 상품 *
              </label>
              <select
                value={formData.productId}
                onChange={(e) => handleProductChange(parseInt(e.target.value))}
                className={`w-full px-3 py-2 border rounded-lg ${
                  errors.productId ? 'border-red-500' : 'border-gray-300'
                }`}
              >
                <option value={0}>상품을 선택하세요</option>
                {products.map((product) => (
                  <option key={product.id} value={product.id}>
                    {product.name} - {formatPrice(product.price)}원
                  </option>
                ))}
              </select>
              {errors.productId && (
                <p className="text-red-500 text-sm mt-1">{errors.productId}</p>
              )}
            </div>
          )}

          {/* 선택된 상품 정보 */}
          {selectedProduct && (
            <div className="mt-4 p-4 bg-gray-50 rounded-lg">
              <div className="flex items-center gap-4">
                {selectedProduct.imageUrl ? (
                  <img
                    src={selectedProduct.imageUrl}
                    alt={selectedProduct.name}
                    className="w-20 h-20 object-cover rounded"
                  />
                ) : (
                  <div className="w-20 h-20 bg-gray-200 rounded flex items-center justify-center">
                    <svg className="w-8 h-8 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                        d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                    </svg>
                  </div>
                )}
                <div>
                  <h3 className="font-medium text-gray-900">{selectedProduct.name}</h3>
                  <p className="text-gray-600">정가: {formatPrice(selectedProduct.price)}원</p>
                  <p className="text-gray-500 text-sm">재고: {selectedProduct.stockQuantity}개</p>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* 가격 설정 */}
        <div className="bg-white border border-gray-200 rounded-lg p-6">
          <h2 className="text-lg font-medium text-gray-900 mb-4">가격 설정</h2>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                딜 가격 (원) *
              </label>
              <input
                type="number"
                value={formData.dealPrice || ''}
                onChange={(e) => handleChange('dealPrice', parseInt(e.target.value) || 0)}
                placeholder="할인된 가격"
                min={0}
                className={`w-full px-3 py-2 border rounded-lg ${
                  errors.dealPrice ? 'border-red-500' : 'border-gray-300'
                }`}
              />
              {errors.dealPrice && (
                <p className="text-red-500 text-sm mt-1">{errors.dealPrice}</p>
              )}
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                할인율
              </label>
              <div className="px-3 py-2 border border-gray-200 rounded-lg bg-gray-50">
                <span className={`text-lg font-bold ${discountRate > 0 ? 'text-red-600' : 'text-gray-400'}`}>
                  {discountRate > 0 ? `${discountRate}% OFF` : '-'}
                </span>
              </div>
            </div>
          </div>

          {selectedProduct && formData.dealPrice > 0 && (
            <div className="mt-4 p-4 bg-red-50 border border-red-200 rounded-lg">
              <div className="flex items-baseline gap-2">
                <span className="text-gray-500 line-through">{formatPrice(selectedProduct.price)}원</span>
                <span className="text-2xl font-bold text-red-600">{formatPrice(formData.dealPrice)}원</span>
                <span className="text-sm text-gray-600">
                  ({formatPrice(selectedProduct.price - formData.dealPrice)}원 할인)
                </span>
              </div>
            </div>
          )}
        </div>

        {/* 수량 설정 */}
        <div className="bg-white border border-gray-200 rounded-lg p-6">
          <h2 className="text-lg font-medium text-gray-900 mb-4">수량 설정</h2>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                총 재고 *
              </label>
              <input
                type="number"
                value={formData.totalStock || ''}
                onChange={(e) => handleChange('totalStock', parseInt(e.target.value) || 0)}
                placeholder="100"
                min={1}
                className={`w-full px-3 py-2 border rounded-lg ${
                  errors.totalStock ? 'border-red-500' : 'border-gray-300'
                }`}
              />
              {errors.totalStock && (
                <p className="text-red-500 text-sm mt-1">{errors.totalStock}</p>
              )}
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                1인당 구매 제한 *
              </label>
              <input
                type="number"
                value={formData.purchaseLimit || ''}
                onChange={(e) => handleChange('purchaseLimit', parseInt(e.target.value) || 0)}
                placeholder="1"
                min={1}
                className={`w-full px-3 py-2 border rounded-lg ${
                  errors.purchaseLimit ? 'border-red-500' : 'border-gray-300'
                }`}
              />
              {errors.purchaseLimit && (
                <p className="text-red-500 text-sm mt-1">{errors.purchaseLimit}</p>
              )}
            </div>
          </div>
        </div>

        {/* 기간 설정 */}
        <div className="bg-white border border-gray-200 rounded-lg p-6">
          <h2 className="text-lg font-medium text-gray-900 mb-4">기간 설정</h2>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                시작 시간 *
              </label>
              <input
                type="datetime-local"
                value={formData.startsAt}
                onChange={(e) => handleChange('startsAt', e.target.value)}
                className={`w-full px-3 py-2 border rounded-lg ${
                  errors.startsAt ? 'border-red-500' : 'border-gray-300'
                }`}
              />
              {errors.startsAt && (
                <p className="text-red-500 text-sm mt-1">{errors.startsAt}</p>
              )}
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                종료 시간 *
              </label>
              <input
                type="datetime-local"
                value={formData.endsAt}
                onChange={(e) => handleChange('endsAt', e.target.value)}
                className={`w-full px-3 py-2 border rounded-lg ${
                  errors.endsAt ? 'border-red-500' : 'border-gray-300'
                }`}
              />
              {errors.endsAt && (
                <p className="text-red-500 text-sm mt-1">{errors.endsAt}</p>
              )}
            </div>
          </div>
        </div>

        {/* 버튼 */}
        <div className="flex justify-end gap-3">
          <Link
            to="/admin/time-deals"
            className="px-4 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50"
          >
            취소
          </Link>
          <button
            type="submit"
            disabled={isPending}
            className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50"
          >
            {isPending ? '생성 중...' : '타임딜 생성'}
          </button>
        </div>
      </form>
    </div>
  )
}

export default AdminTimeDealFormPage
