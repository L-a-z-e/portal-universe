export type PaymentStatus =
  | 'PENDING'
  | 'PROCESSING'
  | 'COMPLETED'
  | 'FAILED'
  | 'CANCELLED'
  | 'REFUNDED'

export type PaymentMethod =
  | 'CARD'
  | 'BANK_TRANSFER'
  | 'VIRTUAL_ACCOUNT'
  | 'MOBILE'
  | 'POINTS'

export interface Payment {
  id: number
  orderNumber: string
  amount: number
  method: PaymentMethod
  status: PaymentStatus
  transactionId?: string
  pgResponse?: string
  paidAt?: string
  failedAt?: string
  failureReason?: string
  createdAt: string
  updatedAt?: string
}

export interface ProcessPaymentRequest {
  orderNumber: string
  paymentMethod: PaymentMethod
  cardNumber?: string
  cardExpiry?: string
  cardCvv?: string
}

export const PAYMENT_STATUS_LABELS: Record<PaymentStatus, string> = {
  PENDING: '결제 대기',
  PROCESSING: '결제 처리 중',
  COMPLETED: '결제 완료',
  FAILED: '결제 실패',
  CANCELLED: '결제 취소',
  REFUNDED: '환불 완료'
}

export const PAYMENT_METHOD_LABELS: Record<PaymentMethod, string> = {
  CARD: '카드',
  BANK_TRANSFER: '계좌이체',
  VIRTUAL_ACCOUNT: '가상계좌',
  MOBILE: '휴대폰',
  POINTS: '포인트'
}
