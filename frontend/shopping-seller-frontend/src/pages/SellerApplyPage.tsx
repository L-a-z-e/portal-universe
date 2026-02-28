import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, Input, Textarea } from '@portal/design-react'
import { sellerApi } from '@/api'

const SellerApplyPage: React.FC = () => {
  const navigate = useNavigate()
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [form, setForm] = useState({
    businessName: '',
    businessNumber: '',
    representativeName: '',
    phone: '',
    email: '',
    bankName: '',
    bankAccount: '',
    reason: '',
  })

  const handleChange = (field: string, value: string) => {
    setForm(prev => ({ ...prev, [field]: value }))
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setIsSubmitting(true)
    setError(null)

    try {
      await sellerApi.apply(form)
      navigate('/pending')
    } catch (err: any) {
      const msg = err?.response?.data?.message || '신청에 실패했습니다.'
      setError(msg)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="max-w-2xl mx-auto py-8 px-4">
      <h1 className="text-2xl font-bold mb-2">판매자 신청</h1>
      <p className="text-text-meta mb-6">
        판매자로 등록하려면 아래 사업자 정보를 입력해 주세요.
        승인 후 상품 등록 및 판매가 가능합니다.
      </p>

      {error && (
        <div className="mb-4 p-3 bg-red-50 text-red-700 rounded-md text-sm">
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="block text-sm font-medium mb-1">사업자명 *</label>
          <Input
            value={form.businessName}
            onChange={(e) => handleChange('businessName', e.target.value)}
            placeholder="사업자명을 입력하세요"
            required
          />
        </div>

        <div>
          <label className="block text-sm font-medium mb-1">사업자 등록번호</label>
          <Input
            value={form.businessNumber}
            onChange={(e) => handleChange('businessNumber', e.target.value)}
            placeholder="000-00-00000"
          />
        </div>

        <div>
          <label className="block text-sm font-medium mb-1">대표자명</label>
          <Input
            value={form.representativeName}
            onChange={(e) => handleChange('representativeName', e.target.value)}
            placeholder="대표자 이름"
          />
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium mb-1">연락처</label>
            <Input
              value={form.phone}
              onChange={(e) => handleChange('phone', e.target.value)}
              placeholder="010-0000-0000"
            />
          </div>
          <div>
            <label className="block text-sm font-medium mb-1">이메일</label>
            <Input
              type="email"
              value={form.email}
              onChange={(e) => handleChange('email', e.target.value)}
              placeholder="seller@example.com"
            />
          </div>
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium mb-1">은행명</label>
            <Input
              value={form.bankName}
              onChange={(e) => handleChange('bankName', e.target.value)}
              placeholder="은행명"
            />
          </div>
          <div>
            <label className="block text-sm font-medium mb-1">계좌번호</label>
            <Input
              value={form.bankAccount}
              onChange={(e) => handleChange('bankAccount', e.target.value)}
              placeholder="계좌번호"
            />
          </div>
        </div>

        <div>
          <label className="block text-sm font-medium mb-1">신청 사유</label>
          <Textarea
            value={form.reason}
            onChange={(e) => handleChange('reason', e.target.value)}
            placeholder="판매자로 등록하려는 이유를 간단히 작성해 주세요"
            rows={3}
          />
        </div>

        <div className="pt-4">
          <Button type="submit" disabled={isSubmitting || !form.businessName}>
            {isSubmitting ? '신청 중...' : '판매자 신청'}
          </Button>
        </div>
      </form>
    </div>
  )
}

export default SellerApplyPage
