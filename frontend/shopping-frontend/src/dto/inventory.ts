export interface Inventory {
  id: number
  productId: number
  availableQuantity: number
  reservedQuantity: number
  totalQuantity: number
  createdAt: string
  updatedAt?: string
}

export interface InventoryUpdateRequest {
  quantity: number
  reason?: string
}

export type MovementType =
  | 'INITIAL'
  | 'RESERVE'
  | 'DEDUCT'
  | 'RELEASE'
  | 'INBOUND'
  | 'RETURN'
  | 'ADJUSTMENT'

export interface StockMovement {
  id: number
  inventoryId: number
  productId: number
  movementType: MovementType
  quantity: number
  previousAvailable: number
  afterAvailable: number
  previousReserved: number
  afterReserved: number
  referenceType?: string
  referenceId?: string
  reason?: string
  performedBy?: string
  createdAt: string
}

export interface InventoryUpdate {
  productId: number
  available: number
  reserved: number
  timestamp: string
}
