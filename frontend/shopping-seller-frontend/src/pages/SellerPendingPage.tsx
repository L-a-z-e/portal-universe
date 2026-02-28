import React, { useState, useEffect } from 'react'
import { sellerApi } from '@/api'

const SellerPendingPage: React.FC = () => {
  const [application, setApplication] = useState<any>(null)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    const load = async () => {
      try {
        const res = await sellerApi.getMyApplication()
        setApplication(res.data?.data || null)
      } catch {
        setApplication(null)
      } finally {
        setIsLoading(false)
      }
    }
    load()
  }, [])

  if (isLoading) {
    return (
      <div className="max-w-2xl mx-auto py-8 px-4 text-center">
        <p className="text-text-meta">로딩 중...</p>
      </div>
    )
  }

  const status = application?.status
  const isRejected = status === 'REJECTED'

  return (
    <div className="max-w-2xl mx-auto py-8 px-4">
      <div className="text-center">
        <div className="text-4xl mb-4">{isRejected ? '😔' : '⏳'}</div>
        <h1 className="text-2xl font-bold mb-2">
          {isRejected ? '신청이 거절되었습니다' : '승인 대기 중'}
        </h1>
        <p className="text-text-meta mb-6">
          {isRejected
            ? '관리자가 신청을 거절했습니다. 아래 사유를 확인해 주세요.'
            : '관리자가 신청을 검토 중입니다. 승인되면 판매자 기능을 사용할 수 있습니다.'}
        </p>
      </div>

      {application && (
        <div className="bg-surface-secondary rounded-lg p-6 space-y-3">
          <div className="flex justify-between">
            <span className="text-text-meta">사업자명</span>
            <span className="font-medium">{application.businessName}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-text-meta">상태</span>
            <span className={`font-medium ${isRejected ? 'text-red-600' : 'text-yellow-600'}`}>
              {status === 'PENDING' ? '대기 중' : status === 'REJECTED' ? '거절됨' : status}
            </span>
          </div>
          <div className="flex justify-between">
            <span className="text-text-meta">신청일</span>
            <span>{application.createdAt ? new Date(application.createdAt).toLocaleDateString('ko-KR') : '-'}</span>
          </div>
          {application.reviewComment && (
            <div className="pt-2 border-t">
              <span className="text-text-meta block mb-1">검토 의견</span>
              <p className="text-sm">{application.reviewComment}</p>
            </div>
          )}
        </div>
      )}
    </div>
  )
}

export default SellerPendingPage
