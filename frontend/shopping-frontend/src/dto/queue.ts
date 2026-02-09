export type QueueStatus = 'WAITING' | 'ENTERED' | 'EXPIRED' | 'LEFT'

export interface QueueStatusResponse {
  entryToken: string
  status: QueueStatus
  position: number
  estimatedWaitSeconds: number
  totalWaiting: number
  message: string
}

export interface QueueActivateRequest {
  maxCapacity: number
  entryBatchSize: number
  entryIntervalSeconds: number
}

export const QUEUE_STATUS_LABELS: Record<QueueStatus, string> = {
  WAITING: '대기 중',
  ENTERED: '입장 완료',
  EXPIRED: '만료됨',
  LEFT: '이탈'
}
