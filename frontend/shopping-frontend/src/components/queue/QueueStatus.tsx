/**
 * QueueStatus Component
 * 대기열 상태 표시 컴포넌트
 */
import { formatWaitTime } from '@/hooks/useQueue'
import type { QueueStatusResponse } from '@/types'

interface QueueStatusProps {
  status: QueueStatusResponse
  onLeave?: () => void
  isLeaving?: boolean
}

export function QueueStatus({ status, onLeave, isLeaving }: QueueStatusProps) {
  const { position, estimatedWaitSeconds, totalWaiting, message } = status

  const progressPercent = totalWaiting > 0
    ? Math.max(0, Math.min(100, ((totalWaiting - position + 1) / totalWaiting) * 100))
    : 0

  return (
    <div className="bg-white rounded-lg border border-gray-200 p-6 shadow-sm">
      {/* 대기 순번 */}
      <div className="text-center mb-6">
        <div className="text-6xl font-bold text-indigo-600 mb-2">
          {position.toLocaleString()}
        </div>
        <div className="text-gray-500">번째 대기 중</div>
      </div>

      {/* 진행 바 */}
      <div className="mb-6">
        <div className="h-3 bg-gray-200 rounded-full overflow-hidden">
          <div
            className="h-full bg-gradient-to-r from-indigo-500 to-indigo-600 transition-all duration-500"
            style={{ width: `${progressPercent}%` }}
          />
        </div>
        <div className="flex justify-between text-sm text-gray-500 mt-2">
          <span>내 순번</span>
          <span>{totalWaiting.toLocaleString()}명 대기 중</span>
        </div>
      </div>

      {/* 예상 대기 시간 */}
      <div className="bg-gray-50 rounded-lg p-4 mb-6">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <svg className="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <span className="text-gray-600">예상 대기 시간</span>
          </div>
          <span className="text-lg font-semibold text-gray-900">
            {formatWaitTime(estimatedWaitSeconds)}
          </span>
        </div>
      </div>

      {/* 안내 메시지 */}
      <div className="text-center text-gray-600 mb-6">
        <p>{message}</p>
      </div>

      {/* 실시간 연결 표시 */}
      <div className="flex items-center justify-center gap-2 text-sm text-gray-500 mb-6">
        <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse"></div>
        <span>실시간 업데이트 중</span>
      </div>

      {/* 이탈 버튼 */}
      {onLeave && (
        <button
          onClick={onLeave}
          disabled={isLeaving}
          className="w-full py-3 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors disabled:opacity-50"
        >
          {isLeaving ? '처리 중...' : '대기열에서 나가기'}
        </button>
      )}

      {/* 안내사항 */}
      <div className="mt-6 text-xs text-gray-400 space-y-1">
        <p>* 페이지를 닫거나 새로고침하면 대기 순번이 유지되지 않을 수 있습니다.</p>
        <p>* 입장 순서가 되면 자동으로 구매 페이지로 이동합니다.</p>
      </div>
    </div>
  )
}

export default QueueStatus
