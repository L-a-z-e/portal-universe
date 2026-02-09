export type CartStatus = 'ACTIVE' | 'CHECKED_OUT'

export interface CartItem {
  id: number
  productId: number
  productName: string
  price: number
  quantity: number
  addedAt: string
}

export interface Cart {
  id: number
  userId: string
  status: CartStatus
  items: CartItem[]
  totalAmount: number
  itemCount: number
  totalQuantity: number
  createdAt: string
  updatedAt?: string
}

export interface AddCartItemRequest {
  productId: number
  quantity: number
}

export interface UpdateCartItemRequest {
  quantity: number
}
