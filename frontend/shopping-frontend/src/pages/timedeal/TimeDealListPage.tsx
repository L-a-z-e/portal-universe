/**
 * TimeDealListPage
 * 타임딜 목록 페이지
 */
import { useActiveTimeDeals } from '@/hooks/useTimeDeals'
import { TimeDealCard } from '@/components/timedeal/TimeDealCard'

export function TimeDealListPage() {
  const { data: timeDeals, isLoading, error, refetch } = useActiveTimeDeals()

  return (
    <div className="container mx-auto px-4 py-8">
      {/* 페이지 헤더 */}
      <div className="mb-8">
        <div className="flex items-center gap-3 mb-2">
          <span className="text-3xl">⚡</span>
          <h1 className="text-2xl font-bold text-gray-900">타임딜</h1>
        </div>
        <p className="text-gray-600">한정 시간 특가! 지금 바로 참여하세요</p>
      </div>

      {/* 로딩 상태 */}
      {isLoading && (
        <div className="flex justify-center items-center py-12">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-red-600"></div>
          <span className="ml-3 text-gray-600">로딩 중...</span>
        </div>
      )}

      {/* 에러 상태 */}
      {error && (
        <div className="text-center py-12">
          <div className="text-red-500 text-5xl mb-4">!</div>
          <p className="text-gray-600 mb-4">타임딜을 불러오는데 실패했습니다</p>
          <button
            onClick={refetch}
            className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700"
          >
            다시 시도
          </button>
        </div>
      )}

      {/* 타임딜 목록 */}
      {!isLoading && !error && (
        <>
          {timeDeals.length === 0 ? (
            <div className="text-center py-12">
              <div className="text-gray-400 text-5xl mb-4">⏰</div>
              <p className="text-gray-600">현재 진행 중인 타임딜이 없습니다</p>
              <p className="text-gray-500 text-sm mt-2">새로운 타임딜을 기대해 주세요!</p>
            </div>
          ) : (
            <>
              {/* 진행 중인 타임딜 */}
              <div className="mb-12">
                <h2 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
                  <span className="w-2 h-2 bg-red-500 rounded-full animate-pulse"></span>
                  진행 중
                </h2>
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
                  {timeDeals
                    .filter((deal) => deal.status === 'ACTIVE')
                    .map((timeDeal) => (
                      <TimeDealCard key={timeDeal.id} timeDeal={timeDeal} />
                    ))}
                </div>
                {timeDeals.filter((deal) => deal.status === 'ACTIVE').length === 0 && (
                  <p className="text-gray-500 text-center py-8">
                    현재 진행 중인 타임딜이 없습니다
                  </p>
                )}
              </div>

              {/* 예정된 타임딜 */}
              {timeDeals.filter((deal) => deal.status === 'SCHEDULED').length > 0 && (
                <div>
                  <h2 className="text-lg font-semibold text-gray-500 mb-4">
                    곧 시작
                  </h2>
                  <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
                    {timeDeals
                      .filter((deal) => deal.status === 'SCHEDULED')
                      .map((timeDeal) => (
                        <TimeDealCard key={timeDeal.id} timeDeal={timeDeal} />
                      ))}
                  </div>
                </div>
              )}
            </>
          )}
        </>
      )}
    </div>
  )
}

export default TimeDealListPage
