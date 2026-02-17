/**
 * QueueWaitingPage
 * 대기열 대기 페이지
 */
import { useEffect } from 'react'
import { useParams, useNavigate, useSearchParams, Link } from 'react-router-dom'
import { Spinner, Button } from '@portal/design-react'
import { useQueue } from '@/hooks/useQueue'
import { QueueStatus } from '@/components/queue/QueueStatus'
import { QUEUE_STATUS_LABELS } from '@/types'

export function QueueWaitingPage() {
  const { eventType, eventId } = useParams<{ eventType: string; eventId: string }>()
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()

  const returnUrl = searchParams.get('returnUrl') || '/'

  const {
    status,
    isLoading,
    error,
    isConnected,
    enterQueue,
    leaveQueue,
    entryToken
  } = useQueue({
    eventType: eventType || 'TIMEDEAL',
    eventId: parseInt(eventId || '0'),
    autoEnter: true
  })

  // 입장 완료 시 returnUrl로 이동
  useEffect(() => {
    if (status?.status === 'ENTERED') {
      navigate(returnUrl, { replace: true })
    }
  }, [status?.status, navigate, returnUrl])

  // 로딩 중
  if (isLoading && !status) {
    return (
      <div className="min-h-screen bg-bg-muted flex items-center justify-center">
        <div className="text-center">
          <Spinner size="xl" className="mx-auto mb-4" />
          <p className="text-text-body">대기열에 진입 중...</p>
        </div>
      </div>
    )
  }

  // 에러
  if (error) {
    return (
      <div className="min-h-screen bg-bg-muted flex items-center justify-center p-4">
        <div className="light:bg-white bg-bg-card rounded-lg border border-status-error p-6 max-w-md w-full text-center">
          <div className="text-status-error text-5xl mb-4">!</div>
          <h2 className="text-xl font-bold text-text-heading mb-2">대기열 진입 실패</h2>
          <p className="text-text-body mb-6">{error.message}</p>
          <div className="space-y-3">
            <Button
              onClick={() => enterQueue()}
              variant="primary"
              className="w-full"
            >
              다시 시도
            </Button>
            <Link
              to={returnUrl}
              className="block w-full py-3 border border-border-default text-text-body rounded-lg hover:bg-bg-muted transition-colors"
            >
              돌아가기
            </Link>
          </div>
        </div>
      </div>
    )
  }

  // 만료됨
  if (status?.status === 'EXPIRED') {
    return (
      <div className="min-h-screen bg-bg-muted flex items-center justify-center p-4">
        <div className="light:bg-white bg-bg-card rounded-lg border border-status-warning p-6 max-w-md w-full text-center">
          <div className="text-status-warning text-5xl mb-4">
            <svg className="w-16 h-16 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <h2 className="text-xl font-bold text-text-heading mb-2">대기열이 만료되었습니다</h2>
          <p className="text-text-body mb-6">대기 시간이 초과되었습니다. 다시 시도해주세요.</p>
          <div className="space-y-3">
            <Button
              onClick={() => enterQueue()}
              variant="primary"
              className="w-full"
            >
              다시 대기하기
            </Button>
            <Link
              to={returnUrl}
              className="block w-full py-3 border border-border-default text-text-body rounded-lg hover:bg-bg-muted transition-colors"
            >
              돌아가기
            </Link>
          </div>
        </div>
      </div>
    )
  }

  // 이탈함
  if (status?.status === 'LEFT') {
    return (
      <div className="min-h-screen bg-bg-muted flex items-center justify-center p-4">
        <div className="light:bg-white bg-bg-card rounded-lg border border-border-default p-6 max-w-md w-full text-center">
          <h2 className="text-xl font-bold text-text-heading mb-2">대기열에서 나왔습니다</h2>
          <p className="text-text-body mb-6">다시 대기하려면 아래 버튼을 클릭하세요.</p>
          <div className="space-y-3">
            <Button
              onClick={() => enterQueue()}
              variant="primary"
              className="w-full"
            >
              다시 대기하기
            </Button>
            <Link
              to={returnUrl}
              className="block w-full py-3 border border-border-default text-text-body rounded-lg hover:bg-bg-muted transition-colors"
            >
              돌아가기
            </Link>
          </div>
        </div>
      </div>
    )
  }

  // 대기 중
  return (
    <div className="min-h-screen bg-bg-muted flex items-center justify-center p-4">
      <div className="max-w-md w-full">
        {/* 헤더 */}
        <div className="text-center mb-6">
          <div className="w-16 h-16 bg-bg-muted rounded-full flex items-center justify-center mx-auto mb-4">
            <svg className="w-8 h-8 text-brand-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
            </svg>
          </div>
          <h1 className="text-2xl font-bold text-text-heading">대기열 안내</h1>
          <p className="text-text-body mt-2">
            현재 많은 고객님이 접속 중입니다.<br />
            잠시만 기다려 주세요.
          </p>
        </div>

        {/* 상태 표시 */}
        {status && (
          <QueueStatus
            status={status}
            onLeave={leaveQueue}
            isLeaving={isLoading}
          />
        )}

        {/* 연결 상태 표시 */}
        {!isConnected && status?.status === 'WAITING' && (
          <div className="mt-4 text-center text-sm text-status-warning">
            <p>실시간 연결이 끊어졌습니다. 재연결 중...</p>
          </div>
        )}
      </div>
    </div>
  )
}

export default QueueWaitingPage
