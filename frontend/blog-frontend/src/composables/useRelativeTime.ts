import { computed, unref, type MaybeRef } from 'vue'

/**
 * 상대 시간 계산 composable
 *
 * 중복 제거: PostCard.vue, CommentItem.vue에서 동일한 로직 사용
 *
 * @example
 * // Reactive (computed)
 * const { relativeTime } = useRelativeTime(post.publishedAt)
 *
 * // Non-reactive (직접 호출)
 * const timeStr = formatRelativeTime('2025-01-20T10:00:00')
 */

interface RelativeTimeOptions {
  /** 7일 이후 표시할 날짜 포맷 (default: 'ko-KR') */
  locale?: string
  /** 방금 전 임계값 (분) */
  justNowThreshold?: number
}

const DEFAULT_OPTIONS: RelativeTimeOptions = {
  locale: 'ko-KR',
  justNowThreshold: 1
}

/**
 * 날짜 문자열/Date를 상대 시간 문자열로 변환
 * - 1분 미만: "방금 전"
 * - 1시간 미만: "N분 전"
 * - 24시간 미만: "N시간 전"
 * - 7일 미만: "N일 전"
 * - 7일 이상: "YYYY년 M월 D일"
 */
export function formatRelativeTime(
  dateInput: string | Date | null | undefined,
  options: RelativeTimeOptions = {}
): string {
  if (!dateInput) return ''

  const { locale, justNowThreshold } = { ...DEFAULT_OPTIONS, ...options }

  const date = typeof dateInput === 'string' ? new Date(dateInput) : dateInput
  const now = new Date()
  const diff = now.getTime() - date.getTime()

  // 음수면 미래 시간 (잘못된 데이터)
  if (diff < 0) return formatFullDate(date, locale!)

  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < justNowThreshold!) return '방금 전'
  if (minutes < 60) return `${minutes}분 전`
  if (hours < 24) return `${hours}시간 전`
  if (days < 7) return `${days}일 전`

  return formatFullDate(date, locale!)
}

/**
 * 전체 날짜 포맷 (7일 이상일 때)
 */
function formatFullDate(date: Date, locale: string): string {
  return date.toLocaleDateString(locale, {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}

/**
 * Vue Composable: 반응형 상대 시간
 *
 * @param dateInput - 날짜 문자열 또는 Date (ref 가능)
 * @param options - 옵션
 * @returns { relativeTime, fullDate }
 */
export function useRelativeTime(
  dateInput: MaybeRef<string | Date | null | undefined>,
  options: RelativeTimeOptions = {}
) {
  const { locale } = { ...DEFAULT_OPTIONS, ...options }

  const relativeTime = computed(() => {
    const value = unref(dateInput)
    return formatRelativeTime(value, options)
  })

  const fullDate = computed(() => {
    const value = unref(dateInput)
    if (!value) return ''
    const date = typeof value === 'string' ? new Date(value) : value
    return formatFullDate(date, locale!)
  })

  return {
    relativeTime,
    fullDate
  }
}

export default useRelativeTime
