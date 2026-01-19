/**
 * CountdownTimer Component
 * 타임딜 카운트다운 타이머
 */
import { useTimeDealCountdown } from '@/hooks/useTimeDeals'

interface CountdownTimerProps {
  endsAt: string
  size?: 'sm' | 'md' | 'lg'
  showLabels?: boolean
}

export function CountdownTimer({ endsAt, size = 'md', showLabels = true }: CountdownTimerProps) {
  const { hours, minutes, seconds, isExpired } = useTimeDealCountdown(endsAt)

  if (isExpired) {
    return (
      <span className="text-red-600 font-medium">
        종료됨
      </span>
    )
  }

  const sizeClasses = {
    sm: 'text-sm',
    md: 'text-base',
    lg: 'text-xl'
  }

  const boxSizeClasses = {
    sm: 'w-8 h-8 text-xs',
    md: 'w-10 h-10 text-sm',
    lg: 'w-14 h-14 text-lg'
  }

  const TimeBox = ({ value, label }: { value: number; label: string }) => (
    <div className="flex flex-col items-center">
      <div className={`
        ${boxSizeClasses[size]}
        bg-red-600 text-white rounded-lg flex items-center justify-center font-bold
      `}>
        {String(value).padStart(2, '0')}
      </div>
      {showLabels && (
        <span className="text-xs text-gray-500 mt-1">{label}</span>
      )}
    </div>
  )

  return (
    <div className={`flex items-center gap-1 ${sizeClasses[size]}`}>
      <TimeBox value={hours} label="시간" />
      <span className="text-gray-400 font-bold">:</span>
      <TimeBox value={minutes} label="분" />
      <span className="text-gray-400 font-bold">:</span>
      <TimeBox value={seconds} label="초" />
    </div>
  )
}

export default CountdownTimer
