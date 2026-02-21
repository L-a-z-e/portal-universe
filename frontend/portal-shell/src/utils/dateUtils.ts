// portal-shell/src/utils/dateUtils.ts

/**
 * Backend LocalDateTime 배열 또는 ISO 문자열을 Date로 변환
 * Backend가 LocalDateTime을 [year, month, day, hour, min, sec, nano] 배열로 직렬화하는 경우 처리
 */
export function parseDate(value: string | number[] | null | undefined): Date | null {
  if (!value) return null

  if (Array.isArray(value) && value.length >= 3) {
    const [year, month, day] = value as [number, number, number, ...number[]]
    return new Date(year, month - 1, day, value[3] ?? 0, value[4] ?? 0, value[5] ?? 0)
  }

  if (typeof value === 'string') {
    const date = new Date(value)
    return isNaN(date.getTime()) ? null : date
  }

  return null
}

/**
 * 상대적 시간 표시 (예: "2시간 전", "3일 전")
 * ISO 문자열 또는 LocalDateTime 배열 모두 지원
 */
export function formatRelativeTime(dateValue: string | number[]): string {
  const date = parseDate(dateValue)
  if (!date) return ''

  const now = new Date()
  const diffMs = now.getTime() - date.getTime()

  const seconds = Math.floor(diffMs / 1000)
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)
  const days = Math.floor(hours / 24)

  if (days > 7) {
    return date.toLocaleDateString('ko-KR', { month: 'short', day: 'numeric' })
  }
  if (days > 0) return `${days}일 전`
  if (hours > 0) return `${hours}시간 전`
  if (minutes > 0) return `${minutes}분 전`
  return '방금 전'
}

/**
 * ISO 날짜 문자열을 한국어 포맷으로 변환
 */
export function formatDate(dateString: string): string {
  const date = new Date(dateString)
  return date.toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}

/**
 * ISO 날짜 문자열을 시간 포함 포맷으로 변환
 */
export function formatDateTime(dateString: string): string {
  const date = new Date(dateString)
  return date.toLocaleString('ko-KR', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}
