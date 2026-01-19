/**
 * AdminCouponFormPage
 * 관리자 쿠폰 생성 페이지
 */
import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useCreateCoupon } from '@/hooks/useAdminCoupons'
import type { DiscountType, CouponCreateRequest } from '@/types'
import { DISCOUNT_TYPE_LABELS } from '@/types'

export function AdminCouponFormPage() {
  const navigate = useNavigate()
  const { mutateAsync: createCoupon, isPending } = useCreateCoupon()

  const [formData, setFormData] = useState<CouponCreateRequest>({
    code: '',
    name: '',
    description: '',
    discountType: 'FIXED',
    discountValue: 0,
    minimumOrderAmount: undefined,
    maximumDiscountAmount: undefined,
    totalQuantity: 100,
    startsAt: '',
    expiresAt: ''
  })

  const [errors, setErrors] = useState<Record<string, string>>({})

  const handleChange = (field: keyof CouponCreateRequest, value: string | number | undefined) => {
    setFormData((prev) => ({ ...prev, [field]: value }))
    setErrors((prev) => ({ ...prev, [field]: '' }))
  }

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {}

    if (!formData.code.trim()) {
      newErrors.code = '쿠폰 코드를 입력해주세요'
    }
    if (!formData.name.trim()) {
      newErrors.name = '쿠폰 이름을 입력해주세요'
    }
    if (formData.discountValue <= 0) {
      newErrors.discountValue = '할인 금액/비율을 입력해주세요'
    }
    if (formData.discountType === 'PERCENTAGE' && formData.discountValue > 100) {
      newErrors.discountValue = '할인율은 100%를 초과할 수 없습니다'
    }
    if (formData.totalQuantity <= 0) {
      newErrors.totalQuantity = '발급 수량을 입력해주세요'
    }
    if (!formData.startsAt) {
      newErrors.startsAt = '시작일을 선택해주세요'
    }
    if (!formData.expiresAt) {
      newErrors.expiresAt = '종료일을 선택해주세요'
    }
    if (formData.startsAt && formData.expiresAt && formData.startsAt >= formData.expiresAt) {
      newErrors.expiresAt = '종료일은 시작일 이후여야 합니다'
    }

    setErrors(newErrors)
    return Object.keys(newErrors).length === 0
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()

    if (!validate()) return

    try {
      await createCoupon({
        ...formData,
        startsAt: new Date(formData.startsAt).toISOString(),
        expiresAt: new Date(formData.expiresAt).toISOString()
      })
      alert('쿠폰이 생성되었습니다!')
      navigate('/admin/coupons')
    } catch (err) {
      const message = err instanceof Error ? err.message : '쿠폰 생성에 실패했습니다'
      alert(message)
    }
  }

  return (
    <div className="p-6 max-w-2xl">
      {/* 헤더 */}
      <div className="mb-6">
        <Link
          to="/admin/coupons"
          className="inline-flex items-center text-gray-600 hover:text-gray-900 mb-4"
        >
          <svg className="w-5 h-5 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
          </svg>
          쿠폰 목록
        </Link>
        <h1 className="text-2xl font-bold text-gray-900">새 쿠폰 생성</h1>
      </div>

      {/* 폼 */}
      <form onSubmit={handleSubmit} className="space-y-6">
        {/* 기본 정보 */}
        <div className="bg-white border border-gray-200 rounded-lg p-6">
          <h2 className="text-lg font-medium text-gray-900 mb-4">기본 정보</h2>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                쿠폰 코드 *
              </label>
              <input
                type="text"
                value={formData.code}
                onChange={(e) => handleChange('code', e.target.value.toUpperCase())}
                placeholder="예: WELCOME2024"
                className={`w-full px-3 py-2 border rounded-lg ${
                  errors.code ? 'border-red-500' : 'border-gray-300'
                }`}
              />
              {errors.code && (
                <p className="text-red-500 text-sm mt-1">{errors.code}</p>
              )}
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                쿠폰 이름 *
              </label>
              <input
                type="text"
                value={formData.name}
                onChange={(e) => handleChange('name', e.target.value)}
                placeholder="예: 신규가입 환영 쿠폰"
                className={`w-full px-3 py-2 border rounded-lg ${
                  errors.name ? 'border-red-500' : 'border-gray-300'
                }`}
              />
              {errors.name && (
                <p className="text-red-500 text-sm mt-1">{errors.name}</p>
              )}
            </div>

            <div className="md:col-span-2">
              <label className="block text-sm font-medium text-gray-700 mb-1">
                설명
              </label>
              <textarea
                value={formData.description || ''}
                onChange={(e) => handleChange('description', e.target.value)}
                placeholder="쿠폰에 대한 설명을 입력하세요"
                rows={3}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg"
              />
            </div>
          </div>
        </div>

        {/* 할인 설정 */}
        <div className="bg-white border border-gray-200 rounded-lg p-6">
          <h2 className="text-lg font-medium text-gray-900 mb-4">할인 설정</h2>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                할인 유형 *
              </label>
              <select
                value={formData.discountType}
                onChange={(e) => handleChange('discountType', e.target.value as DiscountType)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg"
              >
                {(Object.entries(DISCOUNT_TYPE_LABELS) as [DiscountType, string][]).map(
                  ([value, label]) => (
                    <option key={value} value={value}>
                      {label}
                    </option>
                  )
                )}
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                {formData.discountType === 'FIXED' ? '할인 금액 (원) *' : '할인율 (%) *'}
              </label>
              <input
                type="number"
                value={formData.discountValue || ''}
                onChange={(e) => handleChange('discountValue', parseInt(e.target.value) || 0)}
                placeholder={formData.discountType === 'FIXED' ? '1000' : '10'}
                min={0}
                max={formData.discountType === 'PERCENTAGE' ? 100 : undefined}
                className={`w-full px-3 py-2 border rounded-lg ${
                  errors.discountValue ? 'border-red-500' : 'border-gray-300'
                }`}
              />
              {errors.discountValue && (
                <p className="text-red-500 text-sm mt-1">{errors.discountValue}</p>
              )}
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                최소 주문 금액 (원)
              </label>
              <input
                type="number"
                value={formData.minimumOrderAmount || ''}
                onChange={(e) =>
                  handleChange('minimumOrderAmount', e.target.value ? parseInt(e.target.value) : undefined)
                }
                placeholder="10000"
                min={0}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg"
              />
            </div>

            {formData.discountType === 'PERCENTAGE' && (
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  최대 할인 금액 (원)
                </label>
                <input
                  type="number"
                  value={formData.maximumDiscountAmount || ''}
                  onChange={(e) =>
                    handleChange('maximumDiscountAmount', e.target.value ? parseInt(e.target.value) : undefined)
                  }
                  placeholder="5000"
                  min={0}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg"
                />
              </div>
            )}
          </div>
        </div>

        {/* 발급 설정 */}
        <div className="bg-white border border-gray-200 rounded-lg p-6">
          <h2 className="text-lg font-medium text-gray-900 mb-4">발급 설정</h2>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                총 발급 수량 *
              </label>
              <input
                type="number"
                value={formData.totalQuantity || ''}
                onChange={(e) => handleChange('totalQuantity', parseInt(e.target.value) || 0)}
                placeholder="100"
                min={1}
                className={`w-full px-3 py-2 border rounded-lg ${
                  errors.totalQuantity ? 'border-red-500' : 'border-gray-300'
                }`}
              />
              {errors.totalQuantity && (
                <p className="text-red-500 text-sm mt-1">{errors.totalQuantity}</p>
              )}
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                발급 시작일 *
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
                발급 종료일 *
              </label>
              <input
                type="datetime-local"
                value={formData.expiresAt}
                onChange={(e) => handleChange('expiresAt', e.target.value)}
                className={`w-full px-3 py-2 border rounded-lg ${
                  errors.expiresAt ? 'border-red-500' : 'border-gray-300'
                }`}
              />
              {errors.expiresAt && (
                <p className="text-red-500 text-sm mt-1">{errors.expiresAt}</p>
              )}
            </div>
          </div>
        </div>

        {/* 버튼 */}
        <div className="flex justify-end gap-3">
          <Link
            to="/admin/coupons"
            className="px-4 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50"
          >
            취소
          </Link>
          <button
            type="submit"
            disabled={isPending}
            className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50"
          >
            {isPending ? '생성 중...' : '쿠폰 생성'}
          </button>
        </div>
      </form>
    </div>
  )
}

export default AdminCouponFormPage
